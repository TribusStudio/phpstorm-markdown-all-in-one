package com.tribus.markdown.editor

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.tribus.markdown.util.MarkdownFileUtil

class MarkdownBackspaceHandler : BackspaceHandlerDelegate() {

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        // TODO: Phase 2 — smart backspace for list items
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (!MarkdownFileUtil.isMarkdownEditor(editor)) {
            return false
        }

        // TODO: Phase 2 — smart backspace handling

        return false
    }
}
