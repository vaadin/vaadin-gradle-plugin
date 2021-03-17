package com.vaadin.gradle

import com.vaadin.flow.plugin.base.PluginAdapterBuild
import com.vaadin.flow.server.frontend.scanner.ClassFinder
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.War
import java.io.File
import java.net.URI
import java.nio.file.Path

internal class GradlePluginAdapter(val project: Project): PluginAdapterBuild {
    val extension: VaadinFlowPluginExtension =
        VaadinFlowPluginExtension.get(project)

    override fun applicationProperties(): File = extension.applicationProperties

    override fun eagerServerLoad(): Boolean = extension.eagerServerLoad

    override fun frontendDirectory(): File = extension.frontendDirectory

    override fun generatedFolder(): File = extension.generatedFolder

    override fun generatedTsFolder(): File = extension.generatedTsFolder

    override fun getClassFinder(): ClassFinder = getClassFinder(project)

    override fun getJarFiles(): MutableSet<File> {
        val jarFiles = project.configurations
            .getByName("runtimeClasspath")
            .resolve()
            .filter { it.name.endsWith(".jar", true) }
        return jarFiles.toMutableSet()
    }

    override fun isJarProject(): Boolean = project.tasks.withType(War::class.java).isEmpty()

    override fun getUseDeprecatedV14Bootstrapping(): String = extension.useDeprecatedV14Bootstrapping.toString()

    override fun isDebugEnabled(): Boolean = true

    override fun javaSourceFolder(): File = extension.javaSourceFolder

    override fun logDebug(debugMessage: CharSequence) {
        project.logger.debug(debugMessage.toString())
    }

    override fun logInfo(infoMessage: CharSequence) {
        project.logger.info(infoMessage.toString())
    }

    override fun logWarn(warningMessage: CharSequence) {
        project.logger.warn(warningMessage.toString())
    }

    override fun logWarn(warningMessage: CharSequence, throwable: Throwable?) {
        project.logger.warn(warningMessage.toString(), throwable)
    }

    override fun logError(warning: CharSequence, e: Throwable?) {
        project.logger.error(warning.toString(), e)
    }

    override fun nodeDownloadRoot(): URI =
        URI.create(extension.nodeDownloadRoot)

    override fun nodeVersion(): String = extension.nodeVersion

    override fun npmFolder(): File = extension.npmFolder

    override fun openApiJsonFile(): File = extension.openApiJsonFile

    override fun pnpmEnable(): Boolean = extension.pnpmEnable

    override fun productionMode(): Boolean = extension.productionMode

    override fun projectBaseDirectory(): Path = project.projectDir.toPath()

    override fun requireHomeNodeExec(): Boolean = extension.requireHomeNodeExec

    override fun servletResourceOutputDirectory(): File =
        requireNotNull(extension.resourceOutputDirectory) { "VaadinFlowPluginExtension.autoconfigure() was not called" }

    override fun webpackOutputDirectory(): File =
        requireNotNull(extension.webpackOutputDirectory) { "VaadinFlowPluginExtension.autoconfigure() was not called" }

    override fun frontendResourcesDirectory(): File = extension.frontendResourcesDirectory

    override fun generateBundle(): Boolean = extension.generateBundle

    override fun generateEmbeddableWebComponents(): Boolean = extension.generateEmbeddableWebComponents

    override fun optimizeBundle(): Boolean = extension.optimizeBundle

    override fun runNpmInstall(): Boolean = extension.runNpmInstall

    override fun webpackGeneratedTemplate(): String = extension.webpackGeneratedTemplate

    override fun webpackTemplate(): String = extension.webpackTemplate
}
