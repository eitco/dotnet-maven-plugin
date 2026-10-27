// the managed sdk is cached in the plugin project's target directory (see dotnetSdkCacheBaseDirectory in pom.xml)
File cacheDirectory = new File(basedir, "../../.dotnet-sdk").canonicalFile

assert cacheDirectory.isDirectory(): "no sdk cache found at $cacheDirectory"

File[] ridDirectories = cacheDirectory.listFiles({ File file -> file.isDirectory() } as FileFilter)

assert ridDirectories.length == 1: "expected exactly one rid directory in $cacheDirectory"

File ridDirectory = ridDirectories[0]

assert new File(ridDirectory, "8.0.424.provisioned").isFile(): "no provisioning marker found in $ridDirectory"

String executableName = ridDirectory.name.startsWith("win") ? "dotnet.exe" : "dotnet"
File executable = new File(new File(ridDirectory, "8.0.424"), executableName)

assert executable.isFile(): "no dotnet executable found at $executable"

String buildLog = new File(basedir, "build.log").text

assert buildLog.contains("Executing command: " + executable.path + " build"): "dotnet build was not executed with $executable"
