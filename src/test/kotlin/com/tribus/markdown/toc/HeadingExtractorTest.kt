package com.tribus.markdown.toc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HeadingExtractorTest {

    @Test
    fun `extracts simple ATX headings`() {
        val text = """
            # Heading 1
            ## Heading 2
            ### Heading 3
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(3, headings.size)
        assertEquals(1, headings[0].level)
        assertEquals("Heading 1", headings[0].rawText)
        assertEquals(2, headings[1].level)
        assertEquals("Heading 2", headings[1].rawText)
        assertEquals(3, headings[2].level)
        assertEquals("Heading 3", headings[2].rawText)
    }

    @Test
    fun `extracts up to H6`() {
        val text = """
            ###### Heading 6
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(1, headings.size)
        assertEquals(6, headings[0].level)
    }

    @Test
    fun `ignores heading with 7 hashes`() {
        val text = "####### Not a heading"
        val headings = HeadingExtractor.extract(text)
        assertEquals(0, headings.size)
    }

    @Test
    fun `removes trailing hashes`() {
        val text = "## Hello World ##"
        val headings = HeadingExtractor.extract(text)
        assertEquals(1, headings.size)
        assertEquals("Hello World", headings[0].rawText)
    }

    @Test
    fun `skips headings inside fenced code blocks (backticks)`() {
        val text = """
            # Real Heading
            ```
            # Not a heading
            ## Also not
            ```
            ## Another Real Heading
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(2, headings.size)
        assertEquals("Real Heading", headings[0].rawText)
        assertEquals("Another Real Heading", headings[1].rawText)
    }

    @Test
    fun `skips headings inside fenced code blocks (tildes)`() {
        val text = """
            # Before
            ~~~
            # Inside code
            ~~~
            # After
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(2, headings.size)
        assertEquals("Before", headings[0].rawText)
        assertEquals("After", headings[1].rawText)
    }

    @Test
    fun `code fence must close with same or more chars`() {
        val text = """
            ````
            # Inside
            ```
            # Still inside
            ````
            # Outside
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(1, headings.size)
        assertEquals("Outside", headings[0].rawText)
    }

    @Test
    fun `skips YAML front matter`() {
        val text = """
            ---
            title: Test
            ---
            # Real Heading
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(1, headings.size)
        assertEquals("Real Heading", headings[0].rawText)
    }

    @Test
    fun `front matter only at start of document`() {
        // --- after content text is a setext H2 underline per CommonMark
        // so "not front matter" becomes a level-2 heading
        val text = """
            # Heading
            ---
            not front matter
            ---
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        // "# Heading" is ATX H1, "not front matter" + "---" is setext H2
        // "# Heading" + "---" is NOT setext because # Heading was consumed as ATX
        assertEquals(2, headings.size)
        assertEquals("Heading", headings[0].rawText)
        assertEquals("not front matter", headings[1].rawText)
    }

    @Test
    fun `skips HTML comments`() {
        val text = """
            # Before
            <!-- This is a comment -->
            # After
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(2, headings.size)
    }

    @Test
    fun `skips multi-line HTML comments`() {
        val text = """
            # Before
            <!--
            # Not a heading
            -->
            # After
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(2, headings.size)
    }

    @Test
    fun `omit from toc - inline marker`() {
        val text = "# Hidden <!-- omit from toc -->"
        val headings = HeadingExtractor.extract(text)
        assertEquals(1, headings.size)
        assertFalse(headings[0].canInToc)
        assertEquals("Hidden", headings[0].rawText)
    }

    @Test
    fun `omit in toc - inline marker`() {
        val text = "# Hidden <!-- omit in toc -->"
        val headings = HeadingExtractor.extract(text)
        assertEquals(1, headings.size)
        assertFalse(headings[0].canInToc)
    }

    @Test
    fun `omit from toc - previous line marker`() {
        val text = """
            <!-- omit from toc -->
            # Hidden Heading
            ## Visible Heading
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(2, headings.size)
        assertFalse(headings[0].canInToc)
        assertTrue(headings[1].canInToc)
    }

    @Test
    fun `extracts setext headings`() {
        val text = """
            Heading One
            ===========

            Heading Two
            -----------
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(2, headings.size)
        assertEquals(1, headings[0].level)
        assertEquals("Heading One", headings[0].rawText)
        assertEquals(2, headings[1].level)
        assertEquals("Heading Two", headings[1].rawText)
    }

    @Test
    fun `setext - does not treat list items as headings`() {
        val text = """
            - list item
            -----------
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(0, headings.size)
    }

    @Test
    fun `setext - does not treat blockquotes as headings`() {
        val text = """
            > quoted text
            =============
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(0, headings.size)
    }

    @Test
    fun `mixed ATX and setext headings`() {
        val text = """
            # ATX Heading

            Setext Heading
            ==============

            ## Another ATX
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(3, headings.size)
        assertEquals("ATX Heading", headings[0].rawText)
        assertEquals("Setext Heading", headings[1].rawText)
        assertEquals("Another ATX", headings[2].rawText)
    }

    @Test
    fun `empty document returns no headings`() {
        assertEquals(0, HeadingExtractor.extract("").size)
    }

    @Test
    fun `heading with inline formatting`() {
        val text = "# **Bold** and *italic* heading"
        val headings = HeadingExtractor.extract(text)
        assertEquals(1, headings.size)
        assertEquals("**Bold** and *italic* heading", headings[0].rawText)
    }
}
