/**
 *    Copyright 2000-2020 Vaadin Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import kotlin.test.expect

/**
 * @author mavi
 */
class VaadinSmokeTest {
    @Rule @JvmField
    val testProject = TemporaryFolder()

    private lateinit var testProjectDir: File
    private lateinit var buildFile: File

    @Before
    fun setup() {
        testProjectDir = testProject.root
        buildFile = File(testProjectDir, "build.gradle")
        buildFile.writeText("""
            plugins {
                id 'com.vaadin'
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
            }
        """)
    }

    @Test
    fun smoke() {
        build("vaadinClean")
    }

    @Test
    fun testPrepareNode() {
        build("vaadinPrepareNode")

        val nodejs = File(testProjectDir, "node")
        expect(true, nodejs.toString()) { nodejs.isDirectory }
    }

    @Test
    fun testPrepareFrontend() {
        build("vaadinPrepareNode", "vaadinPrepareFrontend")

        val generatedPackageJson = File(testProjectDir, "target/frontend/package.json")
        expect(true, generatedPackageJson.toString()) { generatedPackageJson.isFile }
        val generatedFlowBuildInfoJson = File(testProjectDir, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        expect(true, generatedFlowBuildInfoJson.toString()) { generatedPackageJson.isFile }
    }

    @Test
    fun testBuildFrontend() {
        val result: BuildResult = build("vaadinPrepareNode", "vaadinBuildFrontend")
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProjectDir, "build/vaadin-generated/META-INF/VAADIN/build")
        expect(true, build.toString()) { build.isDirectory }
        expect(true) { build.listFiles()!!.isNotEmpty() }
        val webcomponentsjs = File(build, "webcomponentsjs")
        expect(true, webcomponentsjs.toString()) { webcomponentsjs.isDirectory }
        expect(true) { webcomponentsjs.listFiles()!!.isNotEmpty() }
    }

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
        expectArchiveContainsVaadinWebpackBundle(war)
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
        expectArchiveContainsVaadinWebpackBundle(jar)
    }

    /**
     * Runs build on [testProjectDir]; a `build.gradle` [buildFile] is expected
     * to be located there.
     *
     * The function checks that all tasks have succeeded; if not, throws an informative exception.
     */
    private fun build(vararg tasks: String): BuildResult {
        val result: BuildResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(tasks.toList() + "--stacktrace")
                .withPluginClasspath()
                .build()

        for (task: String in tasks) {
            result.expectTaskSucceded(task)
        }
        return result
    }
}
