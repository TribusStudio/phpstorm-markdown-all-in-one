package com.tribus.markdown.highlighting

import com.tribus.markdown.lang.highlighting.MarkdownHighlightingColors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.awt.Font

class EditorDecorationsTest {

    @Test
    fun `strikethrough effect attributes has STRIKEOUT effect`() {
        val attrs = MarkdownHighlightingColors.Defaults.STRIKETHROUGH_EFFECT_ATTRS
        assertEquals(com.intellij.openapi.editor.markup.EffectType.STRIKEOUT, attrs.effectType)
        assertEquals(Font.PLAIN, attrs.fontType)
    }

    @Test
    fun `code span background colors are defined for light and dark`() {
        val light = MarkdownHighlightingColors.Defaults.CODE_SPAN_BG_LIGHT
        val dark = MarkdownHighlightingColors.Defaults.CODE_SPAN_BG_DARK
        assertNotNull(light)
        assertNotNull(dark)
        // Light should be lighter than dark
        assert(light.red > dark.red)
    }

    @Test
    fun `trailing space colors are defined`() {
        assertNotNull(MarkdownHighlightingColors.Defaults.TRAILING_SPACE_BG_LIGHT)
        assertNotNull(MarkdownHighlightingColors.Defaults.TRAILING_SPACE_BG_DARK)
    }

    @Test
    fun `hard break colors are defined`() {
        assertNotNull(MarkdownHighlightingColors.Defaults.HARD_BREAK_BG_LIGHT)
        assertNotNull(MarkdownHighlightingColors.Defaults.HARD_BREAK_BG_DARK)
    }

    @Test
    fun `TextAttributesKey entries exist for all decorations`() {
        assertNotNull(MarkdownHighlightingColors.CODE_SPAN_DECORATED)
        assertNotNull(MarkdownHighlightingColors.STRIKETHROUGH_EFFECT)
        assertNotNull(MarkdownHighlightingColors.FORMATTING_MARKER)
        assertNotNull(MarkdownHighlightingColors.TRAILING_SPACE)
        assertNotNull(MarkdownHighlightingColors.HARD_LINE_BREAK)
    }

    @Test
    fun `trailing space regex matches trailing spaces`() {
        val pattern = Regex(""" +$""")
        val match = pattern.find("hello world   ")
        assertNotNull(match)
        assertEquals(11, match!!.range.first)
        assertEquals("   ", match.value)
    }

    @Test
    fun `trailing space regex does not match non-trailing spaces`() {
        val pattern = Regex(""" +$""")
        val match = pattern.find("hello world")
        assertNull(match)
    }

    @Test
    fun `hard line break is 2+ trailing spaces`() {
        val pattern = Regex(""" +$""")
        val match = pattern.find("text  ")
        assertNotNull(match)
        assertEquals(2, match!!.value.length)
    }

    @Test
    fun `formatting marker pattern finds bold markers`() {
        val boldPattern = Regex("""\*\*(.+?)\*\*|__(.+?)__""")
        val match = boldPattern.find("**bold text**")
        assertNotNull(match)
        // Markers are at positions 0-1 and 11-12
        assertEquals(0, match!!.range.first)
        assertEquals(12, match.range.last)
    }

    @Test
    fun `strikethrough pattern finds content between tildes`() {
        val pattern = Regex("""~~(.+?)~~""")
        val match = pattern.find("~~struck~~")
        assertNotNull(match)
        val content = match!!.groups[1]!!
        assertEquals("struck", content.value)
        // Content starts after ~~ (position 2) and ends before ~~ (position 7)
        assertEquals(2, content.range.first)
        assertEquals(7, content.range.last)
    }
}
