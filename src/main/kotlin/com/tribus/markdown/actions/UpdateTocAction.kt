package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.toc.TocGenerator
import com.tribus.markdown.util.MarkdownFileUtil

class UpdateTocAction : AnAction(), MarkdownAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document
        val documentText = document.text

        val range = TocGenerator.findTocRange(documentText)
        if (range == null) {
            // No TOC region found — nothing to update
            return
        }

        val newToc = TocGenerator.generateWithMarkers(documentText)
        val currentToc = documentText.substring(range.startOffset, range.endOffset)

        if (currentToc != newToc) {
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(range.startOffset, range.endOffset, newToc)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val hasFile = MarkdownFileUtil.isMarkdownFile(e)
        e.presentation.isEnabled = hasFile
        // Show "Update" only when TOC exists
        if (hasFile) {
            val editor = e.getData(CommonDataKeys.EDITOR)
            if (editor != null) {
                val hasToc = TocGenerator.findTocRange(editor.document.text) != null
                e.presentation.isEnabled = hasToc
            }
        }
    }
}
