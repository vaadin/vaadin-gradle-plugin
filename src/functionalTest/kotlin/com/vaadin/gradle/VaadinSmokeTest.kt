package com.vaadin.gradle

import org.junit.Assert.*
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * @author mavi
 */
class VaadinSmokeTest {
    @Rule @JvmField
    val testProjectDir = TemporaryFolder()

    private lateinit var buildFile: File

    @Before
    fun setup() {
        buildFile = testProjectDir.newFile("build.gradle")
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
                .withProjectDir(testProjectDir.root)
                .withArguments("vaadinClean", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":vaadinClean")!!.outcome)
    }

    @Test
    fun testPrepareNode() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("vaadinPrepareNode", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":vaadinPrepareNode")!!.outcome)
        val nodejs = File(testProjectDir.root, "node")
        assertTrue(nodejs.toString(), nodejs.exists())
        assertTrue(nodejs.toString(), nodejs.isDirectory())
    }

    @Test
    fun testPrepareFrontend() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("vaadinPrepareNode", "vaadinPrepareFrontend", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":vaadinPrepareNode")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":vaadinPrepareFrontend")!!.outcome)
        val generatedPackageJson = File(testProjectDir.root, "target/frontend/package.json")
        assertTrue(generatedPackageJson.toString(), generatedPackageJson.isFile())
        val generatedFlowBuildInfoJson = File(testProjectDir.root, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        assertTrue(generatedFlowBuildInfoJson.toString(), generatedPackageJson.isFile())
    }

    @Test
    fun testBuildFrontend() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments("vaadinPrepareNode", "vaadinBuildFrontend", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":vaadinPrepareNode")!!.outcome)
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        assertEquals(TaskOutcome.SUCCESS, result.task(":vaadinPrepareFrontend")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":vaadinBuildFrontend")!!.outcome)
        val build = File(testProjectDir.root, "build/vaadin-generated/META-INF/VAADIN/build")
        assertTrue(build.toString(), build.isDirectory())
        assertTrue(build.listFiles()!!.isNotEmpty())
        val webcomponentsjs = File(build, "webcomponentsjs")
        assertTrue(webcomponentsjs.toString(), webcomponentsjs.isDirectory())
        assertTrue(webcomponentsjs.listFiles()!!.isNotEmpty())
    }
}
