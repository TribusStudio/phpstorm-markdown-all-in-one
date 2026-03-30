package com.tribus.markdown.lang.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.tribus.markdown.lang.MarkdownTokenTypes
import com.tribus.markdown.settings.MarkdownSettings
import java.awt.Color
import java.awt.Font

/**
 * Annotator for inline Markdown formatting that the lexer doesn't handle:
 * bold, italic, strikethrough, links, math, and editor decorations.
 *
 * Operates on TEXT and HEADING_CONTENT tokens and applies sub-range highlighting.
 */
class MarkdownAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val node = element.node ?: return
        val elementType = node.elementType

        // Check file size limit for decorations
        val settings = try { MarkdownSettings.getInstance().state } catch (_: Exception) { null }
        val fileLength = element.containingFile?.textLength ?: 0
        val decorationLimit = settings?.decorationFileSizeLimit ?: 500_000
        val decorationsEnabled = fileLength <= decorationLimit

        if (elementType == MarkdownTokenTypes.TEXT ||
            elementType == MarkdownTokenTypes.HEADING_CONTENT
        ) {
            val text = element.text
            val baseOffset = element.textRange.startOffset

            annotateBold(text, baseOffset, holder, settings, decorationsEnabled)
            annotateItalic(text, baseOffset, holder, settings, decorationsEnabled)
            annotateStrikethrough(text, baseOffset, holder, settings, decorationsEnabled)
            annotateLinks(text, baseOffset, holder)
            annotateImages(text, baseOffset, holder)
            annotateMath(text, baseOffset, holder)

            if (decorationsEnabled) {
                annotateTrailingSpaces(text, baseOffset, holder, settings)
            }
        }

        // Code span background decoration
        if (elementType == MarkdownTokenTypes.CODE_SPAN && decorationsEnabled) {
            if (settings?.decorationCodeSpanBackground != false) {
                annotateCodeSpanBackground(element, holder)
            }
        }
    }

    private fun annotateBold(text: String, baseOffset: Int, holder: AnnotationHolder,
                             settings: MarkdownSettings.State?, decorationsEnabled: Boolean) {
        for (match in BOLD_PATTERN.findAll(text)) {
            // Full match highlighting
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.BOLD)
                .create()

            // Dim the markers (**/__) if decoration enabled
            if (decorationsEnabled && settings?.decorationFormattingMarkerDimming != false) {
                val markerLen = if (match.value.startsWith("**")) 2 else 2
                annotateMarkerDimming(baseOffset, match.range.first, markerLen, holder)
                annotateMarkerDimming(baseOffset, match.range.last + 1 - markerLen, markerLen, holder)
            }
        }
    }

    private fun annotateItalic(text: String, baseOffset: Int, holder: AnnotationHolder,
                               settings: MarkdownSettings.State?, decorationsEnabled: Boolean) {
        for (pattern in listOf(ITALIC_STAR_PATTERN, ITALIC_UNDER_PATTERN)) {
            for (match in pattern.findAll(text)) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.last + 1))
                    .textAttributes(MarkdownHighlightingColors.ITALIC)
                    .create()

                if (decorationsEnabled && settings?.decorationFormattingMarkerDimming != false) {
                    annotateMarkerDimming(baseOffset, match.range.first, 1, holder)
                    annotateMarkerDimming(baseOffset, match.range.last, 1, holder)
                }
            }
        }
    }

    private fun annotateStrikethrough(text: String, baseOffset: Int, holder: AnnotationHolder,
                                      settings: MarkdownSettings.State?, decorationsEnabled: Boolean) {
        for (match in STRIKETHROUGH_PATTERN.findAll(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.STRIKETHROUGH)
                .create()

            // Apply actual strikethrough effect on the content (between ~~)
            if (decorationsEnabled && settings?.decorationStrikethrough != false) {
                val contentStart = match.range.first + 2
                val contentEnd = match.range.last + 1 - 2
                if (contentEnd > contentStart) {
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(TextRange(baseOffset + contentStart, baseOffset + contentEnd))
                        .enforcedTextAttributes(MarkdownHighlightingColors.Defaults.STRIKETHROUGH_EFFECT_ATTRS)
                        .create()
                }
            }

            if (decorationsEnabled && settings?.decorationFormattingMarkerDimming != false) {
                annotateMarkerDimming(baseOffset, match.range.first, 2, holder)
                annotateMarkerDimming(baseOffset, match.range.last - 1, 2, holder)
            }
        }
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

    private fun annotateMath(text: String, baseOffset: Int, holder: AnnotationHolder) {
        // Display math: $$...$$
        for (match in DISPLAY_MATH_PATTERN.findAll(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.first + 2))
                .textAttributes(MarkdownHighlightingColors.MATH_DELIMITER)
                .create()
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.last - 1, baseOffset + match.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_DELIMITER)
                .create()
            val contentGroup = match.groups[1] ?: continue
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + contentGroup.range.first, baseOffset + contentGroup.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_CONTENT)
                .create()
        }
        // Inline math: $...$
        for (match in INLINE_MATH_PATTERN.findAll(text)) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.first, baseOffset + match.range.first + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_DELIMITER)
                .create()
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + match.range.last, baseOffset + match.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_DELIMITER)
                .create()
            val contentGroup = match.groups[1] ?: continue
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + contentGroup.range.first, baseOffset + contentGroup.range.last + 1))
                .textAttributes(MarkdownHighlightingColors.MATH_CONTENT)
                .create()
        }
    }

    // ── Phase 12: Decoration helpers ─────────────────────────────────

    private fun annotateMarkerDimming(baseOffset: Int, start: Int, length: Int, holder: AnnotationHolder) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(TextRange(baseOffset + start, baseOffset + start + length))
            .textAttributes(MarkdownHighlightingColors.FORMATTING_MARKER)
            .create()
    }

    private fun annotateCodeSpanBackground(element: PsiElement, holder: AnnotationHolder) {
        val isDark = EditorColorsManager.getInstance().isDarkEditor
        val bg = if (isDark) MarkdownHighlightingColors.Defaults.CODE_SPAN_BG_DARK
                 else MarkdownHighlightingColors.Defaults.CODE_SPAN_BG_LIGHT
        // Apply background to the full code span (backticks + content) for a
        // uniform visual, matching GitHub/VSCode rendering. Using textAttributes
        // via a key rather than enforcedTextAttributes so we layer on top of the
        // existing syntax highlighting instead of replacing it.
        val attrs = TextAttributes()
        attrs.backgroundColor = bg
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element.textRange)
            .enforcedTextAttributes(attrs)
            .create()
    }

    private fun annotateTrailingSpaces(text: String, baseOffset: Int, holder: AnnotationHolder,
                                       settings: MarkdownSettings.State?) {
        // Find trailing whitespace at end of text (which corresponds to end of line for TEXT tokens)
        val trailingMatch = TRAILING_SPACE_PATTERN.find(text) ?: return
        val spaces = trailingMatch.value
        val start = trailingMatch.range.first
        val end = trailingMatch.range.last + 1

        if (spaces.length >= 2 && settings?.decorationHardLineBreak != false) {
            // Hard line break: 2+ trailing spaces
            val isDark = EditorColorsManager.getInstance().isDarkEditor
            val bg = if (isDark) MarkdownHighlightingColors.Defaults.HARD_BREAK_BG_DARK
                     else MarkdownHighlightingColors.Defaults.HARD_BREAK_BG_LIGHT
            val attrs = TextAttributes(null, bg, null, null, Font.PLAIN)
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + start, baseOffset + end))
                .enforcedTextAttributes(attrs)
                .create()
        } else if (settings?.decorationTrailingSpace != false) {
            // Regular trailing whitespace
            val isDark = EditorColorsManager.getInstance().isDarkEditor
            val bg = if (isDark) MarkdownHighlightingColors.Defaults.TRAILING_SPACE_BG_DARK
                     else MarkdownHighlightingColors.Defaults.TRAILING_SPACE_BG_LIGHT
            val attrs = TextAttributes(null, bg, null, null, Font.PLAIN)
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(baseOffset + start, baseOffset + end))
                .enforcedTextAttributes(attrs)
                .create()
        }
    }

    companion object {
        // Bold: **text** or __text__
        private val BOLD_PATTERN = Regex("""\*\*(.+?)\*\*|__(.+?)__""")
        // Italic: *text* (single star)
        private val ITALIC_STAR_PATTERN = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""")
        // Italic: _text_ (single underscore)
        private val ITALIC_UNDER_PATTERN = Regex("""(?<!_)_(?!_)(.+?)(?<!_)_(?!_)""")
        // Strikethrough: ~~text~~
        private val STRIKETHROUGH_PATTERN = Regex("""~~(.+?)~~""")
        // Link: [text](url)
        private val LINK_PATTERN = Regex("""\[([^\]]+)]\(([^)]+)\)""")
        // Image: ![alt](url)
        private val IMAGE_PATTERN = Regex("""!\[([^\]]*)]\([^)]+\)""")
        // Display math: $$...$$
        private val DISPLAY_MATH_PATTERN = Regex("""\$\$(.+?)\$\$""")
        // Inline math: $...$
        private val INLINE_MATH_PATTERN = Regex("""(?<!\$)\$(?!\$)(.+?)(?<!\$)\$(?!\$)""")
        // Trailing whitespace at end of text
        private val TRAILING_SPACE_PATTERN = Regex(""" +$""")
    }
}
