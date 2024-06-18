package de.eitco.cicd.dotnet;

import com.google.common.base.Strings;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public record DotnetExecutor(
        File workingDirectory,
        File executable,
        File targetDirectory,
        String version,
        Map<String, String> customProperties,
        Map<String, String> environment,
        Log log,
        boolean ignoreResult
) {

    public int execute(String... parameters) throws MojoExecutionException {

        return execute(defaultOptions().mergeIgnoreResult(ignoreResult), List.of(parameters), Set.of(), Map.of());
    }

    private static class ExecutionOptions {
        private boolean ignoreResult = false;
        private boolean inheritIo = true;

        public ExecutionOptions ignoreResult() {
            ignoreResult = true;
            return this;
        }

        public ExecutionOptions silent() {
            inheritIo = false;
            return this;
        }

        public ExecutionOptions mergeIgnoreResult(boolean ignoreResult) {

            this.ignoreResult = ignoreResult || this.ignoreResult;

            return this;
        }
    }

    private static ExecutionOptions defaultOptions() {

        return new ExecutionOptions();
    }

    private int execute(ExecutionOptions executionOptions, List<String> parameters, Set<String> obfuscation, Map<String, String> propertyOverrides) throws MojoExecutionException {

        ProcessBuilder builder = new ProcessBuilder();

        builder.directory(workingDirectory);

        List<String> command = buildCommand(parameters, propertyOverrides);

        builder.command(command);

        if (executionOptions.inheritIo) {

            builder.inheritIO();
        }

        builder.environment().put("DOTNET_CLI_TELEMETRY_OPTOUT", "TRUE");

        environment.forEach((key, value) -> builder.environment().put(key, value));

        try {

            log.info("Executing command: " + presentCommand(command, obfuscation));

            Process process = builder.start();

            int returnCode = process.waitFor();

            if (returnCode != 0 && !executionOptions.ignoreResult) {

                throw new MojoExecutionException("process " + presentCommand(command, obfuscation) + " returned code " + returnCode);
            }

            return returnCode;

        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException(e);
        }
    }

    private static String presentCommand(List<String> command, Set<String> obfuscation) {

        return command.stream().map(x -> obfuscation.contains(x) ? "****" : x).collect(Collectors.joining(" "));
    }

    private List<String> buildCommand(List<String> parameters, Map<String, String> propertyOverrides) {

        List<String> command = new ArrayList<>();

        command.add(executable == null ? (SystemUtils.IS_OS_WINDOWS ? "dotnet.exe" : "dotnet") : executable.getPath());

        command.addAll(parameters);

        // propertyOverrides == null marks commands not supporting properties
        if (propertyOverrides != null) {

            Map<String, String> properties = new HashMap<>(customProperties);
            properties.putAll(propertyOverrides);

            properties.forEach((key, value) -> command.add("-p:" + key + "=" + value));
        }

        return command;
    }

    private void retry(int times, List<String> parameters, Set<String> obfuscation, Map<String, String> propertyOverrides) throws MojoExecutionException {

        for (int index = 0; index < times; index++) {

            int returnCode = execute(defaultOptions().ignoreResult(), parameters, obfuscation, propertyOverrides);

            if (returnCode == 0) {

                return;
            }
        }

        execute(defaultOptions(), parameters, obfuscation, propertyOverrides);
    }

    public void build(String assemblyVersion, String vendor, String configuration) throws MojoExecutionException {

        List<String> parameters = new ArrayList<>(List.of("build", "-p:Version=" + version));

        Map<String, String> propertyOverrides = new HashMap<>();

        if (assemblyVersion != null) {
            propertyOverrides.put("AssemblyVersion", assemblyVersion);
        }

        if (vendor != null) {
            propertyOverrides.put("Company", vendor);
        }

        if (configuration != null) {
            parameters.add("--configuration=" + configuration);
        }

        retry(1, parameters, Set.of(), propertyOverrides);
    }

    public void pack(String vendor, String description, String repositoryUrl) throws MojoExecutionException {

        List<String> parameters = new ArrayList<>(List.of(
                "pack",
                "--no-build"
        ));

        Map<String, String> propertyOverrides = new HashMap<>();

        propertyOverrides.put("Version", version);

        if (vendor != null) {
            propertyOverrides.put("Company", vendor);
        }

        if (description != null) {
            propertyOverrides.put("Description", description);
        }

        if (repositoryUrl != null) {
            propertyOverrides.put("RepositoryUrl", repositoryUrl);
        }

        parameters.add("--output");
        parameters.add(targetDirectory.getPath());

        execute(defaultOptions(), parameters, Set.of(), propertyOverrides);
    }

    public int test(String logger, String testResultDirectory) throws MojoExecutionException {

        return execute("test", "--no-build", "--logger", logger, "--results-directory", testResultDirectory);
    }

    public void push(String apiKey, String repository) throws MojoExecutionException {

        List<String> parameters = new ArrayList<>(List.of("nuget", "push", targetDirectory.getPath() + "/**.nupkg"));

        if (apiKey != null) {
            parameters.add("--api-key");
            parameters.add(apiKey);
        }

        if (repository != null) {
            parameters.add("--source");
            parameters.add(repository);
        }

        execute(defaultOptions().mergeIgnoreResult(ignoreResult), parameters, Optional.ofNullable(apiKey).stream().collect(Collectors.toSet()), null);
    }

    public enum NugetConfigLocation {

        PROJECT, USER, SYSTEM
    }

    public void upsertNugetSource(String url, String sourceName, String username, String apiToken, NugetConfigLocation configLocation) throws MojoExecutionException {

        Objects.requireNonNull(url);
        Objects.requireNonNull(sourceName);

        Set<String> obfuscation = apiToken != null ? Set.of(apiToken) : Set.of();

        List<String> updateParameters = getUpsertParameters(username, apiToken, configLocation, "nuget", "update", "source", sourceName, "--source", url);

        int result = execute(defaultOptions().ignoreResult(), updateParameters, obfuscation, null);

        if (result != 0) {

            List<String> addParameters = getUpsertParameters(username, apiToken, configLocation, "nuget", "add", "source", url, "--name", sourceName);
            execute(defaultOptions(), addParameters, obfuscation, null);
        }
    }

    private static List<String> getUpsertParameters(String userName, String apiToken, NugetConfigLocation configLocation, String... firstParameters) {

        List<String> parameters = new ArrayList<>(List.of(firstParameters));

        if (configLocation == NugetConfigLocation.PROJECT) {

            parameters.add("--configfile");
            parameters.add("NuGet.Config");
        }

        if (configLocation == NugetConfigLocation.USER) {

            parameters.add("--configfile");

            if (SystemUtils.IS_OS_WINDOWS) {

                parameters.add(System.getenv("appdata") + "\\NuGet\\NuGet.Config");

            } else {

                parameters.add("~/.nuget/NuGet/NuGet.Config");
            }
        }

        if (configLocation == NugetConfigLocation.SYSTEM) {

            parameters.add("--configfile");

            if (SystemUtils.IS_OS_WINDOWS) {

                parameters.add(System.getenv("ProgramFiles(x86)") + "\\NuGet\\Config\\Nuget.Config");

            } else {

                String customAppData = System.getenv("NUGET_COMMON_APPLICATION_DATA");

                if (Strings.isNullOrEmpty(customAppData)) {

                    if (SystemUtils.IS_OS_LINUX) {

                        parameters.add("/etc/opt/NuGet/Config/NuGet.Config");

                    } else {

                        parameters.add("/Library/Application Support/NuGet.Config");
                    }
                } else {

                    parameters.add(customAppData + "/NuGet/Config/NuGet.Config");
                }

            }

        }

        appendCredentials(userName, apiToken, parameters);

        return parameters;
    }

    private static void appendCredentials(String username, String apiToken, List<String> parameters) {
        if (username != null) {
            parameters.add("--username");
            parameters.add(username);
        }

        if (apiToken != null) {
            parameters.add("--password");
            parameters.add(apiToken);

            if (!SystemUtils.IS_OS_WINDOWS) {
                parameters.add("--store-password-in-clear-text");
            }
        }
    }

    public void clean() throws MojoExecutionException {

        execute(defaultOptions(), List.of("clean"), Set.of(), Map.of("Version", version));
    }
}
