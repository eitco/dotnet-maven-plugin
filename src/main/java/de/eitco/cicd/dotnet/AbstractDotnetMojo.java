package de.eitco.cicd.dotnet;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public abstract class AbstractDotnetMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}")
    protected File workingDirectory;

    @Parameter
    protected File dotnetExecutable;

    @Parameter(defaultValue = "${project.build.directory}")
    protected File targetDirectory;

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


}
