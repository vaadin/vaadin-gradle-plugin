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
package com.vaadin.gradle.tasks

import com.vaadin.gradle.actions.JavaPluginAction
import com.vaadin.gradle.models.ApplicationType
import com.vaadin.gradle.creators.VaadinProjectCreator
import com.vaadin.gradle.creators.VaadinThemeCreator
import com.vaadin.gradle.extensions.VaadinPluginExtension
import com.vaadin.gradle.models.ProjectType
import com.vaadin.gradle.models.VaadinProject

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.nio.file.Paths

/**
 * Creates a new project using a template
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateProjectTask extends DefaultTask {

    static final String NAME = 'vaadinCreateProject'

    private static final String LUMO = 'lumo'

    @Option(option = 'name', description = 'Application name')
    String applicationName

    @Option(option = 'package', description = 'Application UI package')
    String applicationPackage

    @Option(option = 'baseTheme', description = "The base theme of the application, can be either 'lumo' or 'material'")
    String applicationBaseTheme

    private final VaadinProjectCreator projectCreator = new VaadinProjectCreator()
    private final VaadinThemeCreator themeCreator = new VaadinThemeCreator()

    CreateProjectTask() {
        description = 'Creates a Vaadin Flow project'
        group = 'Vaadin'
    }

    @TaskAction
    void run() {

        if (applicationBaseTheme &&
                applicationBaseTheme.toLowerCase() != 'material' &&
                applicationBaseTheme.toLowerCase() != LUMO) {
            throw new GradleException("Wrong base theme value. Valid values are 'lumo' or 'material'")
        }

        VaadinPluginExtension vaadin = project.extensions.getByType(VaadinPluginExtension)
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        VaadinProject vaadinProject = new VaadinProject(
                applicationName: applicationName ?: project.name.capitalize(),
                applicationPackage : applicationPackage ?: "com.example.${project.name.toLowerCase()}",
                applicationBaseTheme :applicationBaseTheme ?: LUMO,
                rootDirectory : project.projectDir,
                webappDirectory: assembleTask.webappDir,
                productionMode : vaadin.productionMode,
                compatibilityMode: vaadin.compatibilityMode,
                projectType: ProjectType.get(project),
                applicationType: ApplicationType.get(project)
        )

        projectCreator.generate(vaadinProject)

        themeCreator.generateCssTheme(vaadinProject)
    }

}
