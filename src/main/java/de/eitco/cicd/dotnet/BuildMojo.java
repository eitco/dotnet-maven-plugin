package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name = "build", defaultPhase = LifecyclePhase.COMPILE)
public class BuildMojo extends AbstractDotnetMojo {

    @Override
    public void execute() throws MojoExecutionException {

        newExecutor().build(projectVersion, assemblyVersion, vendor);
    }
}
