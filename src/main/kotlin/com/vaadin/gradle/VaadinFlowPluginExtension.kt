package com.vaadin.gradle

import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.frontend.FrontendUtils
import org.gradle.api.Project
import java.io.File

open class VaadinFlowPluginExtension(project: Project) {
    /**
     * Whether or not we are running in productionMode.
     */
    var productionMode = false

    // this is target/classes in Maven. Yes, I know I should use gradle's build/something/
    // ideally create new build/output directory, however Intellij won't pick generated files
    // (namely flow-build-info.json) from such directory when launching project in Tomcat.
    // for now I'm thus generating into the src/ .
    var buildOutputDirectory = "${project.projectDir}/src/main/resources"

    /**
     * The folder where webpack should output index.js and other generated
     * files.
     */
    var webpackOutputDirectory = File("$buildOutputDirectory/META-INF/VAADIN/")

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
}
