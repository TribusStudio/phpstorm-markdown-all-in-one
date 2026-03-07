package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Outdents list items by removing leading spaces. Removes one indent level
 * (width of the list marker + trailing space). On non-list lines, removes
 * up to 2 leading spaces.
 */
class ListOutdentAction : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document
        val selectionModel = editor.selectionModel

        val startLine = document.getLineNumber(
            if (selectionModel.hasSelection()) selectionModel.selectionStart else editor.caretModel.offset
        )
        val endLine = document.getLineNumber(
            if (selectionModel.hasSelection()) selectionModel.selectionEnd else editor.caretModel.offset
        )

        WriteCommandAction.runWriteCommandAction(project) {
            for (line in endLine downTo startLine) {
                val lineStart = document.getLineStartOffset(line)
                val lineEnd = document.getLineEndOffset(line)
                val lineText = document.getText(TextRange(lineStart, lineEnd))

                if (lineText.isBlank()) continue

                val leadingSpaces = lineText.length - lineText.trimStart().length
                if (leadingSpaces == 0) continue

                val outdentSize = determineOutdentSize(lineText, leadingSpaces)
                document.deleteString(lineStart, lineStart + outdentSize)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }

    companion object {
        /**
         * Determines how many spaces to remove when outdenting.
         * Uses the list marker width for proper nesting alignment.
         */
        fun determineOutdentSize(lineText: String, leadingSpaces: Int): Int {
            val indentSize = ListIndentAction.determineIndentSize(lineText)
            return minOf(indentSize, leadingSpaces)
        }
    }
}
