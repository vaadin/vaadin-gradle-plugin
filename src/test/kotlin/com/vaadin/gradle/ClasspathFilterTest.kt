package com.vaadin.gradle

import com.github.mvysny.dynatest.DynaTest
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
})
