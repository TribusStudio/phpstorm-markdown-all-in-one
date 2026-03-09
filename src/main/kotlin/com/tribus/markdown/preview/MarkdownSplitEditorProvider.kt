package com.tribus.markdown.preview

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
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

        return MarkdownSplitEditor(textEditor, previewEditor)
    }

    override fun getEditorTypeId(): String = "markdown-aio-split-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

/**
 * A split editor that combines a text editor with a markdown preview.
 * Inherits the three-state toggle (Editor | Split | Preview) from TextEditorWithPreview.
 */
class MarkdownSplitEditor(
    editor: TextEditor,
    private val preview: MarkdownPreviewFileEditor
) : TextEditorWithPreview(editor, preview, "Markdown Editor", Layout.SHOW_EDITOR_AND_PREVIEW) {

    fun getPreviewEditor(): MarkdownPreviewFileEditor = preview
}
