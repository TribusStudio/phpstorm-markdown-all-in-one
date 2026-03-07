package com.tribus.markdown.toc

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.tribus.markdown.settings.MarkdownSettings
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Auto-updates TOC in markdown files before save when the setting is enabled.
 */
class TocAutoUpdateListener : FileDocumentManagerListener {

    override fun beforeDocumentSaving(document: Document) {
        val settings = MarkdownSettings.getInstance()
        if (!settings.state.tocUpdateOnSave) return

        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!MarkdownFileUtil.isMarkdownFile(file)) return

        val range = TocGenerator.findTocRange(document.text) ?: return
        val newToc = TocGenerator.generateWithMarkers(document.text)
        val currentToc = document.text.substring(range.startOffset, range.endOffset)

        if (currentToc != newToc) {
            // Use WriteCommandAction to make the document modification
            // We need a project for this — get it from the file
            val projects = com.intellij.openapi.project.ProjectManager.getInstance().openProjects
            val project = projects.firstOrNull() ?: return

            WriteCommandAction.runWriteCommandAction(project, "Update Table of Contents", null, {
                document.replaceString(range.startOffset, range.endOffset, newToc)
            })
        }
    }
}
