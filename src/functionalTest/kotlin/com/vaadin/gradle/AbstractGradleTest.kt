package com.vaadin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Prepares a test Gradle project - creates a temp dir for the [testProject] and allow you to run gradle
 * tasks. Does not generate `build.gradle` for you
 * though - just write the `build.gradle` contents to the [buildFile] instead.
 *
 * Call [build] to run the Gradle build on the test project.
 * @author mavi
 */
abstract class AbstractGradleTest {

    /**
     * The testing Gradle project. Automatically deleted after every test.
     */
    @Rule
    @JvmField
    val testProject = TemporaryFolder()

    /**
     * The testing Gradle project root.
     */
    protected val testProjectDir: File get() = testProject.root
    protected val buildFile: File get() = File(testProjectDir, "build.gradle")
    protected val settingsFile: File get() = File(testProjectDir, "settings.gradle")

    /**
     * Runs build on [testProjectDir]; a `build.gradle` [buildFile] is expected
     * to be located there.
     *
     * The function checks that all tasks have succeeded; if not, throws an informative exception.
     */
    protected fun build(vararg tasks: String): BuildResult {
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
