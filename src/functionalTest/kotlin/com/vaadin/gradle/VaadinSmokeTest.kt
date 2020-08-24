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

import com.vaadin.flow.server.Constants
import elemental.json.JsonObject
import elemental.json.impl.JsonUtil
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.expect

/**
 * The most basic tests. If these fail, the plugin is completely broken and all
 * other test classes will possibly fail as well.
 * @author mavi
 */
class VaadinSmokeTest : AbstractGradleTest() {
    @Before
    fun setup() {
        buildFile.writeText("""
            plugins {
                id 'com.vaadin'
            }
            repositories {
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                // Vaadin 17
                compile("com.vaadin:vaadin-core:$vaadin17Version") {
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

    @Test
    fun smoke() {
        build("vaadinClean")
    }

    @Test
    fun testPrepareFrontend() {
        build("vaadinPrepareFrontend")

        val generatedFlowBuildInfoJson = File(testProjectDir, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        expect(true, generatedFlowBuildInfoJson.toString()) { generatedFlowBuildInfoJson.isFile }
    }

    @Test
    fun `vaadinBuildFrontend not ran by default in development mode`() {
        val result: BuildResult = build("build")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)
        expect(null) { result.task(":vaadinBuildFrontend") }

        val build = File(testProjectDir, "build/resources/main/META-INF/VAADIN/build")
        expect(false, build.toString()) { build.exists() }
    }

    @Test
    fun `vaadinBuildFrontend can be run manually in development mode`() {
        val result: BuildResult = build("vaadinBuildFrontend")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProjectDir, "build/resources/main/META-INF/VAADIN/build")
        expect(true, build.toString()) { build.exists() }
        build.find("*.gz", 5..8)
        build.find("*.js", 5..8)

        val tokenFile = File(build, "../config/flow-build-info.json")
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect(false, buildInfo.toJson()) { buildInfo.getBoolean(Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER) }
    }

    @Test
    fun testBuildFrontendInProductionMode() {
        val result: BuildResult = build("-Pvaadin.productionMode", "vaadinBuildFrontend")
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        // let's explicitly check that vaadinPrepareFrontend has been run
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProjectDir, "build/resources/main/META-INF/VAADIN/build")
        expect(true, build.toString()) { build.isDirectory }
        expect(true) { build.listFiles()!!.isNotEmpty() }
        build.find("*.gz", 5..8)
        build.find("*.js", 5..8)
    }

    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/73
     */
    @Test
    fun vaadinCleanDoesntDeletePnpmFiles() {
        val pnpmLockYaml = testProjectDir.touch("pnpm-lock.yaml")
        val pnpmFileJs = testProjectDir.touch("pnpmfile.js")
        val webpackConfigJs = testProjectDir.touch("webpack.config.js")
        build("vaadinClean")
        expect(false) { pnpmLockYaml.exists() }
        expect(false) { pnpmFileJs.exists() }
        // don't delete webpack.config.js: https://github.com/vaadin/vaadin-gradle-plugin/pull/74#discussion_r444457296
        expect(true) { webpackConfigJs.exists() }
    }
}
