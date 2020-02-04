package com.vaadin.gradle

import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.FrontendUtils
import com.vaadin.flow.server.frontend.NodeTasks
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
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

        // We need to run before 'processResources' which automatically packages
        // the outcome of this task for us.
        //
        // However, we also need access to the produced classes, to be able to analyze e.g. @CssImport annotations used by the project.
        // And we can't depend on the 'classes' task since that depends on 'processResources'
        // which would create a circular reference.
        //
        // We will therefore depend on all non-test compile tasks in this "hacky" way.
        // See https://stackoverflow.com/questions/27239028/how-to-depend-on-all-compile-and-testcompile-tasks-in-gradle for more info.
        dependsOn(project.tasks.withType(AbstractCompile::class.java).matching { !it.name.toLowerCase().contains("test") })

        // Make sure to run this task before the `processResources` task.
        project.tasks.named("processResources") { task ->
            task.mustRunAfter("vaadinBuildFrontend")
        }
    }

    @TaskAction
    fun vaadinBuildFrontend() {
        val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(project)
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
        val jarFiles: Set<File> = project.configurations.getByName("runtimeClasspath").resolve().filter { it.name.endsWith(".jar") }.toSet()
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
