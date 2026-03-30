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
 * Cycles the current line's list marker through candidates: -, *, +, 1., 1)
 * If the line has no marker, the first candidate is applied.
 * If the line has the last candidate, the marker is removed (plain text).
 */
class ToggleListAction : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val candidates = try {
            parseCandidates(MarkdownSettings.getInstance().state.listToggleCandidates)
        } catch (_: Exception) {
            DEFAULT_CANDIDATES
        }

        val startLine = document.getLineNumber(
            if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else editor.caretModel.offset
        )
        val endLine = document.getLineNumber(
            if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionEnd else editor.caretModel.offset
        )

        WriteCommandAction.runWriteCommandAction(project) {
            for (line in endLine downTo startLine) {
                val lineStart = document.getLineStartOffset(line)
                val lineEnd = document.getLineEndOffset(line)
                val lineText = document.getText(TextRange(lineStart, lineEnd))
                cycleLine(document, lineStart, lineEnd, lineText, candidates)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }

    companion object {
        val DEFAULT_CANDIDATES = listOf("-", "*", "+", "1.", "1)")

        private val MARKER_PATTERN = Regex("""^(\s*)([-+*]|[0-9]+[.)]) +""")

        fun parseCandidates(setting: String): List<String> {
            val parsed = setting.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            return if (parsed.isEmpty()) DEFAULT_CANDIDATES else parsed
        }

        fun cycleLine(
            document: com.intellij.openapi.editor.Document,
            lineStart: Int,
            lineEnd: Int,
            lineText: String,
            candidates: List<String>
        ) {
            val match = MARKER_PATTERN.find(lineText)
            if (match != null) {
                val indent = match.groupValues[1]
                val currentMarker = match.groupValues[2]
                val content = lineText.substring(match.range.last + 1)

                // Normalize for comparison: "2." -> "1.", "3)" -> "1)"
                val normalizedMarker = normalizeMarker(currentMarker)
                val currentIndex = candidates.indexOf(normalizedMarker)

                if (currentIndex >= 0 && currentIndex < candidates.size - 1) {
                    // Advance to next candidate
                    val nextMarker = candidates[currentIndex + 1]
                    document.replaceString(lineStart, lineEnd, "$indent$nextMarker $content")
                } else {
                    // Last candidate or unknown — remove marker (plain text)
                    document.replaceString(lineStart, lineEnd, "$indent$content")
                }
            } else if (lineText.isNotBlank()) {
                // No marker — apply first candidate
                val indent = lineText.length - lineText.trimStart().length
                val content = lineText.trimStart()
                val prefix = lineText.substring(0, indent)
                document.replaceString(lineStart, lineEnd, "$prefix${candidates[0]} $content")
            }
        }

        private fun normalizeMarker(marker: String): String {
            return when {
                marker.endsWith(".") -> "1."
                marker.endsWith(")") -> "1)"
                else -> marker
            }
        }
    }
}
