package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.util.MarkdownFileUtil
import com.tribus.markdown.util.MarkdownFormattingUtil
import com.tribus.markdown.util.MarkdownFormattingUtil.FormattingWrapper

/**
 * Base action for toggling inline formatting (bold, italic, strikethrough, code span).
 * Handles both selected text and word-at-cursor scenarios.
 */
open class BaseToggleFormattingAction(
    private val wrapper: FormattingWrapper
) : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document
        val selectionModel = editor.selectionModel
        val caretModel = editor.caretModel

        WriteCommandAction.runWriteCommandAction(project) {
            if (selectionModel.hasSelection()) {
                val start = selectionModel.selectionStart
                val end = selectionModel.selectionEnd
                val selectedText = selectionModel.selectedText ?: ""

                val result = MarkdownFormattingUtil.toggleFormatting(selectedText, wrapper)
                document.replaceString(start, end, result)
            } else {
                // No selection: find word at cursor and wrap it
                val offset = caretModel.offset
                val text = document.text
                val wordRange = MarkdownFormattingUtil.findWordAt(text, offset)

                if (wordRange != null) {
                    val word = text.substring(wordRange.first, wordRange.second)
                    val result = MarkdownFormattingUtil.toggleFormatting(word, wrapper)
                    document.replaceString(wordRange.first, wordRange.second, result)
                } else {
                    // No word at cursor — insert empty wrapper
                    val marker = wrapper.marker
                    document.insertString(offset, "$marker$marker")
                    caretModel.moveToOffset(offset + marker.length)
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = MarkdownFileUtil.isMarkdownFile(e)
    }
}
