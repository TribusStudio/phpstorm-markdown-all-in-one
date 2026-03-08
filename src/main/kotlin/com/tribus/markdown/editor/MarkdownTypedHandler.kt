package com.tribus.markdown.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.tribus.markdown.lang.MarkdownLanguage

/**
 * Handles selection wrapping for markdown formatting characters.
 * When text is selected and a trigger character is typed, wraps the selection
 * instead of replacing it.
 *
 * Uses beforeSelectionRemoved (NOT beforeCharTyped) because the selection is
 * deleted before beforeCharTyped fires. Inspired by AsciiDoc plugin's
 * FormattingQuotedTypedHandler.
 */
class MarkdownTypedHandler : TypedHandlerDelegate() {

    override fun beforeSelectionRemoved(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        val selectionModel = editor.selectionModel

        if (file.language == MarkdownLanguage
            && CodeInsightSettings.getInstance().SURROUND_SELECTION_ON_QUOTE_TYPED
            && selectionModel.hasSelection()
            && isWrapChar(c)
        ) {
            val selectedText = selectionModel.selectedText
            if (!selectedText.isNullOrEmpty()) {
                val selectionStart = selectionModel.selectionStart
                val selectionEnd = selectionModel.selectionEnd

                val newText = wrapText(c, selectedText, editor.document.charsSequence, selectionStart, selectionEnd)
                val border = newText.length - selectedText.length - borderOffset(c, selectedText, editor.document.charsSequence, selectionStart)

                val ltrSelection = selectionModel.leadSelectionOffset != selectionModel.selectionEnd
                val restoreStickySelection = editor is EditorEx && editor.isStickySelection

                selectionModel.removeSelection()
                editor.document.replaceString(selectionStart, selectionEnd, newText)

                // Restore selection around the wrapped content (excluding wrapper chars)
                val innerStart = selectionStart + border
                val innerEnd = selectionStart + newText.length - border
                val replacedRange = TextRange(innerStart, innerEnd)

                if (replacedRange.endOffset <= editor.document.textLength) {
                    if (restoreStickySelection) {
                        val editorEx = editor as EditorEx
                        val caretModel = editorEx.caretModel
                        caretModel.moveToOffset(if (ltrSelection) replacedRange.startOffset else replacedRange.endOffset)
                        editorEx.setStickySelection(true)
                        caretModel.moveToOffset(if (ltrSelection) replacedRange.endOffset else replacedRange.startOffset)
                    } else {
                        if (ltrSelection) {
                            editor.selectionModel.setSelection(replacedRange.startOffset, replacedRange.endOffset)
                        } else {
                            editor.selectionModel.setSelection(replacedRange.endOffset, replacedRange.startOffset)
                        }
                        editor.caretModel.moveToOffset(if (ltrSelection) replacedRange.endOffset else replacedRange.startOffset)
                    }
                }

                return Result.STOP
            }
        }

        return super.beforeSelectionRemoved(c, project, editor, file)
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file.language != MarkdownLanguage) {
            return Result.CONTINUE
        }
        // TODO: Phase 2 — smart list editing, auto-pairs, etc.
        return Result.CONTINUE
    }

    companion object {
        private val WRAP_CHARS = setOf('*', '~', '_', '`')
        private val TABLE_CHARS = setOf('|', '-')
        private val ALL_CHARS = WRAP_CHARS + TABLE_CHARS

        private fun isWrapChar(c: Char): Boolean = c in ALL_CHARS

        /**
         * Produces the wrapped text for the given character and selection.
         */
        private fun wrapText(c: Char, selected: String, docText: CharSequence, selStart: Int, selEnd: Int): String {
            return when (c) {
                '|' -> wrapPipe(selected, docText, selStart)
                '-' -> wrapDash(selected, docText, selStart, selEnd)
                else -> "$c$selected$c"
            }
        }

        /**
         * Returns the number of characters added before the selected text
         * (used to calculate selection restoration offset).
         */
        private fun borderOffset(c: Char, selected: String, docText: CharSequence, selStart: Int): Int {
            return when (c) {
                '|' -> if (hasPrecedingPipe(docText, selStart)) 0 else 2  // "| " = 2 chars
                '-' -> if (isInsideTableCell(docText, selStart, selStart + selected.length)) 0 else 1
                else -> 1  // single wrap char
            }
        }

        /**
         * Pipe: smart table cell wrapping.
         * - No preceding pipe: "| text |"
         * - Preceding pipe exists: "text |"
         */
        private fun wrapPipe(selected: String, docText: CharSequence, selStart: Int): String {
            return if (hasPrecedingPipe(docText, selStart)) {
                "$selected |"
            } else {
                "| $selected |"
            }
        }

        /**
         * Dash: table header border generation.
         * - Inside table cell: fills with " --- " (space-padded dashes matching cell width)
         * - Outside: wraps symmetrically
         */
        private fun wrapDash(selected: String, docText: CharSequence, selStart: Int, selEnd: Int): String {
            return if (isInsideTableCell(docText, selStart, selEnd)) {
                val dashCount = maxOf(selected.length - 2, 3)
                " ${"-".repeat(dashCount)} "
            } else {
                "-$selected-"
            }
        }

        private fun hasPrecedingPipe(text: CharSequence, selectionStart: Int): Boolean {
            var i = selectionStart - 1
            while (i >= 0 && text[i] == ' ') i--
            return i >= 0 && text[i] == '|'
        }

        private fun isInsideTableCell(text: CharSequence, selStart: Int, selEnd: Int): Boolean {
            var lineStart = selStart
            while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
            var lineEnd = selEnd
            while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++
            val beforeSel = text.subSequence(lineStart, selStart)
            val afterSel = text.subSequence(selEnd, lineEnd)
            return beforeSel.contains('|') && afterSel.contains('|')
        }
    }
}
