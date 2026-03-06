package com.tribus.markdown.actions

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ToggleFormattingActionTest : BasePlatformTestCase() {

    // ── Bold ──────────────────────────────────────────────────────

    fun testToggleBoldWrapsSelectedText() {
        myFixture.configureByText("test.md", "hello <selection>world</selection>")
        myFixture.testAction(ToggleBoldAction())
        myFixture.checkResult("hello **world**")
    }

    fun testToggleBoldUnwrapsAlreadyBoldText() {
        myFixture.configureByText("test.md", "hello <selection>**world**</selection>")
        myFixture.testAction(ToggleBoldAction())
        myFixture.checkResult("hello world")
    }

    fun testToggleBoldWrapsWordAtCursor() {
        myFixture.configureByText("test.md", "hello wor<caret>ld")
        myFixture.testAction(ToggleBoldAction())
        myFixture.checkResult("hello **world**")
    }

    fun testToggleBoldInsertsEmptyMarkersWhenNoWordAtCursor() {
        myFixture.configureByText("test.md", "hello <caret> world")
        myFixture.testAction(ToggleBoldAction())
        myFixture.checkResult("hello **<caret>** world")
    }

    // ── Italic ────────────────────────────────────────────────────

    fun testToggleItalicWrapsSelectedText() {
        myFixture.configureByText("test.md", "hello <selection>world</selection>")
        myFixture.testAction(ToggleItalicAction())
        myFixture.checkResult("hello *world*")
    }

    fun testToggleItalicUnwrapsAlreadyItalicText() {
        myFixture.configureByText("test.md", "hello <selection>*world*</selection>")
        myFixture.testAction(ToggleItalicAction())
        myFixture.checkResult("hello world")
    }

    // ── Strikethrough ─────────────────────────────────────────────

    fun testToggleStrikethroughWrapsSelectedText() {
        myFixture.configureByText("test.md", "hello <selection>world</selection>")
        myFixture.testAction(ToggleStrikethroughAction())
        myFixture.checkResult("hello ~~world~~")
    }

    fun testToggleStrikethroughUnwrapsAlreadyStruckText() {
        myFixture.configureByText("test.md", "hello <selection>~~world~~</selection>")
        myFixture.testAction(ToggleStrikethroughAction())
        myFixture.checkResult("hello world")
    }

    // ── Code Span ─────────────────────────────────────────────────

    fun testToggleCodeSpanWrapsSelectedText() {
        myFixture.configureByText("test.md", "hello <selection>world</selection>")
        myFixture.testAction(ToggleCodeSpanAction())
        myFixture.checkResult("hello `world`")
    }

    fun testToggleCodeSpanUnwrapsAlreadyCodeText() {
        myFixture.configureByText("test.md", "hello <selection>`world`</selection>")
        myFixture.testAction(ToggleCodeSpanAction())
        myFixture.checkResult("hello world")
    }

    // ── Multiline selection ───────────────────────────────────────

    fun testToggleBoldOnMultilineSelection() {
        myFixture.configureByText("test.md", "<selection>hello\nworld</selection>")
        myFixture.testAction(ToggleBoldAction())
        myFixture.checkResult("**hello\nworld**")
    }
}
