package com.vaadin.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import kotlin.test.expect
import kotlin.test.fail

/**
 * Expects that given task succeeded. If not, fails with an informative exception.
 * @param taskName the name of the task, e.g. `vaadinPrepareNode`
 */
fun BuildResult.expectTaskSucceded(taskName: String) {
    val task: BuildTask = task(":$taskName") ?: fail("Task $taskName was not ran\n$output")
    expect(TaskOutcome.SUCCESS, "$taskName did not succeed: ${task.outcome}") {
        task.outcome
    }
}

/**
 * Finds all files matching given [glob] pattern, for example `build/libs/ *.war`
 * @param expectedCount expected number of files, defaults to 1.
 */
fun File.find(glob: String, expectedCount: Int = 1): List<File> {
    val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:$absolutePath/$glob")
    val found: List<File> = absoluteFile.walk()
            .filter { matcher.matches(it.toPath()) }
            .toList()
    if (found.size != expectedCount) {
        fail("Expected $expectedCount $glob but found ${found.size}: $found")
    }
    return found
}
