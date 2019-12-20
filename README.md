# Vaadin Gradle Plugin

This is an experimental Vaadin Gradle Plugin for Vaadin 14 and newer. A stable version for older Vaadin versions can be found [here](https://devsoap.com/gradle-vaadin-flow-plugin/).

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

## License

This plugin is distributed under the Apache License 2.0 license. For more information about the license see the LICENSE file 
in the root directory of the repository. A signed CLA is required when contributing to the project.

See [CONTRIBUTING](CONTRIBUTING.md) for instructions for getting the plugin sources, and for compiling and using the plugin locally.
