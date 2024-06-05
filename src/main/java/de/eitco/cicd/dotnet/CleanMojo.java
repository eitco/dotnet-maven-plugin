package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN)
public class CleanMojo extends AbstractDotnetMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        newExecutor().clean();
    }
}
