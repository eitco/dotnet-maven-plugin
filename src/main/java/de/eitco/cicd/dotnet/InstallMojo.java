package de.eitco.cicd.dotnet;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL)
public class InstallMojo extends AbstractDotnetMojo {

    public static final String NUPKG_SUFFIX = ".nupkg";
    private static File globalsCacheDirectory;

    @Override
    public void execute() throws MojoExecutionException {

        File[] files = targetDirectory.listFiles(file -> file.getName().endsWith(NUPKG_SUFFIX));

        if (files == null) {

            getLog().debug("target directory does not exist");
            return;
        }

        if (files.length == 0) {

            getLog().debug("target directory is empty");
        }

        File localNugetRepository = createLocalNugetRepositoryDirectory();

        for (File file : files) {

            try {

                File targetFile = new File(localNugetRepository, file.getName());

                getLog().info("installing " + targetFile.getAbsolutePath());
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);


                getLog().debug("get package id for  " + file.getName() + " project version is " + projectVersion);
                String packageId = file.getName().substring(0, file.getName().length() - NUPKG_SUFFIX.length() - projectVersion.length() - 1);

                File cacheDirectory = getGlobalsCacheDirectory();

                File cacheEntry = new File(new File(cacheDirectory, packageId), projectVersion);
                getLog().debug("cache entry for " + file.getName() + " is " + cacheEntry.getAbsolutePath());

                if (cacheEntry.isDirectory()) {

                    getLog().debug("deleting cache entry " + cacheEntry.getAbsolutePath());

                    FileUtils.deleteDirectory(cacheEntry);
                }

            } catch (IOException e) {

                throw new MojoExecutionException(e);
            }
        }
    }

    private File getGlobalsCacheDirectory() throws MojoExecutionException {

        if (globalsCacheDirectory != null) {

            return globalsCacheDirectory;
        }

        return globalsCacheDirectory = new File(newExecutor().getLocalArtifactCache());
    }

}
