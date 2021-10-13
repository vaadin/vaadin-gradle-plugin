package com.vaadin.gradle

import com.github.mvysny.dynatest.DynaNodeGroup
import java.lang.RuntimeException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Prepares a test Gradle project - creates a temp dir for the [testProject] and allow you to run gradle
 * tasks. Does not generate `build.gradle` for you
 * though - just write the `build.gradle` contents to the [buildFile] instead.
 *
 * Call [build] to run the Gradle build on the test project.
 * @author mavi
 */
val vaadin14Version = "14.6.7"

/**
 * Sets up a folder for a test project.
 */
fun DynaNodeGroup.withTestProject(): ReadWriteProperty<Any?, TestProject> {
    /**
     * The testing Gradle project. Automatically deleted after every test.
     */
    val testProjectProperty = LateinitProperty<TestProject>("testproject")
    var testProject: TestProject by testProjectProperty
    beforeEach {
        testProject = TestProject()
        println("Test project directory: ${testProject.dir}")
    }
    afterEach {
        // comment out if a test is failing and you need to investigate the project files.
        testProject.delete()
    }
    return testProjectProperty
}

data class LateinitProperty<V: Any>(val name: String, private var value: V? = null) : ReadWriteProperty<Any?, V> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        return value ?: throw RuntimeException("$this: not initialized")
    }
}
