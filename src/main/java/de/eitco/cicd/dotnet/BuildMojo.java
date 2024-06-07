package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name = "build", defaultPhase = LifecyclePhase.COMPILE)
public class BuildMojo extends AbstractDotnetMojo {

    @Parameter
    private String configurationName;

    @Override
    public void execute() throws MojoExecutionException {

        newExecutor().build(projectVersion, assemblyVersion, vendor, configurationName);
    }
}
