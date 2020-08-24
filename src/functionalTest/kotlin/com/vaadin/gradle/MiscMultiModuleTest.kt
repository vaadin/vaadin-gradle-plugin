package com.vaadin.gradle

import org.gradle.testkit.runner.BuildResult
import org.junit.Test
import kotlin.test.expect

class MiscMultiModuleTest : AbstractGradleTest() {
    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/38
     */
    @Test
    fun `vaadinPrepareFrontend waits for artifacts from dependent projects`() {
        settingsFile.writeText("include 'lib', 'web'")
        buildFile.writeText("""
            plugins {
                id 'java'
                id 'com.vaadin' apply false
            }
            allprojects {
                repositories {
                    jcenter()
                    maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                }
            }
            project(':lib') {
                apply plugin: 'java'
            }
            project(':web') {
                apply plugin: 'war'
                apply plugin: 'com.vaadin'
                
                dependencies {
                    compile project(':lib')
                    compile("com.vaadin:vaadin-core:$vaadin17Version") {
                //         Webjars are only needed when running in Vaadin 13 compatibility mode
                        ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                         "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                         "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                                .forEach { group -> exclude(group: group) }
                    }
                }
            }
        """.trimIndent())
        testProject.newFolder("lib")
        testProject.newFolder("web")

        // the vaadinPrepareFrontend task would work erratically because of dependent jars not yet produced,
        // or it would blow up with FileNotFoundException straight away.
        build("web:vaadinPrepareFrontend")
    }

    /**
     * Tests that `vaadinPrepareFrontend` and `vaadinBuildFrontend` tasks are run only
     * on the `web` project.
     */
    @Test
    fun `vaadinBuildFrontend only runs on the web project`() {
        settingsFile.writeText("include 'lib', 'web'")
        buildFile.writeText("""
            plugins {
                id 'java'
                id 'com.vaadin' apply false
            }
            allprojects {
                repositories {
                    jcenter()
                    maven { url = 'https://maven.vaadin.com/vaadin-prereleases' }
                }
            }
            project(':lib') {
                apply plugin: 'java'
            }
            project(':web') {
                apply plugin: 'war'
                apply plugin: 'com.vaadin'
                
                dependencies {
                    compile project(':lib')
                    compile("com.vaadin:vaadin-core:$vaadin17Version") {
                //         Webjars are only needed when running in Vaadin 13 compatibility mode
                        ["com.vaadin.webjar", "org.webjars.bowergithub.insites",
                         "org.webjars.bowergithub.polymer", "org.webjars.bowergithub.polymerelements",
                         "org.webjars.bowergithub.vaadin", "org.webjars.bowergithub.webcomponents"]
                                .forEach { group -> exclude(group: group) }
                    }
                }
            }
        """.trimIndent())
        testProject.newFolder("lib")
        testProject.newFolder("web")

        val b: BuildResult = build("-Pvaadin.productionMode", "vaadinBuildFrontend", checkTasksSuccessful = false)
        b.expectTaskSucceded("web:vaadinPrepareFrontend")
        b.expectTaskSucceded("web:vaadinBuildFrontend")
        expect(null) { b.task(":lib:vaadinPrepareFrontend") }
        expect(null) { b.task(":lib:vaadinBuildFrontend") }
        expect(null) { b.task(":vaadinPrepareFrontend") }
        expect(null) { b.task(":vaadinBuildFrontend") }
    }
}
