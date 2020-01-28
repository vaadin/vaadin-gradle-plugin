package com.vaadin.gradle

import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.FrontendUtils
import com.vaadin.flow.server.frontend.NodeTasks
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Goal that builds the frontend bundle.
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
        dependsOn("vaadinPrepareFrontend")
        // Maven's task run in the LifecyclePhase.PREPARE_PACKAGE phase
        mustRunAfter("classes")
    }

    @TaskAction
    fun vaadinBuildFrontend() {
        val extension = project.extensions.getByName("vaadinFlow") as VaadinFlowPluginExtension
        val configFolder = File("${extension.buildOutputDirectory}/META-INF/VAADIN/config")

        // update build file
        val tokenFile = File(configFolder, "flow-build-info.json")
        val json: String = tokenFile.readText()
        val buildInfo: JsonObject = JsonUtil.parse(json)
        buildInfo.apply {
            remove(Constants.NPM_TOKEN)
            remove(Constants.GENERATED_TOKEN)
            remove(Constants.FRONTEND_TOKEN)
            put("productionMode", true)
            put(Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER, false)
        }
        buildInfo.writeToFile(tokenFile)

        // runNodeUpdater()
        val jarFiles: Set<File> = project.configurations.getByName("runtime").filter { it.name.endsWith(".jar") }.toSet()
        NodeTasks.Builder(getClassFinder(project),
                extension.npmFolder,
                extension.generatedFolder,
                extension.frontendDirectory)
                .runNpmInstall(extension.runNpmInstall)
                .enablePackagesUpdate(true)
                .useByteCodeScanner(extension.optimizeBundle)
                .copyResources(jarFiles)
                .copyLocalResources(extension.frontendResourcesDirectory)
                .enableImportsUpdate(true)
                .withEmbeddableWebComponents(extension.generateEmbeddableWebComponents)
                .withTokenFile(tokenFile)
                .build().execute()

        if (extension.generateBundle) {
            // runWebpack()
            val webpackCommand = "webpack/bin/webpack.js"
            val webpackExecutable = File(extension.npmFolder, FrontendUtils.NODE_MODULES + webpackCommand)
            check(webpackExecutable.isFile) { "Unable to locate webpack executable by path '${webpackExecutable.absolutePath}'. Double check that the plugin is executed correctly" }
            val nodePath: String = FrontendUtils.getNodeExecutable(extension.npmFolder.absolutePath)
            exec(project.logger, project.projectDir, nodePath, webpackExecutable.absolutePath)
        }
    }
}
