package com.tribus.markdown.table

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Regression coverage for the Tab / Shift+Tab editor action handlers.
 *
 * These run through the real editor action chain, which is the path that used
 * to throw "Write access is allowed inside write-action only" — the handlers
 * mutated the document without wrapping it in a write command.
 */
class TableTabHandlerTest : BasePlatformTestCase() {

    // ── Tab: list indentation ────────────────────────────────────────────

    fun testTabIndentsUnorderedListItem() {
        myFixture.configureByText("test.md", "- item<caret>\n")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_TAB)
        myFixture.checkResult("  - item\n")
    }

    fun testTabIndentsOrderedListItem() {
        myFixture.configureByText("test.md", "1. item<caret>\n")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_TAB)
        myFixture.checkResult("   1. item\n")
    }

    fun testIndentIsUndoable() {
        myFixture.configureByText("test.md", "- item<caret>\n")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_TAB)
        myFixture.checkResult("  - item\n")

        // The mutation must run inside a write *command*, otherwise it is not
        // registered with the undo manager.
        myFixture.performEditorAction(IdeActions.ACTION_UNDO)
        myFixture.checkResult("- item\n")
    }

    // ── Shift+Tab: list outdentation ─────────────────────────────────────

    fun testShiftTabOutdentsListItem() {
        myFixture.configureByText("test.md", "  - item<caret>\n")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_UNINDENT_SELECTION)
        myFixture.checkResult("- item\n")
    }

    fun testShiftTabLeavesTopLevelItemAlone() {
        myFixture.configureByText("test.md", "- item<caret>\n")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_UNINDENT_SELECTION)
        myFixture.checkResult("- item\n")
    }

    // ── Direct companion-API calls (the crashing call site) ──────────────

    fun testHandleListIndentWrapsItsOwnWriteAction() {
        myFixture.configureByText("test.md", "- item<caret>\n")
        val handled = TableTabHandler.handleListIndent(myFixture.editor)
        assertTrue("expected the list line to be handled", handled)
        assertEquals("  - item\n", myFixture.editor.document.text)
    }

    fun testHandleListOutdentWrapsItsOwnWriteAction() {
        myFixture.configureByText("test.md", "  - item<caret>\n")
        val handled = TableShiftTabHandler.handleListOutdent(myFixture.editor)
        assertTrue("expected the list line to be handled", handled)
        assertEquals("- item\n", myFixture.editor.document.text)
    }

    fun testHandleListIndentIgnoresNonListLines() {
        myFixture.configureByText("test.md", "plain text<caret>\n")
        assertFalse(TableTabHandler.handleListIndent(myFixture.editor))
        assertEquals("plain text\n", myFixture.editor.document.text)
    }

    // ── Tab: table navigation still wins over list indentation ───────────

    fun testTabNavigatesToNextTableCell() {
        myFixture.configureByText(
            "test.md",
            """
            | A<caret> | B |
            | --- | --- |
            | 1 | 2 |
            """.trimIndent()
        )
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_TAB)

        // Navigation selects the next cell's content rather than editing text.
        assertEquals("B", myFixture.editor.selectionModel.selectedText)
    }
}
