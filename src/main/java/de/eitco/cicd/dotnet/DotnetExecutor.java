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

        return execute(ignoreResult, List.of(parameters));
    }

    public int execute(boolean ignoreResult, List<String> parameters) throws MojoExecutionException {

        ProcessBuilder builder = new ProcessBuilder();

        builder.directory(workingDirectory);

        List<String> command = buildCommand(parameters);

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

        int returnCode = execute(true, List.of("build"));

        if (returnCode != 0) {

            execute("build");
        }
    }

    public void pack(String version, String vendor, String description, String repositoryUrl) throws MojoExecutionException {

        List<String> parameters = new ArrayList<>(List.of(
                "pack",
                "--no-build",
                "-p:PackageVersion=" + version
        ));

        if (vendor != null) {
            parameters.add("-p:Company=" + vendor);
        }

        if (description != null) {
            parameters.add("-p:Description=" + description);
        }

        if (repositoryUrl != null) {
            parameters.add("-p:RepositoryUrl=" + repositoryUrl);
        }

        parameters.add("--output");
        parameters.add(targetDirectory.getPath());

        execute(false, parameters);
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

        execute(ignoreResult, parameters);
    }

    public void addNugetSource(String url, String sourceName, String username, String apiToken) throws MojoExecutionException {

        execute("nuget", "add", "source", url, "--name", sourceName, "--username", username, "--password", apiToken);
    }

    public void clean() throws MojoExecutionException {

        execute("clean");
    }
}
