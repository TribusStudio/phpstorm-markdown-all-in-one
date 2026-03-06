package com.tribus.markdown.util

object MarkdownFormattingUtil {

    enum class FormattingWrapper(val marker: String) {
        BOLD("**"),
        ITALIC("*"),
        STRIKETHROUGH("~~"),
        CODE_SPAN("`")
    }

    /**
     * Toggle formatting markers on text. If the text is already wrapped with the
     * marker, removes it. Otherwise, adds the marker around the text.
     */
    fun toggleFormatting(text: String, wrapper: FormattingWrapper): String {
        val marker = wrapper.marker

        return if (text.startsWith(marker) && text.endsWith(marker) && text.length >= marker.length * 2) {
            // Remove formatting
            text.removePrefix(marker).removeSuffix(marker)
        } else {
            // Add formatting
            "$marker$text$marker"
        }
    }

    /**
     * Find the word boundaries at the given offset in the text.
     * Returns a Pair(start, end) or null if no word found at offset.
     */
    fun findWordAt(text: String, offset: Int): Pair<Int, Int>? {
        if (offset < 0 || offset > text.length) return null
        if (text.isEmpty()) return null

        // Adjust offset if at end of text
        val pos = if (offset == text.length) offset - 1 else offset
        if (pos < 0 || !isWordChar(text[pos])) return null

        var start = pos
        var end = pos

        while (start > 0 && isWordChar(text[start - 1])) start--
        while (end < text.length - 1 && isWordChar(text[end + 1])) end++

        return Pair(start, end + 1)
    }

    private fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_' || c == '-'
    }
}
