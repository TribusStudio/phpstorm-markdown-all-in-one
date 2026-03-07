package com.tribus.markdown.actions.generate

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.actions.MarkdownAction
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Inserts an image markdown tag at the cursor.
 * If text is selected, uses it as the alt text.
 */
class GenerateImageAction : AnAction("Image", "Insert an image tag", null), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val selectedText = editor.selectionModel.selectedText
        val altText = if (!selectedText.isNullOrEmpty()) selectedText else "alt text"
        val selStart = if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else editor.caretModel.offset
        val selEnd = if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionEnd else editor.caretModel.offset

        val image = "![${altText}]()"
        val cursorOffset = selStart + image.length - 1  // Position inside the ()

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(selStart, selEnd, image)
            editor.caretModel.moveToOffset(cursorOffset)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
