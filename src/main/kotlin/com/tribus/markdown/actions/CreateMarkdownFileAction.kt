package com.tribus.markdown.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.tribus.markdown.lang.MarkdownIcons

/**
 * Adds "Markdown File" to the New file menu (right-click > New, or Cmd+N in project view).
 * Shows a template chooser dialog with multiple markdown templates.
 */
class CreateMarkdownFileAction : CreateFileFromTemplateAction(
    "Markdown File",
    "Create a new Markdown file from template",
    MarkdownIcons.FILE
) {

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder.setTitle("New Markdown File")
            .addKind("Blank", MarkdownIcons.FILE, "Blank Markdown")
            .addKind("README", MarkdownIcons.FILE, "README")
            .addKind("Document", MarkdownIcons.FILE, "Document")
            .addKind("Meeting Notes", MarkdownIcons.FILE, "Meeting Notes")
            .addKind("Changelog", MarkdownIcons.FILE, "Changelog")
            .addKind("API Documentation", MarkdownIcons.FILE, "API Documentation")
    }

    override fun getActionName(
        directory: PsiDirectory,
        newName: String,
        templateName: String
    ): String = "Create Markdown File: $newName"
}
