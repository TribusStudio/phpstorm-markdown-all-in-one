package com.tribus.markdown.lang.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

object MarkdownHighlightingColors {

    // External key names are namespaced with an MDAIO_ prefix.
    //
    // TextAttributesKey names are a single flat namespace shared by every
    // plugin in the IDE. PhpStorm still bundles JetBrains' own Markdown plugin,
    // which registers its own MARKDOWN_* keys; using the same names meant
    // whichever plugin loaded first won, the loser's fallback was silently
    // discarded, and the IDE logged a SEVERE error per collision at startup.
    //
    // Colors for these keys ship in colorSchemes/ and are wired up through
    // <additionalTextAttributes> in plugin.xml, so highlighting no longer
    // depends on the bundled plugin being installed or enabled.

    val HEADING_MARKER = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_HEADING_MARKER", DefaultLanguageHighlighterColors.KEYWORD
    )
    val HEADING_CONTENT = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_HEADING_CONTENT", DefaultLanguageHighlighterColors.KEYWORD
    )
    val CODE_FENCE = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_CODE_FENCE", DefaultLanguageHighlighterColors.MARKUP_TAG
    )
    val CODE_BLOCK_CONTENT = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_CODE_BLOCK", DefaultLanguageHighlighterColors.MARKUP_TAG
    )
    val CODE_SPAN = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_CODE_SPAN", DefaultLanguageHighlighterColors.MARKUP_TAG
    )
    val BLOCKQUOTE = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_BLOCKQUOTE", DefaultLanguageHighlighterColors.DOC_COMMENT
    )
    val LIST_MARKER = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_LIST_MARKER", DefaultLanguageHighlighterColors.KEYWORD
    )
    val HORIZONTAL_RULE = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_HORIZONTAL_RULE", DefaultLanguageHighlighterColors.BRACES
    )
    val TEXT = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_TEXT", HighlighterColors.TEXT
    )

    // Inline formatting — used by MarkdownAnnotator
    // Fall back to KEYWORD for bold (renders as bold in most themes), DOC_COMMENT_TAG for italic
    val BOLD = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_BOLD", DefaultLanguageHighlighterColors.KEYWORD
    )
    val ITALIC = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_ITALIC", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE
    )
    val STRIKETHROUGH = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_STRIKETHROUGH", DefaultLanguageHighlighterColors.LINE_COMMENT
    )
    val LINK_TEXT = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_LINK_TEXT", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE
    )
    val LINK_DESTINATION = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_LINK_DESTINATION", DefaultLanguageHighlighterColors.STRING
    )
    val IMAGE_MARKER = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_IMAGE", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE
    )

    // Math — displayed in a distinct color to distinguish from regular text
    val MATH_DELIMITER = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_MATH_DELIMITER", DefaultLanguageHighlighterColors.MARKUP_TAG
    )
    val MATH_CONTENT = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_MATH_CONTENT", DefaultLanguageHighlighterColors.NUMBER
    )

    // ── Phase 12: Editor Decorations ─────────────────────────────────

    // Code span background — subtle background tint for inline `code`
    val CODE_SPAN_DECORATED: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_CODE_SPAN_DECORATED", CODE_SPAN
    )

    // Strikethrough — actual strikethrough effect on ~~text~~
    val STRIKETHROUGH_EFFECT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_STRIKETHROUGH_EFFECT", STRIKETHROUGH
    )

    // Formatting mark dimming — muted foreground for **, ~~, ` markers
    val FORMATTING_MARKER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_FORMATTING_MARKER", DefaultLanguageHighlighterColors.BRACES
    )

    // Trailing whitespace — visible indicator for trailing spaces
    val TRAILING_SPACE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_TRAILING_SPACE", DefaultLanguageHighlighterColors.BRACES
    )

    // Hard line break — trailing double-space that renders as <br>
    val HARD_LINE_BREAK: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "MDAIO_MARKDOWN_HARD_LINE_BREAK", DefaultLanguageHighlighterColors.BRACES
    )

    // Default TextAttributes with enforced visual effects (used as base defaults)
    object Defaults {
        val STRIKETHROUGH_EFFECT_ATTRS = TextAttributes(null, null, null, EffectType.STRIKEOUT, Font.PLAIN)
        val CODE_SPAN_BG_LIGHT = Color(0xF0, 0xF0, 0xF0)
        val CODE_SPAN_BG_DARK = Color(0x38, 0x3B, 0x3D)
        val TRAILING_SPACE_BG_LIGHT = Color(0xFF, 0xEB, 0xEB, 80)
        val TRAILING_SPACE_BG_DARK = Color(0xFF, 0x60, 0x60, 40)
        val HARD_BREAK_BG_LIGHT = Color(0xDB, 0xED, 0xFF, 100)
        val HARD_BREAK_BG_DARK = Color(0x40, 0x80, 0xC0, 60)
    }
}
