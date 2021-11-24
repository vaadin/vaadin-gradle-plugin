package com.vaadin.gradle

import com.github.mvysny.dynatest.DynaTest
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import kotlin.test.expect

class ClasspathFilterTest : DynaTest({
    group("GlobMatcher") {
        test("*") {
            val m = GlobMatcher("*")
            expect(true) { m.test("flow-server") }
            expect(true) { m.test("com.vaadin") }
        }
        test("ends-with") {
            val m = GlobMatcher("com.*")
            expect(false) { m.test("flow-server") }
            expect(true) { m.test("com.vaadin") }
            expect(false) { m.test("org.foo") }
            expect(false) { m.test("comma.foo") }
        }
        test("string") {
            val m = GlobMatcher("flow-server")
            expect(true) { m.test("flow-server") }
            expect(false) { m.test("com.vaadin") }
            expect(false) { m.test("org.foo") }
        }
    }

    group("ModuleIdentifierPredicate") {
        test("*:*") {
            val m = ModuleIdentifierPredicate.fromGroupNameGlob("*:*")
            expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
            expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
            expect(true) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
        }
        test("com.vaadin:*") {
            val m = ModuleIdentifierPredicate.fromGroupNameGlob("com.vaadin:*")
            expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
            expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
            expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
        }
        test("com.vaadin:flow-*") {
            val m = ModuleIdentifierPredicate.fromGroupNameGlob("com.vaadin:flow-*")
            expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
            expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
            expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
        }
    }

    group("ClasspathFilter") {
        test("empty") {
            val m = ClasspathFilter().toPredicate()
            expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
            expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
            expect(true) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
        }
        group("exclude") {
            test("flow-server cannot be excluded") {
                val m = ClasspathFilter().apply {
                    exclude("com.vaadin:flow-*")
                }.toPredicate()
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-something")) }
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
                expect(true) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
            }
            test("multiple excludes") {
                val m = ClasspathFilter().apply {
                    exclude("com.vaadin:*")
                    exclude("org.foo:*")
                }.toPredicate()
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
            }
        }
        group("include") {
            test("flow-server cannot be excluded by omission") {
                val m = ClasspathFilter().apply {
                    include("com.vaadin:checkbox")
                }.toPredicate()
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-something")) }
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
            }
            test("multiple includes") {
                val m = ClasspathFilter().apply {
                    include("com.vaadin:*")
                    include("org.foo:*")
                }.toPredicate()
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
                expect(true) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("com.foo", "bar")) }
            }
        }
        group("include+exclude") {
            test("flow-server cannot be excluded by omission") {
                val m = ClasspathFilter().apply {
                    include("com.vaadin:checkbox")
                    exclude("com.vaadin:flow-server")
                }.toPredicate()
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-something")) }
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
            }
            test("exclude takes precedence") {
                val m = ClasspathFilter().apply {
                    include("com.vaadin:*")
                    exclude("com.vaadin:checkbox")
                }.toPredicate()
                expect(true) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "flow-server")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("com.vaadin", "checkbox")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("org.foo", "bar")) }
                expect(false) { m.test(DefaultModuleIdentifier.newId("com.foo", "bar")) }
            }
        }
    }
})
