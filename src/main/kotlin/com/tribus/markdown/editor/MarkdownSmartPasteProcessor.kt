package com.tribus.markdown.editor

import com.intellij.codeInsight.editorActions.CopyPastePreProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RawText
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.tribus.markdown.settings.MarkdownSettings
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Smart paste for markdown: when pasting a URL over selected text,
 * wraps the selection as a markdown link `[selected text](url)`.
 *
 * Also handles pasting a URL with no selection — if the clipboard contains
 * a bare URL and the cursor is in a markdown file, the URL is inserted as-is
 * (no transformation needed for that case).
 */
class MarkdownSmartPasteProcessor : CopyPastePreProcessor {

    private val URL_PATTERN = Regex("""^https?://\S+$""")
    private val IMAGE_URL_PATTERN = Regex("""^https?://.*\.(png|jpg|jpeg|gif|svg|webp|bmp|ico|tiff)(\?.*)?$""", RegexOption.IGNORE_CASE)

    override fun preprocessOnCopy(
        file: PsiFile,
        startOffsets: IntArray,
        endOffsets: IntArray,
        text: String
    ): String? = null

    override fun preprocessOnPaste(
        project: Project,
        file: PsiFile,
        editor: Editor,
        text: String,
        rawText: RawText?
    ): String {
        if (!MarkdownFileUtil.isMarkdownFile(file)) return text

        val settings = try {
            MarkdownSettings.getInstance()
        } catch (_: Exception) {
            return text
        }
        if (!settings.state.smartPasteEnabled) return text

        val pastedText = text.trim()
        if (!URL_PATTERN.matches(pastedText)) return text

        val selectionModel = editor.selectionModel
        if (!selectionModel.hasSelection()) return text

        val selectedText = selectionModel.selectedText
        if (selectedText.isNullOrEmpty()) return text

        // Don't transform if already inside a markdown link
        val document = editor.document
        val selStart = selectionModel.selectionStart
        if (selStart > 0 && document.getText(
                com.intellij.openapi.util.TextRange(maxOf(0, selStart - 2), selStart)
            ).contains("[")
        ) return text

        return if (IMAGE_URL_PATTERN.matches(pastedText)) {
            "![${selectedText}](${pastedText})"
        } else {
            "[${selectedText}](${pastedText})"
        }
    }
}
