# Vaadin 14 Gradle Plugin

This is an unofficial Vaadin Gradle Plugin supporting Vaadin 14 only.

## Official VS Unofficial Vaadin Gradle Plugins

There are two Vaadin Gradle Plugins:
* Official Vaadin Gradle plugin only works with Vaadin 19+ and is part of the [Vaadin Flow Github repo](https://github.com/vaadin/flow/).
* Unofficial (this one) which only works with Vaadin 14.

This plugin only supports Vaadin 14. This plugin is developed by Vaadin employees,
but it is only offered as a gesture of good will - it's not an official Vaadin offering.
Official Vaadin Gradle plugin only works for Vaadin 19+ and is part of the [Vaadin Flow Github repo](https://github.com/vaadin/flow/).

Note that both this plugin and the official Vaadin Gradle Plugin are deployed to
the same plugin space: [com.vaadin at plugins.gradle.org](https://plugins.gradle.org/plugin/com.vaadin).
The unofficial plugin version numbering starts with a zero: `0.*`.

### Official Plugin (not this one)

More links for the official Vaadin Gradle plugin:
* [README](https://github.com/vaadin/flow/tree/master/flow-plugins/flow-gradle-plugin)
* [Vaadin 19+ documentation: Starting a Gradle project](https://vaadin.com/docs/latest/guide/start/gradle)
* [Bug tracker](https://github.com/vaadin/flow/issues)

## Features

Compared to Maven plugin, there are the following limitations:

* Vaadin 14 Compatibility mode is not supported
* Migration from Vaadin 13 to Vaadin 14 is not supported.

Prerequisites:
* Java 8 or higher
* node.js and npm. Vaadin will now automatically install node.js and npm, but you can also install those locally:
  * Windows/Mac: [node.js Download site](https://nodejs.org/en/download/)
  * Linux: Use package manager e.g. `sudo apt install npm` 

As opposed to the older version of Gradle plugin, the new plugin doesn't create
projects any more. We plan to support Gradle projects via [start.vaadin.com](https://start.vaadin.com)
at some point. In the meantime, refer to project examples that you can use
as a basis for your Vaadin modules.

## Getting Started

Please see the [Vaadin 14 documentation: Starting a Gradle Project](https://vaadin.com/docs/v14/guide/start/gradle)
for more details.

Check out the example project setups for basic WAR project and Spring Boot:

* [Basic WAR project](https://github.com/vaadin/base-starter-gradle)
* [Spring Boot project](https://github.com/vaadin/base-starter-spring-gradle)
* [karibu-dsl example app](https://github.com/mvysny/karibu10-helloworld-application) (uses .kts Kotlin Gradle build script)
* [Using the plugin in Gradle multi-project setup: vok-helloworld-app](https://github.com/mvysny/vok-helloworld-app) (uses .kts Kotlin Gradle build script)

To include the plugin in your project, simply add the plugin into the `plugins{}`
section of your `build.gradle`: 

```
plugins {
    id 'com.vaadin' version '0.14.6.0'
}
```

Compatibility chart:

| Vaadin Gradle Plugin version | Supports |
|------------------------------|----------|
| -                            | Vaadin 13 and lower are unsupported |
| 0.6.0 and lower              | Vaadin 14.1.x and lower |
| 0.7.0                        | Vaadin 14.2.x |
| 0.8.0                        | Vaadin 14.3.x and higher |
| 0.14.3.7                     | Vaadin 14.3.x and higher |
| 0.14.5.1                     | Vaadin 14.5.x and higher |
| 0.14.6.0                     | Vaadin 14.6.x and higher |
| -                            | Vaadin 15 and higher are unsupported by this unofficial plugin |

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
  pnpmEnable = false // false is the default, this is just an example
}
```

All configuration options follow. With the exception of the `productionMode` setting,
all other settings are auto-configured by the Plugin with sensible defaults and
should not be changed, otherwise weird JavaScript toolchain-related bugs might occur:

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
* `pnpmEnable = false` (since 0.7.0): Instructs to use pnpm for installing npm frontend resources.
  pnpm, a.k.a. performant npm, is a better front-end dependency management option.
  With pnpm, packages are cached locally by default and linked (instead of
  downloaded) for every project. This results in reduced disk space usage
  and faster recurring builds when compared to npm.
* `requireHomeNodeExec = false` (since 0.7.0): Whether vaadin home node executable usage is forced. If it's set to
  `true` then vaadin home 'node' is checked and installed if it's
  absent. Then it will be used instead of globally 'node' or locally
  installed installed 'node'.
* `nodeVersion = "v12.16.0"` (since 0.8.0): The node.js version to be used when node.js is
  installed automatically by Vaadin
* `nodeDownloadRoot = "https://nodejs.org/dist/"` (since 0.8.0): Download node.js from this URL.
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
  id "com.vaadin" version "0.8.0" apply false
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

## IDE Support

Intellij support for projects using Gradle and Vaadin Gradle Plugin is excellent.

There's a known issue with Eclipse and VSCode. Eclipse+BuildShip may need a workaround
in order for Gradle projects to work, please see [https://vaadin.com/forum/thread/18241436](https://vaadin.com/forum/thread/18241436) for more info.
This applies to Visual Studio Code (VSCode) as well since it also uses Eclipse bits and BuildShip
underneath - see [https://github.com/mvysny/vaadin14-embedded-jetty-gradle/issues/4](https://github.com/mvysny/vaadin14-embedded-jetty-gradle/issues/4)
for more details.

## Developing The Plugin

See [CONTRIBUTING](CONTRIBUTING.md) for instructions for getting the plugin sources, and for compiling and using the plugin locally.

## License

This plugin is distributed under the Apache License 2.0 license. For more information about the license see the [LICENSE](LICENSE) file. 
