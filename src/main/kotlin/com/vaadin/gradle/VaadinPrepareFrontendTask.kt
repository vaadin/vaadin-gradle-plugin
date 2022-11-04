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

import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.InitParameters
import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.FrontendUtils
import com.vaadin.flow.server.frontend.NodeTasks
import elemental.json.Json
import elemental.json.JsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.War
import java.io.File
import java.nio.file.Files

/**
 * This task checks that node and npm tools are installed, copies frontend
 * resources available inside `.jar` dependencies to `node_modules`, and creates
 * or updates `package.json` and `webpack.config.json` files.
 */
public open class VaadinPrepareFrontendTask : DefaultTask() {

    init {
        group = "Vaadin"
        description = "checks that node and npm tools are installed, copies frontend resources available inside `.jar` dependencies to `node_modules`, and creates or updates `package.json` and `webpack.config.json` files."

        // Maven's task run in the LifecyclePhase.PROCESS_RESOURCES phase

        project.tasks.getByName("processResources").mustRunAfter("vaadinPrepareFrontend")

        // make sure all dependent projects have finished building their jars, otherwise
        // the Vaadin classpath scanning will not work properly. See
        // https://github.com/vaadin/vaadin-gradle-plugin/issues/38
        // for more details.
        dependsOn(project.configurations.runtimeClasspath.jars)
    }

    @TaskAction
    public fun vaadinPrepareFrontend() {
        val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(project)
        logger.info("Running the vaadinPrepareFrontend task with effective configuration $extension")

        Files.createDirectories(extension.frontendDirectory.toPath())
        Files.createDirectories(extension.buildOutputDirectory.toPath())
        Files.createDirectories(extension.webpackOutputDirectory!!.toPath())

        propagateBuildInfo(extension)

        // Do nothing when compatibility mode
        if (extension.compatibility) {
            logger.debug("Skipped 'prepare-frontend' goal because compatibility mode is set.")
            return
        }

        val tools: FrontendTools = extension.createFrontendTools()
        if (extension.requireHomeNodeExec) {
            logger.info("Forcing alternative node executable from ${FrontendUtils.getVaadinHomeDirectory().absolutePath}")
            tools.forceAlternativeNodeExecutable()
        }
        logger.info("validating node and npm version")
        tools.validateNodeAndNpmVersion()

        // produce target/frontend/package.json
        Files.createDirectories(extension.generatedFolder.toPath())

        logger.info("Running NodeTasks")
        logger.info("runNodeUpdater: npmFolder=${extension.npmFolder}, generatedPath=${extension.generatedFolder}, frontendDirectory=${extension.frontendDirectory}")
        logger.info("runNodeUpdater: withWebpack(${extension.webpackOutputDirectory}, ${extension.webpackTemplate}, ${extension.webpackGeneratedTemplate})")
        logger.info("runNodeUpdater: createMissingPackageJson(true), enableImportsUpdate(false), enablePackagesUpdate(false)")
        logger.info("runNodeUpdater: not running npm install: it's supposed to be run either by Vaadin Servlet in development mode, or by the `vaadinBuildFrontend` task.")
        val builder: NodeTasks.Builder = extension.createNodeTasksBuilder(project)
                .withWebpack(extension.webpackOutputDirectory!!, extension.webpackTemplate, extension.webpackGeneratedTemplate)
                .createMissingPackageJson(true)
                .enableImportsUpdate(false)
                .enablePackagesUpdate(false)
                .runNpmInstall(false)

        // If building a jar project copy jar artifact contents now as we might
        // not be able to read files from jar path.
        val isJarPackaging: Boolean = project.tasks.withType(War::class.java).isEmpty()
        if (isJarPackaging) {
            val jarFiles: Set<File> = project.configurations.runtimeClasspath.jars.toSet()
            logger.info("runNodeUpdater: using JAR packaging, copyResources=${jarFiles.toPrettyFormat()}")
            builder.copyResources(jarFiles)
        } else {
            logger.info("Not copying resources since we're not using JAR packaging")
        }

        builder.build().execute()
        logger.info("runNodeUpdater: done!")
    }

    private fun propagateBuildInfo(extension: VaadinFlowPluginExtension) {
        val buildInfo: JsonObject = Json.createObject().apply {
            put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE, extension.compatibility)
            put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, extension.productionMode)
            put(Constants.NPM_TOKEN, extension.npmFolder.absolutePath)
            put(Constants.GENERATED_TOKEN, extension.generatedFolder.absolutePath)
            put(Constants.FRONTEND_TOKEN, extension.frontendDirectory.absolutePath)
            put(Constants.SERVLET_PARAMETER_ENABLE_PNPM, extension.pnpmEnable)
            put(InitParameters.NODE_VERSION, extension.nodeVersion)
            put(InitParameters.NODE_DOWNLOAD_ROOT, extension.nodeDownloadRoot)
        }
        // Don't generate the file here (taken from the Maven plugin code)
        // val tokenFile = File(extension.webpackOutputDirectory!!, FrontendUtils.TOKEN_FILE)
        val configFolder = File("${extension.buildOutputDirectory}/META-INF/VAADIN/config")
        val tokenFile = File(configFolder, "flow-build-info.json")
        Files.createDirectories(tokenFile.parentFile.toPath())
        buildInfo.writeToFile(tokenFile)
        logger.info("Wrote token file $tokenFile: $buildInfo")
    }
}
