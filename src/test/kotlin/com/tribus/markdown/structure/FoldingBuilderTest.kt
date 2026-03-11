package com.tribus.markdown.structure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for the folding region detection logic.
 * These test the regex patterns and line-based logic without needing a full editor.
 */
class FoldingBuilderTest {

    private val builder = MarkdownFoldingBuilder()

    // Helper to test fence pattern matching
    private val FENCE_PATTERN = Regex("^\\s{0,3}(`{3,}|~{3,})")
    private val FRONT_MATTER = Regex("^---\\s*$")
    private val BLOCKQUOTE = Regex("^\\s{0,3}>")
    private val ATX_HEADING = Regex("^\\s{0,3}(#{1,6})(\\s.*|$)")

    @Test
    fun `fence pattern matches backtick fences`() {
        assertTrue(FENCE_PATTERN.containsMatchIn("```"))
        assertTrue(FENCE_PATTERN.containsMatchIn("```kotlin"))
        assertTrue(FENCE_PATTERN.containsMatchIn("````"))
        assertTrue(FENCE_PATTERN.containsMatchIn("  ```"))
    }

    @Test
    fun `fence pattern matches tilde fences`() {
        assertTrue(FENCE_PATTERN.containsMatchIn("~~~"))
        assertTrue(FENCE_PATTERN.containsMatchIn("~~~~"))
        assertTrue(FENCE_PATTERN.containsMatchIn("  ~~~"))
    }

    @Test
    fun `fence pattern rejects too much indentation`() {
        // 4+ spaces = code block, not a fence
        assertTrue(!FENCE_PATTERN.containsMatchIn("    ```"))
    }

    @Test
    fun `front matter pattern matches`() {
        assertTrue(FRONT_MATTER.matches("---"))
        assertTrue(FRONT_MATTER.matches("---  "))
        assertTrue(!FRONT_MATTER.matches("----"))
        assertTrue(!FRONT_MATTER.matches("--- text"))
    }

    @Test
    fun `blockquote pattern matches`() {
        assertTrue(BLOCKQUOTE.containsMatchIn("> text"))
        assertTrue(BLOCKQUOTE.containsMatchIn("  > text"))
        assertTrue(BLOCKQUOTE.containsMatchIn(">"))
        assertTrue(!BLOCKQUOTE.containsMatchIn("    > too indented"))
    }

    @Test
    fun `heading pattern matches ATX headings`() {
        assertTrue(ATX_HEADING.matches("# H1"))
        assertTrue(ATX_HEADING.matches("## H2"))
        assertTrue(ATX_HEADING.matches("###### H6"))
        assertTrue(ATX_HEADING.matches("  ## indented"))
        assertTrue(!ATX_HEADING.matches("####### seven"))
    }

    @Test
    fun `breadcrumb path for offset in first section`() {
        val text = """
            # Top
            ## Sub
            Content here
            ## Another Sub
        """.trimIndent()
        // Offset in "Content here" line (after "# Top\n## Sub\n")
        val offset = "# Top\n## Sub\n".length + 2
        val path = MarkdownBreadcrumbsProvider.getBreadcrumbPath(text, offset)

        assertEquals(2, path.size)
        assertEquals("Top", path[0].rawText)
        assertEquals("Sub", path[1].rawText)
    }

    @Test
    fun `breadcrumb path for offset before any heading`() {
        val text = """
            Some preamble text
            # First Heading
        """.trimIndent()
        val path = MarkdownBreadcrumbsProvider.getBreadcrumbPath(text, 5)
        assertTrue(path.isEmpty())
    }

    @Test
    fun `breadcrumb path updates when entering sibling section`() {
        val text = """
            # H1
            ## Sub A
            ## Sub B
            Content in Sub B
        """.trimIndent()
        val offset = text.indexOf("Content in Sub B")
        val path = MarkdownBreadcrumbsProvider.getBreadcrumbPath(text, offset)

        assertEquals(2, path.size)
        assertEquals("H1", path[0].rawText)
        assertEquals("Sub B", path[1].rawText)
    }

    @Test
    fun `breadcrumb path handles deeply nested headings`() {
        val text = """
            # L1
            ## L2
            ### L3
            #### L4
            Content
        """.trimIndent()
        val offset = text.indexOf("Content")
        val path = MarkdownBreadcrumbsProvider.getBreadcrumbPath(text, offset)

        assertEquals(4, path.size)
        assertEquals(1, path[0].level)
        assertEquals(2, path[1].level)
        assertEquals(3, path[2].level)
        assertEquals(4, path[3].level)
    }

    @Test
    fun `breadcrumb path resets at same-level heading`() {
        val text = """
            # Section 1
            ## Details
            # Section 2
            Content
        """.trimIndent()
        val offset = text.indexOf("Content")
        val path = MarkdownBreadcrumbsProvider.getBreadcrumbPath(text, offset)

        assertEquals(1, path.size)
        assertEquals("Section 2", path[0].rawText)
    }
}
