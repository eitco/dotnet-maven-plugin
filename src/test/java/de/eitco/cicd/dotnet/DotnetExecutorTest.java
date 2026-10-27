package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DotnetExecutorTest {

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void localArtifactCacheLookupUsesConfiguredEnvironment(@TempDir File directory)
            throws IOException, MojoExecutionException {

        File environmentDump = new File(directory, "environment.txt");
        File fakeDotnet = new File(directory, "dotnet");
        Files.writeString(fakeDotnet.toPath(),
                "#!/bin/sh\n" +
                        "env > '" + environmentDump.getAbsolutePath() + "'\n" +
                        "echo 'global-packages: /some/cache/'\n");
        assertTrue(fakeDotnet.setExecutable(true));

        DotnetExecutor executor = new DotnetExecutor(
                directory, fakeDotnet, directory, "1.0.0", Map.of(),
                Map.of("DOTNET_NOLOGO", "1"), Map.of(), new SystemStreamLog(), false);

        assertEquals("/some/cache/", executor.getLocalArtifactCache());
        assertTrue(Files.readAllLines(environmentDump.toPath()).contains("DOTNET_NOLOGO=1"),
                "dotnet nuget locals should be run with the configured environment");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void buildPassesNullAndEmptyCustomPropertiesAsEmptyString(@TempDir File directory)
            throws IOException, MojoExecutionException {

        File argumentDump = new File(directory, "arguments.txt");
        File fakeDotnet = new File(directory, "dotnet");
        Files.writeString(fakeDotnet.toPath(),
                "#!/bin/sh\n" +
                        "printf '%s\\n' \"$@\" > '" + argumentDump.getAbsolutePath() + "'\n");
        assertTrue(fakeDotnet.setExecutable(true));

        Map<String, String> customProperties = new HashMap<>();
        customProperties.put("Null", null);
        customProperties.put("Empty", "");

        DotnetExecutor executor = new DotnetExecutor(
                directory, fakeDotnet, directory, "1.0.0", customProperties,
                Map.of(), Map.of(), new SystemStreamLog(), false);

        executor.build(null, null, null);

        List<String> arguments = Files.readAllLines(argumentDump.toPath());

        assertTrue(arguments.contains("-p:Null="), "null property should be passed as empty string: " + arguments);
        assertTrue(arguments.contains("-p:Empty="), "empty property should be passed as empty string: " + arguments);
        assertTrue(arguments.stream().noneMatch(argument -> argument.endsWith("=null")),
                "no property should be passed as 'null': " + arguments);
    }
}
