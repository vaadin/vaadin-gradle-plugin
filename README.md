# Vaadin Gradle Plugin

This is an experimental version of the official Vaadin Gradle Plugin for Vaadin 14 and newer. The implementation is now mostly based on the similar Maven plugin. Compared to Maven plugin, there are the following limitations:

* Vaadin 14 Compatibility mode is not supported
* Migration from Vaadin 13 to Vaadin 14 is not supported.

*Features of the "old Vaadin gradle plugin" are currently behind a flag, but most likely removed from the final release. Please let us know if you find those essential! [See more...](#old-plugin-mode)*

Prerequisites:
* Java 8 or higher
* node.js and npm installed locally. To install:
  * Windows/Mac: [node.js Download site](https://nodejs.org/en/download/)
  * Linux: Use package manager e.g. `sudo apt install npm` 

As opposed to the older version of Gradle pluging, the plugin don't create projects any more. We plan to support Gradle projects via vaadin.com/start at some point. In the mean time, refer to project examples that you can use as a basis for your Vaadin modules:

* [Basic war project](https://github.com/vaadin/base-starter-gradle)
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
* `vaadinPrepareNode` will prepare a local distribution of node.js and npm for use by Vaadin.
  Please see below for more information.

Most common commands for the WAR project:

* `./gradlew clean vaadinPrepareFrontend` - prepares the project for development
* `./gradlew clean vaadinBuildFrontend build` - will compile Vaadin in production mode, then packages everything into the WAR archive.

Spring Boot project: TBD

## Configuration

To configure the plugin, you can use the following snippet in your `build.gradle` file:

`build.gradle` in Groovy:
```groovy
vaadinFlow {
  optimizeBundle = false
}
```

`build.gradle.kts` in Kotlin:
```kotlin
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
able to run, both in dev mode and in the production mode. The plugin will automatically register
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

## Automatic Download of node.js and npm

You do not have node.js nor npm installed in your system, in order to use Vaadin. The Vaadin Gradle Plugin
is able to help you with downloading of the node.js+npm distribution.

In order to do that, you will need the [com.github.node-gradle.node](https://plugins.gradle.org/plugin/com.github.node-gradle.node) plugin.
Add the following to your `build.gradle` file:

```groovy
plugins {
    id "com.github.node-gradle.node" version "2.2.1"  // in order for vaadinPrepareNode to work
}
node {
    version = "10.15.2"
    download = true
    // to download node+npm, just run the `vaadinPrepareNode` task
}
```

In your development environment, all you need to do is to run:
```bash
./gradlew vaadinPrepareNode
```
to download and prepare a local distribution of node.js. You only need to run this once:
a folder named `node/` is created in the project directory, which will be used by Vaadin
from now on.

In your CI, don't forget to call the `vaadinPrepareNode` before the `vaadinPrepareFrontend` task:
```bash
./gradlew clean vaadinPrepareNode vaadinBuildFrontend build
```

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
