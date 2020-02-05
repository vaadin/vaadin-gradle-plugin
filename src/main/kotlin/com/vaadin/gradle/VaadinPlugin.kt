package com.vaadin.gradle

import com.moowork.gradle.node.NodePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * @author mavi
 */
class VaadinPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // we need Java Plugin conventions so that we can ensure the order of tasks
        project.pluginManager.apply(JavaPlugin::class.java)
        project.pluginManager.apply(NodePlugin::class.java)
        var extensionName = "vaadin"
        if (project.extensions.findByName(extensionName) != null) {
            // fixes https://github.com/vaadin/vaadin-gradle-plugin/issues/26
            extensionName = "vaadin14"
        }
        project.extensions.create("vaadin", VaadinFlowPluginExtension::class.java, project)

        project.tasks.apply {
            findByPath("clean")!!.doLast {
                project.delete("${project.projectDir}/target")
            }
            register("vaadinClean", VaadinCleanTask::class.java)
            register("vaadinPrepareFrontend", VaadinPrepareFrontendTask::class.java)
            register("vaadinBuildFrontend", VaadinBuildFrontendTask::class.java)
            register("vaadinPrepareNode", VaadinPrepareNodeTask::class.java)
        }
    }
}
