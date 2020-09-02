package com.vaadin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
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

    // find the newest Vaadin version here: https://github.com/vaadin/platform/tags
    val vaadin17Version = "17.0.0"

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
     * The function by default checks that all tasks have succeeded; if not, throws an informative exception.
     * You can suppress this functionality by setting [checkTasksSuccessful] to false.
     */
    protected fun build(vararg args: String, checkTasksSuccessful: Boolean = true): BuildResult {
        println("$testProjectDir/./gradlew ${args.joinToString(" ")}")
        val result: BuildResult = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(args.toList() + "--stacktrace" + "--info")
                .withPluginClasspath()
                .withDebug(true)
                .forwardOutput()   // a must, otherwise ./gradlew check freezes on windows!
                .withGradleVersion("5.0")
                .build()

        if (checkTasksSuccessful) {
            for (arg: String in args) {
                val isTask: Boolean = !arg.startsWith("-")
                if (isTask) {
                    result.expectTaskSucceded(arg)
                }
            }
        }
        return result
    }

    @Before
    fun dumpEnvironment() {
        println("Test project directory: $testProjectDir")
    }
}
