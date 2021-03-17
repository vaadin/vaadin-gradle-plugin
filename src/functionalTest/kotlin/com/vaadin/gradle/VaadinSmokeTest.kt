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
                id 'war'
                id 'com.vaadin'
            }
            repositories {
                mavenCentral()
                jcenter()
                maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
            }
            dependencies {
                compile("com.vaadin:vaadin-core:$vaadinVersion")
                // @todo mavi see https://github.com/vaadin/flow/issues/10312 for more details
                compile("com.vaadin:fusion-endpoint:7.0.0.alpha1")
                providedCompile("javax.servlet:javax.servlet-api:3.1.0")
                compile("org.slf4j:slf4j-simple:1.7.30")
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

        val tokenFile = File(testProjectDir, "build/vaadin-generated/META-INF/VAADIN/config/flow-build-info.json")
        expect(true, tokenFile.toString()) { tokenFile.isFile }
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect(false, buildInfo.toJson()) { buildInfo.getBoolean(Constants.SERVLET_PARAMETER_PRODUCTION_MODE) }
    }

    @Test
    fun `vaadinBuildFrontend not ran by default in development mode`() {
        val result: BuildResult = build("build")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskOutcome("vaadinPrepareFrontend", TaskOutcome.SUCCESS)
        // vaadinBuildFrontend should NOT have been executed automatically
        result.expectTaskNotRan("vaadinBuildFrontend")

        val build = File(testProjectDir, "build/resources/main/META-INF/VAADIN/webapp/VAADIN/build")
        expect(false, build.toString()) { build.exists() }
    }

    @Test
    fun `vaadinBuildFrontend can be run manually in development mode`() {
        val result: BuildResult = build("vaadinBuildFrontend")
        // let's explicitly check that vaadinPrepareFrontend has been run.
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProjectDir, "build/resources/main/META-INF/VAADIN/webapp/VAADIN/build")
        expect(true, build.toString()) { build.exists() }
        build.find("*.gz", 5..10)
        build.find("*.js", 5..10)

        val tokenFile = File(testProjectDir, "build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect(false, buildInfo.toJson()) { buildInfo.getBoolean(Constants.SERVLET_PARAMETER_PRODUCTION_MODE) }
    }

    @Test
    fun testBuildFrontendInProductionMode() {
        val result: BuildResult = build("-Pvaadin.productionMode", "vaadinBuildFrontend")
        // vaadinBuildFrontend depends on vaadinPrepareFrontend
        // let's explicitly check that vaadinPrepareFrontend has been run
        result.expectTaskSucceded("vaadinPrepareFrontend")

        val build = File(testProjectDir, "build/resources/main/META-INF/VAADIN/webapp/VAADIN/build")
        expect(true, build.toString()) { build.isDirectory }
        expect(true) { build.listFiles()!!.isNotEmpty() }
        build.find("*.gz", 5..10)
        build.find("*.js", 5..10)
        val tokenFile = File(testProjectDir, "build/resources/main/META-INF/VAADIN/config/flow-build-info.json")
        val buildInfo: JsonObject = JsonUtil.parse(tokenFile.readText())
        expect(true, buildInfo.toJson()) { buildInfo.getBoolean(Constants.SERVLET_PARAMETER_PRODUCTION_MODE) }
    }

    @Test
    fun testBuildWarBuildsFrontendInProductionMode() {
        createProjectFile("src/main/java/org/vaadin/example/MainView.java", """
            package org.vaadin.example;

            import com.vaadin.flow.component.html.Span;
            import com.vaadin.flow.component.orderedlayout.VerticalLayout;
            import com.vaadin.flow.router.Route;

            @Route("")
            public class MainView extends VerticalLayout {

                public MainView() {
                    add(new Span("It works!"));
                }
            }
        """.trimIndent())

        val result: BuildResult = build("-Pvaadin.productionMode", "build")
        result.expectTaskSucceded("vaadinPrepareFrontend")
        result.expectTaskSucceded("vaadinBuildFrontend")
        File(testProjectDir, "build/libs").find("*.war", 1..1)
        // @todo mavi check WAR contents
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

    /**
     * Tests that VaadinClean task removes TS-related files.
     */
    @Test
    fun vaadinCleanDeletesTsFiles() {
        val tsconfigJson = testProjectDir.touch("tsconfig.json")
        val typesDTs = testProjectDir.touch("types.d.ts")
        build("vaadinClean")
        expect(false) { tsconfigJson.exists() }
        expect(false) { typesDTs.exists() }
    }
}
