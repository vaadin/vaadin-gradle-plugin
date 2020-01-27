package com.vaadin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Cleans everything Vaadin-related. Useful if npm fails to run after Vaadin
 * version upgrade. Deletes:
 *
 * * `node_modules`
 * * `package.json`
 * * `package-lock.json`
 * * `webpack.config.js`
 * * `webpack.generated.js`
 *
 * Afterwards, run the `vaadinPrepareFrontend` task to re-create some of the files;
 * the rest of the files will be re-created by Vaadin Servlet, simply by running the application
 * in the development mode.
 */
open class VaadinCleanTask : DefaultTask() {
    init {
        dependsOn("clean")
    }

    @TaskAction
    fun clean() {
        project.delete("${project.projectDir}/node_modules",
                "${project.projectDir}/package.json",
                "${project.projectDir}/package-lock.json",
                "${project.projectDir}/webpack.config.js",
                "${project.projectDir}/webpack.generated.js")
    }
}
