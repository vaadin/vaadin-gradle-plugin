package com.vaadin.gradle

import org.junit.After
import org.junit.Before

/**
 * Prepares a test Gradle project - creates a temp dir for the [testProject] and allow you to run gradle
 * tasks. Does not generate `build.gradle` for you
 * though - just write the `build.gradle` contents to the [buildFile] instead.
 *
 * Call [build] to run the Gradle build on the test project.
 * @author mavi
 */
abstract class AbstractGradleTest {

    val vaadin14Version = "14.6.7"

    /**
     * The testing Gradle project. Automatically deleted after every test.
     * Don't use TemporaryFolder JUnit `@Rule` since it will always delete the folder afterwards,
     * making it impossible to investigate the folder in case of failure.
     */
    lateinit var testProject: TestProject

    @Before
    fun createTestProjectFolder() {
        testProject = TestProject()
    }

    @After
    fun deleteTestProjectFolder() {
        // comment out if a test is failing and you need to investigate the project files.
        testProject.delete()
    }

    @Before
    fun dumpEnvironment() {
        println("Test project directory: ${testProject.dir}")
    }
}
