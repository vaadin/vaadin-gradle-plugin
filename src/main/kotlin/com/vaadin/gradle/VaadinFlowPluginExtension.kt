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
import com.vaadin.flow.server.frontend.FrontendUtils
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

open class VaadinFlowPluginExtension(project: Project) {
    /**
     * Whether or not we are running in productionMode. Defaults to false.
     * Responds to the `-Pvaadin.productionMode` property.
     */
    var productionMode: Boolean = false

    /**
     * The plugin will generate additional resource files here. These files need
     * to be present on the classpath, in order for Vaadin to be
     * able to run, both in dev mode and in the production mode. The plugin will automatically register
     * this as an additional resource folder, which should then be picked up by the IDE.
     * That will allow the app to run for example in Intellij with Tomcat.
     *
     * For example the `flow-build-info.json` goes here. Also see [webpackOutputDirectory].
     */
    var buildOutputDirectory = File(project.buildDir, "vaadin-generated")

    /**
     * The folder where webpack should output index.js and other generated
     * files. Defaults to `null` which will use the auto-detected value of
     * resoucesDir of the main SourceSet, usually `build/resources/main/META-INF/VAADIN/`.
     *
     * In the dev mode, the `flow-build-info.json` file is generated here.
     */
    var webpackOutputDirectory: File? = null

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
    var webpackGeneratedTemplate: String = FrontendUtils.WEBPACK_GENERATED
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
     * Instructs to use pnpm for installing npm frontend resources.
     *
     * pnpm, a.k.a. performant npm, is a better front-end dependency management option.
     * With pnpm, packages are cached locally by default and linked (instead of
     * downloaded) for every project. This results in reduced disk space usage
     * and faster recurring builds when compared to npm.
     */
    var pnpmEnable = false

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * `true` then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     */
    var requireHomeNodeExec = false

    companion object {
        fun get(project: Project): VaadinFlowPluginExtension =
                project.extensions.getByType(VaadinFlowPluginExtension::class.java)
    }

    internal fun autoconfigure(project: Project) {
        // calculate webpackOutputDirectory if not set by the user
        if (webpackOutputDirectory == null) {
            val sourceSets: SourceSetContainer = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets
            val resourcesDir: File = sourceSets.getByName("main").output.resourcesDir!!
            webpackOutputDirectory = File(resourcesDir, Constants.VAADIN_SERVLET_RESOURCES)
        }

        if (System.getProperty("vaadin.productionMode") != null) {
            val pm: String = System.getProperty("vaadin.productionMode")
            productionMode = pm.isBlank() || pm.toBoolean()
        }
        if (project.hasProperty("vaadin.productionMode")) {
            val pm: String = project.property("vaadin.productionMode") as String
            productionMode = pm.isBlank() || pm.toBoolean()
        }
    }

    override fun toString(): String = "VaadinFlowPluginExtension(" +
            "productionMode=$productionMode, " +
            "buildOutputDirectory=$buildOutputDirectory, " +
            "webpackOutputDirectory=$webpackOutputDirectory, " +
            "npmFolder=$npmFolder, " +
            "webpackTemplate='$webpackTemplate', " +
            "webpackGeneratedTemplate='$webpackGeneratedTemplate', " +
            "generatedFolder=$generatedFolder, " +
            "frontendDirectory=$frontendDirectory, " +
            "generateBundle=$generateBundle, " +
            "runNpmInstall=$runNpmInstall, " +
            "generateEmbeddableWebComponents=$generateEmbeddableWebComponents, " +
            "frontendResourcesDirectory=$frontendResourcesDirectory, " +
            "optimizeBundle=$optimizeBundle, " +
            "pnpmEnable=$pnpmEnable, " +
            "requireHomeNodeExec=$requireHomeNodeExec)"
}
