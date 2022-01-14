package com.vaadin.gradle

import com.github.mvysny.dynatest.*
import kotlin.properties.ReadWriteProperty

/**
 * The Vaadin version to test with. Generally the newest 14.x release.
 */
val vaadin14Version = "14.8.1"

/**
 * Sets up a folder for a test project -
 * creates a temp dir for the [TestProject] and allow you to run gradle
 * tasks. Does not generate `build.gradle` for you
 * though - just write the `build.gradle` contents to the [TestProject.buildFile] instead.
 * @param gradleVersion which Gradle version to test with.
 */
@DynaTestDsl
fun DynaNodeGroup.withTestProject(gradleVersion: GradleVersion): ReadWriteProperty<Any?, TestProject> {
    // A new testing Gradle project. Automatically deleted after every test.
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

/**
 * Runs all tests on given [gradleVersion].
 */
@DynaTestDsl
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

/**
 * The test class which runs all tests. See the
 * [DynaTest](https://github.com/mvysny/dynatest) testing framework on
 * more details on how exactly this works.
 *
 * See [Gradle Releases](https://gradle.org/releases/) to find the available
 * Gradle versions.
 */
class AllTests : DynaTest({
    // test with the oldest Gradle supported, but only on JDK 8 or 11 since
    // Gradle 6.x and lower doesn't really work on JDK 16+
    if (jvmVersion <= 15) {
        allTests(GradleVersion.V5_0)
        allTests(GradleVersion(5, 6, 4))

        // test with Gradle 6.x as well.
        allTests(GradleVersion(6, 9, 1))
    }
    // test with the newest Gradle on all JDKs
    allTests(GradleVersion(7, 3))
})
