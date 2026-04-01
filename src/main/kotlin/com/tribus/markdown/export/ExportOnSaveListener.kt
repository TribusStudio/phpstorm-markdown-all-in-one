package com.tribus.markdown.export

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.tribus.markdown.settings.MarkdownSettings
import com.tribus.markdown.util.MarkdownFileUtil
import java.io.File

/**
 * Automatically exports the markdown file to HTML when saving,
 * if the `exportOnSave` setting is enabled. The HTML file is written
 * alongside the markdown file with the same name and .html extension.
 */
class ExportOnSaveListener : FileDocumentManagerListener {

    override fun beforeDocumentSaving(document: Document) {
        val enabled = try {
            MarkdownSettings.getInstance().state.exportOnSave
        } catch (_: Exception) { false }
        if (!enabled) return

        val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!MarkdownFileUtil.isMarkdownFile(virtualFile)) return

        val mdFile = File(virtualFile.path)
        val htmlFile = File(mdFile.parentFile, mdFile.nameWithoutExtension + ".html")

        try {
            HtmlExporter.exportFile(mdFile, htmlFile)
        } catch (_: Exception) {
            // Silently fail — auto-export shouldn't interrupt the save flow
        }
    }
}
