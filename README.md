# Vaadin Gradle Plugin

This is an experimental version of the official Vaadin Gradle Plugin for Vaadin 14
(Vaadin 15 is not supported at the moment, please see [#50](https://github.com/vaadin/vaadin-gradle-plugin/issues/50) for more details).
The implementation is now mostly based on the similar Maven plugin.
Compared to Maven plugin, there are the following limitations:

* Vaadin 14 Compatibility mode is not supported
* Migration from Vaadin 13 to Vaadin 14 is not supported.

Prerequisites:
* Java 8 or higher
* node.js and npm. Vaadin will now automatically install node.js and npm, but you can also install those locally:
  * Windows/Mac: [node.js Download site](https://nodejs.org/en/download/)
  * Linux: Use package manager e.g. `sudo apt install npm` 

As opposed to the older version of Gradle plugin, the new plugin doesn't create
projects any more. We plan to support Gradle projects via [vaadin.com/start](https://vaadin.com/start)
at some point. In the meantime, refer to project examples that you can use
as a basis for your Vaadin modules:

## Installation

Check out the example project setups for basic WAR project and Spring Boot:

* [Basic WAR project](https://github.com/vaadin/base-starter-gradle)
* [Spring Boot project](https://github.com/vaadin/base-starter-spring-gradle)

The actual plugin part is as follows (please check the latest version at
[plugins.gradle.org](https://plugins.gradle.org/plugin/com.vaadin)): 

```
plugins {
    id 'com.vaadin' version '0.7.0'
}
```

Compatibility chart:

| Vaadin Gradle Plugin version | Supports |
|------------------------------|----------|
| -                            | Vaadin 13 and lower are unsupported |
| 0.6.0 and lower              | Vaadin 14.1.x and lower |
| 0.7.0                        | Vaadin 14.2.x |
| 0.8.0                        | Vaadin 14.3.x and higher |
| -                            | Vaadin 15 and higher are currently unsupported |

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
  need to be invoked explicitly. Note: this task will not be triggered automatically if `productionMode` is set to false.

Most common commands for all projects:

* `./gradlew clean build` - builds the project and prepares the project for development. Automatically
  calls the `vaadinPrepareFrontend` task, but doesn't call the `vaadinBuildFrontend` task by default.
* `./gradlew clean vaadinPrepareFrontend` - quickly prepares the project for development.
* `./gradlew clean build -Pvaadin.productionMode` - will compile Vaadin in production mode,
   then packages everything into the war/jar archive. Automatically calls the
   `vaadinPrepareFrontend` and `vaadinBuildFrontend` tasks.

*Note* (after you built the project in production mode): In order to prepare the project
setup back to development mode, you must run `./gradlew vaadinPrepareFrontend`
with the `productionMode` effectively set to false (e.g. by ommitting the `-Pvaadin.productionMode` flag).

## Configuration

To configure the plugin, you can use the following snippet in your `build.gradle` file:

`build.gradle` in Groovy:
```groovy
vaadin {
  pnpmEnable = true
}
```

All configuration options follow. Note that you **RARELY** need to change anything of the below.

* `productionMode = false`: Whether or not the plugin should run in productionMode. Defaults to false.
  Responds to the `-Pvaadin.productionMode` property. You need to set this to `true` if you wish
  to build a production-ready war/jar artifact. If this is false, the `vaadinBuildFrontend`
  task is not triggered automatically by the build.
* `buildOutputDirectory = File(project.buildDir, "vaadin-generated")`: 
  The plugin will generate additional resource files here. These files need
  to be present on the classpath, in order for Vaadin to be
  able to run, both in dev mode and in the production mode.
  The plugin will automatically register
  this as an additional resource folder, which should then be picked up by the IDE.
  That will allow the app to run for example in Intellij with Tomcat.
  For example the `flow-build-info.json` goes here.
* `webpackOutputDirectory`: The folder where webpack should output index.js and other generated
  files. Defaults to `build/resources/main/META-INF/VAADIN/`.
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
* `pnpmEnable = false`: Instructs to use pnpm for installing npm frontend resources.
  pnpm, a.k.a. performant npm, is a better front-end dependency management option.
  With pnpm, packages are cached locally by default and linked (instead of
  downloaded) for every project. This results in reduced disk space usage
  and faster recurring builds when compared to npm.
* `requireHomeNodeExec = false`: Whether vaadin home node executable usage is forced. If it's set to
  `true` then vaadin home 'node' is checked and installed if it's
  absent. Then it will be used instead of globally 'node' or locally
  installed installed 'node'.
* `nodeVersion = "v12.16.0"`: The node.js version to be used when node.js is
  installed automatically by Vaadin
* `nodeDownloadRoot = "https://nodejs.org/dist/"`: Download node.js from this URL.
  Handy in heavily firewalled corporate environments where the node.js
  download can be provided from an intranet mirror.

## Automatic Download of node.js and npm/pnpm

Since Vaadin Gradle Plugin 0.7.0, you no longer need to have node.js nor
npm installed in your system in order to use Vaadin.
Vaadin will download the node.js and npm (and pnpm if `pnpmEnable` is true) and will place it
into the `$HOME/.vaadin` folder.

This functionality is triggered automatically,
you do not need to call a Gradle task nor configure your CI environment in a special way.

## Multi-project builds

It is important to apply this plugin only to projects building the final war/jar artifact. You can
achieve that by having the `com.vaadin` plugin in the `plugins{}` block not applied by default, then
applying the plugin only in the war project:

```groovy
plugins {
  id 'java'
  id "com.vaadin" version "0.7.0" apply false
}

project("lib") {
  apply plugin: 'java'
}
project("web") {
  apply plugin: 'war'
  apply plugin: "com.vaadin"
  dependencies {
    compile project(':lib')
  }
}
```

# Developing The Plugin

Please read the Gradle Tutorial on [Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
to understand how Gradle plugins are developed.

The main entry to the plugin is the `VaadinPlugin` class. When applied to the project, it will register
all necessary tasks and extensions into the project.

Use Intellij (Community is enough) to open the project.

## Running The IT/Functional Tests

There is a comprehensive test suite which tests the plugin in various generated projects.
To run all tests from the suite:

```bash
./gradlew check
```

That will run the `functionalTest` task which will run all tests from the `src/functionalTest` folder.

### Running Individual Functional Tests from Intellij

Just right-click the test class and select "Run". If running the test fails, try one of the following:

1. Use Intellij, Community edition is enough
2. Go to "File / Settings / Build, Execution, Deployment / Build Tools / Gradle" and make sure that
   "Run tests using" is set to "Gradle".

## Developing the plugin and testing it on-the-fly at the same time

You can take advantage of [composite builds](https://docs.gradle.org/current/userguide/composite_builds.html),
which will allow you to join together the plugin itself along with an example project using that plugin,
into one composite project. The easiest way is to use the [Base Starter Gradle](https://github.com/vaadin/base-starter-gradle)
example project.

1. Clone the Base Starter Gradle project and open it in Intellij
2. Create a `settings.gradle` file containing the following line: `includeBuild("/path/to/your/plugin/project/vaadin-gradle-plugin")`
   (use full path on your system to the Gradle Plugin project)
3. Reimport the Base Starter project: Gradle / Reimport. A new project named `vaadin-gradle-plugin`
   should appear in your workspace.
4. Open the terminal with Alt+F12.
5. If you now type `./gradlew vaadinPrepareFronend` into the command line, Gradle will compile any changes done to
   the Gradle plugin and will run updated code. You can verify that by adding `println()` statements
   into the `VaadinPrepareFrontendTask` class.

## License

This plugin is distributed under the Apache License 2.0 license. For more information about the license see the LICENSE file 
in the root directory of the repository. A signed CLA is required when contributing to the project.

See [CONTRIBUTING](CONTRIBUTING.md) for instructions for getting the plugin sources, and for compiling and using the plugin locally.
