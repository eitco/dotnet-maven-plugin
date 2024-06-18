package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;

@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class NugetPushMojo extends AbstractDotnetMojo {

    @Parameter(defaultValue = "nuget-server")
    protected String nugetServerId;
    @Parameter
    private String nugetServerUrl;

    @Parameter
    private String nugetSnapshotServerUrl;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter
    private Boolean forceAddSource;

    @Parameter
    private String repositoryName;

    @Override
    public void execute() throws MojoExecutionException {

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
