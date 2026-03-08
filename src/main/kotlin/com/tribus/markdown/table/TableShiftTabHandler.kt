package com.tribus.markdown.table

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler

/**
 * Handles Shift+Tab key inside GFM tables to navigate to the previous cell.
 * Falls through to the original handler when not inside a table.
 */
class TableShiftTabHandler(private val originalHandler: EditorActionHandler) : EditorActionHandler() {

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        if (!TableTabHandler.handleTableNavigation(editor, reverse = true)) {
            originalHandler.execute(editor, caret, dataContext)
        }
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean {
        return TableTabHandler.isInMarkdownTable(editor) || originalHandler.isEnabled(editor, caret, dataContext)
    }
}
