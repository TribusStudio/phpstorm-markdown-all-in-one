package com.tribus.markdown.lang.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.tribus.markdown.lang.MarkdownIcons
import javax.swing.Icon

/**
 * Registers markdown color attributes in Settings > Editor > Color Scheme > Markdown.
 * Allows users to customize all highlighting colors for markdown elements.
 */
class MarkdownColorSettingsPage : ColorSettingsPage {

    override fun getDisplayName(): String = "Markdown"

    override fun getIcon(): Icon = MarkdownIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = MarkdownSyntaxHighlighter()

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = TAG_MAP

    override fun getDemoText(): String = DEMO_TEXT

    companion object {
        private val DESCRIPTORS = arrayOf(
            // Block-level
            AttributesDescriptor("Heading//Marker", MarkdownHighlightingColors.HEADING_MARKER),
            AttributesDescriptor("Heading//Content", MarkdownHighlightingColors.HEADING_CONTENT),
            AttributesDescriptor("Code//Fence markers", MarkdownHighlightingColors.CODE_FENCE),
            AttributesDescriptor("Code//Block content", MarkdownHighlightingColors.CODE_BLOCK_CONTENT),
            AttributesDescriptor("Code//Inline code span", MarkdownHighlightingColors.CODE_SPAN),
            AttributesDescriptor("Blockquote", MarkdownHighlightingColors.BLOCKQUOTE),
            AttributesDescriptor("List marker", MarkdownHighlightingColors.LIST_MARKER),
            AttributesDescriptor("Horizontal rule", MarkdownHighlightingColors.HORIZONTAL_RULE),
            AttributesDescriptor("Text", MarkdownHighlightingColors.TEXT),
            // Inline formatting
            AttributesDescriptor("Bold", MarkdownHighlightingColors.BOLD),
            AttributesDescriptor("Italic", MarkdownHighlightingColors.ITALIC),
            AttributesDescriptor("Strikethrough", MarkdownHighlightingColors.STRIKETHROUGH),
            AttributesDescriptor("Link//Text", MarkdownHighlightingColors.LINK_TEXT),
            AttributesDescriptor("Link//Destination", MarkdownHighlightingColors.LINK_DESTINATION),
            AttributesDescriptor("Image", MarkdownHighlightingColors.IMAGE_MARKER),
            // Math
            AttributesDescriptor("Math//Delimiter", MarkdownHighlightingColors.MATH_DELIMITER),
            AttributesDescriptor("Math//Content", MarkdownHighlightingColors.MATH_CONTENT),
            // Editor decorations
            AttributesDescriptor("Decorations//Code span background", MarkdownHighlightingColors.CODE_SPAN_DECORATED),
            AttributesDescriptor("Decorations//Strikethrough effect", MarkdownHighlightingColors.STRIKETHROUGH_EFFECT),
            AttributesDescriptor("Decorations//Formatting marker", MarkdownHighlightingColors.FORMATTING_MARKER),
            AttributesDescriptor("Decorations//Trailing space", MarkdownHighlightingColors.TRAILING_SPACE),
            AttributesDescriptor("Decorations//Hard line break", MarkdownHighlightingColors.HARD_LINE_BREAK),
        )

        private val TAG_MAP = mapOf(
            "bold" to MarkdownHighlightingColors.BOLD,
            "italic" to MarkdownHighlightingColors.ITALIC,
            "strike" to MarkdownHighlightingColors.STRIKETHROUGH,
            "link_text" to MarkdownHighlightingColors.LINK_TEXT,
            "link_dest" to MarkdownHighlightingColors.LINK_DESTINATION,
            "image" to MarkdownHighlightingColors.IMAGE_MARKER,
            "math_delim" to MarkdownHighlightingColors.MATH_DELIMITER,
            "math_content" to MarkdownHighlightingColors.MATH_CONTENT,
            "marker" to MarkdownHighlightingColors.FORMATTING_MARKER,
        )

        private val DEMO_TEXT = """
# Heading 1
## Heading 2

Regular text with <bold>**bold**</bold> and <italic>*italic*</italic> formatting.
Text with <strike>~~strikethrough~~</strike> and `inline code` spans.

- Unordered list item
1. Ordered list item

> This is a blockquote

[<link_text>Link text</link_text>](<link_dest>https://example.com</link_dest>)
<image>![Alt text](image.png)</image>

<math_delim>$</math_delim><math_content>E = mc^2</math_content><math_delim>$</math_delim>

```
code block content
```

---
""".trimIndent()
    }
}
