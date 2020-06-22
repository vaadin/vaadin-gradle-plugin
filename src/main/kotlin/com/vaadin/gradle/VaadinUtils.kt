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

import com.vaadin.flow.server.frontend.scanner.ClassFinder
import com.vaadin.flow.server.scanner.ReflectionsClassFinder
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.SourceSetContainer
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import java.io.File
import java.net.URL

private val servletApiJarRegex = Regex(".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$")
internal fun getClassFinder(project: Project): ClassFinder {
    val runtimeClasspathJars: List<File> = project.configurations.findByName("runtimeClasspath")
            ?.toList() ?: listOf()

    // we need to also analyze the project's classes
    val sourceSet: SourceSetContainer = project.properties["sourceSets"] as SourceSetContainer
    val classesDirs: List<File> = sourceSet.getByName("main").output.classesDirs
            .toList()
            .filter { it.exists() }

    // for Spring Boot project there is no "providedCompile" scope: the WAR plugin brings that in.
    val providedDeps: Configuration? = project.configurations.findByName("providedCompile")
    val servletJar: List<File> = providedDeps
            ?.filter { it.absolutePath.matches(servletApiJarRegex) }
            ?.toList()
            ?: listOf()

    val apis: Set<File> = (runtimeClasspathJars + classesDirs + servletJar).toSet()

    // eagerly check that all the files/folders exist, to avoid spamming the console later on
    // see https://github.com/vaadin/vaadin-gradle-plugin/issues/38 for more details
    apis.forEach {
        check(it.exists()) { "$it doesn't exist" }
    }

    val apiUrls: List<URL> = apis
            .map { it.toURI().toURL() }
    val classFinder = ReflectionsClassFinder(*apiUrls.toTypedArray())

    // sanity check that the project has flow-server.jar as a dependency
    try {
        classFinder.loadClass<Any>("com.vaadin.flow.server.webcomponent.WebComponentModulesWriter")
    } catch (e: ClassNotFoundException) {
        throw RuntimeException("Failed to find classes from flow-server.jar. The project '${project.name}' needs to have a dependency on flow-server.jar")
    }

    project.logger.info("Passing this classpath to NodeTasks.Builder: ${apis.toPrettyFormat()}")

    return classFinder
}

private fun ProcessExecutor.executeAndCheckOk(): ProcessResult {
    val result: ProcessResult = execute()
    if (result.exitValue != 0) {
        throw RuntimeException("Command '${command.joinToString(" ")}' failed with exit code ${result.exitValue}: ${result.outputUTF8()}")
    }
    return result
}

/**
 * Runs given subprocess, waiting until it finishes. Logs any stdout and stderr as INFO.
 * If the subprocess fails with a non-zero exit code, throws [RuntimeException] containing the exit code
 * and any stdout/stderr.
 * @param cwd the current working directory for the subprocess
 * @param args the program to run, including the arguments.
 */
internal fun exec(logger: Logger, cwd: File, vararg args: String) {
    logger.info("Running in '$cwd': ${args.joinToString(separator = "' '", prefix = "'", postfix = "'")}")
    val result: ProcessResult = ProcessExecutor()
            .command(*args)
            .directory(cwd)
            .readOutput(true)
            .destroyOnExit()
            .executeAndCheckOk()
    logger.info(result.outputUTF8())
}

/**
 * Writes JSON to a file, nicely formatted with default [indentation] of 2.
 */
internal fun JsonObject.writeToFile(file: File, indentation: Int = 2) {
    file.writeText(JsonUtil.stringify(this, indentation) + "\n")
}

/**
 * Allows Kotlin-based gradle scripts to be configured via
 * ```
 * vaadin {
 *   optimizeBundle = false
 * }
 * ```
 */
fun Project.vaadin(block: VaadinFlowPluginExtension.() -> Unit) =
        convention.findByType(VaadinFlowPluginExtension::class.java)!!.apply(block)

internal fun Collection<File>.toPrettyFormat(): String = joinToString(prefix = "[", postfix = "]") { if (it.isFile) it.name else it.absolutePath }
