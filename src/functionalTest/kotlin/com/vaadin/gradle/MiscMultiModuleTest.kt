package com.vaadin.gradle

import org.junit.Test

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
            repositories {
                jcenter()
            }
            project(':lib') {
                apply plugin: 'java'
            }
            project(':web') {
                apply plugin: 'war'
                apply plugin: 'com.vaadin'
                
                dependencies {
                    compile project(':lib')
                }
            }
        """.trimIndent())
        testProject.newFolder("lib")
        testProject.newFolder("web")

        // the vaadinPrepareFrontend task would work erratically because of dependent jars not yet produced,
        // or it would blow up with FileNotFoundException straight away.
        build("web:vaadinPrepareFrontend")
    }
}
