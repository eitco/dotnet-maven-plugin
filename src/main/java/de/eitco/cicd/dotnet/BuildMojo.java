package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * This goal calls `dotnet build` on the current project, be that a {@code .sln}
 * or {@code .csproj} file. It will always add the command line option {@code -p:Version=<projectVersion>} with
 * {@code projectVersion} being the goals parameter of the same name. This way the version of the build artifacts
 * are managed in the pom.
 */
@Mojo(name = "build", defaultPhase = LifecyclePhase.COMPILE)
public class BuildMojo extends AbstractDotnetMojo {

    /**
     * This parameter specifies which configuration will be built. Normally, at least {@code RELEASE}
     * and {@code DEBUG} are available.
     */
    @Parameter
    private String configurationName;

    @Override
    public void execute() throws MojoExecutionException {

        newExecutor().build(assemblyVersion, vendor, configurationName);
    }
}
