package de.eitco.cicd.dotnet;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This goal calls {@code dotnet pack} creating nuget packages ({@code *.nupgk}). The goal will
 * call dotnet with {@code --no-build} as the project was already built in the {@code compile} phase
 * by the {@link BuildMojo {@code build} goal}.
 */
@Mojo(name = "pack", defaultPhase = LifecyclePhase.PACKAGE)
public class PackMojo extends AbstractDotnetMojo {

    /**
     * This parameter specifies a description for the package created.
     */
    @Parameter(defaultValue = "${project.description}")
    private String description;

    /**
     * This parameter specifies a URL to be added tp the package as its source.
     */
    @Parameter(defaultValue = "${project.scm.url}")
    private String repositoryUrl;

    @Override
    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("Skipping execution");
            return;
        }

        newExecutor().pack(vendor, description, repositoryUrl);
    }
}
