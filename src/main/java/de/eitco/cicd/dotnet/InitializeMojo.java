package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.settings.Server;

import java.util.Map;

@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE)
public class InitializeMojo extends AbstractDotnetMojo {


    @Override
    public void execute() throws MojoExecutionException {

        DotnetExecutor dotnetExecutor = newExecutor();

        for (Map.Entry<String, String> entry : nugetSources.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Server server = findServer(key);

            dotnetExecutor.updateNugetSource(value, key, decrypt(server.getUsername()), decrypt(server.getPassword()));
        }

    }
}
