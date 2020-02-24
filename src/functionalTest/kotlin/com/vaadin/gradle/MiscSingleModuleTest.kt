package com.vaadin.gradle

import org.gradle.testkit.runner.BuildResult
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.expect

class MiscSingleModuleTest : AbstractGradleTest() {
    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/26
     */
    @Test
    fun testVaadin8Vaadin14MPRProject() {
        buildFile.writeText("""
            plugins {
                id "com.devsoap.plugin.vaadin" version "1.4.1"
                id 'com.vaadin'
            }
            repositories {
                jcenter()
            }
            // test that we can configure both plugins
            vaadin {
                version = "8.9.4"
            }
            vaadin14 {
                optimizeBundle = true
            }
        """)

        // the collision between devsoap's `vaadin` extension and com.vaadin's `vaadin`
        // extension would crash even this very simple build.
        build("tasks")
    }

    /**
     * This test covers the [Base Starter Gradle](https://github.com/vaadin/base-starter-gradle)
     * example project.
     */
    @Test
    fun testWarProject() {
        buildFile.writeText("""
            plugins {
                id 'war'
                id 'org.gretty' version '3.0.1'
                id("com.vaadin")
            }
            repositories {
                jcenter()
            }
            vaadin {
                optimizeBundle = true
            }
            dependencies {
                // Vaadin 14
                compile("com.vaadin:vaadin-core:14.1.16") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")

                // logging
                // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
        """.trimIndent())

        val build: BuildResult = build("clean", "vaadinPrepareNode", "vaadinBuildFrontend", "build")

        val war: File = testProjectDir.find("build/libs/*.war").first()
        expect(true, "$war is missing\n${build.output}") { war.isFile }
        expectArchiveContainsVaadinWebpackBundle(war, false)
    }

    /**
     * This test covers the https://github.com/mvysny/vaadin14-embedded-jetty-gradle example.
     */
    @Test
    fun testJarProject() {
        buildFile.writeText("""
            plugins {
                id 'java'
                id("com.vaadin")
            }
            repositories {
                jcenter()
            }
            def jettyVersion = "9.4.20.v20190813"
            vaadin {
                optimizeBundle = true
            }
            dependencies {
                // Vaadin 14
                compile("com.vaadin:vaadin-core:14.1.16") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
            
                compile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.eclipse.jetty:jetty-continuation:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty:jetty-server:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty.websocket:websocket-server:${"$"}{jettyVersion}")
                compile("org.eclipse.jetty.websocket:javax-websocket-server-impl:${"$"}{jettyVersion}") {
                    exclude(module: "javax.websocket-client-api")
                }
            
                // logging
                // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
                compile("org.slf4j:slf4j-simple:1.7.30")
            }
        """.trimIndent())

        val build: BuildResult = build("clean", "vaadinBuildFrontend", "build")

        val jar: File = testProjectDir.find("build/libs/*.jar").first()
        expect(true, "$jar is missing\n${build.output}") { jar.isFile }
        expectArchiveContainsVaadinWebpackBundle(jar, false)
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
    @Test
    fun testVaadin14SpringProject() {
        buildFile.writeText("""
            plugins {
                id 'org.springframework.boot' version '2.2.4.RELEASE'
                id 'io.spring.dependency-management' version '1.0.9.RELEASE'
                id 'java'
                id("com.vaadin")
            }
            
            repositories {
                mavenCentral()
            }
            
            ext {
                set('vaadinVersion', "14.1.16")
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
        """)

        // need to create the Application.java file otherwise bootJar will fail
        val appPkg = File(testProjectDir, "src/main/java/com/example/demo")
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

        val build: BuildResult = build("vaadinPrepareNode", "vaadinBuildFrontend", "build")

        val jar: File = testProjectDir.find("build/libs/*.jar").first()
        expect(true, "$jar is missing\n${build.output}") { jar.isFile }
        expectArchiveContainsVaadinWebpackBundle(jar, true)
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/42
     */
    @Test
    fun testCircularDepsBug() {
        buildFile.writeText("""
            plugins {
                id 'war'
                id 'org.gretty' version '3.0.1'
                id("com.vaadin")
            }
            repositories {
                jcenter()
            }
            dependencies {
                // Vaadin 14
                compile("com.vaadin:vaadin-core:14.1.16") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")

                // logging
                // currently we are logging through the SLF4J API to SLF4J-Simple. See src/main/resources/simplelogger.properties file for the logger configuration
                compile("org.slf4j:slf4j-simple:1.7.30")
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
        """.trimIndent())

        val build: BuildResult = build("clean", "vaadinPrepareNode", "vaadinBuildFrontend", "build")

        val war: File = testProjectDir.find("build/libs/*.war").first()
        expect(true, "$war is missing\n${build.output}") { war.isFile }
        expectArchiveContainsVaadinWebpackBundle(war, false)
    }
}
