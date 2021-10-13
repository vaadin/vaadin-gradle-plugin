package com.vaadin.gradle

import com.github.mvysny.dynatest.DynaNodeGroup
import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.dynatest.LateinitProperty
import kotlin.properties.ReadWriteProperty

/**
 * Prepares a test Gradle project - creates a temp dir for the [testProject] and allow you to run gradle
 * tasks. Does not generate `build.gradle` for you
 * though - just write the `build.gradle` contents to the [buildFile] instead.
 *
 * Call [build] to run the Gradle build on the test project.
 * @author mavi
 */
val vaadin14Version = "14.7.1"

/**
 * Sets up a folder for a test project.
 * @param gradleVersion which Gradle version to test with, for example "5.0" or "7.2".
 */
fun DynaNodeGroup.withTestProject(gradleVersion: GradleVersion): ReadWriteProperty<Any?, TestProject> {
    /**
     * The testing Gradle project. Automatically deleted after every test.
     */
    val testProjectProperty = LateinitProperty<TestProject>("testproject")
    var testProject: TestProject by testProjectProperty
    beforeEach {
        testProject = TestProject(gradleVersion)
        println("Test project directory: ${testProject.dir}")
    }
    afterEach {
        // comment out if a test is failing and you need to investigate the project files.
        testProject.delete()
    }
    return testProjectProperty
}

fun DynaNodeGroup.allTests(gradleVersion: GradleVersion) {
    group("Gradle $gradleVersion") {
        group("smoke tests") {
            vaadinSmokeTests(gradleVersion)
        }
        group("single module tests") {
            singleModuleTests(gradleVersion)
        }
        group("multi module tests") {
            multiModuleTests(gradleVersion)
        }
    }
}

class AllTests : DynaTest({
    // test with the oldest Gradle supported, but only on JDK 8 or 11 since
    // Gradle 5.0 doesn't really work on JDK 16+
    if (jvmVersion < 16) {
        allTests(GradleVersion(5, 0))
    }
    // test with the newest Gradle on all JDKs
    allTests(GradleVersion(7, 2))
})
