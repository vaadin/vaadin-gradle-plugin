package com.vaadin.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static junit.framework.Assert.assertTrue
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author mavi
 */
class VaadinCleanTest {
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
        """
    }

    @Test
    void smoke() {
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('vaadinClean')
                .withPluginClasspath()
                .build()

        result.task(":vaadinClean").outcome == SUCCESS
    }

    @Test
    void downloadNodeHa() {
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('vaadinPrepareNode')
                .withPluginClasspath()
                .build()

        result.task(":vaadinPrepareNode").outcome == SUCCESS
        println testProjectDir.root.listFiles().toList()
        def nodejs = new File(testProjectDir.root, "node")
        assertTrue(nodejs.toString(), nodejs.exists())
        assertTrue(nodejs.toString(), nodejs.isDirectory())
    }
}
