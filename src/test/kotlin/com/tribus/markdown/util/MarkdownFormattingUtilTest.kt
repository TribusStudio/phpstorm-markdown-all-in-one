package com.tribus.markdown.util

import com.tribus.markdown.util.MarkdownFormattingUtil.FormattingWrapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MarkdownFormattingUtilTest {

    @Test
    fun `toggle bold adds markers to plain text`() {
        assertEquals("**hello**", MarkdownFormattingUtil.toggleFormatting("hello", FormattingWrapper.BOLD))
    }

    @Test
    fun `toggle bold removes markers from bold text`() {
        assertEquals("hello", MarkdownFormattingUtil.toggleFormatting("**hello**", FormattingWrapper.BOLD))
    }

    @Test
    fun `toggle italic adds markers to plain text`() {
        assertEquals("*hello*", MarkdownFormattingUtil.toggleFormatting("hello", FormattingWrapper.ITALIC))
    }

    @Test
    fun `toggle italic removes markers from italic text`() {
        assertEquals("hello", MarkdownFormattingUtil.toggleFormatting("*hello*", FormattingWrapper.ITALIC))
    }

    @Test
    fun `toggle strikethrough adds markers`() {
        assertEquals("~~hello~~", MarkdownFormattingUtil.toggleFormatting("hello", FormattingWrapper.STRIKETHROUGH))
    }

    @Test
    fun `toggle strikethrough removes markers`() {
        assertEquals("hello", MarkdownFormattingUtil.toggleFormatting("~~hello~~", FormattingWrapper.STRIKETHROUGH))
    }

    @Test
    fun `toggle code span adds backticks`() {
        assertEquals("`hello`", MarkdownFormattingUtil.toggleFormatting("hello", FormattingWrapper.CODE_SPAN))
    }

    @Test
    fun `toggle code span removes backticks`() {
        assertEquals("hello", MarkdownFormattingUtil.toggleFormatting("`hello`", FormattingWrapper.CODE_SPAN))
    }

    @Test
    fun `toggle empty text wraps with markers`() {
        assertEquals("****", MarkdownFormattingUtil.toggleFormatting("", FormattingWrapper.BOLD))
    }

    @Test
    fun `findWordAt returns word boundaries`() {
        val result = MarkdownFormattingUtil.findWordAt("hello world", 2)
        assertEquals(Pair(0, 5), result)
    }

    @Test
    fun `findWordAt returns second word`() {
        val result = MarkdownFormattingUtil.findWordAt("hello world", 8)
        assertEquals(Pair(6, 11), result)
    }

    @Test
    fun `findWordAt returns null for space`() {
        val result = MarkdownFormattingUtil.findWordAt("hello world", 5)
        assertNull(result)
    }

    @Test
    fun `findWordAt returns null for empty text`() {
        val result = MarkdownFormattingUtil.findWordAt("", 0)
        assertNull(result)
    }

    @Test
    fun `findWordAt handles end of text`() {
        val result = MarkdownFormattingUtil.findWordAt("hello", 5)
        assertEquals(Pair(0, 5), result)
    }

    @Test
    fun `findWordAt includes hyphens and underscores`() {
        val result = MarkdownFormattingUtil.findWordAt("hello-world_foo", 7)
        assertEquals(Pair(0, 15), result)
    }
}
