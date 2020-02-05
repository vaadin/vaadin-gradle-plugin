# Vaadin Gradle Plugin

This is an experimental version of the official Vaadin Gradle Plugin for Vaadin 14
and newer. The implementation is now mostly based on the similar Maven plugin.
Compared to Maven plugin, there are the following limitations:

* Vaadin 14 Compatibility mode is not supported
* Migration from Vaadin 13 to Vaadin 14 is not supported.

> Features of the "old Vaadin gradle plugin" are currently behind a flag, and
>will be most likely removed from the final release. Please let us know if you
>find those essential! [See more...](#old-plugin-mode)

Prerequisites:
* Java 8 or higher
* node.js and npm, either installed locally or automatically by the Vaadin Gradle Plugin via the `vaadinPrepareNode` task. To install locally:
  * Windows/Mac: [node.js Download site](https://nodejs.org/en/download/)
  * Linux: Use package manager e.g. `sudo apt install npm` 

As opposed to the older version of Gradle plugin, the new plugin doesn't create
projects any more. We plan to support Gradle projects via [vaadin.com/start](https://vaadin.com/start)
at some point. In the mean time, refer to project examples that you can use
as a basis for your Vaadin modules:

* [Basic WAR project](https://github.com/vaadin/base-starter-gradle)
* [Spring Boot project](https://github.com/vaadin/base-starter-spring-gradle)

## Tasks

There are the following tasks:

* `vaadinClean` will clean the project completely and removes `node_modules`, `package*.json` and `webpack.*.js`.
  You can use this task to clean up your project in case Vaadin throws mysterious exceptions,
  especially after you upgraded Vaadin to a newer version.
* `vaadinPrepareFrontend` will prepare your project for development. Calling this task
  will allow you to run the project e.g. in Tomcat with Intellij Ultimate.
  The task checks that node and npm tools are installed, copies frontend resources available inside
  `.jar` dependencies to `node_modules`, and creates or updates `package.json` and `webpack.config.json` files.
* `vaadinBuildFrontend` will use webpack to compile all JavaScript and CSS files into one huge bundle in production mode,
  and will place that by default into the `build/vaadin-generated` folder. The folder is
  then later picked up by `jar` and `war` tasks which then package the folder contents properly
  onto the classpath. Note that this task is not automatically hooked into `war`/`jar`/`assemble`/`build` and
  need to be invoked explicitly.
* `vaadinPrepareNode` will download a local distribution of node.js and npm into the `node/` folder for use by Vaadin.
  Please see below for more information.

Most common commands for all projects:

* `./gradlew clean vaadinPrepareFrontend` - prepares the project for development
* `./gradlew clean vaadinBuildFrontend build` - will compile Vaadin in production mode, then packages everything into the WAR archive.

## Configuration

To configure the plugin, you can use the following snippet in your `build.gradle` file:

`build.gradle` in Groovy:
```groovy
vaadin {
  optimizeBundle = false
}
```

All configuration options follow. Note that you **RARELY** need to change anything of the below.

* `productionMode = false`: Whether or not we are running in productionMode.
  The `vaadinBuildFrontend` task will automatically switch this to true, there is no need for you to configure anything.
* `buildOutputDirectory = File(project.buildDir, "vaadin-generated")`: 
  The plugin will generate additional resource files here. These files need
  to be present on the classpath, in order for Vaadin to be
  able to run, both in dev mode and in the production mode.
  The plugin will automatically register
  this as an additional resource folder, which should then be picked up by the IDE.
  That will allow the app to run for example in Intellij with Tomcat.
  For example the `flow-build-info.json` goes here. See [webpackOutputDirectory]
  for more details.
* `webpackOutputDirectory = File(buildOutputDirectory, "META-INF/VAADIN/")`:
  The folder where webpack should output index.js and other generated files.
  In the dev mode, the `flow-build-info.json` file is generated here.
* `npmFolder: File = project.projectDir`: The folder where
  `package.json` file is located. Default is project root dir.
* `webpackTemplate: String = "webpack.config.js"`:
  Copy the `webapp.config.js` from the specified URL if missing. Default is
  the template provided by this plugin. Set it to empty string to disable
  the feature.
* `webpackGeneratedTemplate = "webpack.generated.js"`:
  Copy the `webapp.generated.js` from the specified URL. Default is the
  template provided by this plugin. Set it to empty string to disable the
  feature.  
* `generatedFolder = File(project.projectDir, "target/frontend")`:
  The folder where flow will put generated files that will be used by
  webpack. Should be `build/frontend/` but this is only supported in Vaadin 15+
* `frontendDirectory = File(project.projectDir, "frontend")`:
  A directory with project's frontend source files.
* `generateBundle = true`: Whether to generate a bundle from the project frontend sources or not.
* `runNpmInstall = true`: Whether to run `npm install` after updating dependencies.
* `generateEmbeddableWebComponents = true`:
  Whether to generate embeddable web components from WebComponentExporter inheritors.
* `frontendResourcesDirectory = File(project.projectDir, "src/main/resources/META-INF/resources/frontend")`:
  Defines the project frontend directory from where resources should be
  copied from for use with webpack.
* `optimizeBundle = true`: Whether to use byte code scanner strategy to discover frontend
  components.
* `nodeVersion = "12.14.1"`: When using the `vaadinPrepareNode` task, this property
  specifies which node version to download. Please see the [list of all node.js releases](https://nodejs.org/en/download/releases/). Usually
  it's best to select the LTS release.

## Automatic Download of node.js and npm

You do not need to have node.js nor npm installed in your system, in order to use Vaadin.
The `vaadinPrepareNode` task will download the node.js+npm distribution and will place it
into the `node/` folder which will then be picked automatically by Vaadin.

In your development environment, just run:
```bash
./gradlew vaadinPrepareNode
```
to download and prepare a local distribution of node.js. You only need to run this once,
in order to populate the folder `node/`.

In your CI, don't forget to call the `vaadinPrepareNode` before the `vaadinPrepareFrontend` task:
```bash
./gradlew clean vaadinPrepareNode vaadinBuildFrontend build
```

If you wish to override the node version which will be downloaded, simply specify
the node.js version in the `vaadin {}` block:

```groovy
vaadin {
    nodeVersion = "10.15.2"
}
```

Please see the [list of all node.js releases](https://nodejs.org/en/download/releases/). Usually
it's best to select the LTS release.

# Developing The Plugin

Open the project in Intellij.

TBD

## Running The IT/Functional Tests

There is a comprehensive test suite which tests the plugin in various generated projects.
To run the suite, simply run

```bash
./gradlew check
```

That will run the `functionalTest` task which will run all tests from the `src/functionalTest` folder.

### Running Individual Functional Tests from Intellij

Just right-click the test class and select "Run". If running the test fails, try one of the following:

1. Use Intellij, Community edition is enough
2. Go to "File / Settings / Build, Execution, Deployment / Build Tools / Gradle" and make sure that
   "Run tests using" is set to "Gradle".

# Old Plugin Mode

The old plugin mode can be enabled by running Gradle with the `-Dvaadin.enableOldPlugin=true` switch.
Please read on for the documentation on the old plugin mode.

This is an experimental Vaadin Gradle Plugin for Vaadin 14 and newer.
A stable version for older Vaadin versions can be found [here](https://devsoap.com/gradle-vaadin-flow-plugin/).

Gradle 6 is not yet supported. Use version 5.6.4.

Currently builds are available only from temporary pre-release repository. You can start with following build.gradle file (with final release the start will be simpler):

```
buildscript {
    repositories {
        gradlePluginPortal()
        maven {
            url "https://repo.vaadin.com/nexus/content/repositories/vaadin-prereleases-201912/"
        }
    }
    dependencies {
        classpath "com.vaadin:vaadin-gradle-plugin:0.1.0"
        classpath "com.moowork.gradle:gradle-node-plugin:1.2.0"
        classpath 'io.github.classgraph:classgraph:4.8.49'
        classpath "gradle.plugin.org.gretty:gretty:3.0.1"
    }
}
apply plugin: 'com.vaadin'
apply plugin: "org.gretty"

repositories {
    maven {
        url "https://repo.vaadin.com/nexus/content/repositories/vaadin-prereleases-201912/"
    }
}

vaadin.autoconfigure()

```

[Preliminary documentation (targeting for final release)](https://github.com/vaadin-learning-center/learning-content/blob/author/magi/gradle-plugin/learn/tutorials/gradle-plugin/content.adoc)

## Developing The Plugin

Please read the Gradle Tutorial on [Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
to understand how Gradle plugins are developed.

The main entry to the plugin is the `VaadinPlugin` class. When applied to the project, it will register
all necessary tasks and extensions into the project.

Launching all tests - TBD

### Developing the plugin and testing it on-the-fly

You can take advantage of [composite builds](https://docs.gradle.org/current/userguide/composite_builds.html),
which will allow you to join together the plugin itself along with an example project using that plugin,
into one composite project. The easiest way is to use the [Skeleton Starter Gradle](https://github.com/vaadin/skeleton-starter-gradle)
example project.

1. Clone the Skeleton Starter Gradle project and open it in Intellij
2. Remove the entire `buildscript{}` block and the `apply plugin:"com.vaadin"` line and
   uncomment the `id("com.vaadin")` line.
3. Create a `settings.gradle` file containing the following line: `includeBuild("/home/mavi/work/vaadin/vaadin-gradle-plugin")`
   (use full path on your system to the Gradle Plugin project)
4. Reimport the Skeleton Starter project: Gradle / Reimport. A new project named `vaadin-gradle-plugin`
   should appear in your workspace.
5. Open the terminal with Alt+F12.
6. If you now type `./gradlew vaadinPrepareFronend` into the command line, Gradle will compile any changes done to
   the Gradle plugin and will run updated code. You can verify that by adding `println()` statements
   into the `VaadinPrepareFrontendTask` class.

## License

This plugin is distributed under the Apache License 2.0 license. For more information about the license see the LICENSE file 
in the root directory of the repository. A signed CLA is required when contributing to the project.

See [CONTRIBUTING](CONTRIBUTING.md) for instructions for getting the plugin sources, and for compiling and using the plugin locally.
