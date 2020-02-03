package com.vaadin.gradle

import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.FrontendUtils
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

open class VaadinFlowPluginExtension(project: Project) {
    /**
     * Whether or not we are running in productionMode.
     */
    var productionMode = false

    /**
     * The plugin will generate additional resource files here. These files need
     * to be present on the classpath, in order for Vaadin to be
     * able to run, both in dev mode and in the production mode. The plugin will automatically register
     * this as an additional resource folder, which should then be picked up by the IDE.
     * That will allow the app to run for example in Intellij with Tomcat.
     *
     * For example the `flow-build-info.json` goes here. See [webpackOutputDirectory]
     * for more details.
     */
    var buildOutputDirectory = File(project.buildDir, "vaadin-generated")

    /**
     * The folder where webpack should output index.js and other generated
     * files.
     *
     * In the dev mode, the `flow-build-info.json` file is generated here.
     */
    var webpackOutputDirectory = File(buildOutputDirectory, Constants.VAADIN_SERVLET_RESOURCES)

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    var npmFolder: File = project.projectDir
    /**
     * Copy the `webapp.config.js` from the specified URL if missing. Default is
     * the template provided by this plugin. Set it to empty string to disable
     * the feature.
     */
    var webpackTemplate: String = FrontendUtils.WEBPACK_CONFIG
    /**
     * Copy the `webapp.generated.js` from the specified URL. Default is the
     * template provided by this plugin. Set it to empty string to disable the
     * feature.
     */
    var webpackGeneratedTemplate = FrontendUtils.WEBPACK_GENERATED
    /**
     * The folder where flow will put generated files that will be used by
     * webpack.
     *
     * @todo mavi we should move this to `build/frontend/` but in order to do that we need Flow 2.2 or higher. Leaving as-is for now.
     */
    var generatedFolder = File(project.projectDir, "target/frontend")
    /**
     * A directory with project's frontend source files.
     */
    var frontendDirectory = File(project.projectDir, "frontend")

    /**
     * Whether to generate a bundle from the project frontend sources or not.
     */
    var generateBundle = true

    /**
     * Whether to run `npm install` after updating dependencies.
     */
    var runNpmInstall = true

    /**
     * Whether to generate embeddable web components from WebComponentExporter
     * inheritors.
     */
    var generateEmbeddableWebComponents = true

    /**
     * Defines the project frontend directory from where resources should be
     * copied from for use with webpack.
     */
    var frontendResourcesDirectory = File(project.projectDir, Constants.LOCAL_FRONTEND_RESOURCES_PATH)

    /**
     * Whether to use byte code scanner strategy to discover frontend
     * components.
     */
    var optimizeBundle = true

    /**
     * When using the `vaadinPrepareNode` task, you can specify the node version to download here.
     */
    val nodeVersion: String = "12.14.1"

    init {
        project.afterEvaluate {
            val sourceSets: SourceSetContainer = it.properties["sourceSets"] as SourceSetContainer
            sourceSets.getByName("main").resources.srcDirs(buildOutputDirectory)
        }
    }

    companion object {
        fun get(project: Project): VaadinFlowPluginExtension =
                project.extensions.getByName("vaadin") as VaadinFlowPluginExtension
    }
}
