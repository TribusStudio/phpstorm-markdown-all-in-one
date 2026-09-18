package com.tribus.markdown.util

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor

/**
 * Helpers for mutating documents from editor action handlers.
 *
 * Editor action handlers run on the EDT but — since the 2024.2 threading model —
 * the platform no longer grants them implicit write access. Any document
 * mutation has to be wrapped explicitly, otherwise the platform throws
 * "Write access is allowed inside write-action only".
 */
object EditorWriteUtil {

    /**
     * True when [editor] backs a document that we may actually modify.
     * Guards against viewers, consoles and read-only files, which share the
     * same global editor action handlers as regular editors.
     */
    fun isWritable(editor: Editor): Boolean =
        !editor.isViewer && editor.document.isWritable

    /**
     * Run [block] inside a write command so it is undoable and satisfies the
     * platform's write-access assertion.
     */
    fun runWriteCommand(editor: Editor, dataContext: DataContext?, name: String, block: () -> Unit) {
        val project = dataContext?.getData(CommonDataKeys.PROJECT) ?: editor.project
        WriteCommandAction.runWriteCommandAction(project, name, null, block)
    }
}
