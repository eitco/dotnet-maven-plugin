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

    /**
     * This parameter specifies the directory where to execute {@code dotnet} and thus where the
     * project files are located
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File workingDirectory;

    /**
     * This parameter specifies the location of the {@code dotnet} executable. If not set {@code dotnet} being
     * available by the {@code PATH} environment variable is assumed.
     */
    @Parameter
    protected File dotnetExecutable;

    /**
     * This parameter specifies the directory where {@code dotnet} writes its generated package(s) to.
     */
    @Parameter(defaultValue = "${project.build.directory}/dotnet")
    protected File targetDirectory;

    /**
     * This parameter specifies the version of this project given to {@code dotnet} by the {@code -p:Version=}
     * command line parameter.
     */
    @Parameter(defaultValue = "${project.version}")
    protected String projectVersion;

    /**
     * This parameter specifies the assembly version of this project. Keep in mind that the assembly version needs to
     * consist of up to 4 numeric values separated by '.' e.g.: {@code <major>.<minor>.<patch>.<build>}.
     */
    @Parameter
    protected String assemblyVersion;

    /**
     * This parameter specifies the {@code Company} added to the compiles executables or libraries.
     */
    @Parameter(defaultValue = "${project.organization.name}")
    protected String vendor;

    /**
     * This parameter specifies nuget sources to be added to the project. These are the locations where {@code nuget}
     * will read dependencies from. The value of each entry is the url of the remote repository. The key is a unique
     * name {@code nuget} will use to identify the repository by. It can also be used to add
     * <a href="https://maven.apache.org/settings.html#servers">credentials</a> to using {@code <server>}
     * elements in the {@code settings.xml}.
     *
     * Consider to <a href="https://maven.apache.org/guides/mini/guide-encryption.html">encrypt passwords</a> in
     * your {@code settings.xml}
     *
     * Note that crendentials added this way will be written to your nuget configuration file. On windows systems
     * the credentials will be encrypted, however on linux systems password encryption is not supported by
     * {@code nuget} - so the credentials will be written to the {code nuget} config way unencrypted.
     */
    @Parameter
    protected Map<String, String> nugetSources = Map.of();

    /**
     * This parameter specifies custom properties given to dotnet via '-p:'. Keep in mind that the 'Version', 'Company',
     * 'Description', 'RepositoryUrl' and 'AssemblyVersion' properties will be overwritten by their respective
     * parameters, when specified.
     */
    @Parameter
    protected Map<String, String> customProperties = Map.of();

    /**
     * This parameter specifies additional environment variables to be added to the {@code dotnet} process.
     * <br/>
     * Note that the {@code DOTNET_CLI_TELEMETRY_OPTOUT} will be set to {@code TRUE} if not overwritten with
     * this parameter.
     */
    @Parameter
    protected Map<String, String> environmentVariables = Map.of();

    /**
     * This parameter specifies the symbolic name of the local nuget repository. The local nuget repository is a
     * special file system repository this plugin copies the generated packages in the {@code install} phase to.
     * This simulates mavens local repository.
     * <br/>
     * The location of the local nuget repository can be configured by {@link #localMavenNugetRepositoryBaseDirectory}.
     */
    @Parameter(defaultValue = "maven-nuget-local")
    protected String localMavenNugetRepositoryName;

    /**
     * This parameter specifies the location of the local nuget repository. The local nuget repository is a
     * special file system repository this plugin copies the generated packages in the {@code install} phase to.
     * This simulates mavens local repository.
     */
    @Parameter(defaultValue = "${settings.localRepository}")
    protected String localMavenNugetRepositoryBaseDirectory;

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

        return new File(baseDirectory, "." + localMavenNugetRepositoryName);
    }

    protected File getUnresolvedLocalNugetRepositoryDirectory() {

        return new File(localMavenNugetRepositoryBaseDirectory, "." + localMavenNugetRepositoryName);
    }

    protected File createLocalNugetRepositoryDirectory() throws MojoExecutionException {

        try {

            File localNugetRepository = getResolvedNugetRepoDirectory();

            getLog().debug("creating directory " + localNugetRepository.getAbsolutePath());
            FileUtils.forceMkdir(localNugetRepository);

            return localNugetRepository;

        } catch (IOException e) {

            throw new MojoExecutionException(e);
        }
    }
}
