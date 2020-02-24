package com.vaadin.gradle

import org.junit.Test

class MiscMultiModuleTest : AbstractGradleTest() {
    /**
     * Tests https://github.com/vaadin/vaadin-gradle-plugin/issues/38
     */
    @Test
    fun testFileNotFoundException() {
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
        """)
        testProject.newFolder("lib")
        testProject.newFolder("web")

        // the vaadinPrepareFrontend task would work erratically because of dependent jars not yet produced.
        build("web:vaadinPrepareFrontend")
    }
}
