package com.tribus.markdown.lang.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object MarkdownHighlightingColors {

    val HEADING_MARKER = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_HEADING_MARKER", DefaultLanguageHighlighterColors.KEYWORD
    )
    val HEADING_CONTENT = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_HEADING_CONTENT", DefaultLanguageHighlighterColors.KEYWORD
    )
    val CODE_FENCE = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_CODE_FENCE", DefaultLanguageHighlighterColors.MARKUP_TAG
    )
    val CODE_BLOCK_CONTENT = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_CODE_BLOCK", DefaultLanguageHighlighterColors.MARKUP_TAG
    )
    val CODE_SPAN = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_CODE_SPAN", DefaultLanguageHighlighterColors.MARKUP_TAG
    )
    val BLOCKQUOTE = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_BLOCKQUOTE", DefaultLanguageHighlighterColors.DOC_COMMENT
    )
    val LIST_MARKER = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_LIST_MARKER", DefaultLanguageHighlighterColors.KEYWORD
    )
    val HORIZONTAL_RULE = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_HORIZONTAL_RULE", DefaultLanguageHighlighterColors.BRACES
    )
    val TEXT = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_TEXT", HighlighterColors.TEXT
    )

    // Inline formatting — used by MarkdownAnnotator
    // Fall back to KEYWORD for bold (renders as bold in most themes), DOC_COMMENT_TAG for italic
    val BOLD = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_BOLD", DefaultLanguageHighlighterColors.KEYWORD
    )
    val ITALIC = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_ITALIC", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE
    )
    val STRIKETHROUGH = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_STRIKETHROUGH", DefaultLanguageHighlighterColors.LINE_COMMENT
    )
    val LINK_TEXT = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_LINK_TEXT", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE
    )
    val LINK_DESTINATION = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_LINK_DESTINATION", DefaultLanguageHighlighterColors.STRING
    )
    val IMAGE_MARKER = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_IMAGE", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE
    )

    // Math — displayed in a distinct color to distinguish from regular text
    val MATH_DELIMITER = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_MATH_DELIMITER", DefaultLanguageHighlighterColors.MARKUP_TAG
    )
    val MATH_CONTENT = TextAttributesKey.createTextAttributesKey(
        "MARKDOWN_MATH_CONTENT", DefaultLanguageHighlighterColors.NUMBER
    )
}
