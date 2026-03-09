package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.ui.Messages
import com.tribus.markdown.export.HtmlExporter
import com.tribus.markdown.util.MarkdownFileUtil
import java.io.File

class ExportHtmlAction : AnAction(), MarkdownAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val markdownFile = File(virtualFile.path)

        val descriptor = FileSaverDescriptor(
            "Export to HTML",
            "Choose where to save the HTML file",
            "html"
        )
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val defaultName = markdownFile.nameWithoutExtension + ".html"
        val wrapper = dialog.save(virtualFile.parent, defaultName) ?: return
        val outputFile = File(wrapper.file.path)

        try {
            val options = HtmlExporter.defaultOptions()
            val result = HtmlExporter.exportFile(markdownFile, outputFile, options)

            if (result.warnings.isNotEmpty()) {
                val warningText = result.warnings.joinToString("\n• ", prefix = "• ")
                Messages.showWarningDialog(
                    project,
                    "HTML exported to:\n${result.outputFile.absolutePath}\n\nWarnings:\n$warningText",
                    "Export Complete with Warnings"
                )
            } else {
                Messages.showInfoMessage(
                    project,
                    "HTML exported to:\n${result.outputFile.absolutePath}",
                    "Export Complete"
                )
            }
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                "Export failed: ${ex.message}",
                "Export Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
