package com.tribus.markdown.editor

import com.intellij.openapi.editor.Editor

/**
 * Dispatches typed characters to selection wrapper strategies when text is
 * selected in a markdown editor. Each wrapper handles a specific character
 * with contextual markdown-aware behavior.
 *
 * Returns true if the character was handled (caller should consume the event).
 */
object SelectionWrapperService {

    private val wrappers: Map<Char, SelectionWrapper> = mapOf(
        '*' to AsteriskWrapper,
        '|' to PipeWrapper,
        '-' to DashWrapper,
        '`' to SimpleSymmetricWrapper('`'),
        '~' to SimpleSymmetricWrapper('~'),
        '_' to SimpleSymmetricWrapper('_'),
    )

    fun handleIfSelected(c: Char, editor: Editor): Boolean {
        val selectionModel = editor.selectionModel
        if (!selectionModel.hasSelection()) return false

        val wrapper = wrappers[c] ?: return false
        return wrapper.wrap(editor)
    }
}

/**
 * Strategy interface for selection wrapping. Each implementation handles
 * a specific trigger character with its own contextual logic.
 */
interface SelectionWrapper {
    fun wrap(editor: Editor): Boolean
}

/**
 * Wraps selection symmetrically with a single character: c + selection + c
 * Used for *, `, ~, _
 */
class SimpleSymmetricWrapper(private val c: Char) : SelectionWrapper {
    override fun wrap(editor: Editor): Boolean {
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: return false
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd

        val replacement = "$c$selectedText$c"
        editor.document.replaceString(start, end, replacement)
        // Place caret after the closing wrapper char
        editor.caretModel.moveToOffset(start + replacement.length)
        selectionModel.removeSelection()
        return true
    }
}

/**
 * Asterisk wrapper: wraps selection with * for italic-style formatting.
 * Identical to SimpleSymmetricWrapper('*') but exists as a named object
 * for clarity and potential future enhancement (e.g., double-* detection).
 */
object AsteriskWrapper : SelectionWrapper {
    override fun wrap(editor: Editor): Boolean {
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: return false
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd

        val replacement = "*${selectedText}*"
        editor.document.replaceString(start, end, replacement)
        editor.caretModel.moveToOffset(start + replacement.length)
        selectionModel.removeSelection()
        return true
    }
}

/**
 * Pipe wrapper for table cell creation:
 * - If no pipe before selection: wraps as "| selection |"
 * - If pipe already exists before selection: only adds closing " |"
 * Adds spaces between pipes and content for legibility.
 */
object PipeWrapper : SelectionWrapper {
    override fun wrap(editor: Editor): Boolean {
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: return false
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd
        val docText = editor.document.charsSequence

        // Look backwards from selection start for an existing pipe
        val hasPipeBefore = hasPrecedingPipe(docText, start)

        val replacement = if (hasPipeBefore) {
            // Pipe already before — just add closing pipe with space
            "${selectedText} |"
        } else {
            // No pipe — wrap fully as a table cell
            "| ${selectedText} |"
        }

        editor.document.replaceString(start, end, replacement)
        editor.caretModel.moveToOffset(start + replacement.length)
        selectionModel.removeSelection()
        return true
    }

    /**
     * Checks if there's a pipe character before the selection start,
     * ignoring only whitespace between the pipe and the selection.
     */
    private fun hasPrecedingPipe(text: CharSequence, selectionStart: Int): Boolean {
        var i = selectionStart - 1
        // Skip whitespace
        while (i >= 0 && text[i] == ' ') i--
        return i >= 0 && text[i] == '|'
    }
}

/**
 * Dash wrapper for table header borders:
 * - When inside a table cell (between pipes), replaces the selected text
 *   with dashes of exactly the same length, preserving the cell width.
 * - Outside a table context, wraps symmetrically: - selection -
 */
object DashWrapper : SelectionWrapper {
    override fun wrap(editor: Editor): Boolean {
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText ?: return false
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd

        val replacement = if (isInsideTableCell(editor.document.charsSequence, start, end)) {
            // Fill the selected region with dashes (preserves cell width)
            "-".repeat(selectedText.length)
        } else {
            // Outside table context: simple symmetric wrap
            "-${selectedText}-"
        }

        editor.document.replaceString(start, end, replacement)
        editor.caretModel.moveToOffset(start + replacement.length)
        selectionModel.removeSelection()
        return true
    }

    /**
     * Checks if the selection is between two pipe characters on the same line,
     * indicating we're inside a table cell.
     */
    private fun isInsideTableCell(text: CharSequence, selStart: Int, selEnd: Int): Boolean {
        // Find the start of the current line
        var lineStart = selStart
        while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--

        // Find the end of the current line
        var lineEnd = selEnd
        while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++

        // Look for a pipe before the selection on this line
        val beforeSel = text.subSequence(lineStart, selStart)
        val afterSel = text.subSequence(selEnd, lineEnd)

        return beforeSel.contains('|') && afterSel.contains('|')
    }
}
