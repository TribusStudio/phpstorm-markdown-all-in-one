package com.tribus.markdown.editor

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.tribus.markdown.util.MarkdownFileUtil

class MarkdownTypedHandler : TypedHandlerDelegate() {

    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        if (!MarkdownFileUtil.isMarkdownEditor(editor)) {
            return Result.CONTINUE
        }

        // If text is selected and the character has a wrapper, handle it
        if (editor.selectionModel.hasSelection()) {
            val handled = WriteCommandAction.writeCommandAction(project)
                .withName("Markdown Selection Wrap")
                .compute<Boolean, RuntimeException> {
                    SelectionWrapperService.handleIfSelected(c, editor)
                }
            if (handled) return Result.STOP
        }

        return Result.CONTINUE
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (!MarkdownFileUtil.isMarkdownEditor(editor)) {
            return Result.CONTINUE
        }

        // TODO: Phase 2 — smart list editing, auto-pairs, etc.

        return Result.CONTINUE
    }
}
