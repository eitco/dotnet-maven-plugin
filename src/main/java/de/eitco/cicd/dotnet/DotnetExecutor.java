package de.eitco.cicd.dotnet;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record DotnetExecutor(
        File workingDirectory,
        File executable,
        File targetDirectory,
        Log log,
        boolean ignoreResult
) {

    public int execute(String... parameters) throws MojoExecutionException {

        ProcessBuilder builder = new ProcessBuilder();

        builder.directory(workingDirectory);

        List<String> command = buildCommand(List.of(parameters));

        builder.command(command);

        builder.inheritIO();
        builder.environment().put("DOTNET_CLI_TELEMETRY_OPTOUT", "TRUE");

        try {

            log.info("Executing command: " + String.join(" ", command));

            Process process = builder.start();

            int returnCode = process.waitFor();

            if (returnCode != 0 && !ignoreResult) {

                throw new MojoExecutionException("process " + String.join(" ", command) + " returned code " + returnCode);
            }

            return returnCode;

        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException(e);
        }
    }

    private List<String> buildCommand(List<String> parameters) {

        List<String> command = new ArrayList<>();

        command.add(executable == null ? (SystemUtils.IS_OS_WINDOWS ? "dotnet.exe" : "dotnet") : executable.getPath());

        command.addAll(parameters);

        return command;
    }

    public void build() throws MojoExecutionException {

        execute("build");
    }

    public void pack(String packageId, String version, String vendor, String description, String repositoryUrl) throws MojoExecutionException {

        execute("pack", "--no-build", "-p:PackageId=" + packageId, "-p:PackageVersion=" + version, "-p:Company=" + vendor, "-p:Description=" + description, "-p:RepositoryUrl=" + repositoryUrl);
    }

    public int test(String logger, String testResultDirectory) throws MojoExecutionException {

        return execute("test", "--no-build", "--logger", logger, "--results-directory", testResultDirectory);
    }
}
