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

import org.gradle.testkit.runner.BuildResult
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
            }
            dependencies {
                // Vaadin 14
                compile("com.vaadin:vaadin-core:14.1.16") {
            //         Webjars are only needed when running in Vaadin 13 compatibility mode
                    ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                     "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                     "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                            .forEach { group -> exclude(group: group) }
                }
            }
        """)
    }

    @Test
    fun smoke() {
        build("vaadinClean")
    }

    @Test
    fun testPrepareNode() {
        build("vaadinPrepareNode")

        val nodejs = File(testProjectDir, "node")
        expect(true, nodejs.toString()) { nodejs.isDirectory }
    }

    @Test
    fun testPrepareFrontend() {
        build("vaadinPrepareNode", "vaadinPrepareFrontend")

        val generatedPackageJson = File(testProjectDir, "target/frontend/package.json")
        expect(true, generatedPackageJson.toString()) { generatedPackageJson.isFile }
        val generatedFlowBuildInfoJson = File(testProjectDir, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        expect(true, generatedFlowBuildInfoJson.toString()) { generatedPackageJson.isFile }
    }

    @Test
    fun testBuildFrontend() {
        val result: BuildResult = build("vaadinPrepareNode", "vaadinBuildFrontend")
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        // let's explicitly check that vaadinPrepareFrontend has been run
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProjectDir, "build/resources/main/META-INF/VAADIN/build")
        expect(true, build.toString()) { build.isDirectory }
        expect(true) { build.listFiles()!!.isNotEmpty() }
        build.find("*.gz", 5..7)
        build.find("*.js", 5..7)
        build.find("webcomponentsjs/webcomponents-*.js", 2..2)
        build.find("webcomponentsjs/bundles/webcomponents-*.js", 4..4)
    }
}
