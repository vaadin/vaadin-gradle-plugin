package com.vaadin.gradle

import com.moowork.gradle.node.NodeExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.IllegalStateException

/**
 * Copies the node.js distro downloaded by the com.github.node-gradle.node plugin
 * to the node/ folder which Vaadin expects. Automatically includes and invokes
 * the com.github.node-gradle.node plugin.
 * @author mavi
 */
open class VaadinPrepareNodeTask : DefaultTask() {
    init {
        group = "Vaadin"
        description = "prepares a local node distribution for use by Vaadin. Requires com.github.node-gradle.node plugin"

        val nodeExtension: NodeExtension = project.extensions.getByType(NodeExtension::class.java)
        val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(project)
        nodeExtension.isDownload = true
        nodeExtension.version = extension.nodeVersion

        dependsOn("nodeSetup")
        project.tasks.findByPath("nodeSetup")!!.enabled = true
    }

    @TaskAction
    fun vaadinPrepareNode() {
        val localNodeDir = File("${project.projectDir}/node")
        project.delete(localNodeDir)

        // discover the node.js distro downloaded by the com.github.node-gradle.node plugin
        // the plugin downloads to .gradle/nodejs
        val nodejs = File(project.projectDir, ".gradle/nodejs")
        val nodeDirs: List<File> = nodejs.listFiles()?.toList() ?: listOf()
        check(nodeDirs.isNotEmpty()) { "No node distros downloaded in $nodejs" }
        val nodeDir: File = nodeDirs.first()
        check(nodeDir.isDirectory) { "$nodeDir is not a directory" }
        val isWindows: Boolean = when {
            File(nodeDir, "node.exe").exists() -> true
            File(nodeDir, "bin/node").exists() -> false
            else -> throw IllegalStateException("$nodeDir doesn't look like a node.js distro: there are no node.exe nor bin/node present")
        }

        // Vaadin expects node to be present in the node/ folder. Copy all necessary files.
        if (isWindows) {
            // Copy the whole directory in
            logger.info("Copying from $nodeDir -> $localNodeDir")
            project.copy {
                it.from(nodeDir)
                it.into(localNodeDir)
            }
        } else {
            // Copying the node executable and the node_modules directory
            logger.info("Copying from ${nodeDir}/bin/node -> ${localNodeDir}")
            project.copy {
                it.from(File(nodeDir, "bin/node"))
                it.into(localNodeDir)
            }

            val moduleDir = File("$nodeDir/lib/node_modules")
            val target = "${localNodeDir}/node_modules"
            logger.info("Copying from $moduleDir -> $target")
            project.copy {
                it.from(moduleDir)
                it.into(target)
            }
        }
    }
}
