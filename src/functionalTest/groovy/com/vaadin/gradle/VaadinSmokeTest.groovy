package com.vaadin.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.junit.Assert.*

/**
 * A very simple tests testing only the most basic functionality.
 * @author mavi
 */
class VaadinSmokeTest {
    @Rule
    public TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
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
        """
    }

    @Test
    void smoke() {
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('vaadinClean', '--stacktrace')
                .withPluginClasspath()
                .build()

        assertEquals(SUCCESS, result.task(":vaadinClean").outcome)
    }

    @Test
    void testPrepareNode() {
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('vaadinPrepareNode')
                .withPluginClasspath()
                .build()

        assertEquals(SUCCESS, result.task(":vaadinPrepareNode").outcome)
        def nodejs = new File(testProjectDir.root, "node")
        assertTrue(nodejs.toString(), nodejs.exists())
        assertTrue(nodejs.toString(), nodejs.isDirectory())
    }

    @Test
    void testPrepareFrontend() {
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('vaadinPrepareNode', 'vaadinPrepareFrontend', '--stacktrace')
                .withPluginClasspath()
                .build()

        assertEquals(SUCCESS, result.task(":vaadinPrepareNode").outcome)
        assertEquals(SUCCESS, result.task(":vaadinPrepareFrontend").outcome)
        def generatedPackageJson = new File(testProjectDir.root, "target/frontend/package.json")
        assertTrue(generatedPackageJson.toString(), generatedPackageJson.isFile())
        def generatedFlowBuildInfoJson = new File(testProjectDir.root, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        assertTrue(generatedFlowBuildInfoJson.toString(), generatedPackageJson.isFile())
    }

    @Test
    void testBuildFrontend() {
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('vaadinPrepareNode', 'vaadinBuildFrontend', '--stacktrace')
                .withPluginClasspath()
                .build()

        assertEquals(SUCCESS, result.task(":vaadinPrepareNode").outcome)
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        assertEquals(SUCCESS, result.task(":vaadinPrepareFrontend").outcome)
        assertEquals(SUCCESS, result.task(":vaadinBuildFrontend").outcome)
        def build = new File(testProjectDir.root, "build/vaadin-generated/META-INF/VAADIN/build")
        assertTrue(build.toString(), build.isDirectory())
        assertTrue(build.listFiles().size() > 0)
        def webcomponentsjs = new File(build, "webcomponentsjs")
        assertTrue(webcomponentsjs.toString(), webcomponentsjs.isDirectory())
        assertTrue(webcomponentsjs.listFiles().size() > 0)
    }
}
