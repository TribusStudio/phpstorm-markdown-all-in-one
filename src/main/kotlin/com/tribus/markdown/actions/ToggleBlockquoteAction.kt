package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Toggles blockquote (`> `) prefix on the current line or selection.
 * If all selected lines are already blockquoted, removes the prefix.
 * Otherwise, adds the prefix to all lines.
 */
class ToggleBlockquoteAction : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val startLine = document.getLineNumber(
            if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else editor.caretModel.offset
        )
        val endLine = document.getLineNumber(
            if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionEnd else editor.caretModel.offset
        )

        // Check if all lines are already blockquoted
        val allQuoted = (startLine..endLine).all { line ->
            val lineStart = document.getLineStartOffset(line)
            val lineEnd = document.getLineEndOffset(line)
            val lineText = document.getText(TextRange(lineStart, lineEnd))
            lineText.trimStart().startsWith(">")
        }

        WriteCommandAction.runWriteCommandAction(project) {
            for (line in endLine downTo startLine) {
                val lineStart = document.getLineStartOffset(line)
                val lineEnd = document.getLineEndOffset(line)
                val lineText = document.getText(TextRange(lineStart, lineEnd))

                if (allQuoted) {
                    // Remove blockquote prefix
                    val stripped = lineText.replaceFirst(Regex("^(\\s*)>\\s?"), "$1")
                    document.replaceString(lineStart, lineEnd, stripped)
                } else {
                    // Add blockquote prefix
                    document.insertString(lineStart, "> ")
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
