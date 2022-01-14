package com.vaadin.gradle

import com.github.mvysny.dynatest.DynaNodeDsl
import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.expectList
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.expect

@DynaNodeDsl
fun DynaNodeGroup.singleModuleTests(gradleVersion: GradleVersion) {
    val compile = gradleVersion.compile
    val testProject: TestProject by withTestProject(gradleVersion)

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/26
     */
    if (gradleVersion.supportsVaadin8Plugin) {
        test("testVaadin8Vaadin14MPRProject") {
            testProject.buildFile.writeText("""
                plugins {
                    id "com.devsoap.plugin.vaadin" version "${gradleVersion.vaadin8PluginVersion}"
                    id 'com.vaadin'
                }
                repositories {
                    mavenCentral()
                    maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                }
                // test that we can configure both plugins
                vaadin {
                    version = "8.9.4"
                }
                vaadin14 {
                    optimizeBundle = true
                }""".trimIndent()
            )

            // the collision between devsoap's `vaadin` extension and com.vaadin's `vaadin`
            // extension would crash even this very simple build.
            testProject.build("tasks")
        }
    }

    /**
     * This test covers the [Base Starter Gradle](https://github.com/vaadin/base-starter-gradle)
     * example project.
     */
    test("testWarProjectDevelopmentMode") {
        testProject.buildFile.writeText("""
            plugins {
                id 'war'
                id 'org.gretty' version '3.0.1'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")

                // logging
                // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
                $compile("org.slf4j:slf4j-simple:1.7.30")
            }
            vaadin {
                pnpmEnable = true
            }
        """.trimIndent())

        val build: BuildResult = testProject.build("clean", "build")
        // vaadinBuildFrontend should NOT have been executed automatically
        build.expectTaskNotRan("vaadinBuildFrontend")

        expectArchiveDoesntContainVaadinWebpackBundle(testProject.builtWar, false)
    }

    /**
     * This test covers the [Base Starter Gradle](https://github.com/vaadin/base-starter-gradle)
     * example project.
     */
    test("testWarProjectProductionMode") {
        testProject.buildFile.writeText("""
            plugins {
                id 'war'
                id 'org.gretty' version '3.0.1'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            vaadin {
                pnpmEnable = true
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")

                // logging
                // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
                $compile("org.slf4j:slf4j-simple:1.7.30")
            }
        """.trimIndent())

        val build: BuildResult = testProject.build("-Pvaadin.productionMode", "clean", "build")
        // vaadinBuildFrontend should have been executed automatically
        build.expectTaskSucceded("vaadinBuildFrontend")

        expectArchiveContainsVaadinWebpackBundle(testProject.builtWar, false)
    }

    /**
     * This test covers the https://github.com/mvysny/vaadin14-embedded-jetty-gradle example.
     */
    test("testJarProjectDevelopmentMode") {
        testProject.buildFile.writeText("""
            plugins {
                id 'java'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            def jettyVersion = "9.4.20.v20190813"
            vaadin {
                pnpmEnable = true
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
            
                $compile("javax.servlet:javax.servlet-api:3.1.0")
                $compile("org.eclipse.jetty:jetty-continuation:${"$"}{jettyVersion}")
                $compile("org.eclipse.jetty:jetty-server:${"$"}{jettyVersion}")
                $compile("org.eclipse.jetty.websocket:websocket-server:${"$"}{jettyVersion}")
                $compile("org.eclipse.jetty.websocket:javax-websocket-server-impl:${"$"}{jettyVersion}") {
                    exclude(module: "javax.websocket-client-api")
                }
            
                // logging
                // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
                $compile("org.slf4j:slf4j-simple:1.7.30")
            }
        """.trimIndent())

        val build: BuildResult = testProject.build("clean", "build")
        // vaadinBuildFrontend should NOT have been executed automatically
        expect(null) { build.task(":vaadinBuildFrontend") }

        expectArchiveDoesntContainVaadinWebpackBundle(testProject.builtJar, false)
    }

    /**
     * This test covers the https://github.com/mvysny/vaadin14-embedded-jetty-gradle example.
     */
    test("JarProjectProductionMode") {
        testProject.buildFile.writeText("""
            plugins {
                id 'java'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            def jettyVersion = "9.4.20.v20190813"
            vaadin {
                pnpmEnable = true
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
            
                $compile("javax.servlet:javax.servlet-api:3.1.0")
                $compile("org.eclipse.jetty:jetty-continuation:${"$"}{jettyVersion}")
                $compile("org.eclipse.jetty:jetty-server:${"$"}{jettyVersion}")
                $compile("org.eclipse.jetty.websocket:websocket-server:${"$"}{jettyVersion}")
                $compile("org.eclipse.jetty.websocket:javax-websocket-server-impl:${"$"}{jettyVersion}") {
                    exclude(module: "javax.websocket-client-api")
                }
            
                // logging
                // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
                $compile("org.slf4j:slf4j-simple:1.7.30")
            }
        """.trimIndent())

        val build: BuildResult = testProject.build("-Pvaadin.productionMode", "clean", "build")
        build.expectTaskSucceded("vaadinPrepareFrontend")
        build.expectTaskSucceded("vaadinBuildFrontend")

        expectArchiveContainsVaadinWebpackBundle(testProject.builtJar, false)
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/24
     *
     * The `implementation()` dependency type would cause incorrect jar list computation,
     * which would then not populate the `node_modules/@vaadin/flow-frontend` folder,
     * which would case webpack to fail during vaadinBuildFrontend.
     *
     * This build script covers the [Spring Boot example](https://github.com/vaadin/base-starter-spring-gradle)
     */
    test("Vaadin14SpringProjectProductionMode") {
        testProject.buildFile.writeText("""
            plugins {
                id 'org.springframework.boot' version '${gradleVersion.springBootPlugin}'
                id 'io.spring.dependency-management' version '1.0.9.RELEASE'
                id 'java'
                id("com.vaadin")
            }
            
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            
            ext {
                set('vaadinVersion', "$vaadin14Version")
            }
            
            configurations {
                developmentOnly
                runtimeClasspath {
                    extendsFrom developmentOnly
                }
            }
            
            dependencies {
                implementation('com.vaadin:vaadin-spring-boot-starter') {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
                developmentOnly 'org.springframework.boot:spring-boot-devtools'
                testImplementation('org.springframework.boot:spring-boot-starter-test') {
                    exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
                }
            }
            
            dependencyManagement {
                imports {
                    mavenBom "com.vaadin:vaadin-bom:${"$"}{vaadinVersion}"
                }
            }

            vaadin {
                pnpmEnable = true
            }
            
            ${if (gradleVersion.springBootPlugin.startsWith("2.5")) "jar { enabled = false }" else ""}
        """)
        // jar { enabled = false } :
        // See https://stackoverflow.com/questions/67663728/spring-boot-2-5-0-generates-plain-jar-file-can-i-remove-it

        // need to create the Application.java file otherwise bootJar will fail
        val appPkg = File(testProject.dir, "src/main/java/com/example/demo")
        Files.createDirectories(appPkg.toPath())
        File(appPkg, "DemoApplication.java").writeText("""
            package com.example.demo;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class DemoApplication {
            
                public static void main(String[] args) {
                    SpringApplication.run(DemoApplication.class, args);
                }
            
            }
        """.trimIndent())

        val build: BuildResult = testProject.build("-Pvaadin.productionMode", "build")
        build.expectTaskSucceded("vaadinPrepareFrontend")
        build.expectTaskSucceded("vaadinBuildFrontend")

        expectArchiveContainsVaadinWebpackBundle(testProject.builtJar, true)
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/42
     */
    test("CircularDepsBug") {
        testProject.buildFile.writeText("""
            plugins {
                id 'war'
                id 'org.gretty' version '3.0.1'
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")

                // logging
                // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
                $compile("org.slf4j:slf4j-simple:1.7.30")
            }
            
            sourceSets {
              guiceConfig
            }

            configurations {
              guiceConfigCompile.extendsFrom compile
            }

            dependencies {
              // This seems to be a problem with the vaadin-gradle-plugin, but we need this
              // to have access to classes of the main sourceSet in the guice sourceSet.
              guiceConfigCompile sourceSets.main.output
            }

            compileGuiceConfigJava {
              options.compilerArgs << "-Xlint:all"
              options.compilerArgs << "-Xlint:-serial"
            }

            jar {
              from sourceSets.guiceConfig.output
            }

            vaadin {
                pnpmEnable = true
            }
        """.trimIndent())

        val build: BuildResult = testProject.build("-Pvaadin.productionMode", "clean", "build")
        build.expectTaskSucceded("vaadinPrepareFrontend")
        build.expectTaskSucceded("vaadinBuildFrontend")

        expectArchiveContainsVaadinWebpackBundle(testProject.builtWar, false)
    }

    /**
     * https://github.com/vaadin/vaadin-gradle-plugin/issues/76
     */
    test("NodeDownload") {
        testProject.buildFile.writeText("""
            plugins {
                id 'com.vaadin'
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
            }
            vaadin {
                requireHomeNodeExec = true
                nodeVersion = "v12.10.0"
                nodeDownloadRoot = "http://localhost:8080/non-existent"
            }
        """)

        val result: BuildResult
        
        // Vaadin downloads the node to ${user.home}/.vaadin.
        // Set user.home to be the testProject root directory
        val originalUserHome = System.getProperty("user.home")
        System.setProperty("user.home", testProject.dir.absolutePath)

        try {
            result = testProject.buildAndFail("vaadinPrepareFrontend", "-Duser.home=${testProject.dir.absolutePath}")
        } finally {
            // Return original user home value
            System.setProperty("user.home", originalUserHome)
        }
        // the task should fail to download the node.js
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.FAILED)
        expect(true, result.output) { result.output.contains("Could not download http://localhost:8080/v12.10.0/") }
        expect(true, result.output) { result.output.contains("Could not download Node.js") }
    }

    /**
     * Fixes https://github.com/vaadin/vaadin-gradle-plugin/issues/115
     */
    test("skipNonJarDependencies") {
        testProject.buildFile.writeText("""
            plugins {
                id 'com.vaadin'
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
                implementation("com.microsoft.sqlserver:mssql-jdbc_auth:8.4.0.x64")
            }
        """)
        val output = testProject.build("vaadinPrepareFrontend").output
        expect(false, output) { output.contains("could not create Vfs.Dir from url") }
    }

    test("include/exclude") {
        testProject.buildFile.writeText("""
            plugins {
                id 'com.vaadin'
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
            }
            vaadin {
                pnpmEnable = true
                filterClasspath {
                    include("com.vaadin:flow-*")
                    exclude("com.vaadin:flow-data")
                    exclude("com.vaadin:flow-dnd")
                }
            }
        """)

        val output = testProject.build("vaadinPrepareFrontend").output
        val classpathLines = output.lines().filter { it.startsWith("Passing this classpath to NodeTasks.Builder") }
        expect(1, output) { classpathLines.size }
        // parse the list of jars out of the classpath line
        val classpath = classpathLines[0].dropWhile { it != '[' } .trim('[', ']') .split(',')
            .map { it.trim() } .sorted()
        // remove version numbers to make the test more stable: drop -2.7.4.jar from flow-dnd-2.7.4.jar
        expectList("flow-client-", "flow-html-components-", "flow-lit-template-", "flow-push-", "flow-server-") { classpath.map { it.dropLastWhile { it != '-' } } }
    }

    if (gradleVersion.major >= 6) {
        // Kotlin-based build scripts only work on Gradle 6+
        test("include/exclude kotlin") {
            testProject.buildFileKts.writeText(
                """
            plugins {
                id("com.vaadin")
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                // Vaadin 14
                $compile("com.vaadin:vaadin-core:$vaadin14Version") {
                    // Webjars are only needed when running in Vaadin 13 compatibility mode
                    listOf("com.vaadin.webjar", "org.webjars.bowergithub.insites",
                            "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                            "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents")
                            .forEach { exclude(group = it) }
                }
            }
            vaadin {
                pnpmEnable = true
                filterClasspath {
                    include("com.vaadin:flow-*")
                    exclude("com.vaadin:flow-data")
                    exclude("com.vaadin:flow-dnd")
                }
            }
        """
            )

            val output = testProject.build("vaadinPrepareFrontend").output
            val classpathLines = output.lines()
                .filter { it.startsWith("Passing this classpath to NodeTasks.Builder") }
            expect(1, output) { classpathLines.size }
            // parse the list of jars out of the classpath line
            val classpath = classpathLines[0].dropWhile { it != '[' }.trim('[', ']')
                .split(',')
                .map { it.trim() }.sorted()
            // remove version numbers to make the test more stable: drop -2.7.4.jar from flow-dnd-2.7.4.jar
            expectList(
                "flow-client-",
                "flow-html-components-",
                "flow-lit-template-",
                "flow-push-",
                "flow-server-"
            ) { classpath.map { it.dropLastWhile { it != '-' } } }
        }
    }
}
