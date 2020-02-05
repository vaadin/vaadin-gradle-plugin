package com.vaadin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
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
        val result: BuildResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("vaadinClean", "--stacktrace")
                .withPluginClasspath()
                .build()

        expect(TaskOutcome.SUCCESS) { result.task(":vaadinClean")!!.outcome }
    }

    @Test
    fun testPrepareNode() {
        val result: BuildResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("vaadinPrepareNode", "--stacktrace")
                .withPluginClasspath()
                .build()

        expect(TaskOutcome.SUCCESS) { result.task(":vaadinPrepareNode")!!.outcome }
        val nodejs = File(testProjectDir, "node")
        expect(true, nodejs.toString()) { nodejs.isDirectory }
    }

    @Test
    fun testPrepareFrontend() {
        val result: BuildResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("vaadinPrepareNode", "vaadinPrepareFrontend", "--stacktrace")
                .withPluginClasspath()
                .build()

        expect(TaskOutcome.SUCCESS) { result.task(":vaadinPrepareNode")!!.outcome }
        expect(TaskOutcome.SUCCESS) { result.task(":vaadinPrepareFrontend")!!.outcome }
        val generatedPackageJson = File(testProjectDir, "target/frontend/package.json")
        expect(true, generatedPackageJson.toString()) { generatedPackageJson.isFile }
        val generatedFlowBuildInfoJson = File(testProjectDir, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        expect(true, generatedFlowBuildInfoJson.toString()) { generatedPackageJson.isFile }
    }

    @Test
    fun testBuildFrontend() {
        val result: BuildResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("vaadinPrepareNode", "vaadinBuildFrontend", "--stacktrace")
                .withPluginClasspath()
                .build()

        expect(TaskOutcome.SUCCESS) { result.task(":vaadinPrepareNode")!!.outcome }
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        expect(TaskOutcome.SUCCESS) { result.task(":vaadinPrepareFrontend")!!.outcome }
        expect(TaskOutcome.SUCCESS) { result.task(":vaadinBuildFrontend")!!.outcome }
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
        val result: BuildResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--stacktrace")
                .withPluginClasspath()
                .build()

        expect(TaskOutcome.SUCCESS) { result.task(":tasks")!!.outcome }
    }
}
