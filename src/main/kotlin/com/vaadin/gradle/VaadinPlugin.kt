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

import com.moowork.gradle.node.NodePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar

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
        project.extensions.create(extensionName, VaadinFlowPluginExtension::class.java, project)

        project.tasks.apply {
            findByPath("clean")!!.doLast {
                project.delete("${project.projectDir}/target")
            }
            register("vaadinClean", VaadinCleanTask::class.java)
            register("vaadinPrepareFrontend", VaadinPrepareFrontendTask::class.java)
            register("vaadinBuildFrontend", VaadinBuildFrontendTask::class.java)
            register("vaadinPrepareNode", VaadinPrepareNodeTask::class.java)
        }

        project.afterEvaluate {
            // make sure files produced by vaadinPrepareFrontend and vaadinBuildFrontend
            // will end up in the resulting jar/war file.
            project.tasks.withType(Jar::class.java) { task ->
                val extension: VaadinFlowPluginExtension = VaadinFlowPluginExtension.get(project)
                // make sure to copy the generated stuff into the resulting jar/war file.
                task.from(extension.buildOutputDirectory)
            }
        }
    }
}
