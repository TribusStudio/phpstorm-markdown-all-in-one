package com.tribus.markdown.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Hand-written Markdown lexer that handles block-level structure and inline code spans.
 *
 * Block-level: headings, code fences, blockquotes, list markers, horizontal rules.
 * Inline: code spans (backtick-delimited). Bold/italic/links are handled by [MarkdownAnnotator].
 *
 * State encoding for incremental re-lexing: 0 = normal, 1 = inside fenced code block.
 */
class MarkdownLexer : LexerBase() {

    private lateinit var buffer: CharSequence
    private var bufferEnd = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null
    private var inCodeBlock = false
    private var atLineStart = true
    private var inHeading = false

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenEnd = startOffset
        this.inCodeBlock = initialState and 1 != 0
        this.inHeading = false
        this.atLineStart = startOffset == 0 || (startOffset > 0 && buffer[startOffset - 1] == '\n')
        advance()
    }

    override fun getState(): Int = if (inCodeBlock) 1 else 0
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= bufferEnd) {
            tokenType = null
            return
        }

        val c = buffer[tokenStart]

        // Newlines always reset to line start
        if (c == '\n') {
            tokenEnd = tokenStart + 1
            tokenType = MarkdownTokenTypes.EOL
            atLineStart = true
            inHeading = false
            return
        }

        // Inside fenced code block
        if (inCodeBlock) {
            lexCodeBlock(c)
            return
        }

        // Heading content (after marker, until EOL)
        if (inHeading) {
            tokenEnd = findLineEnd(tokenStart)
            tokenType = MarkdownTokenTypes.HEADING_CONTENT
            return
        }

        // Start of line — check block-level constructs
        if (atLineStart) {
            atLineStart = false

            // Leading whitespace
            if (c == ' ' || c == '\t') {
                tokenEnd = tokenStart
                while (tokenEnd < bufferEnd && (buffer[tokenEnd] == ' ' || buffer[tokenEnd] == '\t')) {
                    tokenEnd++
                }
                tokenType = TokenType.WHITE_SPACE
                atLineStart = true
                return
            }

            // Code fence (``` or ~~~)
            val fenceEnd = matchCodeFence(tokenStart)
            if (fenceEnd > 0) {
                tokenEnd = fenceEnd
                tokenType = MarkdownTokenTypes.CODE_FENCE
                inCodeBlock = true
                return
            }

            // ATX heading
            if (c == '#') {
                val headingEnd = matchHeading(tokenStart)
                if (headingEnd > 0) {
                    tokenEnd = headingEnd
                    tokenType = MarkdownTokenTypes.HEADING_MARKER
                    inHeading = true
                    return
                }
            }

            // Blockquote
            if (c == '>') {
                tokenEnd = tokenStart + 1
                if (tokenEnd < bufferEnd && buffer[tokenEnd] == ' ') tokenEnd++
                tokenType = MarkdownTokenTypes.BLOCKQUOTE_MARKER
                atLineStart = true
                return
            }

            // Horizontal rule (must check before list marker)
            if (c == '-' || c == '*' || c == '_') {
                val hrEnd = matchHorizontalRule(tokenStart)
                if (hrEnd > 0) {
                    tokenEnd = hrEnd
                    tokenType = MarkdownTokenTypes.HORIZONTAL_RULE
                    return
                }
            }

            // Unordered list marker
            if ((c == '-' || c == '*' || c == '+') &&
                tokenStart + 1 < bufferEnd && buffer[tokenStart + 1] == ' '
            ) {
                tokenEnd = tokenStart + 2
                tokenType = MarkdownTokenTypes.LIST_MARKER
                return
            }

            // Ordered list marker
            if (c.isDigit()) {
                val olEnd = matchOrderedListMarker(tokenStart)
                if (olEnd > 0) {
                    tokenEnd = olEnd
                    tokenType = MarkdownTokenTypes.LIST_MARKER
                    return
                }
            }
        }

        // Inline: code span
        if (c == '`') {
            val codeSpanEnd = matchCodeSpan(tokenStart)
            if (codeSpanEnd > 0) {
                tokenEnd = codeSpanEnd
                tokenType = MarkdownTokenTypes.CODE_SPAN
                return
            }
        }

        // Regular text — consume until next special char or EOL
        tokenEnd = tokenStart
        while (tokenEnd < bufferEnd && buffer[tokenEnd] != '\n' && buffer[tokenEnd] != '`') {
            tokenEnd++
        }
        if (tokenEnd == tokenStart) {
            tokenEnd = tokenStart + 1
        }
        tokenType = MarkdownTokenTypes.TEXT
    }

    private fun lexCodeBlock(c: Char) {
        if (atLineStart) {
            atLineStart = false

            // Leading whitespace inside code block
            if (c == ' ' || c == '\t') {
                tokenEnd = tokenStart
                while (tokenEnd < bufferEnd && (buffer[tokenEnd] == ' ' || buffer[tokenEnd] == '\t')) {
                    tokenEnd++
                }
                tokenType = TokenType.WHITE_SPACE
                atLineStart = true
                return
            }

            // Check for closing fence
            val fenceEnd = matchCodeFence(tokenStart)
            if (fenceEnd > 0) {
                tokenEnd = fenceEnd
                tokenType = MarkdownTokenTypes.CODE_FENCE
                inCodeBlock = false
                return
            }
        }

        // Code block content until EOL
        tokenEnd = findLineEnd(tokenStart)
        if (tokenEnd == tokenStart) {
            tokenEnd = tokenStart + 1
        }
        tokenType = MarkdownTokenTypes.CODE_BLOCK_CONTENT
    }

    private fun findLineEnd(from: Int): Int {
        var pos = from
        while (pos < bufferEnd && buffer[pos] != '\n') pos++
        return pos
    }

    private fun matchCodeFence(pos: Int): Int {
        val c = buffer[pos]
        if (c != '`' && c != '~') return 0
        var end = pos
        while (end < bufferEnd && buffer[end] == c) end++
        if (end - pos < 3) return 0
        // Consume rest of line (language info for opening fence, or whitespace for closing)
        while (end < bufferEnd && buffer[end] != '\n') end++
        return end
    }

    private fun matchHeading(pos: Int): Int {
        var end = pos
        while (end < bufferEnd && buffer[end] == '#' && end - pos < 6) end++
        if (end >= bufferEnd) return 0
        val next = buffer[end]
        if (next != ' ' && next != '\n') return 0
        if (next == ' ') end++
        return end
    }

    private fun matchHorizontalRule(pos: Int): Int {
        val c = buffer[pos]
        var end = pos
        var charCount = 0
        while (end < bufferEnd && buffer[end] != '\n') {
            when (buffer[end]) {
                c -> charCount++
                ' ', '\t' -> {}
                else -> return 0
            }
            end++
        }
        return if (charCount >= 3) end else 0
    }

    private fun matchOrderedListMarker(pos: Int): Int {
        var end = pos
        while (end < bufferEnd && buffer[end].isDigit()) end++
        if (end >= bufferEnd) return 0
        if (buffer[end] != '.' && buffer[end] != ')') return 0
        end++
        if (end >= bufferEnd || buffer[end] != ' ') return 0
        return end + 1
    }

    private fun matchCodeSpan(pos: Int): Int {
        var openEnd = pos
        while (openEnd < bufferEnd && buffer[openEnd] == '`') openEnd++
        val backtickCount = openEnd - pos

        var searchPos = openEnd
        while (searchPos < bufferEnd) {
            if (buffer[searchPos] == '\n') return 0
            if (buffer[searchPos] == '`') {
                var closeEnd = searchPos
                while (closeEnd < bufferEnd && buffer[closeEnd] == '`') closeEnd++
                if (closeEnd - searchPos == backtickCount) return closeEnd
                searchPos = closeEnd
            } else {
                searchPos++
            }
        }
        return 0
    }
}
