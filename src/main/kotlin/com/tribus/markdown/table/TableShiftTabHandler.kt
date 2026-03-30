package com.tribus.markdown.table

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.TextRange
import com.tribus.markdown.actions.ListOutdentAction

/**
 * Handles Shift+Tab in markdown files:
 * 1. Inside a GFM table: navigates to the previous cell
 * 2. On a list line: outdents the list item
 * 3. Otherwise: falls through to the original handler
 */
class TableShiftTabHandler(private val originalHandler: EditorActionHandler) : EditorActionHandler() {

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        // Table navigation takes priority
        if (TableTabHandler.handleTableNavigation(editor, reverse = true)) return

        // List outdentation
        if (handleListOutdent(editor)) return

        originalHandler.execute(editor, caret, dataContext)
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean {
        return TableTabHandler.isInMarkdownFile(editor) || originalHandler.isEnabled(editor, caret, dataContext)
    }

    companion object {
        private val LIST_MARKER_PATTERN = Regex("""^\s*([-+*]|[0-9]+[.)]) +""")

        fun handleListOutdent(editor: Editor): Boolean {
            if (!TableTabHandler.isInMarkdownFile(editor)) return false
            val document = editor.document
            val caretLine = editor.caretModel.logicalPosition.line
            val lineStart = document.getLineStartOffset(caretLine)
            val lineEnd = document.getLineEndOffset(caretLine)
            val lineText = document.getText(TextRange(lineStart, lineEnd))

            if (!LIST_MARKER_PATTERN.containsMatchIn(lineText)) return false

            val leadingSpaces = lineText.length - lineText.trimStart().length
            if (leadingSpaces == 0) return false

            val outdentSize = ListOutdentAction.determineOutdentSize(lineText, leadingSpaces)
            document.deleteString(lineStart, lineStart + outdentSize)
            return true
        }
    }
}
