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

import com.github.mvysny.dynatest.expectFiles
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream
import kotlin.test.expect
import kotlin.test.fail

/**
 * Expects that given task succeeded. If not, fails with an informative exception.
 * @param taskName the name of the task, e.g. `vaadinPrepareNode`
 */
fun BuildResult.expectTaskSucceded(taskName: String) {
    expectTaskOutcome(taskName, TaskOutcome.SUCCESS)
}

/**
 * Expects that given task has [expectedOutcome]. If not, fails with an informative exception.
 * @param taskName the name of the task, e.g. `vaadinPrepareNode` or `web:vaadinBuildFrontend`.
 */
fun BuildResult.expectTaskOutcome(taskName: String, expectedOutcome: TaskOutcome) {
    val task: BuildTask = task(":$taskName")
            ?: fail("Task $taskName was not ran. Executed tasks: ${tasks}. Stdout:\n$output")
    expect(expectedOutcome, "$taskName outcome was ${task.outcome}. Stdout:\n$output") {
        task.outcome
    }
}

/**
 * Expects that given task was not executed. If it was, fails with an informative exception.
 * @param taskName the name of the task, e.g. `vaadinPrepareNode` or `web:vaadinBuildFrontend`.
 */
fun BuildResult.expectTaskNotRan(taskName: String) {
    val task: BuildTask? = task(":$taskName")
    expect(null, "$taskName was not expected to be run. Executed tasks: $tasks. Stdout:\n$output") {
        task
    }
}

/**
 * Converts glob such as `*.jar` into a Regex which matches such files. Always
 * pass in forward slashes as path separators, even on Windows.
 */
private fun String.globToRegex(): Regex =
        Regex(this.replace("?", "[^/]?").replace("*", "[^/]*"))

/**
 * Lists all files in this zip archive, e.g. `META-INF/VAADIN/config/stats.json`.
 * Always returns forward slashes as path separators, even on Windows.
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
 * Expects that given archive doesn't contain any file matching any glob in the [globs] list.
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
 * Asserts that given archive (jar/war) doesn't contain the Vaadin webpack bundle:
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

/**
 * A testing Gradle project, created in a temporary directory.
 *
 * Used to test the plugin. Contains helpful utility methods to manipulate folders
 * and files in the project.
 * @property gradleVersion which Gradle version to test with, for example "5.0" or "7.2".
 */
class TestProject(val gradleVersion: GradleVersion) {
    /**
     * The project root dir.
     */
    val dir: File = createTempDir("junit-vaadin-gradle-plugin")

    /**
     * The main `build.gradle` file.
     */
    val buildFile: File get() = File(dir, "build.gradle")

    /**
     * The same as [buildFile] but in Kotlin.
     */
    val buildFileKts: File get() = File(dir, "build.gradle.kts")

    /**
     * The main `settings.gradle` file.
     */
    val settingsFile: File get() = File(dir, "settings.gradle")

    private fun createGradleRunner(): GradleRunner = GradleRunner.create()
        .withProjectDir(dir)
        .withPluginClasspath()
        .withDebug(true) // use --debug to catch ReflectionsException: https://github.com/vaadin/vaadin-gradle-plugin/issues/99
        .forwardOutput()   // a must, otherwise ./gradlew check freezes on windows!
        .withGradleVersion(gradleVersion.toString())

    override fun toString(): String = "TestProject(dir=$dir)"

    /**
     * Deletes the project directory and nukes all project files.
     */
    fun delete() {
        // don't throw an exception if the folder fails to be deleted. The folder
        // is temporary anyway, and Windows tends to randomly fail with
        // java.nio.file.FileSystemException: C:\Users\RUNNER~1\AppData\Local\Temp\junit-vaadin-gradle-plugin8993583259614232822.tmp\lib\build\libs\lib.jar: The process cannot access the file because it is being used by another process.
        if (!dir.deleteRecursively()) {
            println("Failed to delete temp project folder $dir")
        }
    }

    /**
     * Creates a new [folder] in the project folder. Does nothing if the folder
     * already exists.
     */
    fun newFolder(folder: String): File {
        val newFolder = Files.createDirectories(File(dir.absoluteFile, folder).toPath())
        return newFolder.toFile()
    }

