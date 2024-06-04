package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

@Mojo(name = "push", defaultPhase = LifecyclePhase.DEPLOY)
public class NugetPushMojo extends AbstractDotnetMojo {

    @Parameter
    private String nugetServerId;

    @Parameter
    private String nugetServerUrl;

    @Parameter
    private String nugetSnapshotServerUrl;

    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Component(hint = "dotnet-security")
    private SecDispatcher securityDispatcher;

    @Override
    public void execute() throws MojoExecutionException {

        String apiKey = findApiKey();

        String repositoryUrl = decideRepositoryUrl();

        newExecutor().push(apiKey, repositoryUrl);

    }

    private String decideRepositoryUrl() {

        boolean isSnapshot = project.getVersion().endsWith("-SNAPSHOT");

        if (isSnapshot) {

            return coalesce(nugetSnapshotServerUrl, nugetServerUrl, project.getDistributionManagement().getSnapshotRepository().getUrl(), project.getDistributionManagement().getRepository().getUrl());
        }

        return coalesce(nugetServerUrl, project.getDistributionManagement().getRepository().getUrl());
    }

    private String coalesce(String... strings) {

        for (String string : strings) {

            if (string != null) {

                return string;
            }
        }

        return null;
    }

    private String findApiKey() throws MojoExecutionException {

        if (nugetServerId == null) {

            return null;
        }

        Server server = settings.getServer(nugetServerId);

        if (server == null) {

            throw new MojoExecutionException("server " + nugetServerId + " not found");
        }

        try {

            return securityDispatcher.decrypt(server.getPassword());

        } catch (SecDispatcherException e) {

            throw new MojoExecutionException(e);
        }
    }
}
