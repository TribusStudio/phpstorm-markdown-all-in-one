package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.tribus.markdown.settings.MarkdownSettings
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Indents list items by adding leading spaces. When on a list line,
 * adds one indent level (width of the list marker + trailing space).
 * On non-list lines, falls back to standard indentation.
 */
class ListIndentAction : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document
        val selectionModel = editor.selectionModel

        val startLine = document.getLineNumber(
            if (selectionModel.hasSelection()) selectionModel.selectionStart else editor.caretModel.offset
        )
        val endLine = document.getLineNumber(
            if (selectionModel.hasSelection()) selectionModel.selectionEnd else editor.caretModel.offset
        )

        WriteCommandAction.runWriteCommandAction(project) {
            // Process lines in reverse to preserve offsets
            for (line in endLine downTo startLine) {
                val lineStart = document.getLineStartOffset(line)
                val lineEnd = document.getLineEndOffset(line)
                val lineText = document.getText(TextRange(lineStart, lineEnd))

                if (lineText.isBlank()) continue

                val indentSize = determineIndentSize(lineText)
                document.insertString(lineStart, " ".repeat(indentSize))
            }

            // Auto-renumber ordered lists in the affected area
            val autoRenumber = try {
                MarkdownSettings.getInstance().state.autoRenumberOrderedLists
            } catch (_: Exception) { true }
            if (autoRenumber) {
                val affectStart = maxOf(0, startLine - 1)
                val affectEnd = minOf(document.lineCount - 1, endLine + 1)
                MoveLineAction.renumberOrderedLists(document, affectStart, affectEnd)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }

    companion object {
        private val LIST_MARKER_PATTERN = Regex("""^\s*([-+*]|[0-9]+[.)]) +""")

        /**
         * Determines indent size based on the list marker width.
         * For list lines, uses the marker+space width for proper nesting.
         * For non-list lines, defaults to 2 spaces.
         */
        fun determineIndentSize(lineText: String): Int {
            val match = LIST_MARKER_PATTERN.find(lineText)
            if (match != null) {
                // Indent by the width of the marker portion (excluding existing leading space)
                val markerPart = match.value.trimStart()
                return markerPart.length
            }
            return 2
        }
    }
}
