package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.util.MarkdownFileUtil

class HeadingDownAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document
        val caretModel = editor.caretModel

        val lineNumber = document.getLineNumber(caretModel.offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))

        WriteCommandAction.runWriteCommandAction(project) {
            val headingMatch = Regex("^(#{2,6})\\s(.*)").find(lineText)
            if (headingMatch != null) {
                val hashes = headingMatch.groupValues[1]
                val content = headingMatch.groupValues[2]
                document.replaceString(lineStart, lineEnd, "${hashes.drop(1)} ${content}")
            } else {
                val singleHeading = Regex("^#\\s(.*)").find(lineText)
                if (singleHeading != null) {
                    // Remove heading entirely
                    document.replaceString(lineStart, lineEnd, singleHeading.groupValues[1])
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = MarkdownFileUtil.isMarkdownFile(e)
    }
}
