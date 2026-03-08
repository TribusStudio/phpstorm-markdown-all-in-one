package com.tribus.markdown.completion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests the reference link definition extraction logic used by
 * ReferenceLinkCompletionProvider.
 */
class ReferenceLinkCompletionTest {

    private val REF_DEFINITION = Regex("""^\s{0,3}\[([^\]]+)\]:\s+(.+)$""", RegexOption.MULTILINE)

    @Test
    fun `finds reference definitions`() {
        val text = """
            # Document

            Some text with [a link][ref1] and [another][ref2].

            [ref1]: https://example.com
            [ref2]: https://other.com "Title"
        """.trimIndent()

        val definitions = REF_DEFINITION.findAll(text).toList()
        assertEquals(2, definitions.size)
        assertEquals("ref1", definitions[0].groupValues[1])
        assertEquals("https://example.com", definitions[0].groupValues[2])
        assertEquals("ref2", definitions[1].groupValues[1])
    }

    @Test
    fun `handles indented definitions`() {
        val text = "   [indented]: https://example.com"
        val definitions = REF_DEFINITION.findAll(text).toList()
        assertEquals(1, definitions.size)
        assertEquals("indented", definitions[0].groupValues[1])
    }

    @Test
    fun `ignores definitions with 4+ spaces indent`() {
        val text = "    [code]: not a definition"
        val definitions = REF_DEFINITION.findAll(text).toList()
        assertEquals(0, definitions.size)
    }

    @Test
    fun `ref link pattern matches correctly`() {
        val pattern = Regex("""\[[^\]]*\]\[[^\]]*$""")
        assertTrue(pattern.containsMatchIn("[text]["))
        assertTrue(pattern.containsMatchIn("[text][ref"))
        assertTrue(pattern.containsMatchIn("[Link Text][some-r"))
        assertTrue(!pattern.containsMatchIn("[text](url)"))
        assertTrue(!pattern.containsMatchIn("just text"))
    }

    @Test
    fun `deduplicates case-insensitive labels`() {
        val text = """
            [REF]: https://example.com
            [ref]: https://example.com/other
        """.trimIndent()
        val definitions = REF_DEFINITION.findAll(text).toList()
        val seen = mutableSetOf<String>()
        val unique = definitions.filter { seen.add(it.groupValues[1].lowercase()) }
        assertEquals(1, unique.size)
    }
}
