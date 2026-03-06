package com.tribus.markdown.actions

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class HeadingActionTest : BasePlatformTestCase() {

    // ── Heading Up ────────────────────────────────────────────────

    fun testHeadingUpOnPlainTextCreatesH1() {
        myFixture.configureByText("test.md", "Hello<caret> World")
        myFixture.testAction(HeadingUpAction())
        myFixture.checkResult("# Hello World")
    }

    fun testHeadingUpOnH1CreatesH2() {
        myFixture.configureByText("test.md", "# Hello<caret> World")
        myFixture.testAction(HeadingUpAction())
        myFixture.checkResult("## Hello World")
    }

    fun testHeadingUpOnH2CreatesH3() {
        myFixture.configureByText("test.md", "## Hello<caret> World")
        myFixture.testAction(HeadingUpAction())
        myFixture.checkResult("### Hello World")
    }

    fun testHeadingUpOnH5CreatesH6() {
        myFixture.configureByText("test.md", "##### Hello<caret> World")
        myFixture.testAction(HeadingUpAction())
        myFixture.checkResult("###### Hello World")
    }

    fun testHeadingUpOnH6StaysAtH6() {
        myFixture.configureByText("test.md", "###### Hello<caret> World")
        myFixture.testAction(HeadingUpAction())
        myFixture.checkResult("###### Hello World")
    }

    // ── Heading Down ──────────────────────────────────────────────

    fun testHeadingDownOnH2CreatesH1() {
        myFixture.configureByText("test.md", "## Hello<caret> World")
        myFixture.testAction(HeadingDownAction())
        myFixture.checkResult("# Hello World")
    }

    fun testHeadingDownOnH1RemovesHeading() {
        myFixture.configureByText("test.md", "# Hello<caret> World")
        myFixture.testAction(HeadingDownAction())
        myFixture.checkResult("Hello World")
    }

    fun testHeadingDownOnPlainTextDoesNothing() {
        myFixture.configureByText("test.md", "Hello<caret> World")
        myFixture.testAction(HeadingDownAction())
        myFixture.checkResult("Hello World")
    }

    fun testHeadingDownOnH6CreatesH5() {
        myFixture.configureByText("test.md", "###### Hello<caret> World")
        myFixture.testAction(HeadingDownAction())
        myFixture.checkResult("##### Hello World")
    }

    // ── Multi-line documents ──────────────────────────────────────

    fun testHeadingUpOnlyAffectsCurrentLine() {
        myFixture.configureByText("test.md", "first line\nHello<caret> World\nthird line")
        myFixture.testAction(HeadingUpAction())
        myFixture.checkResult("first line\n# Hello World\nthird line")
    }
}
