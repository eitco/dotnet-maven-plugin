package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class DotnetExecutableResolverTest {

    private static final DotnetSdkProvisioner STUB_PROVISIONER = new DotnetSdkProvisioner(
            new File("/unused"), 600, null, (version, dir, rid, timeout) -> {});

    @Test
    void modeA_neitherSetReturnsNull() throws MojoExecutionException {
        File resolved = DotnetExecutableResolver.resolve(null, null, STUB_PROVISIONER);
        assertNull(resolved);
    }

    @Test
    void modeB_executableSetReturnsIt() throws MojoExecutionException {
        File executable = new File("/some/path/dotnet");
        File resolved = DotnetExecutableResolver.resolve(executable, null, STUB_PROVISIONER);
        assertSame(executable, resolved);
    }

    @Test
    void modeC_versionSetDelegesToProvisioner() throws MojoExecutionException {
        File cachedSdk = new File("/cache/8.0.404/dotnet");
        DotnetSdkProvisioner provisioner = new DotnetSdkProvisioner(
                new File("/cache"), 600, null, (version, dir, rid, timeout) -> {}) {
            @Override
            public File resolveExecutable(String version) throws MojoExecutionException {
                if ("8.0.404".equals(version)) return cachedSdk;
                throw new MojoExecutionException("unexpected version: " + version);
            }
        };
        File resolved = DotnetExecutableResolver.resolve(null, "8.0.404", provisioner);
        assertSame(cachedSdk, resolved);
    }

    @Test
    void bothSetFailsImmediately() {
        File executable = new File("/some/path/dotnet");
        MojoExecutionException ex = assertThrows(
                MojoExecutionException.class,
                () -> DotnetExecutableResolver.resolve(executable, "8.0.404", STUB_PROVISIONER));
        assertTrue(ex.getMessage().contains("dotnetExecutable") || ex.getMessage().contains("dotnetSdkVersion"),
                "Error message should mention the conflicting parameters");
    }

    @Test
    void versionBlankTreatedAsNotSet() throws MojoExecutionException {
        File executable = new File("/some/path/dotnet");
        File resolved = DotnetExecutableResolver.resolve(executable, "   ", STUB_PROVISIONER);
        assertSame(executable, resolved);
    }
}
