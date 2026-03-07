package com.tribus.markdown.editor

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import com.tribus.markdown.lang.MarkdownLanguage

/**
 * Smart Enter key handling for markdown lists and blockquotes:
 * - Auto-continues unordered lists (-, +, *)
 * - Auto-continues ordered lists (1., 2), etc.)
 * - Auto-continues task lists (- [ ], - [x])
 * - Auto-continues blockquotes (> )
 * - Removes empty list markers on Enter (outdent or delete)
 */
class MarkdownEnterHandler : EnterHandlerDelegate {

    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): Result {
        if (file.language != MarkdownLanguage) {
            return Result.Continue
        }

        val document = editor.document
        val offset = caretOffset.get()
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))
        val cursorInLine = offset - lineStart
        val textBeforeCursor = lineText.substring(0, cursorInLine)
        val textAfterCursor = lineText.substring(cursorInLine)

        // Check for horizontal rule (---, ***, etc.) — don't interfere
        val noSpaces = lineText.replace("\\s".toRegex(), "")
        if (noSpaces.length > 2 && (noSpaces.replace("-", "").isEmpty() || noSpaces.replace("*", "").isEmpty())) {
            return Result.Continue
        }

        // Empty list item — remove the marker
        val emptyListMatch = EMPTY_LIST_PATTERN.matchEntire(textBeforeCursor.trimEnd())
        if (emptyListMatch != null && textAfterCursor.trim().isEmpty()) {
            return handleEmptyListItem(editor, document, lineStart, lineEnd, textBeforeCursor)
        }

        // Blockquote continuation
        val blockquoteMatch = BLOCKQUOTE_PATTERN.find(textBeforeCursor)
        if (blockquoteMatch != null) {
            return handleBlockquote(editor, document, offset, lineText, textBeforeCursor, textAfterCursor, caretOffset)
        }

        // Unordered list continuation (with optional task checkbox)
        val unorderedMatch = UNORDERED_LIST_PATTERN.find(textBeforeCursor)
        if (unorderedMatch != null) {
            val continuation = unorderedMatch.groupValues[1].replace("[x]", "[ ]")
            return insertContinuation(editor, document, offset, continuation, caretOffset, caretAdvance)
        }

        // Ordered list continuation
        val orderedMatch = ORDERED_LIST_PATTERN.find(textBeforeCursor)
        if (orderedMatch != null) {
            val leadingSpace = orderedMatch.groupValues[1]
            val prevMarker = orderedMatch.groupValues[2]
            val delimiter = orderedMatch.groupValues[3]
            val trailingSpace = orderedMatch.groupValues[4]
            val checkbox = orderedMatch.groupValues[5].replace("[x]", "[ ]")

            val nextMarker = (prevMarker.toIntOrNull() ?: 0) + 1
            val textIndent = (prevMarker + delimiter + trailingSpace).length
            // Align trailing spaces so text stays at same indent level
            val fixedTrailing = " ".repeat(maxOf(1, textIndent - ("$nextMarker$delimiter").length))

            val continuation = "$leadingSpace$nextMarker$delimiter$fixedTrailing$checkbox"
            return insertContinuation(editor, document, offset, continuation, caretOffset, caretAdvance)
        }

        return Result.Continue
    }

    override fun postProcessEnter(
        file: PsiFile,
        editor: Editor,
        dataContext: DataContext
    ): Result {
        return Result.Continue
    }

    private fun handleEmptyListItem(
        editor: Editor,
        document: com.intellij.openapi.editor.Document,
        lineStart: Int,
        lineEnd: Int,
        textBeforeCursor: String
    ): Result {
        val indentMatch = INDENTED_LIST_PATTERN.find(textBeforeCursor)
        if (indentMatch != null) {
            // Indented list item — outdent by removing one level of indentation
            val indent = indentMatch.groupValues[1]
            val rest = textBeforeCursor.substring(indent.length)
            // Remove leading spaces (one indent level = length of marker+space)
            val markerMatch = MARKER_LENGTH_PATTERN.find(rest)
            val outdentSize = if (markerMatch != null) markerMatch.groupValues[0].length else 2
            val newIndent = if (indent.length > outdentSize) indent.substring(outdentSize) else ""
            document.replaceString(lineStart, lineEnd, "$newIndent$rest")
            editor.caretModel.moveToOffset(lineStart + newIndent.length + rest.length)
            return Result.Stop
        }

        // Top-level list item — check if it has a task checkbox
        val taskMatch = TASK_CHECKBOX_PATTERN.find(textBeforeCursor)
        if (taskMatch != null) {
            // Remove the checkbox portion, keep the marker
            val checkboxStart = taskMatch.range.first + taskMatch.groupValues[1].length
            document.deleteString(lineStart + checkboxStart, lineStart + textBeforeCursor.length)
            editor.caretModel.moveToOffset(lineStart + checkboxStart)
            return Result.Stop
        }

        // Plain top-level marker — delete the entire marker, leave empty line
        document.replaceString(lineStart, lineEnd, "")
        editor.caretModel.moveToOffset(lineStart)
        return Result.Stop
    }

    private fun handleBlockquote(
        editor: Editor,
        document: com.intellij.openapi.editor.Document,
        offset: Int,
        lineText: String,
        textBeforeCursor: String,
        textAfterCursor: String,
        caretOffset: Ref<Int>
    ): Result {
        val isEmptyQuoteLine = lineText.trimEnd() == ">"

        if (isEmptyQuoteLine) {
            // Empty blockquote line — end the blockquote
            val lineNumber = document.getLineNumber(offset)
            val lineStart = document.getLineStartOffset(lineNumber)
            val lineEnd = document.getLineEndOffset(lineNumber)
            document.replaceString(lineStart, lineEnd, "")
            editor.caretModel.moveToOffset(lineStart)
            return Result.Stop
        }

        // Continue blockquote on next line
        val continuation = "> "
        return insertContinuation(editor, document, offset, continuation, caretOffset, Ref(0))
    }

    /**
     * Inserts a newline followed by the continuation prefix (list marker, blockquote, etc.)
     * and positions the caret after it.
     */
    private fun insertContinuation(
        editor: Editor,
        document: com.intellij.openapi.editor.Document,
        offset: Int,
        continuation: String,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>
    ): Result {
        val toInsert = "\n$continuation"
        document.insertString(offset, toInsert)
        editor.caretModel.moveToOffset(offset + toInsert.length)
        // Tell IntelliJ we handled the enter key completely
        return Result.Stop
    }

    companion object {
        // Matches any list marker (unordered, ordered, task) that is empty (no content after marker)
        private val EMPTY_LIST_PATTERN = Regex(
            """^(\s*)([-+*]|[0-9]+[.)])( +\[[ x]])?(\s*)$"""
        )

        // Matches indented list items (at least one space before marker)
        private val INDENTED_LIST_PATTERN = Regex(
            """^(\s+)([-+*]|[0-9]+[.)]) +(\[[ x]] +)?$"""
        )

        // Matches the marker+space portion for determining outdent size
        private val MARKER_LENGTH_PATTERN = Regex(
            """^([-+*]|[0-9]+[.)]) +"""
        )

        // Matches task checkbox portion: "- [ ] " or "- [x] "
        private val TASK_CHECKBOX_PATTERN = Regex(
            """^([-+*]|[0-9]+[.)]) +(\[[ x]] )$"""
        )

        // Blockquote: line starts with "> "
        private val BLOCKQUOTE_PATTERN = Regex(
            """^> """
        )

        // Unordered list with optional task checkbox: "- ", "- [ ] ", "  * [x] "
        private val UNORDERED_LIST_PATTERN = Regex(
            """^((\s*[-+*] +)(\[[ x]] +)?)"""
        )

        // Ordered list: "1. ", "  2) ", "1. [ ] "
        private val ORDERED_LIST_PATTERN = Regex(
            """^(\s*)([0-9]+)([.)])( +)(\[[ x]] +)?"""
        )
    }
}
