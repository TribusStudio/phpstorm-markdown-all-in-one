package com.tribus.markdown.completion

import com.tribus.markdown.toc.HeadingExtractor
import com.tribus.markdown.toc.Slugify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests the heading extraction and slug generation logic used by
 * HeadingReferenceCompletionProvider. The actual CompletionProvider
 * integration is tested via BasePlatformTestCase in integration tests.
 */
class HeadingReferenceCompletionTest {

    @Test
    fun `extracts headings for completion`() {
        val text = """
            # Introduction
            ## Getting Started
            ### Installation
            ## Usage
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        assertEquals(4, headings.size)
        assertEquals("Introduction", headings[0].rawText)
        assertEquals("Getting Started", headings[1].rawText)
        assertEquals("Installation", headings[2].rawText)
        assertEquals("Usage", headings[3].rawText)
    }

    @Test
    fun `generates correct slugs for heading references`() {
        assertEquals("getting-started", Slugify.slugify("Getting Started"))
        assertEquals("api-v2-endpoints", Slugify.slugify("API v2 Endpoints"))
        assertEquals("whats-new-in-20", Slugify.slugify("What's New in 2.0"))
    }

    @Test
    fun `handles duplicate headings with unique slugs`() {
        val occurrences = mutableMapOf<String, Int>()
        val slug1 = Slugify.makeUnique(Slugify.slugify("Section"), occurrences)
        val slug2 = Slugify.makeUnique(Slugify.slugify("Section"), occurrences)
        val slug3 = Slugify.makeUnique(Slugify.slugify("Section"), occurrences)
        assertEquals("section", slug1)
        assertEquals("section-1", slug2)
        assertEquals("section-2", slug3)
    }

    @Test
    fun `strips inline markdown from heading text`() {
        assertEquals("bold heading", Slugify.stripInlineMarkdown("**bold** heading"))
        assertEquals("code heading", Slugify.stripInlineMarkdown("`code` heading"))
        assertEquals("link text", Slugify.stripInlineMarkdown("[link text](url)"))
    }

    @Test
    fun `heading ref pattern matches correctly`() {
        val pattern = Regex("""\[[^\]]*\]\(#[^)]*$""")
        assertTrue(pattern.containsMatchIn("[text](#"))
        assertTrue(pattern.containsMatchIn("[text](#getting"))
        assertTrue(pattern.containsMatchIn("[Link Text](#some-heading"))
        assertTrue(!pattern.containsMatchIn("[text](http://"))
        assertTrue(!pattern.containsMatchIn("just text"))
    }
}
