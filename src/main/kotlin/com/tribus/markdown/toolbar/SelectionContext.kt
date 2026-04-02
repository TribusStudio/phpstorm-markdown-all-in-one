package com.tribus.markdown.toolbar

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.tribus.markdown.table.TableParser

/**
 * Detects the markdown context at the current selection to determine
 * which floating toolbar actions are relevant.
 *
 * Context priority (highest wins): TOC > Code Block > Table > Math > Blockquote > Default
 */
object SelectionContext {

    enum class Context {
        DEFAULT,
        TOC,
        TABLE,
        CODE_BLOCK,
        MATH
    }

    /**
     * Detect the context at the current selection position.
     */
    fun detect(editor: Editor): Context {
        if (!editor.selectionModel.hasSelection()) return Context.DEFAULT

        val text = editor.document.text
        val selStart = editor.selectionModel.selectionStart
        val selEnd = editor.selectionModel.selectionEnd

        // Check contexts in priority order
        if (isInToc(text, selStart, selEnd)) return Context.TOC
        if (isInCodeBlock(text, selStart, selEnd)) return Context.CODE_BLOCK
        if (isInTable(editor)) return Context.TABLE
        if (isInMath(text, selStart)) return Context.MATH

        return Context.DEFAULT
    }

    /**
     * Check if the selection overlaps a TOC block (<!-- TOC --> ... <!-- /TOC -->).
     */
    fun isInToc(text: String, selStart: Int, selEnd: Int): Boolean {
        val tocOpenPattern = Regex("<!--\\s*TOC[^>]*-->", RegexOption.IGNORE_CASE)
        val tocClosePattern = Regex("<!--\\s*/TOC\\s*-->", RegexOption.IGNORE_CASE)

        for (openMatch in tocOpenPattern.findAll(text)) {
            val closeMatch = tocClosePattern.find(text, openMatch.range.last + 1) ?: continue
            val tocStart = openMatch.range.first
            val tocEnd = closeMatch.range.last + 1

            // Selection overlaps this TOC block
            if (selStart < tocEnd && selEnd > tocStart) return true
        }
        return false
    }

    /**
     * Check if the selection is inside a fenced code block.
     */
    fun isInCodeBlock(text: String, selStart: Int, selEnd: Int): Boolean {
        val fencePattern = Regex("^\\s{0,3}(`{3,}|~{3,})", RegexOption.MULTILINE)
        var inFence = false
        var fenceChar = ' '
        var fenceLength = 0
        var blockStart = 0

        for (match in fencePattern.findAll(text)) {
            val matchChar = match.groupValues[1][0]
            val matchLength = match.groupValues[1].length
            val matchPos = match.range.first

            if (inFence) {
                if (matchChar == fenceChar && matchLength >= fenceLength) {
                    val blockEnd = match.range.last + 1
                    // Check if selection is inside this block
                    if (selStart >= blockStart && selEnd <= blockEnd) return true
                    inFence = false
                }
            } else {
                inFence = true
                fenceChar = matchChar
                fenceLength = matchLength
                blockStart = matchPos
            }
        }
        return false
    }

    /**
     * Check if the selection is inside a table.
     */
    fun isInTable(editor: Editor): Boolean {
        val caretLine = editor.document.getLineNumber(editor.selectionModel.selectionStart)
        return TableParser.findTableAt(editor.document.text, caretLine) != null
    }

    /**
     * Check if the selection is inside math delimiters.
     */
    fun isInMath(text: String, selStart: Int): Boolean {
        // Display math: $$...$$
        val displayPattern = Regex("\\$\\$.+?\\$\\$", RegexOption.DOT_MATCHES_ALL)
        for (match in displayPattern.findAll(text)) {
            if (selStart >= match.range.first && selStart <= match.range.last) return true
        }
        // Inline math: $...$
        val inlinePattern = Regex("(?<!\\$)\\$(?!\\$).+?(?<!\\$)\\$(?!\\$)")
        for (match in inlinePattern.findAll(text)) {
            if (selStart >= match.range.first && selStart <= match.range.last) return true
        }
        return false
    }

    /**
     * Check if all selected lines are blockquote lines.
     */
    fun isInBlockquote(editor: Editor): Boolean {
        val doc = editor.document
        val startLine = doc.getLineNumber(editor.selectionModel.selectionStart)
        val endLine = doc.getLineNumber(editor.selectionModel.selectionEnd)

        for (line in startLine..endLine) {
            val lineStart = doc.getLineStartOffset(line)
            val lineEnd = doc.getLineEndOffset(line)
            val lineText = doc.getText(TextRange(lineStart, lineEnd))
            if (!lineText.trimStart().startsWith(">")) return false
        }
        return true
    }
}
