package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.tribus.markdown.util.MarkdownFileUtil

class ToggleTaskListAction : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document
        val caretModel = editor.caretModel

        val lineNumber = document.getLineNumber(caretModel.offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, lineEnd))

        WriteCommandAction.runWriteCommandAction(project) {
            val unchecked = Regex("^(\\s*[-*+]\\s)\\[ ](.*)").find(lineText)
            val checked = Regex("^(\\s*[-*+]\\s)\\[x](.*)").find(lineText)

            when {
                unchecked != null -> {
                    val prefix = unchecked.groupValues[1]
                    val rest = unchecked.groupValues[2]
                    document.replaceString(lineStart, lineEnd, "${prefix}[x]${rest}")
                }
                checked != null -> {
                    val prefix = checked.groupValues[1]
                    val rest = checked.groupValues[2]
                    document.replaceString(lineStart, lineEnd, "${prefix}[ ]${rest}")
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
