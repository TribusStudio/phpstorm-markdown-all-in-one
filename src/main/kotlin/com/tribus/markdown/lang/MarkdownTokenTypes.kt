package com.tribus.markdown.lang

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

object MarkdownTokenTypes {
    // Block-level tokens
    val HEADING_MARKER = IElementType("HEADING_MARKER", MarkdownLanguage)
    val HEADING_CONTENT = IElementType("HEADING_CONTENT", MarkdownLanguage)
    val CODE_FENCE = IElementType("CODE_FENCE", MarkdownLanguage)
    val CODE_BLOCK_CONTENT = IElementType("CODE_BLOCK_CONTENT", MarkdownLanguage)
    val BLOCKQUOTE_MARKER = IElementType("BLOCKQUOTE_MARKER", MarkdownLanguage)
    val LIST_MARKER = IElementType("LIST_MARKER", MarkdownLanguage)
    val HORIZONTAL_RULE = IElementType("HORIZONTAL_RULE", MarkdownLanguage)

    // Inline tokens
    val CODE_SPAN = IElementType("CODE_SPAN", MarkdownLanguage)
    val TEXT = IElementType("TEXT", MarkdownLanguage)
    val EOL = IElementType("EOL", MarkdownLanguage)

    val COMMENTS = TokenSet.EMPTY
    val STRINGS = TokenSet.create(CODE_SPAN, CODE_BLOCK_CONTENT)
}
