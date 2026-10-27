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
}
