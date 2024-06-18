package de.eitco.cicd.dotnet;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public abstract class AbstractDotnetMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}")
    protected File workingDirectory;

    @Parameter
    protected File dotnetExecutable;

    @Parameter(defaultValue = "${project.build.directory}/dotnet")
    protected File targetDirectory;

    @Parameter(defaultValue = "${project.version}")
    protected String projectVersion;

    @Parameter
    protected String assemblyVersion;

    @Parameter(defaultValue = "${project.organization.name}")
    protected String vendor;

    @Parameter
    protected Map<String, String> nugetSources = Map.of();

    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Parameter(defaultValue = "maven-nuget-local")
    protected String localMavenNugetRepositoryName;

    @Parameter(defaultValue = "${settings.localRepository}")
    protected String localMavenNugetRepositoryBaseDirectory;

    /**
     * This parameter specifies custom properties given to dotnet via '-p:'. Keep in mind that the 'Version', 'Company',
     * 'Description', 'RepositoryUrl' and 'AssemblyVersion' properties will be overwritten by their respective
     * parameters, when specified.
     *
     */
    @Parameter
    protected Map<String, String> customProperties = Map.of();

    @Parameter
    protected Map<String, String> environmentVariables = Map.of();

    @Component(hint = "dotnet-security")
    private SecDispatcher securityDispatcher;

    protected DotnetExecutor newExecutor() {

        return newExecutor(false);
    }

    protected DotnetExecutor newExecutor(boolean ignoreResult) {
        return new DotnetExecutor(
                workingDirectory,
                dotnetExecutable,
                targetDirectory,
                projectVersion,
                customProperties,
                environmentVariables,
                getLog(),
                ignoreResult
        );
    }


    protected String decrypt(String text) throws MojoExecutionException {
        try {

            return securityDispatcher.decrypt(text);

        } catch (SecDispatcherException e) {

            throw new MojoExecutionException(e);
        }
    }

    protected Server findServer(String serverId) throws MojoExecutionException {

        Server server = settings.getServer(serverId);

        if (server == null) {

            throw new MojoExecutionException("server " + serverId + " not found");
        }
        return server;
    }

    protected File getResolvedNugetRepoDirectory() {

        String baseDirectory = localMavenNugetRepositoryBaseDirectory;

        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {

            String key = entry.getKey();
            String value = entry.getValue();

            baseDirectory = baseDirectory.replace("%" + key + "%", value);
        }

        for (Map.Entry<String, String> entry : environmentVariables.entrySet()) {

            String key = entry.getKey();
            String value = entry.getValue();

            baseDirectory = baseDirectory.replace("%" + key + "%", value);
        }

        return getNugetRepoDirectory();
    }

    private File getNugetRepoDirectory() {

        return new File(localMavenNugetRepositoryBaseDirectory, "." + localMavenNugetRepositoryName);
    }

    protected File createLocalNugetRepoDirectory() throws MojoExecutionException {
        try {

            File localNugetRepository = getResolvedNugetRepoDirectory();

            FileUtils.forceMkdir(localNugetRepository);

            return getNugetRepoDirectory();

        } catch (IOException e) {

            throw new MojoExecutionException(e);
        }
    }
}
