package de.eitco.cicd.dotnet;

import com.sun.nio.file.ExtendedCopyOption;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL)
public class InstallMojo extends AbstractDotnetMojo {

    @Override
    public void execute() throws MojoExecutionException {

        File[] files = targetDirectory.listFiles(file -> file.getName().endsWith(".nupkg"));

        if (files == null) {

            getLog().info("target directory does not exist");
            return;
        }

        if (files.length == 0) {

            getLog().info("target directory is empty");
        }

        File localNugetRepository = createLocalNugetRepoDirectory();

        for (File file : files) {

            try {

                File targetFile = new File(localNugetRepository, file.getName());

                getLog().info("installing " + targetFile.getAbsolutePath());
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            } catch (IOException e) {

                throw new MojoExecutionException(e);
            }
        }
    }
}
