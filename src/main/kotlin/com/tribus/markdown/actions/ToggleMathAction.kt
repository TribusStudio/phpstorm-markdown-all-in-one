package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Toggle math formatting on the current selection or word at cursor.
 * Cycles: plain text → $inline$ → $$display$$ → plain text
 *
 * If [reverse] is true, cycles in the opposite direction:
 * plain text → $$display$$ → $inline$ → plain text
 */
open class ToggleMathAction(private val reverse: Boolean = false) : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

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
                val result = toggleMath(selectedText, reverse)
                document.replaceString(start, end, result)
            } else {
                // Try to detect math at cursor position
                val offset = caretModel.offset
                val text = document.text

                // Check if cursor is inside $$...$$ (display math)
                val displayRange = findSurroundingDelimiter(text, offset, "$$")
                if (displayRange != null) {
                    val inner = text.substring(displayRange.first + 2, displayRange.second - 2)
                    val replacement = if (reverse) "\$${inner}\$" else inner
                    document.replaceString(displayRange.first, displayRange.second, replacement)
                    return@runWriteCommandAction
                }

                // Check if cursor is inside $...$ (inline math)
                val inlineRange = findSurroundingDelimiter(text, offset, "$")
                if (inlineRange != null) {
                    val inner = text.substring(inlineRange.first + 1, inlineRange.second - 1)
                    val replacement = if (reverse) inner else "\$\$${inner}\$\$"
                    document.replaceString(inlineRange.first, inlineRange.second, replacement)
                    return@runWriteCommandAction
                }

                // No math at cursor — wrap word or insert empty math
                val wordRange = findWordAt(text, offset)
                if (wordRange != null) {
                    val word = text.substring(wordRange.first, wordRange.second)
                    val wrapped = if (reverse) "\$\$${word}\$\$" else "\$${word}\$"
                    document.replaceString(wordRange.first, wordRange.second, wrapped)
                } else {
                    val marker = if (reverse) "\$\$\$\$" else "\$\$"
                    document.insertString(offset, marker)
                    caretModel.moveToOffset(offset + marker.length / 2)
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }

    companion object {
        fun toggleMath(text: String, reverse: Boolean = false): String {
            // Already display math: $$...$$ → unwrap or downgrade
            if (text.startsWith("$$") && text.endsWith("$$") && text.length > 4) {
                val inner = text.substring(2, text.length - 2)
                return if (reverse) "\$${inner}\$" else inner
            }
            // Already inline math: $...$ → upgrade or unwrap
            if (text.startsWith("$") && text.endsWith("$") && text.length > 2 &&
                !text.startsWith("$$")
            ) {
                val inner = text.substring(1, text.length - 1)
                return if (reverse) inner else "\$\$${inner}\$\$"
            }
            // Plain text → wrap
            return if (reverse) "\$\$${text}\$\$" else "\$${text}\$"
        }

        /**
         * Find the word boundaries around the given offset.
         */
        private fun findWordAt(text: String, offset: Int): Pair<Int, Int>? {
            if (offset >= text.length) return null
            val ch = text[offset]
            if (ch.isWhitespace() || ch == '$') return null

            var start = offset
            while (start > 0 && !text[start - 1].isWhitespace() && text[start - 1] != '$') start--
            var end = offset
            while (end < text.length && !text[end].isWhitespace() && text[end] != '$') end++
            return if (start < end) Pair(start, end) else null
        }

        /**
         * Find the range of a surrounding delimiter pair ($...$ or $$...$$) around [offset].
         */
        private fun findSurroundingDelimiter(text: String, offset: Int, delimiter: String): Pair<Int, Int>? {
            val len = delimiter.length
            // Search backward for opening delimiter
            var searchFrom = offset
            while (searchFrom >= len) {
                val candidate = searchFrom - len
                if (text.substring(candidate, candidate + len) == delimiter) {
                    // For $, make sure it's not actually $$ (unless we're looking for $$)
                    if (len == 1 && candidate > 0 && text[candidate - 1] == '$') {
                        searchFrom--
                        continue
                    }
                    if (len == 1 && candidate + 1 < text.length && text[candidate + 1] == '$') {
                        searchFrom--
                        continue
                    }
                    // Found opening delimiter — now find closing
                    val searchStart = candidate + len
                    val closeIdx = text.indexOf(delimiter, searchStart)
                    if (closeIdx >= 0 && closeIdx + len > offset) {
                        // For $, make sure closing $ is not part of $$
                        if (len == 1) {
                            if (closeIdx > 0 && text[closeIdx - 1] == '$') return null
                            if (closeIdx + 1 < text.length && text[closeIdx + 1] == '$') return null
                        }
                        return Pair(candidate, closeIdx + len)
                    }
                    return null
                }
                searchFrom--
            }
            return null
        }
    }
}
