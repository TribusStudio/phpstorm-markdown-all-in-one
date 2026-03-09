package com.tribus.markdown.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class HtmlExporterTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `exports single markdown file to HTML`() {
        val mdFile = tempDir.resolve("test.md").toFile()
        mdFile.writeText("# Hello World\n\nSome content here.")
        val outputFile = tempDir.resolve("test.html").toFile()

        val result = HtmlExporter.exportFile(mdFile, outputFile, testOptions())

        assertTrue(outputFile.exists())
        val html = outputFile.readText()
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<h1"))
        assertTrue(html.contains("Hello World"))
        assertTrue(html.contains("Some content here."))
        assertTrue(html.contains("<title>Hello World</title>"))
    }

    @Test
    fun `exported HTML includes theme CSS`() {
        val mdFile = tempDir.resolve("test.md").toFile()
        mdFile.writeText("# Test")
        val outputFile = tempDir.resolve("test.html").toFile()

        HtmlExporter.exportFile(mdFile, outputFile, testOptions())

        val html = outputFile.readText()
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains("markdown-body"))
    }

    @Test
    fun `exported HTML includes meta generator tag`() {
        val mdFile = tempDir.resolve("test.md").toFile()
        mdFile.writeText("# Test")
        val outputFile = tempDir.resolve("test.html").toFile()

        HtmlExporter.exportFile(mdFile, outputFile, testOptions())

        val html = outputFile.readText()
        assertTrue(html.contains("Markdown All-in-One"))
    }

    @Test
    fun `batch exports directory of markdown files`() {
        val inputDir = tempDir.resolve("input").toFile()
        inputDir.mkdirs()
        File(inputDir, "one.md").writeText("# File One")
        File(inputDir, "two.md").writeText("# File Two")
        File(inputDir, "skip.txt").writeText("Not markdown")

        val outputDir = tempDir.resolve("output").toFile()
        val results = HtmlExporter.exportDirectory(inputDir, outputDir, testOptions())

        assertEquals(2, results.size)
        assertTrue(File(outputDir, "one.html").exists())
        assertTrue(File(outputDir, "two.html").exists())
        assertFalse(File(outputDir, "skip.html").exists())
    }

    @Test
    fun `batch export handles recursive directories`() {
        val inputDir = tempDir.resolve("input").toFile()
        val subDir = File(inputDir, "sub")
        subDir.mkdirs()
        File(inputDir, "root.md").writeText("# Root")
        File(subDir, "nested.md").writeText("# Nested")

        val outputDir = tempDir.resolve("output").toFile()
        val results = HtmlExporter.exportDirectory(inputDir, outputDir, testOptions(), recursive = true)

        assertEquals(2, results.size)
        assertTrue(File(outputDir, "root.html").exists())
        assertTrue(File(outputDir, "sub/nested.html").exists())
    }

    @Test
    fun `resolves relative image paths to absolute`() {
        val baseDir = tempDir.toFile()
        val imgFile = File(baseDir, "images/photo.png")
        imgFile.parentFile.mkdirs()
        imgFile.writeText("fake image data")

        val html = """<img src="images/photo.png" alt="Photo">"""
        val warnings = mutableListOf<String>()
        val result = HtmlExporter.resolveImagePaths(html, baseDir, embedBase64 = false, warnings)

        assertTrue(result.contains("file://"))
        assertTrue(result.contains("photo.png"))
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `embeds images as base64 when requested`() {
        val baseDir = tempDir.toFile()
        val imgFile = File(baseDir, "photo.png")
        imgFile.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) // PNG magic bytes

        val html = """<img src="photo.png" alt="Photo">"""
        val warnings = mutableListOf<String>()
        val result = HtmlExporter.resolveImagePaths(html, baseDir, embedBase64 = true, warnings)

        assertTrue(result.contains("data:image/png;base64,"))
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `warns about missing images`() {
        val baseDir = tempDir.toFile()
        val html = """<img src="missing.png" alt="Missing">"""
        val warnings = mutableListOf<String>()
        HtmlExporter.resolveImagePaths(html, baseDir, embedBase64 = false, warnings)

        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("missing.png"))
    }

    @Test
    fun `skips external image URLs`() {
        val baseDir = tempDir.toFile()
        val html = """<img src="https://example.com/img.png" alt="External">"""
        val warnings = mutableListOf<String>()
        val result = HtmlExporter.resolveImagePaths(html, baseDir, embedBase64 = false, warnings)

        assertTrue(result.contains("https://example.com/img.png"))
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `validates anchor links`() {
        val html = """<h1 id="hello">Hello</h1><a href="#hello">ok</a><a href="#missing">broken</a>"""
        val warnings = HtmlExporter.validateLinks(html, tempDir.toFile(), "")

        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("#missing"))
    }

    @Test
    fun `validates relative file links`() {
        val baseDir = tempDir.toFile()
        File(baseDir, "exists.md").writeText("# exists")

        val html = """<a href="exists.md">ok</a><a href="gone.md">broken</a>"""
        val warnings = HtmlExporter.validateLinks(html, baseDir, "")

        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("gone.md"))
    }

    @Test
    fun `skips external links in validation`() {
        val html = """<a href="https://example.com">ok</a><a href="mailto:a@b.com">mail</a>"""
        val warnings = HtmlExporter.validateLinks(html, tempDir.toFile(), "")

        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `validates reference-style links`() {
        val markdown = "[text][label]\n\n[other]: http://example.com"
        val html = ""
        val warnings = HtmlExporter.validateLinks(html, tempDir.toFile(), markdown)

        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("[label]"))
    }

    @Test
    fun `extracts title from first ATX heading`() {
        val title = HtmlExporter.extractTitle("# My Document\n\nContent", "fallback")
        assertEquals("My Document", title)
    }

    @Test
    fun `extracts title from setext heading`() {
        val title = HtmlExporter.extractTitle("My Document\n=====\n\nContent", "fallback")
        assertEquals("My Document", title)
    }

    @Test
    fun `falls back to provided name when no heading`() {
        val title = HtmlExporter.extractTitle("Just some text without headings", "README")
        assertEquals("README", title)
    }

    @Test
    fun `export creates parent directories`() {
        val mdFile = tempDir.resolve("test.md").toFile()
        mdFile.writeText("# Test")
        val outputFile = tempDir.resolve("deep/nested/dir/test.html").toFile()

        HtmlExporter.exportFile(mdFile, outputFile, testOptions())

        assertTrue(outputFile.exists())
    }

    @Test
    fun `exported HTML has proper lang and charset`() {
        val mdFile = tempDir.resolve("test.md").toFile()
        mdFile.writeText("# Test")
        val outputFile = tempDir.resolve("test.html").toFile()

        HtmlExporter.exportFile(mdFile, outputFile, testOptions())

        val html = outputFile.readText()
        assertTrue(html.contains("lang=\"en\""))
        assertTrue(html.contains("charset=\"UTF-8\""))
        assertTrue(html.contains("viewport"))
    }

    @Test
    fun `exports with custom CSS`() {
        val customCss = tempDir.resolve("custom.css").toFile()
        customCss.writeText("body { color: red; }")

        val mdFile = tempDir.resolve("test.md").toFile()
        mdFile.writeText("# Test")
        val outputFile = tempDir.resolve("test.html").toFile()

        val options = testOptions().copy(customCssPath = customCss.absolutePath)
        HtmlExporter.exportFile(mdFile, outputFile, options)

        val html = outputFile.readText()
        assertTrue(html.contains("color: red"))
    }

    @Test
    fun `handles empty markdown file`() {
        val mdFile = tempDir.resolve("empty.md").toFile()
        mdFile.writeText("")
        val outputFile = tempDir.resolve("empty.html").toFile()

        val result = HtmlExporter.exportFile(mdFile, outputFile, testOptions())

        assertTrue(outputFile.exists())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `batch export returns empty list for empty directory`() {
        val inputDir = tempDir.resolve("empty").toFile()
        inputDir.mkdirs()
        val outputDir = tempDir.resolve("output").toFile()

        val results = HtmlExporter.exportDirectory(inputDir, outputDir, testOptions())

        assertTrue(results.isEmpty())
    }

    @Test
    fun `strips trailing hashes from title`() {
        val title = HtmlExporter.extractTitle("# My Title ##", "fallback")
        assertEquals("My Title", title)
    }

    private fun testOptions(): HtmlExporter.ExportOptions {
        return HtmlExporter.ExportOptions(
            theme = com.tribus.markdown.preview.PreviewTheme.Theme.GITHUB,
            customCssPath = "",
            resolveImages = true,
            embedImages = false,
            validateLinks = true
        )
    }
}
