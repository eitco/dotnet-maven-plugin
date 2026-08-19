package de.eitco.cicd.dotnet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DotnetSdkProvisioner {

    @FunctionalInterface
    public interface Installer {
        void install(String version, File installDir, String rid, int timeoutSeconds) throws MojoExecutionException;
    }

    private static final Map<String, Object> JVM_LOCKS = new ConcurrentHashMap<>();
    private static final ExecutorService LOCK_EXECUTOR = Executors.newCachedThreadPool();

    private final File cacheRoot;
    private final int timeoutSeconds;
    private final Log log;
    private final Installer installer;

    public DotnetSdkProvisioner(File cacheRoot, int timeoutSeconds, Log log) {
        this(cacheRoot, timeoutSeconds, log, new ScriptBasedInstaller(log));
    }

    public DotnetSdkProvisioner(File cacheRoot, int timeoutSeconds, Log log, Installer installer) {
        this.cacheRoot = cacheRoot;
        this.timeoutSeconds = timeoutSeconds;
        this.log = log;
        this.installer = installer;
    }

    public File resolveExecutable(String version) throws MojoExecutionException {
        String rid = computeRid();
        File sdkDir = new File(new File(new File(cacheRoot, rid), "sdk"), version);
        File markerFile = new File(new File(cacheRoot, rid + "/markers"), version + ".provisioned");
        File lockFile = new File(new File(cacheRoot, rid + "/locks"), version + ".lock");
        File executable = new File(sdkDir, SystemUtils.IS_OS_WINDOWS ? "dotnet.exe" : "dotnet");

        if (isProvisioned(markerFile, executable)) {
            return executable;
        }

        try {
            FileUtils.forceMkdir(lockFile.getParentFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create lock directory", e);
        }

        Object jvmLock = JVM_LOCKS.computeIfAbsent(lockFile.getAbsolutePath(), k -> new Object());
        synchronized (jvmLock) {

            if (isProvisioned(markerFile, executable)) {
                return executable;
            }

            try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
                 FileChannel channel = raf.getChannel()) {

                FileLock lock = acquireWithTimeout(channel, timeoutSeconds);
                try {

                    if (isProvisioned(markerFile, executable)) {
                        return executable;
                    }

                    if (log != null) {
                        log.info("Provisioning .NET SDK " + version + " (" + rid + ") into " + sdkDir);
                    }

                    try {
                        FileUtils.deleteDirectory(sdkDir);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to clean stale SDK directory: " + sdkDir, e);
                    }

                    try {
                        FileUtils.forceMkdir(sdkDir);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to create SDK directory: " + sdkDir, e);
                    }

                    installer.install(version, sdkDir, rid, timeoutSeconds);

                    if (!executable.isFile()) {
                        throw new MojoExecutionException(
                                "dotnet-install reported success but no executable found at " + executable.getAbsolutePath());
                    }

                    writeMarkerAtomically(markerFile, version, rid);
                    return executable;

                } finally {
                    if (lock != null && lock.isValid()) {
                        lock.release();
                    }
                }

            } catch (IOException e) {
                throw new MojoExecutionException("Failed to provision .NET SDK " + version, e);
            }
        }
    }

    private static boolean isProvisioned(File marker, File executable) {
        return marker.isFile() && executable.isFile();
    }

    private static void writeMarkerAtomically(File marker, String version, String rid) throws IOException {
        FileUtils.forceMkdir(marker.getParentFile());
        File tmp = File.createTempFile("provisioned", ".tmp", marker.getParentFile());
        Files.writeString(tmp.toPath(),
                "version=" + version + "\n" +
                        "rid=" + rid + "\n" +
                        "installedAt=" + Instant.now());
        Files.move(tmp.toPath(), marker.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static FileLock acquireWithTimeout(FileChannel channel, int timeoutSeconds) throws MojoExecutionException {
        Future<FileLock> future = LOCK_EXECUTOR.submit(() -> {
            try {
                return channel.lock();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new MojoExecutionException(
                    "Timed out after " + timeoutSeconds + "s waiting for .NET SDK provisioning lock. " +
                            "Another build may be installing the same SDK version. " +
                            "If no other build is running, the lock file may be stale and can be manually removed.");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof java.nio.channels.AsynchronousCloseException) {
                throw new MojoExecutionException(
                        "Timed out waiting for .NET SDK provisioning lock. " +
                                "Another build may be installing the same SDK version.");
            }
            if (cause instanceof IOException) {
                throw new MojoExecutionException("Failed to acquire provisioning lock: " + cause.getMessage(), cause);
            }
            throw new MojoExecutionException("Failed to acquire provisioning lock: " + cause, cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while acquiring provisioning lock", e);
        }
    }

    protected String computeRid() {
        return cacheKey(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    static String cacheKey(String osName, String osArch) {
        String os = osName.toLowerCase(Locale.ROOT).contains("win") ? "win"
                : osName.toLowerCase(Locale.ROOT).contains("mac") ? "osx"
                : "linux";
        String arch = switch (osArch.toLowerCase(Locale.ROOT)) {
            case "amd64", "x86_64" -> "x64";
            case "aarch64", "arm64" -> "arm64";
            case "x86", "i386", "i686" -> "x86";
            default -> osArch.toLowerCase(Locale.ROOT);
        };
        return os + "-" + arch;
    }

    static class ScriptBasedInstaller implements Installer {

        private static final String SCRIPT_BASE_URL = "https://dot.net/v1/";

        private final Log log;

        ScriptBasedInstaller(Log log) {
            this.log = log;
        }

        @Override
        public void install(String version, File installDir, String rid, int timeoutSeconds)
                throws MojoExecutionException {
            boolean windows = SystemUtils.IS_OS_WINDOWS;
            String scriptName = windows ? "dotnet-install.ps1" : "dotnet-install.sh";
            File script = downloadScript(scriptName);
            try {
                java.util.List<String> command = windows
                        ? java.util.List.of("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                        script.getAbsolutePath(), "-Version", version, "-InstallDir",
                        installDir.getAbsolutePath(), "-NoPath")
                        : java.util.List.of("bash", script.getAbsolutePath(), "--version", version, "--install-dir",
                        installDir.getAbsolutePath(), "--no-path");
                runAndCheck(command, timeoutSeconds);
            } finally {
                FileUtils.deleteQuietly(script);
            }
        }

        private File downloadScript(String scriptName) throws MojoExecutionException {
            try {
                File tmpScript = File.createTempFile("dotnet-install", scriptName.substring(scriptName.lastIndexOf('.')));
                String url = SCRIPT_BASE_URL + scriptName;
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                        .build();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                        .GET()
                        .build();
                java.net.http.HttpResponse<String> response = client.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new MojoExecutionException("Failed to download " + url + ": HTTP " + response.statusCode());
                }
                Files.writeString(tmpScript.toPath(), response.body());
                tmpScript.setExecutable(true);
                return tmpScript;
            } catch (IOException | InterruptedException e) {
                throw new MojoExecutionException("Failed to download dotnet-install script", e);
            }
        }

        private void runAndCheck(java.util.List<String> command, int timeoutSeconds) throws MojoExecutionException {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.inheritIO();
                Process process = builder.start();
                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new MojoExecutionException("dotnet-install script timed out after " + timeoutSeconds + " seconds");
                }
                if (process.exitValue() != 0) {
                    throw new MojoExecutionException("dotnet-install script failed with exit code " + process.exitValue());
                }
            } catch (IOException | InterruptedException e) {
                throw new MojoExecutionException("Failed to run dotnet-install script", e);
            }
        }
    }
}
