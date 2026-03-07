package com.tribus.markdown.editor

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.tribus.markdown.util.MarkdownFileUtil

class MarkdownTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (!MarkdownFileUtil.isMarkdownFile(file)) {
            return Result.CONTINUE
        }

        // TODO: Phase 2 — smart list editing, auto-pairs, etc.

        return Result.CONTINUE
    }
}
