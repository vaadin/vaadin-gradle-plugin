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
package com.vaadin.gradle.actions

import org.gradle.api.internal.artifacts.BaseRepositoryFactory
import com.vaadin.gradle.VaadinPlugin
import com.vaadin.gradle.extensions.VaadinPluginExtension
import com.vaadin.gradle.tasks.AssembleClientDependenciesTask
import com.vaadin.gradle.tasks.ConvertGroovyTemplatesToHTML
import com.vaadin.gradle.tasks.InstallBowerDependenciesTask
import com.vaadin.gradle.tasks.InstallYarnDependenciesTask
import com.vaadin.gradle.tasks.VersionCheckTask
import com.vaadin.gradle.tasks.WrapCssTask
import com.vaadin.gradle.util.LogUtils
import com.vaadin.gradle.util.Versions
import com.vaadin.gradle.util.WebJarHelper
import groovy.util.logging.Log
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * Action taken when the Vaadin plugin is applied to a project
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
class VaadinPluginAction extends PluginAction {

    final String pluginId = VaadinPlugin.PLUGIN_ID

    private static final String PLUGIN_VERSION_KEY = 'vaadin.plugin.version'
    private static final String PROCESS_RESOURCES = 'processResources'
    private static final String RUNNING_IN_COMPATIBILITY_MODE_MESSAGE =
            'The project will be compiled for Vaadin 13 (Flow 1) compatibility mode. '
    private static final String COMPILE_ONLY = 'compileOnly'

    @Override
    void apply(Project project) {
        super.apply(project)
        project.plugins.apply('java')
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        project.with {
            tasks[PROCESS_RESOURCES].with {
                dependsOn(VersionCheckTask.NAME)
                dependsOn(WrapCssTask.NAME)
                dependsOn(ConvertGroovyTemplatesToHTML.NAME)
            }

            tasks['jar'].dependsOn(AssembleClientDependenciesTask.NAME)

            repositories.maven { repository ->
                repository.name = 'Gradle Plugin Portal'
                repository.url = (
                        System.getProperty(BaseRepositoryFactory.PLUGIN_PORTAL_OVERRIDE_URL_PROPERTY) ?:
                                BaseRepositoryFactory.PLUGIN_PORTAL_DEFAULT_URL
                )
            }

            String pluginDependency =
                    "com.vaadin:vaadin-gradle-plugin:${Versions.rawVersion(PLUGIN_VERSION_KEY)}"
            Dependency vaadin = dependencies.create(pluginDependency) {
                description = 'Gradle Vaadin Plugin'
            }
            configurations[COMPILE_ONLY].dependencies.add(vaadin)
        }
    }

    @Override
    protected void executeAfterAllEvaluations() {
        super.executeAfterAllEvaluations()
        WebJarHelper.findDependantJarTasks(project).each {
            project.tasks[InstallYarnDependenciesTask.NAME].dependsOn(it)
            project.tasks[InstallBowerDependenciesTask.NAME].dependsOn(it)
        }
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)

        String vaadinVersion = Versions.version(PLUGIN_VERSION_KEY)
        LogUtils.printIfNotPrintedBefore( project,
                "Using Vaadin Gradle Plugin $vaadinVersion (UNLICENSED)"
        )

        VaadinPluginExtension vaadin = project.extensions[VaadinPluginExtension.NAME]
        if (!vaadin.versionSet) {
            LOGGER.warning('vaadin.version is not set, falling back to latest Vaadin version')
        }

        if (vaadin.submitStatisticsUnset) {
            LOGGER.warning('Allow Vaadin to gather usage statistics by setting ' +
                    'vaadin.submitStatistics=true (hide this message by setting it to false)')
        }

        if (vaadin.compatibilityMode) {
            LOGGER.warning(
                    RUNNING_IN_COMPATIBILITY_MODE_MESSAGE +
                    'To disable compatibility mode set vaadin.compatibilityMode=false. (experimental)')
        }
    }
}
