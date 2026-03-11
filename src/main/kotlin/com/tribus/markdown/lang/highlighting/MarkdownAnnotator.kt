package com.tribus.markdown.lang.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.tribus.markdown.lang.MarkdownTokenTypes

/**
 * Annotator for inline Markdown formatting that the lexer doesn't handle:
 * bold, italic, strikethrough, and links.
 *
 * Operates on TEXT tokens and applies sub-range highlighting.
 */
class MarkdownAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val node = element.node ?: return
        val elementType = node.elementType

        if (elementType != MarkdownTokenTypes.TEXT &&
            elementType != MarkdownTokenTypes.HEADING_CONTENT
        ) return

        val text = element.text
        val baseOffset = element.textRange.startOffset

        annotateBold(text, baseOffset, holder)
        annotateItalic(text, baseOffset, holder)
        annotateStrikethrough(text, baseOffset, holder)
        annotateLinks(text, baseOffset, holder)
        annotateImages(text, baseOffset, holder)
        annotateMath(text, baseOffset, holder)
    }

    private fun annotateBold(text: String, baseOffset: Int, holder: AnnotationHolder) {
        annotateInlinePattern(text, baseOffset, holder, BOLD_PATTERN, MarkdownHighlightingColors.BOLD)
    }

    private fun annotateItalic(text: String, baseOffset: Int, holder: AnnotationHolder) {
        annotateInlinePattern(text, baseOffset, holder, ITALIC_STAR_PATTERN, MarkdownHighlightingColors.ITALIC)
        annotateInlinePattern(text, baseOffset, holder, ITALIC_UNDER_PATTERN, MarkdownHighlightingColors.ITALIC)
    }

    private fun annotateStrikethrough(text: String, baseOffset: Int, holder: AnnotationHolder) {
        annotateInlinePattern(text, baseOffset, holder, STRIKETHROUGH_PATTERN, MarkdownHighlightingColors.STRIKETHROUGH)
    }

    private fun annotateLinks(text: String, baseOffset: Int, holder: AnnotationHolder) {
        for (match in LINK_PATTERN.findAll(text)) {
            val linkTextGroup = match.groups[1] ?: continue
            val linkDestGroup = match.groups[2] ?: continue
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + linkTextGroup.range.first, baseOffset + linkTextGroup.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.LINK_TEXT)
                .create()
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + linkDestGroup.range.first, baseOffset + linkDestGroup.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.LINK_DESTINATION)
                .create()
        }
    }

    private fun annotateImages(text: String, baseOffset: Int, holder: AnnotationHolder) {
        for (match in IMAGE_PATTERN.findAll(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.IMAGE_MARKER)
                .create()
        }
    }

    private fun annotateInlinePattern(
        text: String,
        baseOffset: Int,
        holder: AnnotationHolder,
        pattern: Regex,
        key: com.intellij.openapi.editor.colors.TextAttributesKey
    ) {
        for (match in pattern.findAll(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.last + 1))
                .textAttributes(key)
                .create()
        }
    }

    private fun annotateMath(text: String, baseOffset: Int, holder: AnnotationHolder) {
        // Display math: $$...$$
        for (match in DISPLAY_MATH_PATTERN.findAll(text)) {
            // Highlight delimiters
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.first + 2))
                .textAttributes(MarkdownHighlightingColors.MATH_DELIMITER)
                .create()
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.last - 1, baseOffset + match.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_DELIMITER)
                .create()
            // Highlight content
            val contentGroup = match.groups[1] ?: continue
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + contentGroup.range.first, baseOffset + contentGroup.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_CONTENT)
                .create()
        }
        // Inline math: $...$ (not preceded/followed by $)
        for (match in INLINE_MATH_PATTERN.findAll(text)) {
            // Highlight delimiters
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.first + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_DELIMITER)
                .create()
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.last, baseOffset + match.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_DELIMITER)
                .create()
            // Highlight content
            val contentGroup = match.groups[1] ?: continue
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + contentGroup.range.first, baseOffset + contentGroup.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_CONTENT)
                .create()
        }
    }

    companion object {
        // Bold: **text** or __text__ (non-greedy, must not be empty)
        private val BOLD_PATTERN = Regex("""\*\*(.+?)\*\*|__(.+?)__""")
        // Italic: *text* (single star, not preceded/followed by another star)
        private val ITALIC_STAR_PATTERN = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""")
        // Italic: _text_ (single underscore)
        private val ITALIC_UNDER_PATTERN = Regex("""(?<!_)_(?!_)(.+?)(?<!_)_(?!_)""")
        // Strikethrough: ~~text~~
        private val STRIKETHROUGH_PATTERN = Regex("""~~(.+?)~~""")
        // Link: [text](url)
        private val LINK_PATTERN = Regex("""\[([^\]]+)]\(([^)]+)\)""")
        // Image: ![alt](url)
        private val IMAGE_PATTERN = Regex("""!\[([^\]]*)]\([^)]+\)""")
        // Display math: $$...$$ (non-greedy)
        private val DISPLAY_MATH_PATTERN = Regex("""\$\$(.+?)\$\$""")
        // Inline math: $...$ (not preceded/followed by another $)
        private val INLINE_MATH_PATTERN = Regex("""(?<!\$)\$(?!\$)(.+?)(?<!\$)\$(?!\$)""")
    }
}
