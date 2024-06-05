package de.eitco.cicd.dotnet;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "pack", defaultPhase = LifecyclePhase.PACKAGE)
public class PackMojo extends AbstractDotnetMojo {

    @Parameter(defaultValue = "${project.version}")
    private String projectVersion;

    @Parameter(defaultValue = "${project.organization.name}")
    private String vendor;

    @Parameter(defaultValue = "${project.description}")
    private String description;

    @Parameter(defaultValue = "${project.scm.url}")
    private String repositoryUrl;

    @Override
    public void execute() throws MojoExecutionException {

        newExecutor().pack(projectVersion, vendor, description, repositoryUrl);
    }
}
