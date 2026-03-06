package com.tribus.markdown.lang.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.tribus.markdown.lang.MarkdownLexer
import com.tribus.markdown.lang.MarkdownTokenTypes

class MarkdownSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = MarkdownLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        MarkdownTokenTypes.HEADING_MARKER -> pack(MarkdownHighlightingColors.HEADING_MARKER)
        MarkdownTokenTypes.HEADING_CONTENT -> pack(MarkdownHighlightingColors.HEADING_CONTENT)
        MarkdownTokenTypes.CODE_FENCE -> pack(MarkdownHighlightingColors.CODE_FENCE)
        MarkdownTokenTypes.CODE_BLOCK_CONTENT -> pack(MarkdownHighlightingColors.CODE_BLOCK_CONTENT)
        MarkdownTokenTypes.CODE_SPAN -> pack(MarkdownHighlightingColors.CODE_SPAN)
        MarkdownTokenTypes.BLOCKQUOTE_MARKER -> pack(MarkdownHighlightingColors.BLOCKQUOTE)
        MarkdownTokenTypes.LIST_MARKER -> pack(MarkdownHighlightingColors.LIST_MARKER)
        MarkdownTokenTypes.HORIZONTAL_RULE -> pack(MarkdownHighlightingColors.HORIZONTAL_RULE)
        MarkdownTokenTypes.TEXT -> pack(MarkdownHighlightingColors.TEXT)
        else -> TextAttributesKey.EMPTY_ARRAY
    }
}
