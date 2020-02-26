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
fun BuildResult.expectTaskSucceded(taskName: String) = expectTaskOutcome(taskName, TaskOutcome.SUCCESS)

/**
 * Expects that given task has [expectedOutcome]. If not, fails with an informative exception.
 * @param taskName the name of the task, e.g. `vaadinPrepareNode` or `web:vaadinBuildFrontend`.
 */
fun BuildResult.expectTaskOutcome(taskName: String, expectedOutcome: TaskOutcome) {
    val task: BuildTask = task(":$taskName")
            ?: fail("Task $taskName was not ran\n$output")
    expect(expectedOutcome, "$taskName did not succeed: ${task.outcome}") {
        task.outcome
    }
}

/**
 * Finds all files matching given [glob] pattern, for example `build/libs/ *.war`
 * @param expectedCount expected number of files, defaults to 1.
 */
fun File.find(glob: String, expectedCount: IntRange = 1..1): List<File> {
    val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:$absolutePath/$glob")
    val found: List<File> = absoluteFile.walk()
            .filter { matcher.matches(it.toPath()) }
            .toList()
    if (found.size !in expectedCount) {
        fail("Expected $expectedCount $glob but found ${found.size}: $found . Folder dump: ${absoluteFile.walk().joinToString("\n")}")
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
 * Expects that given archive contains at least one file matching every glob in the [globs] list.
 * @param archiveProvider returns the zip file to examine.
 */
fun expectArchiveDoesntContain(vararg globs: String, archiveProvider: () -> File) {
    val archive: File = archiveProvider()
    val allFiles: List<String> = archive.zipListAllFiles()

    globs.forEach { glob: String ->
        val regex: Regex = glob.globToRegex()
        val someFileMatch: Boolean = allFiles.any { it.matches(regex) }
        expect(false, "Unexpected files $glob found in $archive, found ${allFiles.joinToString("\n")}") { someFileMatch }
    }
}

/**
 * Asserts that given archive (jar/war) contains the Vaadin webpack bundle:
 * the `META-INF/VAADIN/build/` directory.
 */
fun expectArchiveContainsVaadinWebpackBundle(archive: File,
                                             isSpringBootJar: Boolean) {
    val isWar: Boolean = archive.name.endsWith(".war", true)
    val isStandaloneJar: Boolean = !isWar && !isSpringBootJar
    val resourcePackaging: String = when {
        isWar -> "WEB-INF/classes/"
        isSpringBootJar -> "BOOT-INF/classes/"
        else -> ""
    }
    expectArchiveContains(
            "${resourcePackaging}META-INF/VAADIN/config/flow-build-info.json",
            "${resourcePackaging}META-INF/VAADIN/config/stats.json",
            "${resourcePackaging}META-INF/VAADIN/build/*.gz",
            "${resourcePackaging}META-INF/VAADIN/build/*.js",
            "${resourcePackaging}META-INF/VAADIN/build/webcomponentsjs/webcomponents-*.js",
            "${resourcePackaging}META-INF/VAADIN/build/webcomponentsjs/bundles/webcomponents-*.js"
    ) { archive }
    if (!isStandaloneJar) {
        val libPrefix: String = if (isSpringBootJar) "BOOT-INF/lib" else "WEB-INF/lib"
        expectArchiveContains("$libPrefix/*.jar") { archive }
    }

    // make sure there is only one flow-build-info.json
    val allFiles: List<String> = archive.zipListAllFiles()
    expect(1, "Multiple flow-build-info.json found: ${allFiles.joinToString("\n")}") {
        allFiles.count { it.contains("flow-build-info.json") }
    }
}

/**
 * Asserts that given archive (jar/war) contains the Vaadin webpack bundle:
 * the `META-INF/VAADIN/build/` directory.
 */
fun expectArchiveDoesntContainVaadinWebpackBundle(archive: File,
                                             isSpringBootJar: Boolean) {
    val isWar: Boolean = archive.name.endsWith(".war", true)
    val isStandaloneJar: Boolean = !isWar && !isSpringBootJar
    val resourcePackaging: String = when {
        isWar -> "WEB-INF/classes/"
        isSpringBootJar -> "BOOT-INF/classes/"
        else -> ""
    }
    expectArchiveContains("${resourcePackaging}META-INF/VAADIN/config/flow-build-info.json") { archive }
    expectArchiveDoesntContain("${resourcePackaging}META-INF/VAADIN/config/stats.json",
            "${resourcePackaging}META-INF/VAADIN/build/*.gz",
            "${resourcePackaging}META-INF/VAADIN/build/*.js",
            "${resourcePackaging}META-INF/VAADIN/build/webcomponentsjs/webcomponents-*.js",
            "${resourcePackaging}META-INF/VAADIN/build/webcomponentsjs/bundles/webcomponents-*.js"
    ) { archive }

    if (!isStandaloneJar) {
        val libPrefix: String = if (isSpringBootJar) "BOOT-INF/lib" else "WEB-INF/lib"
        expectArchiveContains("$libPrefix/*.jar") { archive }
    }

    // make sure there is only one flow-build-info.json
    val allFiles: List<String> = archive.zipListAllFiles()
    expect(1, "Multiple flow-build-info.json found: ${allFiles.joinToString("\n")}") {
        allFiles.count { it.contains("flow-build-info.json") }
    }
}
