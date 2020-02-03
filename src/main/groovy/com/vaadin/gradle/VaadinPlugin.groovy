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
import com.vaadin.gradle.actions.GrettyDeprecatedPluginAction
import com.vaadin.gradle.actions.GrettyPluginAction
import com.vaadin.gradle.actions.JavaPluginAction
import com.vaadin.gradle.actions.NodePluginAction
import com.vaadin.gradle.actions.PluginAction
import com.vaadin.gradle.actions.SassJavaPluginAction
import com.vaadin.gradle.actions.SassWarPluginAction
import com.vaadin.gradle.actions.SpringBootAction
import com.vaadin.gradle.actions.VaadinPluginAction
import com.vaadin.gradle.actions.WarPluginAction
import com.vaadin.gradle.extensions.VaadinClientDependenciesExtension
import com.vaadin.gradle.extensions.VaadinPluginExtension
import com.vaadin.gradle.tasks.AssembleClientDependenciesTask
import com.vaadin.gradle.tasks.ConvertGroovyTemplatesToHTML
import com.vaadin.gradle.tasks.CreateComponentTask
import com.vaadin.gradle.tasks.CreateCompositeTask
import com.vaadin.gradle.tasks.CreateProjectTask
import com.vaadin.gradle.tasks.CreateWebComponentTask
import com.vaadin.gradle.tasks.CreateWebTemplateTask
import com.vaadin.gradle.tasks.InstallBowerDependenciesTask
import com.vaadin.gradle.tasks.InstallYarnDependenciesTask
import com.vaadin.gradle.tasks.TranspileDependenciesTask
import com.vaadin.gradle.tasks.VersionCheckTask
import com.vaadin.gradle.tasks.WrapCssTask
import com.vaadin.gradle.util.Versions
import groovy.util.logging.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaPlugin
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.VersionNumber

import javax.inject.Inject

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
    private final List<PluginAction> actions = []
    private final enableOldPlugin = Boolean.valueOf(System.getProperty("vaadin.enableOldPlugin"))

    @Inject
    VaadinPlugin(Gradle gradle, Instantiator instantiator) {
        if (enableOldPlugin) {
            validateGradleVersion(gradle)

            actions << instantiator.newInstance(JavaPluginAction)
            actions << instantiator.newInstance(VaadinPluginAction)
            actions << instantiator.newInstance(NodePluginAction)
            actions << instantiator.newInstance(WarPluginAction)
            actions << instantiator.newInstance(GrettyDeprecatedPluginAction)
            actions << instantiator.newInstance(GrettyPluginAction)
            actions << instantiator.newInstance(SpringBootAction)
            actions << instantiator.newInstance(SassJavaPluginAction)
            actions << instantiator.newInstance(SassWarPluginAction)
        }
    }

    @Override
    void apply(Project project) {
        // we need Java Plugin conventions so that we can ensure the order of tasks
        project.getPluginManager().apply(JavaPlugin.class)
        project.getPluginManager().apply(NodePlugin.class)
        def enableOldPlugin = this.enableOldPlugin

        project.with {

            if (enableOldPlugin) {
                actions.each { action ->
                    action.apply(project)
                }
            }

            extensions.with {
                if (enableOldPlugin) {
                    create(VaadinPluginExtension.NAME, VaadinPluginExtension, project)
                    create(VaadinClientDependenciesExtension.NAME, VaadinClientDependenciesExtension, project)
                } else {
                    // need to use reflection since Groovy+Kotlin projects are not really supported.
                    create('vaadin', Class.forName("com.vaadin.gradle.VaadinFlowPluginExtension"), project)
                }
            }

            tasks.with {
                if (enableOldPlugin) {
                    register(CreateProjectTask.NAME, CreateProjectTask)
                    register(CreateWebComponentTask.NAME, CreateWebComponentTask)
                    register(InstallYarnDependenciesTask.NAME, InstallYarnDependenciesTask)
                    register(InstallBowerDependenciesTask.NAME, InstallBowerDependenciesTask)
                    register(TranspileDependenciesTask.NAME, TranspileDependenciesTask)
                    register(AssembleClientDependenciesTask.NAME, AssembleClientDependenciesTask)
                    register(WrapCssTask.NAME, WrapCssTask)
                    register(CreateCompositeTask.NAME, CreateCompositeTask)
                    register(CreateComponentTask.NAME, CreateComponentTask)
                    register(CreateWebTemplateTask.NAME, CreateWebTemplateTask)
                    register(ConvertGroovyTemplatesToHTML.NAME, ConvertGroovyTemplatesToHTML)
                    register(VersionCheckTask.NAME, VersionCheckTask)
                } else {
                    findByPath('clean')?.doLast {
                        project.delete("${project.projectDir}/target")
                    }
                    register('vaadinClean', Class.forName("com.vaadin.gradle.VaadinCleanTask"))
                    register('vaadinPrepareFrontend', Class.forName("com.vaadin.gradle.VaadinPrepareFrontendTask"))
                    register('vaadinBuildFrontend', Class.forName("com.vaadin.gradle.VaadinBuildFrontendTask"))
                    register('vaadinPrepareNode', Class.forName("com.vaadin.gradle.VaadinPrepareNodeTask"))
                }
            }

            if (enableOldPlugin) {
                afterEvaluate {
                    disableStatistics(project)
                    enableProductionMode(project)
                    validateVaadinVersion(project)
                }
            }
        }
    }

    private static void disableStatistics(Project project) {
        VaadinPluginExtension vaadin = project.extensions.getByType(VaadinPluginExtension)
        if (!vaadin.submitStatistics) {
            Dependency statistics = vaadin.disableStatistics()
            project.configurations[COMPILE_CONFIGURATION].dependencies.add(statistics)
            project.configurations.all { DefaultConfiguration config ->
                config.resolutionStrategy.force("${statistics.group}:${statistics.name}:${statistics.version}")
            }
        }
    }

    private static void enableProductionMode(Project project) {
        VaadinPluginExtension vaadin = project.extensions.getByType(VaadinPluginExtension)
        if (vaadin.productionMode) {
            Dependency productionMode = vaadin.enableProductionMode()
            project.configurations[COMPILE_CONFIGURATION].dependencies.add(productionMode)
        }
    }

    private static void validateGradleVersion(Gradle gradle) {
        VersionNumber version = VersionNumber.parse(gradle.gradleVersion)
        VersionNumber requiredVersion = Versions.version('vaadin.plugin.gradle.version')
        if ( version.baseVersion < requiredVersion ) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. " +
                    "Plugin requires Gradle $requiredVersion+")
        }
    }

    private static void validateVaadinVersion(Project project) {
        VaadinPluginExtension vaadin = project.extensions.getByType(VaadinPluginExtension)
        if (vaadin.unSupportedVersion) {
            LOGGER.severe(
                    "The Vaadin version ($vaadin.version) you have selected is not supported by the plugin. " +
                            'Since vaadin.unsupportedVersion is set to True, continuing anyway. You are on your own.')

        } else if (!vaadin.isSupportedVersion()) {
            String[] supportedVersions = Versions.rawVersion('vaadin.supported.versions').split(',')
            throw new UnsupportedVersionException(
                    "The Vaadin version ($vaadin.version) you have selected is not supported by the plugin. " +
                        "Please pick one of the following supported Vaadin versions $supportedVersions. " +
                        'Alternatively you can add vaadin.unsupportedVersion=true to your build.gradle to override ' +
                        'this check but there is no guarantee it will work or that the build will be stable.')
        }
    }
}
