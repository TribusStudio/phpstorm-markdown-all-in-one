package com.tribus.markdown.preview

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Provides a split editor for markdown files with three modes:
 * - Editor only
 * - Preview only
 * - Split (editor + preview side by side)
 *
 * Uses IntelliJ's TextEditorWithPreview for the toggle UI.
 */
class MarkdownSplitEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return MarkdownFileUtil.isMarkdownFile(file)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
        val document = textEditor.editor.document
        val previewEditor = MarkdownPreviewFileEditor(file, document)

        return MarkdownSplitEditor(project, textEditor, previewEditor)
    }

    override fun getEditorTypeId(): String = "markdown-aio-split-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
