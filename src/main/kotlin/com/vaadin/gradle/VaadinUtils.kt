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

import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.FrontendUtils
import com.vaadin.flow.server.frontend.NodeTasks
import com.vaadin.flow.server.frontend.scanner.ClassFinder
import com.vaadin.flow.server.scanner.ReflectionsClassFinder
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.SourceSetContainer
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import java.io.File
import java.net.URI
import java.net.URL
import java.util.function.Supplier

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

    var apis: Set<File> = (runtimeClasspathJars + classesDirs + servletJar).toSet()

    // eagerly check that all the files/folders exist, to avoid spamming the console later on
    // see https://github.com/vaadin/vaadin-gradle-plugin/issues/38 for more details
    apis.forEach {
        check(it.exists()) { "$it doesn't exist" }
    }

    // only accept .jar-type dependencies and folders. Fixes
    // https://github.com/vaadin/vaadin-gradle-plugin/issues/115
    apis = apis.filter { it.isDirectory || it.extension.lowercase() == "jar" } .toSet()

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
 * Finds the value of a boolean property. It searches in gradle and system properties.
 *
 * If the property is defined in both gradle and system properties, then the gradle property is taken.
 *
 * @param propertyName the property name
 *
 * @return `null` if the property is not present, `true` if it's defined or if it's set to "true"
 * and `false` otherwise.
 */
public fun Project.getBooleanProperty(propertyName: String) : Boolean? {
    if (System.getProperty(propertyName) != null) {
        val value: String = System.getProperty(propertyName)
        val valueBoolean: Boolean = value.isBlank() || value.toBoolean()
        logger.info("Set $propertyName to $valueBoolean because of System property $propertyName='$value'")
        return valueBoolean
    }
    if (project.hasProperty(propertyName)) {
        val value: String = project.property(propertyName) as String
        val valueBoolean: Boolean = value.isBlank() || value.toBoolean()
        logger.info("Set $propertyName to $valueBoolean because of Gradle project property $propertyName='$value'")
        return valueBoolean
    }
    return null
}

/**
 * Allows Kotlin-based gradle scripts to be configured via
 * ```
 * vaadin {
 *   optimizeBundle = false
 * }
 * ```
 */
public fun Project.vaadin(block: VaadinFlowPluginExtension.() -> Unit) {
    convention.getByType(VaadinFlowPluginExtension::class.java).apply(block)
}

internal fun Collection<File>.toPrettyFormat(): String =
    map { if (it.isFile) it.name else it.absolutePath }
        .sorted() // sort, so that the list of files is order-independent
        .joinToString(prefix = "[", postfix = "]")

internal fun VaadinFlowPluginExtension.createFrontendTools(): FrontendTools =
        FrontendTools(npmFolder.absolutePath,
                Supplier { FrontendUtils.getVaadinHomeDirectory().absolutePath },
                nodeVersion,
                URI(nodeDownloadRoot))

internal fun VaadinFlowPluginExtension.createNodeTasksBuilder(project: Project): NodeTasks.Builder =
        NodeTasks.Builder(getClassFinder(project), npmFolder, generatedFolder, frontendDirectory)
                .withHomeNodeExecRequired(requireHomeNodeExec)
                .withNodeVersion(nodeVersion)
                .withNodeDownloadRoot(URI(nodeDownloadRoot))

/**
 * Returns the "runtimeClasspath" file collection.
 */
internal val ConfigurationContainer.runtimeClasspath: Configuration
    get() = getByName("runtimeClasspath")

/**
 * Returns only jar files from given file collection.
 */
internal val Configuration.jars: FileCollection
    get() = filter { it.name.endsWith(".jar", true) }
