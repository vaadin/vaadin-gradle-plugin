/*
 * Copyright 2018-2019 Devsoap Inc.
 * Copyright 2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.gradle

import com.moowork.gradle.node.NodePlugin
import groovy.util.logging.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * Main plugin class
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinPlugin implements Plugin<Project> {

    static final String PLUGIN_ID = 'com.vaadin'
    static final String PRODUCT_NAME = 'vaadin-gradle-plugin'

    private static final String COMPILE_CONFIGURATION = 'compile'

    @Override
    void apply(Project project) {
        // we need Java Plugin conventions so that we can ensure the order of tasks
        project.getPluginManager().apply(JavaPlugin.class)
        project.getPluginManager().apply(NodePlugin.class)

        project.with {

            extensions.with {
                // need to use reflection since Groovy+Kotlin projects are not really supported.
                create('vaadin', Class.forName("com.vaadin.gradle.VaadinFlowPluginExtension"), project)
            }

            tasks.with {
                findByPath('clean')?.doLast {
                    project.delete("${project.projectDir}/target")
                }
                register('vaadinClean', Class.forName("com.vaadin.gradle.VaadinCleanTask"))
                register('vaadinPrepareFrontend', Class.forName("com.vaadin.gradle.VaadinPrepareFrontendTask"))
                register('vaadinBuildFrontend', Class.forName("com.vaadin.gradle.VaadinBuildFrontendTask"))
                register('vaadinPrepareNode', Class.forName("com.vaadin.gradle.VaadinPrepareNodeTask"))
            }
        }
    }
}
