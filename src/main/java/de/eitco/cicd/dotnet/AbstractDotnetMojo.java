package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.File;
import java.util.Map;

public abstract class AbstractDotnetMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}")
    protected File workingDirectory;

    @Parameter
    protected File dotnetExecutable;

    @Parameter(defaultValue = "${project.build.directory}")
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
                getLog(),
                ignoreResult
        );
    }


    protected String findApiKey(String serverId) throws MojoExecutionException {

        if (serverId == null) {

            return null;
        }

        Server server = findServer(serverId);

        return decrypt(server.getPassword());
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
}
