package com.vaadin.gradle

import com.vaadin.flow.server.frontend.scanner.ClassFinder
import com.vaadin.flow.server.scanner.ReflectionsClassFinder
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import java.io.File
import java.net.URL

internal fun getClassFinder(project: Project): ClassFinder {
    val servletApiJarRegex = Regex(".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$")
    val runtimeJars: List<File> = project.configurations.getByName("runtime").toList()
    val servletJar: List<File> = project.configurations.getByName("providedCompile")
            .filter { it.absolutePath.matches(servletApiJarRegex) }
            .toList()
    val apis: Set<File> = (runtimeJars + servletJar).toSet()
    val apiUrls: List<URL> = apis
            .map { it.toURI().toURL() }
    return ReflectionsClassFinder(*apiUrls.toTypedArray())
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
    val result: ProcessResult = ProcessExecutor()
            .command(*args)
            .directory(cwd)
            .readOutput(true)
            .destroyOnExit()
            .executeAndCheckOk()
    logger.info(result.outputUTF8())
}

internal fun JsonObject.writeToFile(file: File) {
    file.writeText(JsonUtil.stringify(this, 2) + "\n")
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
