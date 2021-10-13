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

import com.github.mvysny.dynatest.DynaTest
import com.vaadin.flow.server.Constants
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.test.expect

/**
 * The most basic tests. If these fail, the plugin is completely broken and all
 * other test classes will possibly fail as well.
 * @author mavi
 */
class VaadinSmokeTest : DynaTest({
    val testProject: TestProject by withTestProject()

    beforeEach {
        testProject.buildFile.writeText("""
            plugins {
                id 'com.vaadin'
            }
            repositories {
                mavenCentral()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                // Vaadin 14
                compile("com.vaadin:vaadin-core:$vaadin14Version") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
            }
            vaadin {
                pnpmEnable = true
            }
        """)
    }

    test("smoke") {
        testProject.build("vaadinClean")
    }

    test("PrepareFrontend") {
        testProject.build("vaadinPrepareFrontend")

        val generatedFlowBuildInfoJson = File(testProject.dir, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        expect(true, generatedFlowBuildInfoJson.toString()) { generatedFlowBuildInfoJson.isFile }
    }

    test("vaadinBuildFrontend not ran by default in development mode") {
        val result: BuildResult = testProject.build("build")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)
        // vaadinBuildFrontend should NOT have been executed automatically
        result.expectTaskNotRan("vaadinBuildFrontend")

        val build = File(testProject.dir, "build/resources/main/META-INF/VAADIN/build")
        expect(false, build.toString()) { build.exists() }
    }

    test("vaadinBuildFrontend can be run manually in development mode") {
        val result: BuildResult = testProject.build("vaadinBuildFrontend")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProject.dir, "build/resources/main/META-INF/VAADIN/build")
        expect(true, build.toString()) { build.exists() }
        build.find("*.gz", 5..7)
        build.find("*.js", 5..7)
        build.find("webcomponentsjs/webcomponents-*.js", 2..2)
        build.find("webcomponentsjs/bundles/webcomponents-*.js", 4..6)

        val tokenFile = File(build, "../config/flow-build-info.json")
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect(false, buildInfo.toJson()) { buildInfo.getBoolean(Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER) }
    }

    test("BuildFrontendInProductionMode") {
        val result: BuildResult = testProject.build("-Pvaadin.productionMode", "vaadinBuildFrontend")
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        // let's explicitly check that vaadinPrepareFrontend has been run
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProject.dir, "build/resources/main/META-INF/VAADIN/build")
        expect(true, build.toString()) { build.isDirectory }
        expect(true) { build.listFiles()!!.isNotEmpty() }
        build.find("*.gz", 5..7)
        build.find("*.js", 5..7)
        build.find("webcomponentsjs/webcomponents-*.js", 2..2)
        build.find("webcomponentsjs/bundles/webcomponents-*.js", 4..6)
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/73
     */
    test("vaadinCleanDoesntDeletePnpmFiles") {
        val pnpmLockYaml = testProject.newFile("pnpm-lock.yaml")
        val pnpmFileJs = testProject.newFile("pnpmfile.js")
        val webpackConfigJs = testProject.newFile("webpack.config.js")
        testProject.build("vaadinClean")
        expect(false) { pnpmLockYaml.exists() }
        expect(false) { pnpmFileJs.exists() }
        // don't delete webpack.config.js: https://github.com/vaadin/vaadin-gradle-plugin/pull/74#discussion_r444457296
        expect(true) { webpackConfigJs.exists() }
    }
})
