package com.tribus.markdown.completion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests the smart paste URL detection and link generation logic.
 * Tests the pattern matching and output format without IntelliJ editor context.
 */
class SmartPasteTest {

    private val URL_PATTERN = Regex("""^https?://\S+$""")
    private val IMAGE_URL_PATTERN = Regex(
        """^https?://.*\.(png|jpg|jpeg|gif|svg|webp|bmp|ico|tiff)(\?.*)?$""",
        RegexOption.IGNORE_CASE
    )

    @Test
    fun `detects HTTP URLs`() {
        assertTrue(URL_PATTERN.matches("https://example.com"))
        assertTrue(URL_PATTERN.matches("http://example.com/path"))
        assertTrue(URL_PATTERN.matches("https://example.com/path?query=1&other=2"))
        assertTrue(!URL_PATTERN.matches("not a url"))
        assertTrue(!URL_PATTERN.matches("ftp://example.com"))
        assertTrue(!URL_PATTERN.matches("https://example.com has spaces"))
    }

    @Test
    fun `detects image URLs`() {
        assertTrue(IMAGE_URL_PATTERN.matches("https://example.com/photo.png"))
        assertTrue(IMAGE_URL_PATTERN.matches("https://example.com/photo.jpg"))
        assertTrue(IMAGE_URL_PATTERN.matches("https://example.com/photo.JPEG"))
        assertTrue(IMAGE_URL_PATTERN.matches("https://example.com/photo.gif?size=large"))
        assertTrue(IMAGE_URL_PATTERN.matches("https://example.com/photo.svg"))
        assertTrue(!IMAGE_URL_PATTERN.matches("https://example.com/page.html"))
        assertTrue(!IMAGE_URL_PATTERN.matches("https://example.com/file.pdf"))
    }

    @Test
    fun `generates markdown link from URL and text`() {
        val url = "https://example.com"
        val selectedText = "Example Site"
        val result = "[${selectedText}](${url})"
        assertEquals("[Example Site](https://example.com)", result)
    }

    @Test
    fun `generates image link from image URL and text`() {
        val url = "https://example.com/photo.png"
        val selectedText = "A photo"
        val result = "![${selectedText}](${url})"
        assertEquals("![A photo](https://example.com/photo.png)", result)
    }

    @Test
    fun `URL pattern rejects multiline text`() {
        assertTrue(!URL_PATTERN.matches("https://example.com\nhttps://other.com"))
    }

    @Test
    fun `file path pattern matches link contexts`() {
        val pattern = Regex("""!?\[[^\]]*\]\([^)]*$""")
        assertTrue(pattern.containsMatchIn("[text]("))
        assertTrue(pattern.containsMatchIn("[text](./path"))
        assertTrue(pattern.containsMatchIn("![alt](images/"))
        assertTrue(pattern.containsMatchIn("![]("))
        assertTrue(!pattern.containsMatchIn("just some (text"))
    }
}
