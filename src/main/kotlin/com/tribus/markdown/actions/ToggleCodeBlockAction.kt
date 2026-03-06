package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.util.MarkdownFileUtil

class ToggleCodeBlockAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document
        val selectionModel = editor.selectionModel

        WriteCommandAction.runWriteCommandAction(project) {
            val selectedText = selectionModel.selectedText ?: ""
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd

            val fence = "```"
            if (selectedText.startsWith(fence) && selectedText.endsWith(fence)) {
                // Remove code block
                val inner = selectedText
                    .removePrefix("$fence\n").removePrefix(fence)
                    .removeSuffix("\n$fence").removeSuffix(fence)
                document.replaceString(start, end, inner)
            } else {
                // Wrap in code block
                val wrapped = "$fence\n$selectedText\n$fence"
                document.replaceString(start, end, wrapped)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = MarkdownFileUtil.isMarkdownFile(e)
    }
}
