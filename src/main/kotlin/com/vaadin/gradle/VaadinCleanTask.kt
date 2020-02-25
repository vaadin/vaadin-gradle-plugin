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
        group = "Vaadin"
        description = "Cleans the project completely and removes node_modules, package*.json and webpack.*.js"

        dependsOn("clean")
    }

    @TaskAction
    fun clean() {
        project.delete("${project.projectDir}/node_modules",
                "${project.projectDir}/package.json",
                "${project.projectDir}/package-lock.json",
                // don't delete webpack.config.js: https://github.com/vaadin/vaadin-gradle-plugin/issues/43
//                "${project.projectDir}/webpack.config.js",
                "${project.projectDir}/webpack.generated.js")
    }
}
