package com.tribus.markdown.actions

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ToggleCodeBlockActionTest : BasePlatformTestCase() {

    fun testWrapSelectedTextInCodeBlock() {
        myFixture.configureByText("test.md", "<selection>some code</selection>")
        myFixture.testAction(ToggleCodeBlockAction())
        myFixture.checkResult("```\nsome code\n```")
    }

    fun testUnwrapCodeBlock() {
        myFixture.configureByText("test.md", "<selection>```\nsome code\n```</selection>")
        myFixture.testAction(ToggleCodeBlockAction())
        myFixture.checkResult("some code")
    }

    fun testWrapEmptySelectionInCodeBlock() {
        myFixture.configureByText("test.md", "<selection></selection>")
        myFixture.testAction(ToggleCodeBlockAction())
        myFixture.checkResult("```\n\n```")
    }

    fun testWrapMultilineSelectionInCodeBlock() {
        myFixture.configureByText("test.md", "<selection>line one\nline two\nline three</selection>")
        myFixture.testAction(ToggleCodeBlockAction())
        myFixture.checkResult("```\nline one\nline two\nline three\n```")
    }
}
