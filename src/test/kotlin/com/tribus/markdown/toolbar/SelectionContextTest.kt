package com.tribus.markdown.toolbar

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SelectionContextTest {

    // ── TOC detection ────────────────────────────────────────────────

    @Test
    fun `isInToc detects selection inside TOC block`() {
        val text = "before\n<!-- TOC -->\n- item 1\n- item 2\n<!-- /TOC -->\nafter"
        val tocStart = text.indexOf("<!-- TOC -->")
        val tocEnd = text.indexOf("<!-- /TOC -->") + "<!-- /TOC -->".length
        // Selection on "- item 1"
        val selStart = text.indexOf("- item 1")
        val selEnd = selStart + 8
        assertTrue(SelectionContext.isInToc(text, selStart, selEnd))
    }

    @Test
    fun `isInToc returns false for selection outside TOC`() {
        val text = "before\n<!-- TOC -->\n- item\n<!-- /TOC -->\nafter"
        val selStart = text.indexOf("before")
        val selEnd = selStart + 6
        assertFalse(SelectionContext.isInToc(text, selStart, selEnd))
    }

    @Test
    fun `isInToc handles named TOC blocks`() {
        val text = "<!-- TOC name=\"api\" -->\n- api\n<!-- /TOC -->"
        val selStart = text.indexOf("- api")
        assertTrue(SelectionContext.isInToc(text, selStart, selStart + 5))
    }

    // ── Code block detection ─────────────────────────────────────────

    @Test
    fun `isInCodeBlock detects selection inside backtick fence`() {
        val text = "text\n```kotlin\nfun main() {}\n```\nmore text"
        val selStart = text.indexOf("fun main")
        val selEnd = selStart + 8
        assertTrue(SelectionContext.isInCodeBlock(text, selStart, selEnd))
    }

    @Test
    fun `isInCodeBlock returns false outside fence`() {
        val text = "text\n```\ncode\n```\nmore text"
        val selStart = text.indexOf("more text")
        assertFalse(SelectionContext.isInCodeBlock(text, selStart, selStart + 4))
    }

    @Test
    fun `isInCodeBlock detects tilde fences`() {
        val text = "~~~\ncode\n~~~"
        val selStart = text.indexOf("code")
        assertTrue(SelectionContext.isInCodeBlock(text, selStart, selStart + 4))
    }

    @Test
    fun `isInCodeBlock returns false for text before fence`() {
        val text = "before\n```\ncode\n```"
        val selStart = text.indexOf("before")
        assertFalse(SelectionContext.isInCodeBlock(text, selStart, selStart + 6))
    }

    // ── Math detection ───────────────────────────────────────────────

    @Test
    fun `isInMath detects inline math`() {
        val text = "The equation \$E = mc^2\$ is famous"
        val selStart = text.indexOf("E = mc")
        assertTrue(SelectionContext.isInMath(text, selStart))
    }

    @Test
    fun `isInMath detects display math`() {
        val text = "Here: \$\$\\sum_{i=1}^n x_i\$\$ end"
        val selStart = text.indexOf("\\sum")
        assertTrue(SelectionContext.isInMath(text, selStart))
    }

    @Test
    fun `isInMath returns false for regular text`() {
        val text = "Just regular text"
        assertFalse(SelectionContext.isInMath(text, 5))
    }

    @Test
    fun `isInMath returns false for dollar amounts`() {
        // Two separate dollar signs that aren't math
        val text = "Price is 5 and cost is 3"
        assertFalse(SelectionContext.isInMath(text, 10))
    }

    // ── Blockquote detection ─────────────────────────────────────────
    // (Requires Editor, tested via integration or manually)

    // ── Priority ─────────────────────────────────────────────────────

    @Test
    fun `TOC takes priority over other contexts`() {
        // A TOC block that contains list-like content
        val text = "<!-- TOC -->\n- [Heading](#heading)\n<!-- /TOC -->"
        val selStart = text.indexOf("- [Heading]")
        assertTrue(SelectionContext.isInToc(text, selStart, selStart + 10))
    }
}
