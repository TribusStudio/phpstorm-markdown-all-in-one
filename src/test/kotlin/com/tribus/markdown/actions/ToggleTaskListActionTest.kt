package com.tribus.markdown.actions

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ToggleTaskListActionTest : BasePlatformTestCase() {

    fun testCheckUncheckedTask() {
        myFixture.configureByText("test.md", "- [ ] Buy<caret> groceries")
        myFixture.testAction(ToggleTaskListAction())
        myFixture.checkResult("- [x] Buy groceries")
    }

    fun testUncheckCheckedTask() {
        myFixture.configureByText("test.md", "- [x] Buy<caret> groceries")
        myFixture.testAction(ToggleTaskListAction())
        myFixture.checkResult("- [ ] Buy groceries")
    }

    fun testToggleTaskWithAsteriskMarker() {
        myFixture.configureByText("test.md", "* [ ] Task<caret> item")
        myFixture.testAction(ToggleTaskListAction())
        myFixture.checkResult("* [x] Task item")
    }

    fun testToggleTaskWithPlusMarker() {
        myFixture.configureByText("test.md", "+ [ ] Task<caret> item")
        myFixture.testAction(ToggleTaskListAction())
        myFixture.checkResult("+ [x] Task item")
    }

    fun testToggleIndentedTask() {
        myFixture.configureByText("test.md", "    - [ ] Nested<caret> task")
        myFixture.testAction(ToggleTaskListAction())
        myFixture.checkResult("    - [x] Nested task")
    }

    fun testNoOpOnNonTaskLine() {
        myFixture.configureByText("test.md", "Just some<caret> text")
        myFixture.testAction(ToggleTaskListAction())
        myFixture.checkResult("Just some text")
    }

    fun testNoOpOnPlainListItem() {
        myFixture.configureByText("test.md", "- Not a<caret> task")
        myFixture.testAction(ToggleTaskListAction())
        myFixture.checkResult("- Not a task")
    }
}
