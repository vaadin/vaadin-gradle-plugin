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
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.util.zip.ZipInputStream
import kotlin.test.expect
import kotlin.test.fail

/**
 * Expects that given task succeeded. If not, fails with an informative exception.
 * @param taskName the name of the task, e.g. `vaadinPrepareNode`
 */
fun BuildResult.expectTaskSucceded(taskName: String) {
    val task: BuildTask = task(":$taskName")
            ?: fail("Task $taskName was not ran\n$output")
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

/**
 * Converts glob such as `*.jar` into a Regex which matches such files.
 */
private fun String.globToRegex(): Regex =
        Regex(this.replace("?", "[^/]?").replace("*", "[^/]*"))

/**
 * Lists all files in this zip archive, e.g. `META-INF/VAADIN/config/stats.json`.
 */
private fun ZipInputStream.fileNameSequence(): Sequence<String> =
        generateSequence { nextEntry?.name }

/**
 * Lists all files in this zip archive, e.g. `META-INF/VAADIN/config/stats.json`.
 */
private fun File.zipListAllFiles(): List<String> =
        ZipInputStream(this.inputStream().buffered()).use { zin: ZipInputStream ->
            zin.fileNameSequence().toList()
        }

/**
 * Expects that given archive contains at least one file matching every glob in the [globs] list.
 * @param archiveProvider returns the zip file to examine.
 */
fun expectArchiveContains(vararg globs: String, archiveProvider: () -> File) {
    val archive: File = archiveProvider()
    val allFiles: List<String> = archive.zipListAllFiles()

    globs.forEach { glob: String ->
        val regex: Regex = glob.globToRegex()
        val someFileMatch: Boolean = allFiles.any { it.matches(regex) }
        expect(true, "No file $glob in $archive, found ${allFiles.joinToString("\n")}") { someFileMatch }
    }
}

/**
 * Asserts that given archive (jar/war) contains the Vaadin webpack bundle:
 * the `META-INF/VAADIN/build/` directory.
 */
fun expectArchiveContainsVaadinWebpackBundle(archive: File) {
    expectArchiveContains("META-INF/VAADIN/build/*.gz",
            "META-INF/VAADIN/build/*.js",
            "META-INF/VAADIN/build/webcomponentsjs/webcomponents-*.js",
            "META-INF/VAADIN/build/webcomponentsjs/bundles/webcomponents-*.js"
    ) { archive }
}
