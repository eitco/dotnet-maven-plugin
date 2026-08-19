package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

final class DotnetExecutableResolver {

    private DotnetExecutableResolver() {
    }

    static File resolve(File explicitExecutable, String sdkVersion, DotnetSdkProvisioner provisioner)
            throws MojoExecutionException {
        boolean hasExplicit = explicitExecutable != null;
        boolean hasVersion = sdkVersion != null && !sdkVersion.isBlank();

        if (hasExplicit && hasVersion) {
            throw new MojoExecutionException(
                    "Configure either <dotnetExecutable> or <dotnetSdkVersion>, not both.");
        }

        if (hasExplicit) {
            return explicitExecutable;
        }

        if (hasVersion) {
            return provisioner.resolveExecutable(sdkVersion);
        }

        return null;
    }
}
