package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.tribus.markdown.export.HtmlExporter
import java.io.File

class BatchExportHtmlAction : AnAction(), MarkdownAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Select input folder
        val inputDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        inputDescriptor.title = "Select Markdown Folder"
        inputDescriptor.description = "Choose a folder containing markdown files to export"
        val inputFolder = FileChooser.chooseFile(inputDescriptor, project, null) ?: return

        // Select output folder
        val outputDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        outputDescriptor.title = "Select Output Folder"
        outputDescriptor.description = "Choose where to save the exported HTML files"
        val outputFolder = FileChooser.chooseFile(outputDescriptor, project, null) ?: return

        val inputDir = File(inputFolder.path)
        val outputDir = File(outputFolder.path)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Exporting Markdown to HTML", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                val options = HtmlExporter.defaultOptions()
                val results = HtmlExporter.exportDirectory(inputDir, outputDir, options, recursive = true)

                val totalWarnings = results.flatMap { it.warnings }

                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    if (results.isEmpty()) {
                        Messages.showInfoMessage(project, "No markdown files found in:\n$inputDir", "Batch Export")
                    } else if (totalWarnings.isNotEmpty()) {
                        val warningText = totalWarnings.take(20).joinToString("\n• ", prefix = "• ")
                        val suffix = if (totalWarnings.size > 20) "\n… and ${totalWarnings.size - 20} more" else ""
                        Messages.showWarningDialog(
                            project,
                            "Exported ${results.size} files to:\n$outputDir\n\nWarnings:\n$warningText$suffix",
                            "Batch Export Complete with Warnings"
                        )
                    } else {
                        Messages.showInfoMessage(
                            project,
                            "Exported ${results.size} files to:\n$outputDir",
                            "Batch Export Complete"
                        )
                    }
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
