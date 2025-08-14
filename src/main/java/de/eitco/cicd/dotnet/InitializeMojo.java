package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.settings.Server;

import java.io.File;
import java.util.Map;

/**
 * This goal registers nuget source repositories. A special one that helps mimic mavens local repository
 * and additionally all repositories that are configured.
 */
@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitializeMojo extends AbstractDotnetMojo {

    @Override
    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("Skipping execution");
            return;
        }

        DotnetExecutor dotnetExecutor = newExecutor();

        createLocalNugetRepositoryDirectory();

        File localNugetRepository = getUnresolvedLocalNugetRepositoryDirectory();

        dotnetExecutor.upsertNugetSource(localNugetRepository.getPath(), localMavenNugetRepositoryName, null, null, DotnetExecutor.NugetConfigLocation.USER);

        for (Map.Entry<String, String> entry : nugetSources.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Server server = settings.getServer(key);

            if (server == null) {

                dotnetExecutor.upsertNugetSource(value, key, null, null, null);

            } else {

                dotnetExecutor.upsertNugetSource(value, key, decrypt(server.getUsername()), decrypt(server.getPassword()), null);
            }
        }
    }

}
