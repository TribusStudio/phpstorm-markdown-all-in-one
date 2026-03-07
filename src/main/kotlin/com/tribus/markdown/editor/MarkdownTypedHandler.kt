package com.tribus.markdown.editor

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.tribus.markdown.util.MarkdownFileUtil

class MarkdownTypedHandler : TypedHandlerDelegate() {

    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        if (!MarkdownFileUtil.isMarkdownFile(file)) {
            return Result.CONTINUE
        }

        // If text is selected and the character has a wrapper, handle it.
        // beforeCharTyped runs inside the typing write action, so document
        // modifications are safe without an additional WriteCommandAction.
        if (editor.selectionModel.hasSelection()) {
            if (SelectionWrapperService.handleIfSelected(c, editor)) {
                return Result.STOP
            }
        }

        return Result.CONTINUE
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (!MarkdownFileUtil.isMarkdownFile(file)) {
            return Result.CONTINUE
        }

        // TODO: Phase 2 — smart list editing, auto-pairs, etc.

        return Result.CONTINUE
    }
}
