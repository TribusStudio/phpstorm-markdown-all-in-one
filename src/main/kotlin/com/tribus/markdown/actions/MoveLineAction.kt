package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Moves the current line (or selection) up or down, then renumbers
 * any affected ordered lists.
 */
class MoveLineUpAction : AnAction(), MarkdownAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) { e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e) }
    override fun actionPerformed(e: AnActionEvent) = MoveLineAction.move(e, -1)
}

class MoveLineDownAction : AnAction(), MarkdownAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) { e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e) }
    override fun actionPerformed(e: AnActionEvent) = MoveLineAction.move(e, 1)
}

object MoveLineAction {
    private val ORDERED_LIST_PATTERN = Regex("""^(\s*)([0-9]+)([.)]) +""")

    fun move(e: AnActionEvent, direction: Int) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val startLine = document.getLineNumber(
            if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else editor.caretModel.offset
        )
        val endLine = document.getLineNumber(
            if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionEnd else editor.caretModel.offset
        )

        val targetLine = if (direction < 0) startLine - 1 else endLine + 1
        if (targetLine < 0 || targetLine >= document.lineCount) return

        WriteCommandAction.runWriteCommandAction(project) {
            swapLines(document, startLine, endLine, targetLine, direction)

            // Move caret with the line
            val newStartLine = startLine + direction
            val newCaretOffset = document.getLineStartOffset(newStartLine) +
                (editor.caretModel.offset - document.getLineStartOffset(startLine)).coerceAtMost(
                    document.getLineEndOffset(newStartLine) - document.getLineStartOffset(newStartLine)
                )
            editor.caretModel.moveToOffset(newCaretOffset)

            // Renumber ordered lists in the affected area
            val affectStart = maxOf(0, minOf(startLine, targetLine) - 1)
            val affectEnd = minOf(document.lineCount - 1, maxOf(endLine, targetLine) + 1)
            renumberOrderedLists(document, affectStart, affectEnd)
        }
    }

    private fun swapLines(document: Document, startLine: Int, endLine: Int, targetLine: Int, direction: Int) {
        if (direction < 0) {
            // Moving up: take the line above and insert it after the block
            val aboveStart = document.getLineStartOffset(targetLine)
            val aboveEnd = document.getLineEndOffset(targetLine)
            val aboveText = document.getText(TextRange(aboveStart, aboveEnd))

            // Delete the line above (including its newline)
            val deleteEnd = if (aboveEnd < document.textLength) aboveEnd + 1 else aboveEnd
            val deleteStart = aboveStart
            document.deleteString(deleteStart, deleteEnd)

            // Insert it after the block (endLine is now shifted up by 1)
            val newEndLine = endLine - 1
            val insertOffset = document.getLineEndOffset(newEndLine)
            document.insertString(insertOffset, "\n$aboveText")
        } else {
            // Moving down: take the line below and insert it before the block
            val belowStart = document.getLineStartOffset(targetLine)
            val belowEnd = document.getLineEndOffset(targetLine)
            val belowText = document.getText(TextRange(belowStart, belowEnd))

            // Delete the line below (including its preceding newline)
            val deleteStart = if (belowStart > 0) belowStart - 1 else belowStart
            document.deleteString(deleteStart, belowEnd)

            // Insert it before the block
            val insertOffset = document.getLineStartOffset(startLine)
            document.insertString(insertOffset, "$belowText\n")
        }
    }

    fun renumberOrderedLists(document: Document, fromLine: Int, toLine: Int) {
        // Find contiguous runs of ordered list items at the same indent level and renumber them
        var i = fromLine
        while (i <= toLine && i < document.lineCount) {
            val lineStart = document.getLineStartOffset(i)
            val lineEnd = document.getLineEndOffset(i)
            val lineText = document.getText(TextRange(lineStart, lineEnd))
            val match = ORDERED_LIST_PATTERN.find(lineText)

            if (match != null) {
                val indent = match.groupValues[1]
                val delimiter = match.groupValues[3]
                var expectedNumber = 1
                var j = i

                // Scan the contiguous run at this indent level
                while (j <= toLine.coerceAtMost(document.lineCount - 1)) {
                    val jStart = document.getLineStartOffset(j)
                    val jEnd = document.getLineEndOffset(j)
                    val jText = document.getText(TextRange(jStart, jEnd))
                    val jMatch = ORDERED_LIST_PATTERN.find(jText)

                    if (jMatch != null && jMatch.groupValues[1] == indent && jMatch.groupValues[3] == delimiter) {
                        val currentNumber = jMatch.groupValues[2].toIntOrNull() ?: expectedNumber
                        if (currentNumber != expectedNumber) {
                            val rest = jText.substring(jMatch.range.last + 1)
                            val newLine = "$indent$expectedNumber$delimiter ${rest}"
                            document.replaceString(jStart, jEnd, newLine)
                        }
                        expectedNumber++
                        j++
                    } else if (jText.isBlank()) {
                        j++ // Skip blank lines within a list
                    } else {
                        break
                    }
                }
                i = j
            } else {
                i++
            }
        }
    }
}
