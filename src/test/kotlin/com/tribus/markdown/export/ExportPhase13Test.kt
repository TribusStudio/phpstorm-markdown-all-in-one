package com.tribus.markdown.export

import com.tribus.markdown.toc.Slugify
import com.tribus.markdown.toc.TocGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExportPhase13Test {

    // ── HTML title from comment ──────────────────────────────────────

    @Test
    fun `extractTitle uses title comment when present`() {
        val md = "<!-- title: My Custom Title -->\n# Heading"
        assertEquals("My Custom Title", HtmlExporter.extractTitle(md, "fallback"))
    }

    @Test
    fun `extractTitle falls back to heading when no comment`() {
        val md = "# First Heading\nSome text"
        assertEquals("First Heading", HtmlExporter.extractTitle(md, "fallback"))
    }

    @Test
    fun `extractTitle uses fallback when no heading or comment`() {
        val md = "Just some text\nwith no headings"
        assertEquals("fallback", HtmlExporter.extractTitle(md, "fallback"))
    }

    @Test
    fun `extractTitle handles title comment with extra whitespace`() {
        val md = "<!--   title:   Spaced Title   -->\n# Heading"
        assertEquals("Spaced Title", HtmlExporter.extractTitle(md, "fallback"))
    }

    // ── .md → .html link conversion ──────────────────────────────────

    @Test
    fun `convertMdLinksToHtml rewrites md links`() {
        val html = """<a href="other.md">link</a>"""
        val result = HtmlExporter.convertMdLinksToHtml(html)
        assertTrue(result.contains("""href="other.html""""))
    }

    @Test
    fun `convertMdLinksToHtml preserves anchor fragments`() {
        val html = """<a href="other.md#section">link</a>"""
        val result = HtmlExporter.convertMdLinksToHtml(html)
        assertTrue(result.contains("""href="other.html#section""""))
    }

    @Test
    fun `convertMdLinksToHtml skips external URLs`() {
        val html = """<a href="https://example.com/file.md">link</a>"""
        val result = HtmlExporter.convertMdLinksToHtml(html)
        assertTrue(result.contains("https://example.com/file.md"))
    }

    @Test
    fun `convertMdLinksToHtml handles relative paths`() {
        val html = """<a href="../docs/readme.md">link</a>"""
        val result = HtmlExporter.convertMdLinksToHtml(html)
        assertTrue(result.contains("""href="../docs/readme.html""""))
    }

    @Test
    fun `convertMdLinksToHtml does not touch non-md links`() {
        val html = """<a href="style.css">link</a>"""
        val result = HtmlExporter.convertMdLinksToHtml(html)
        assertTrue(result.contains("""href="style.css""""))
    }

    // ── Zola slug mode ───────────────────────────────────────────────

    @Test
    fun `zola slug lowercases and hyphenates`() {
        assertEquals("hello-world", Slugify.slugify("Hello World", Slugify.Mode.ZOLA))
    }

    @Test
    fun `zola slug removes special characters`() {
        assertEquals("whats-new", Slugify.slugify("What's New?", Slugify.Mode.ZOLA))
    }

    @Test
    fun `zola slug collapses multiple hyphens`() {
        assertEquals("a-b", Slugify.slugify("a -- b", Slugify.Mode.ZOLA))
    }

    @Test
    fun `zola slug trims leading and trailing hyphens`() {
        assertEquals("hello", Slugify.slugify("--hello--", Slugify.Mode.ZOLA))
    }

    @Test
    fun `zola slug preserves underscores`() {
        assertEquals("my_var", Slugify.slugify("my_var", Slugify.Mode.ZOLA))
    }

    @Test
    fun `zola slug strips unicode`() {
        assertEquals("caf", Slugify.slugify("Café", Slugify.Mode.ZOLA))
    }

    @Test
    fun `parseSlugMode recognizes zola`() {
        assertEquals(Slugify.Mode.ZOLA, TocGenerator.parseSlugMode("zola"))
    }

    // ── Pure CSS export ──────────────────────────────────────────────

    @Test
    fun `defaultOptions reflects settings`() {
        // Just verify the data class has the new fields
        val options = HtmlExporter.ExportOptions(pureCss = true, convertMdLinks = false)
        assertTrue(options.pureCss)
        assertFalse(options.convertMdLinks)
    }
}
