
[![License](https://img.shields.io/github/license/eitco/bom-maven-plugin.svg?style=for-the-badge)](https://opensource.org/license/mit)
[![Build status](https://img.shields.io/github/actions/workflow/status/eitco/dotnet-maven-plugin/deploy.yaml?branch=main&style=for-the-badge&logo=github)](https://github.com/eitco/dotnet-maven-plugin/actions/workflows/deploy.yaml)
[![Maven Central Version](https://img.shields.io/maven-central/v/de.eitco.cicd/dotnet-maven-plugin?style=for-the-badge&logo=apachemaven)](https://central.sonatype.com/artifact/de.eitco.cicd/dotnet-maven-plugin)

# dotnet maven plugin

This maven plugin provides a build lifecycle for dotnet. It is intended fpr situations where you need to build
one or more dotnet binaries inside a maven build. Such situations arise if you have a java application that does contain
some code that ports it to .Net.

# usage

To activate this build lifecycle add this plugin to your pom and set `packaging` to `nuget`:

````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>my.group.id</groupId>
    <artifactId>my-artifact-id</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>nuget</packaging>

    <build>
        <plugins>
            <plugin>
                <groupId>de.eitco.cicd</groupId>
                <artifactId>dotnet-maven-plugin</artifactId>
                <version>0.0.3</version>
                <extensions>true</extensions> <!-- (1) -->
            </plugin>
        </plugins>
    </build>

</project>
````
> 📘 you need to activate extensions for this plugin (1) 

When maven is started this will activate a build lifecycle that calls the dotnet executable to build the current project. 
This assumes that a valid sln or csproj file is in the same path as the pom and that dotnet is available in the PATH 
environment variable.

# build lifecycle

The following goals are bound to the build lifecycle:

## initialize

The `initialize` goal is bound to the `initialize` phase. This goal registers nuget source repositories. A special one 
that helps mimic mavens local repository and additionally all repositories that are configured. For example:

````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

...
    <build>
        <plugins>
            <plugin>
                <groupId>de.eitco.cicd</groupId>
                <artifactId>dotnet-maven-plugin</artifactId>
                <version>0.0.3</version>
                <extensions>true</extensions>
                <configuration>
                    <nugetSources>
                        <organization-repository-id>https://repo1.organization.org/nuget</organization-repository-id>
                        <project-repository-id>https:///epo1.organization.org/project-nuget</project-repository-id>
                    </nugetSources>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
````

This will add two custom nuget sources one named `organization-repository-id` that is assumed to be located at 
`https://repo1.organization.org/nuget` and another one named `project-repository-id` that is assumed to be located at
`https://repo1.organization.org/project-nuget`.

> 📘 Should you have registered any `<service>` elements in your settings.xml having the source name as `id`, those 
> credentials will be added to the corresponding nuget source.

> ⚠️ On non-windows platforms dotnet/nuget is unable to store credentials encrypted. On these platforms the credentials 
> will be stored in plain text

Additionally, a source `maven-nuget-local` pointing to the local directory `~/.m2/repository` will be added. Should any 
of these sourced already be added, they will be updated with their current config.

### The local nuget repository

The `initialize` goal also creates a local directory as nuget source, where a later goal will 'install' its artifacts to.
This enables a maven builds, where two different dotnet modules of the same build reactor have a dependency between them.

By default, the name of this source is `maven-nuget-local` and its location is `${settings.localRepository}`. Those 
can be overridden with the `<localMavenNugetRepositoryName>` and `<localMavenNugetRepositoryBaseDirectory>` parameters, respectively.   

## build

The `build` goal is bound to the `compile` phase. It will call `dotnet build` on the current project. Be that a .sln 
or .csproj file. It will always add the command line option `-p:Version=<projectVersion>` with `projectVersion` being 
the goals parameter of the same name. This way the version of the build artifacts are managed in the pom.

## test

The `test` goal is bound to the `test` phase. It will call `dotnet test` to execute test. It will add the command line 
parameter `--no-build`, since the `compile` phase will already be called before the `test` phase. It will always configure 
the `trx` logger and transform the results to a valid `junit` description - enabling ci servers to collect the tests 
results in the default format for maven builds. The goal will honour the reactors failure behaviour. Also, it honours the 
property `skipTests`, skipping its execution when set to true.

## pack

The `pack` goal is bound to the `package` phase. It calls `dotnet pack` creating nuget packages (*.nupgk). The goal will
call dotnet with `--no-build` as the project was already built in the `compile` phase. As the `build` goal it will set 
the dotnet `Version` property to the goals `projectVersion` parameter, managing the version in the pom. Also, it will set
the `--output` command line argument to the goals `targetDirectory` parameter, which defaults to `${project.build.directory}/dotnet`
thus being deleted by a `mvn clean` command.

## install

The `install` goal is bound to the `install` phase. It does not call `dotnet` at all. Since the `package` phase was 
already executed, it assumes the builds delivery artifacts to be located in the directory denoted by the goals 
`targetDirectory` parameter. It will simply copy all *.nupkg files located there to directory configured to be the 
[local nuget directory](#the-local-nuget-repository)

## push

The `push` goal is bound to the `deploy` phase. It calls `nuget push` on every nupgk file located in the configured
`targetDirectory`.

# clean lifecycle

This plugin also provides a `clean` lifecycle. It is pretty simple: Additionally to the maven default `clean` goal, a 
custom `clean` goal is called, which simply calls `dotnet clean`

# examples

Examples can be found with the [integration tests](src/it)