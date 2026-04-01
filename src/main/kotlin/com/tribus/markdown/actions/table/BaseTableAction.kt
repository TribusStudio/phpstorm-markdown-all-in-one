package com.tribus.markdown.actions.table

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.actions.MarkdownAction
import com.tribus.markdown.table.TableFormatter
import com.tribus.markdown.table.TableParser
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Base class for table modification actions. Subclasses implement
 * [transformTable] to apply their specific operation (insert, delete, swap, etc.)
 * on the parsed table. The base class handles finding the table, determining
 * cursor position (row/column), and writing the result back to the document.
 */
abstract class BaseTableAction : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    abstract fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document
        val text = document.text
        val caretLine = editor.caretModel.logicalPosition.line

        val table = TableParser.findTableAt(text, caretLine) ?: return

        // Determine which data row the cursor is on (0-based, -1 for header, -2 for separator)
        val dataRowIndex = caretLine - table.startLine - 2 // header=line0, separator=line1, first data=line2

        // Determine which column the cursor is in
        val lineStart = document.getLineStartOffset(caretLine)
        val localOffset = editor.caretModel.offset - lineStart
        val colIndex = findColumnIndex(document.getText(com.intellij.openapi.util.TextRange(lineStart, document.getLineEndOffset(caretLine))), localOffset)

        val newTable = transformTable(table, dataRowIndex, colIndex)
        val formatted = TableFormatter.format(newTable)

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(table.startOffset, table.endOffset, formatted)
        }
    }

    override fun update(e: AnActionEvent) {
        if (!MarkdownFileUtil.isMarkdownFile(e)) {
            e.presentation.isEnabled = false
            return
        }
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) {
            val caretLine = editor.caretModel.logicalPosition.line
            e.presentation.isEnabled = TableParser.findTableAt(editor.document.text, caretLine) != null
        } else {
            e.presentation.isEnabled = false
        }
    }

    companion object {
        /**
         * Determine which column (0-based) the cursor is in based on pipe positions.
         */
        fun findColumnIndex(lineText: String, localOffset: Int): Int {
            var col = 0
            var i = 0
            while (i < lineText.length && i < localOffset) {
                if (lineText[i] == '\\' && i + 1 < lineText.length && lineText[i + 1] == '|') {
                    i += 2
                } else if (lineText[i] == '|') {
                    col++
                    i++
                } else {
                    i++
                }
            }
            // col counts pipes seen so far; subtract 1 for leading pipe
            return (col - 1).coerceAtLeast(0)
        }
    }
}