    /**
     * Runs build on this project; a `build.gradle` [buildFile] is expected
     * to be located there.
     *
     * The function by default checks that all tasks have succeeded; if not, throws an informative exception.
     * You can suppress this functionality by setting [checkTasksSuccessful] to false.
     *
     * Uses the `--info` log level unless [debug] is true; then it uses the `--debug` log level.
     */
    fun build(vararg args: String, checkTasksSuccessful: Boolean = true, debug: Boolean = false): BuildResult {
        expect(true, "$buildFile/$buildFileKts doesn't exist, can't run build") {
            buildFile.exists() || buildFileKts.exists()
        }

        val effectiveArgs = args.toList() + "--stacktrace" + (if (debug) "--debug" else "--info")
        println("$dir/./gradlew ${effectiveArgs.joinToString(" ")}")
        val result: BuildResult = createGradleRunner()
            .withArguments(effectiveArgs)
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

    /**
     * Runs and fails the build on this project;
     */
    fun buildAndFail(vararg args: String): BuildResult {
        println("$dir/./gradlew ${args.joinToString(" ")}")
        return createGradleRunner()
            .withArguments(args.toList() + "--stacktrace" + "--info")
            .buildAndFail()
    }

    /**
     * Creates a file in the temporary test project.
     */
    fun newFile(fileNameWithPath: String, contents: String = ""): File {
        val file = File(dir, fileNameWithPath)
        Files.createDirectories(file.parentFile.toPath())
        file.writeText(contents)
        return file
    }

    /**
     * Looks up a [folder] in the project and returns it.
     */
    fun folder(folder: String): File {
        val dir = File(dir, folder)
        check(dir.exists()) { "$dir doesn't exist" }
        check(dir.isDirectory) { "$dir isn't a directory" }
        return dir
    }

    /**
     * Returns the WAR file built. Fails if there's no war file in `build/libs`.
     */
    val builtWar: File get() {
        val war = folder("build/libs").expectFiles("*.war").first()
        expect(true, "$war is missing") { war.isFile }
        return war
    }

    val builtJar: File get() {
        val jar: File = folder("build/libs").expectFiles("*.jar").first()
        expect(true, "$jar is missing") { jar.isFile }
        return jar
    }
}

/**
 * The Gradle version, such as `5.0` or `7.3` or `6.9.1`.
 */
data class GradleVersion(val major: Int, val minor: Int, val bugfix: Int = 0) : Comparable<GradleVersion> {
    init {
        require(major >= 1 && minor >= 0 && bugfix >= 0) { "Invalid version:$major.$minor.$bugfix" }
    }

    override fun toString(): String = "$major.$minor${if (bugfix == 0) "" else ".$bugfix"}"

    /**
     * JohnDevs' Vaadin 8 Gradle plugin can only run on Gradle 6.x and lower.
     */
    val supportsVaadin8Plugin: Boolean get() = vaadin8PluginVersion != null

    val vaadin8PluginVersion: String? get() = when {
        this >= V7_0 -> null // unsupported
        this >= V6_0 -> "2.0.0.beta2" // https://github.com/johndevs/gradle-vaadin-plugin/issues/549
        else -> "1.4.1"
    }

    /**
     * The `dependency{}` block API which this version of Gradle uses; either `compile` or
     * `implementation`.
     */
    val compile: String get() = if (major < 6) "compile" else "implementation"
    val springBootPlugin: String get() {
        // Spring Boot Gradle plugin 2.5.5 requires Gradle 6.8.x
        // Spring Boot 2.2.4.RELEASE is able to run on Gradle 5.0 but doesn't support JDK 17
        // Which is okay since Gradle 5.0 doesn't support it either
        return when {
            this >= V6_8 -> "2.5.5"
            else -> "2.2.4.RELEASE"
        }
    }

    override fun compareTo(other: GradleVersion): Int = compareValuesBy(this, other, { it.major }, { it.minor })

    companion object {
        val V5_0 = GradleVersion(5, 0)
        val V6_0 = GradleVersion(6, 0)
        val V6_8 = GradleVersion(6, 8)
        val V7_0 = GradleVersion(7, 0)
    }
}
