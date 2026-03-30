package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Copies the current line (or selection) up or down, then renumbers
 * any affected ordered lists.
 */
class CopyLineUpAction : AnAction(), MarkdownAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) { e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e) }
    override fun actionPerformed(e: AnActionEvent) = CopyLineAction.copy(e, -1)
}

class CopyLineDownAction : AnAction(), MarkdownAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) { e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e) }
    override fun actionPerformed(e: AnActionEvent) = CopyLineAction.copy(e, 1)
}

object CopyLineAction {

    fun copy(e: AnActionEvent, direction: Int) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val startLine = document.getLineNumber(
            if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else editor.caretModel.offset
        )
        val endLine = document.getLineNumber(
            if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionEnd else editor.caretModel.offset
        )

        // Collect the lines to duplicate
        val blockStart = document.getLineStartOffset(startLine)
        val blockEnd = document.getLineEndOffset(endLine)
        val blockText = document.getText(TextRange(blockStart, blockEnd))

        WriteCommandAction.runWriteCommandAction(project) {
            if (direction < 0) {
                // Copy up: insert duplicate above the block
                document.insertString(blockStart, "$blockText\n")
                // Caret stays at original position (now in the upper copy)
            } else {
                // Copy down: insert duplicate below the block
                document.insertString(blockEnd, "\n$blockText")
                // Move caret to the new copy
                val newStartLine = endLine + 1
                val newOffset = document.getLineStartOffset(newStartLine) +
                    (editor.caretModel.offset - blockStart).coerceAtMost(blockText.length)
                editor.caretModel.moveToOffset(newOffset)
            }

            // Renumber ordered lists in the affected area
            val affectStart = maxOf(0, startLine - 1)
            val affectEnd = minOf(document.lineCount - 1, endLine + (endLine - startLine) + 2)
            MoveLineAction.renumberOrderedLists(document, affectStart, affectEnd)
        }
    }
}
