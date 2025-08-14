package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;

/**
 * This goal calls {@code nuget push} on every nupgk file located in the configured
 * {@code targetDirectory}.
 */
@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class NugetPushMojo extends AbstractDotnetMojo {

    /**
     * This parameter specifies the id of the nuget server to push to. This id is used to find the
     * corresponding {@code <server>} entry in the {@code settings.xml} for authentication.
     */
    @Parameter(defaultValue = "nuget-server")
    protected String nugetServerId;

    /**
     * This parameter specifies the url of the nuget server to push to.
     */
    @Parameter
    private String nugetServerUrl;

    /**
     * This parameter specifies an alternative url of a server to push to which is only used
     * if the current version is a snapshot version.
     */
    @Parameter
    private String nugetSnapshotServerUrl;

    /**
     * If set to {@code true} this parameter forces the configured target server to be added as
     * a nuget source.
     */
    @Parameter
    private Boolean forceAddSource;

    /**
     * This parameter specifies the repository name that is used when added as nuget source. If
     * it is not specified it defaults to {@link #nugetServerId}.
     */
    @Parameter
    private String repositoryName;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("Skipping execution");
            return;
        }

        Server server = findServer(nugetServerId);

        String apiKey = server != null ? decrypt(server.getPassword()) : null;
        String nugetServerUser = server != null ? decrypt(server.getUsername()) : null;

        String repositoryUrl = decideRepositoryUrl();

        boolean addSource = coalesce(forceAddSource, nugetSources.get(nugetServerId) == null && repositoryUrl.endsWith("/index.json"));

        DotnetExecutor dotnetExecutor = newExecutor();

        String repository = repositoryUrl;

        if (addSource) {

            String usedRepositoryName = coalesce(repositoryName, nugetServerId);

            repository = usedRepositoryName;

            dotnetExecutor.upsertNugetSource(repositoryUrl, usedRepositoryName, nugetServerUser, apiKey, null);
        }

        dotnetExecutor.push(apiKey, repository);

    }

    private String decideRepositoryUrl() {

        boolean isSnapshot = project.getVersion().endsWith("-SNAPSHOT");

        if (isSnapshot) {

            return coalesce(nugetSnapshotServerUrl, nugetServerUrl, project.getDistributionManagement().getSnapshotRepository().getUrl(), nugetSources.get(nugetServerId), project.getDistributionManagement().getRepository().getUrl());
        }

        return coalesce(nugetServerUrl, nugetSources.get(nugetServerId), project.getDistributionManagement().getRepository().getUrl());
    }

    @SafeVarargs
    private <Type> Type coalesce(Type... elements) {

        for (Type element : elements) {

            if (element != null) {

                return element;
            }
        }

        return null;
    }

}
