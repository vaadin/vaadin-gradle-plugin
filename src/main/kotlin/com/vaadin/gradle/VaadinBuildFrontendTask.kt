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
import com.vaadin.flow.server.frontend.FrontendTools
import com.vaadin.flow.server.frontend.FrontendUtils
import com.vaadin.flow.server.frontend.FrontendUtils.DEAULT_FLOW_RESOURCES_FOLDER
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import java.io.File

/**
 * Task that builds the frontend bundle.
 *
 * It performs the following actions when creating a package:
 * * Update [Constants.PACKAGE_JSON] file with the [com.vaadin.flow.component.dependency.NpmPackage]
 * annotations defined in the classpath,
 * * Copy resource files used by flow from `.jar` files to the `node_modules`
 * folder
 * * Install dependencies by running `npm install`
 * * Update the [FrontendUtils.IMPORTS_NAME] file imports with the
 * [com.vaadin.flow.component.dependency.JsModule] [com.vaadin.flow.theme.Theme] and [com.vaadin.flow.component.dependency.JavaScript] annotations defined in
 * the classpath,
 * * Update [FrontendUtils.WEBPACK_CONFIG] file.
 *
 * @since 2.0
 */
open class VaadinBuildFrontendTask : DefaultTask() {
    init {
        group = "Vaadin"
        description = "Builds the frontend bundle with webpack"

        // we need the flow-build-info.json to be created, which is what the vaadinPrepareFrontend task does
        dependsOn("vaadinPrepareFrontend")
        // Maven's task run in the LifecyclePhase.PREPARE_PACKAGE phase

        // We need access to the produced classes, to be able to analyze e.g.
        // @CssImport annotations used by the project.
        dependsOn("classes")

        // Make sure to run this task before the `war`/`jar` tasks, so that
        // webpack bundle will end up packaged in the war/jar archive. The inclusion
        // rule itself is configured in the VaadinPlugin class.
        project.tasks.withType(Jar::class.java) { task: Jar ->
            task.mustRunAfter("vaadinBuildFrontend")
        }
    }

    @TaskAction
    fun vaadinBuildFrontend() {
        val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(project)
        val tokenFile = File(extension.webpackOutputDirectory, FrontendUtils.TOKEN_FILE)
        logger.info("Running the vaadinBuildFrontend task with effective configuration $extension")

        updateBuildFile(tokenFile)

        runNodeUpdater(extension, tokenFile)

        if (extension.generateBundle) {
            runWebpack(extension)
        } else {
            logger.info("Not running webpack since generateBundle is false")
        }
    }

    private fun runWebpack(extension: VaadinFlowPluginExtension) {
        val webpackCommand = "webpack/bin/webpack.js"
        val webpackExecutable = File(extension.npmFolder, FrontendUtils.NODE_MODULES + webpackCommand)
        check(webpackExecutable.isFile) {
            "Unable to locate webpack executable by path '${webpackExecutable.absolutePath}'. Double check that the plugin is executed correctly"
        }
        val tools: FrontendTools = extension.createFrontendTools()
        val nodePath: String = if (extension.requireHomeNodeExec) {
            tools.forceAlternativeNodeExecutable()
        } else {
            tools.nodeExecutable
        }

        logger.info("Running webpack")
        exec(project.logger, project.projectDir, nodePath, webpackExecutable.absolutePath)
    }

    private fun runNodeUpdater(extension: VaadinFlowPluginExtension, tokenFile: File) {
        val jarFiles: Set<File> = project.configurations.getByName("runtimeClasspath")
                .resolve()
                .filter { it.name.endsWith(".jar") }
                .toSet()

        logger.info("runNodeUpdater: npmFolder=${extension.npmFolder}, generatedPath=${extension.generatedFolder}, frontendDirectory=${extension.frontendDirectory}")
        logger.info("runNodeUpdater: runNpmInstall=${extension.runNpmInstall}, enablePackagesUpdate=true, useByteCodeScranner=${extension.optimizeBundle}")
        logger.info("runNodeUpdater: copyResources=${jarFiles.toPrettyFormat()}")
        logger.info("runNodeUpdater: copyLocalResources=${extension.frontendResourcesDirectory}")
        logger.info("runNodeUpdater: enableImportsUpdate=true, embeddableWebComponents=${extension.generateEmbeddableWebComponents}, tokenFile=${tokenFile}")
        logger.info("runNodeUpdater: pnpm=${extension.pnpmEnable}, requireHomeNodeExec=${extension.requireHomeNodeExec}")

        val flowResourcesFolder = File(extension.npmFolder, DEAULT_FLOW_RESOURCES_FOLDER)
        // @formatter:off
        extension.createNodeTasksBuilder(project)
                .runNpmInstall(extension.runNpmInstall)
                .useV14Bootstrap(extension.useDeprecatedV14Bootstrapping)
                .enablePackagesUpdate(true)
                .useByteCodeScanner(extension.optimizeBundle)
                .withFlowResourcesFolder(flowResourcesFolder)
                .copyResources(jarFiles)
                .copyLocalResources(extension.frontendResourcesDirectory)
                .enableImportsUpdate(true)
                .withEmbeddableWebComponents(extension.generateEmbeddableWebComponents)
                .withTokenFile(tokenFile)
                .enablePnpm(extension.pnpmEnable)
                .withConnectApplicationProperties(extension.applicationProperties)
                .withConnectJavaSourceFolder(extension.javaSourceFolder)
                .withConnectGeneratedOpenApiJson(extension.openApiJsonFile)
                .withConnectClientTsApiFolder(extension.generatedTsFolder)
                .withHomeNodeExecRequired(extension.requireHomeNodeExec)
                .build().execute()

        logger.info("runNodeUpdater: done!")
    }

    /**
     * Add the devMode token to build token file so we don't try to start the
     * dev server. Remove the abstract folder paths as they should not be used
     * for prebuilt bundles.
     */
    private fun updateBuildFile(tokenFile: File) {
        check(tokenFile.isFile) { "$tokenFile is missing" }

        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        buildInfo.apply {
            remove(Constants.NPM_TOKEN)
            remove(Constants.GENERATED_TOKEN)
            remove(Constants.FRONTEND_TOKEN)
            remove(Constants.SERVLET_PARAMETER_ENABLE_PNPM)
            remove(Constants.REQUIRE_HOME_NODE_EXECUTABLE)
            remove(Constants.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE);
            remove(Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            remove(Constants.CONNECT_APPLICATION_PROPERTIES_TOKEN);
            remove(Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            remove(Constants.CONNECT_OPEN_API_FILE_TOKEN);
            remove(Constants.CONNECT_GENERATED_TS_DIR_TOKEN);

            put(Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER, false)
        }
        buildInfo.writeToFile(tokenFile)
        logger.info("Updated token file $tokenFile to $buildInfo")
    }
}
