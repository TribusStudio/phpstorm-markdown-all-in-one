package com.tribus.markdown.editor

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.tribus.markdown.lang.MarkdownLanguage

/**
 * Smart Backspace handling for markdown lists:
 * - Indented list marker at cursor: outdent (remove one indent level)
 * - Top-level list marker at cursor: replace marker with spaces (preserve alignment)
 * - Task checkbox at cursor: remove checkbox, keep marker
 */
class MarkdownBackspaceHandler : BackspaceHandlerDelegate() {

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        // No pre-processing needed
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (file.language != MarkdownLanguage) {
            return false
        }

        val document = editor.document
        val offset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, lineEnd))

        // The character was already deleted by the platform before this fires.
        // We check if the remaining text before cursor is a list marker pattern
        // that should trigger smart behavior.
        val cursorInLine = offset - lineStart
        val textBeforeCursor = lineText.substring(0, cursorInLine)

        // Indented list marker with cursor right after trailing space: "  - |"
        val indentedMatch = INDENTED_MARKER_AT_CURSOR.matchEntire(textBeforeCursor)
        if (indentedMatch != null && lineText.substring(cursorInLine).trim().isEmpty()) {
            val indent = indentedMatch.groupValues[1]
            val marker = indentedMatch.groupValues[2]
            // Outdent: remove leading spaces equal to marker+space width
            val outdentSize = minOf(marker.length, indent.length)
            if (outdentSize > 0) {
                document.deleteString(lineStart, lineStart + outdentSize)
                editor.caretModel.moveToOffset(lineStart + indent.length - outdentSize + marker.length)
                return true
            }
        }

        // Top-level marker with cursor right after: "- |" or "1. |"
        val topLevelMatch = TOP_LEVEL_MARKER_AT_CURSOR.matchEntire(textBeforeCursor)
        if (topLevelMatch != null && lineText.substring(cursorInLine).trim().isEmpty()) {
            val marker = topLevelMatch.groupValues[0]
            // Replace marker with spaces to preserve any following content alignment
            document.replaceString(lineStart, lineStart + marker.length, " ".repeat(marker.length))
            editor.caretModel.moveToOffset(lineStart + marker.length)
            return true
        }

        // Task checkbox at cursor: "- [ ] |" → cursor right after checkbox
        val checkboxMatch = CHECKBOX_AT_CURSOR.matchEntire(textBeforeCursor)
        if (checkboxMatch != null) {
            val markerPart = checkboxMatch.groupValues[1]
            val checkboxPart = checkboxMatch.groupValues[2]
            // Remove the checkbox portion, keep the marker
            val checkboxStart = lineStart + markerPart.length
            val checkboxEnd = checkboxStart + checkboxPart.length
            document.deleteString(checkboxStart, checkboxEnd)
            editor.caretModel.moveToOffset(checkboxStart)
            return true
        }

        return false
    }

    companion object {
        // "  - " or "   1. " — indented marker at cursor (at least 1 leading space)
        private val INDENTED_MARKER_AT_CURSOR = Regex(
            """^(\s+)([-+*] |[0-9]+[.)] )$"""
        )

        // "- " or "1. " — top-level marker at cursor (no leading spaces)
        private val TOP_LEVEL_MARKER_AT_CURSOR = Regex(
            """^([-+*] |[0-9]+[.)] )$"""
        )

        // "- [ ] " or "  * [x] " — task checkbox at cursor
        private val CHECKBOX_AT_CURSOR = Regex(
            """^(\s*(?:[-+*]|[0-9]+[.)]) +)(\[[ x]] )$"""
        )
    }
}
