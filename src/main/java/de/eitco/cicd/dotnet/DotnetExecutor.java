package de.eitco.cicd.dotnet;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record DotnetExecutor(
        File workingDirectory,
        File executable,
        File targetDirectory,
        Log log,
        boolean ignoreResult
) {

    public int execute(String... parameters) throws MojoExecutionException {

        return execute(defaultOptions().mergeIgnoreResult(ignoreResult), List.of(parameters), Set.of());
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

    private int execute(ExecutionOptions executionOptions, List<String> parameters, Set<String> obfuscation) throws MojoExecutionException {

        ProcessBuilder builder = new ProcessBuilder();

        builder.directory(workingDirectory);

        List<String> command = buildCommand(parameters);

        builder.command(command);

        if (executionOptions.inheritIo) {

            builder.inheritIO();
        }

        builder.environment().put("DOTNET_CLI_TELEMETRY_OPTOUT", "TRUE");

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

    private List<String> buildCommand(List<String> parameters) {

        List<String> command = new ArrayList<>();

        command.add(executable == null ? (SystemUtils.IS_OS_WINDOWS ? "dotnet.exe" : "dotnet") : executable.getPath());

        command.addAll(parameters);

        return command;
    }

    private void retry(int times, String... parameters) throws MojoExecutionException {

        retry(times, List.of(parameters));
    }

    private void retry(int times, List<String> parameters) throws MojoExecutionException {

        for (int index = 0; index < times; index++) {

            int returnCode = execute(defaultOptions().ignoreResult(), parameters, Set.of());

            if (returnCode == 0) {

                return;
            }
        }

        execute(defaultOptions(), parameters, Set.of());
    }

    public void build(String version, String assemblyVersion, String vendor, String configuration) throws MojoExecutionException {

        List<String> parameters = new ArrayList<>(List.of("build", "-p:Version=" + version));

        if (assemblyVersion != null) {
            parameters.add("-p:AssemblyVersion=" + assemblyVersion);
        }

        if (vendor != null) {
            parameters.add("-p:Company=" + vendor);
        }

        if (configuration != null) {
            parameters.add("--configuration=" + configuration);
        }

        retry(1, parameters);
    }

    public void pack(String version, String vendor, String description, String repositoryUrl) throws MojoExecutionException {

        List<String> parameters = new ArrayList<>(List.of(
                "pack",
                "--no-build",
                "-p:Version=" + version
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

        execute(defaultOptions(), parameters, Set.of());
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

        execute(defaultOptions().mergeIgnoreResult(ignoreResult), parameters, Optional.ofNullable(apiKey).stream().collect(Collectors.toSet()));
    }

    public void updateNugetSource(String url, String sourceName, String username, String apiToken) throws MojoExecutionException {

        List<String> parameters = new ArrayList<>(List.of("nuget", "update", "source", sourceName));

        if (url != null) {
            parameters.add("--source");
            parameters.add(url);
        }

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

        execute(defaultOptions(), parameters, apiToken != null ? Set.of(apiToken) : Set.of());
    }

    public void clean() throws MojoExecutionException {

        execute("clean");
    }
}
