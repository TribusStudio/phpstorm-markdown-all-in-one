package com.tribus.markdown.table

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.tribus.markdown.lang.MarkdownLanguage

/**
 * Handles Tab key inside GFM tables to navigate to the next cell.
 * Falls through to the original handler when not inside a table.
 */
class TableTabHandler(private val originalHandler: EditorActionHandler) : EditorActionHandler() {

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        if (!handleTableNavigation(editor, false)) {
            originalHandler.execute(editor, caret, dataContext)
        }
    }

    override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext): Boolean {
        return isInMarkdownTable(editor) || originalHandler.isEnabled(editor, caret, dataContext)
    }

    companion object {
        fun isInMarkdownTable(editor: Editor): Boolean {
            val file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(editor.document)
            if (file == null || !com.tribus.markdown.util.MarkdownFileUtil.isMarkdownFile(file)) return false
            val caretLine = editor.caretModel.logicalPosition.line
            return TableParser.findTableAt(editor.document.text, caretLine) != null
        }

        fun handleTableNavigation(editor: Editor, reverse: Boolean): Boolean {
            val document = editor.document
            val text = document.text
            val caretOffset = editor.caretModel.offset
            val caretLine = editor.caretModel.logicalPosition.line
            val lines = text.lines()

            if (caretLine >= lines.size) return false

            val file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document)
            if (file == null || !com.tribus.markdown.util.MarkdownFileUtil.isMarkdownFile(file)) return false

            val table = TableParser.findTableAt(text, caretLine) ?: return false
            val lineStartOffset = document.getLineStartOffset(caretLine)
            val localOffset = caretOffset - lineStartOffset

            val target = if (reverse) {
                findPreviousCell(lines, caretLine, localOffset, table, document)
            } else {
                findNextCell(lines, caretLine, localOffset, table, document)
            }

            if (target != null) {
                editor.caretModel.moveToOffset(target.first)
                editor.selectionModel.setSelection(target.first, target.second)
                return true
            }

            return false
        }

        private fun findNextCell(
            lines: List<String>,
            currentLine: Int,
            localOffset: Int,
            table: TableParser.Table,
            document: com.intellij.openapi.editor.Document
        ): Pair<Int, Int>? {
            val lineText = lines[currentLine]
            val pipes = findPipeOffsets(lineText)

            // Find which cell we're in
            var cellIndex = -1
            for (i in 0 until pipes.size - 1) {
                if (localOffset >= pipes[i] && localOffset <= pipes[i + 1]) {
                    cellIndex = i
                    break
                }
            }

            // Move to the next cell on this line
            if (cellIndex >= 0 && cellIndex + 1 < pipes.size - 1) {
                return cellRange(lines, currentLine, cellIndex + 1, document)
            }

            // Move to first cell of next row (skip separator if needed)
            var nextLine = currentLine + 1
            if (nextLine <= table.endLine && TableParser.isSeparatorRow(lines[nextLine])) {
                nextLine++
            }
            if (nextLine <= table.endLine) {
                return cellRange(lines, nextLine, 0, document)
            }

            return null
        }

        private fun findPreviousCell(
            lines: List<String>,
            currentLine: Int,
            localOffset: Int,
            table: TableParser.Table,
            document: com.intellij.openapi.editor.Document
        ): Pair<Int, Int>? {
            val lineText = lines[currentLine]
            val pipes = findPipeOffsets(lineText)

            var cellIndex = -1
            for (i in 0 until pipes.size - 1) {
                if (localOffset >= pipes[i] && localOffset <= pipes[i + 1]) {
                    cellIndex = i
                    break
                }
            }

            // Move to the previous cell on this line
            if (cellIndex > 0) {
                return cellRange(lines, currentLine, cellIndex - 1, document)
            }

            // Move to last cell of previous row (skip separator if needed)
            var prevLine = currentLine - 1
            if (prevLine >= table.startLine && TableParser.isSeparatorRow(lines[prevLine])) {
                prevLine--
            }
            if (prevLine >= table.startLine) {
                val prevPipes = findPipeOffsets(lines[prevLine])
                if (prevPipes.size >= 2) {
                    return cellRange(lines, prevLine, prevPipes.size - 2, document)
                }
            }

            return null
        }

        /**
         * Returns (selectionStart, selectionEnd) for the cell content between pipes.
         */
        private fun cellRange(
            lines: List<String>,
            line: Int,
            cellIndex: Int,
            document: com.intellij.openapi.editor.Document
        ): Pair<Int, Int>? {
            val lineText = lines[line]
            val pipes = findPipeOffsets(lineText)
            if (cellIndex + 1 >= pipes.size) return null

            val cellContentStart = pipes[cellIndex] + 1
            val cellContentEnd = pipes[cellIndex + 1]

            // Trim whitespace to select just the content
            val cellText = lineText.substring(cellContentStart, cellContentEnd)
            val trimStart = cellText.length - cellText.trimStart().length
            val trimEnd = cellText.length - cellText.trimEnd().length

            val selectStart = cellContentStart + trimStart
            val selectEnd = cellContentEnd - trimEnd

            val lineStart = document.getLineStartOffset(line)
            return if (selectStart < selectEnd) {
                Pair(lineStart + selectStart, lineStart + selectEnd)
            } else {
                // Empty cell — place cursor between the pipes
                val mid = (cellContentStart + cellContentEnd) / 2
                Pair(lineStart + mid, lineStart + mid)
            }
        }

        fun findPipeOffsets(line: String): List<Int> {
            val offsets = mutableListOf<Int>()
            var i = 0
            while (i < line.length) {
                if (line[i] == '\\' && i + 1 < line.length && line[i + 1] == '|') {
                    i += 2
                } else if (line[i] == '|') {
                    offsets.add(i)
                    i++
                } else {
                    i++
                }
            }
            return offsets
        }
    }
}
