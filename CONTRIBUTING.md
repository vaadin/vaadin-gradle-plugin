# Developing The Plugin

Please read the Gradle Tutorial on [Developing Custom Gradle Plugins](https://docs.gradle.org/current/userguide/custom_plugins.html)
to understand how Gradle plugins are developed.

The main entry to the plugin is the `VaadinPlugin` class. When applied to the project, it will register
all necessary tasks and extensions into the project.

Use Intellij (Community is enough) to open the project.

## Contributing

A signed CLA is required when contributing to the project.

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

# Releasing

TBW
