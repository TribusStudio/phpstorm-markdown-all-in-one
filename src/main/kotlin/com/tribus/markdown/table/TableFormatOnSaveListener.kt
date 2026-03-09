package com.tribus.markdown.table

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.tribus.markdown.settings.MarkdownSettings
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Auto-formats all GFM tables in markdown files before save when the setting is enabled.
 */
class TableFormatOnSaveListener : FileDocumentManagerListener {

    override fun beforeDocumentSaving(document: Document) {
        val settings = MarkdownSettings.getInstance()
        val state = settings.state
        if (!state.tableFormatterEnabled || !state.tableFormatOnSave) return

        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!MarkdownFileUtil.isMarkdownFile(file)) return

        val formatted = TableFormatter.formatAll(document.text) ?: return

        val projects = com.intellij.openapi.project.ProjectManager.getInstance().openProjects
        val project = projects.firstOrNull() ?: return

        WriteCommandAction.runWriteCommandAction(project, "Format Tables", null, {
            document.setText(formatted)
        })
    }
}
