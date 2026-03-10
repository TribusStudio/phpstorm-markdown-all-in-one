package com.tribus.markdown.preview

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownHtmlConverterTest {

    @Test
    fun `converts ATX headings`() {
        val html = MarkdownHtmlConverter.convert("# Hello\n## World\n### Sub")
        assertTrue(html.contains("<h1"))
        assertTrue(html.contains("Hello"))
        assertTrue(html.contains("<h2"))
        assertTrue(html.contains("World"))
        assertTrue(html.contains("<h3"))
    }

    @Test
    fun `headings have id attributes for anchors`() {
        val html = MarkdownHtmlConverter.convert("# Getting Started")
        assertTrue(html.contains("id=\"getting-started\""))
    }

    @Test
    fun `converts bold and italic`() {
        val html = MarkdownHtmlConverter.convertInline("**bold** and *italic*")
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
    }

    @Test
    fun `converts strikethrough`() {
        val html = MarkdownHtmlConverter.convertInline("~~deleted~~")
        assertTrue(html.contains("<del>deleted</del>"))
    }

    @Test
    fun `converts inline code`() {
        val html = MarkdownHtmlConverter.convertInline("`code here`")
        assertTrue(html.contains("<code>code here</code>"))
    }

    @Test
    fun `converts double backtick code with literal backtick`() {
        val html = MarkdownHtmlConverter.convertInline("`` ` ``")
        assertTrue(html.contains("<code>`</code>"))
    }

    @Test
    fun `converts triple backtick inline code with double backticks inside`() {
        val html = MarkdownHtmlConverter.convertInline("``` `` ```")
        assertTrue(html.contains("<code>``</code>"))
    }

    @Test
    fun `converts links`() {
        val html = MarkdownHtmlConverter.convertInline("[text](https://example.com)")
        assertTrue(html.contains("<a href=\"https://example.com\">text</a>"))
    }

    @Test
    fun `converts images`() {
        val html = MarkdownHtmlConverter.convertInline("![alt](image.png)")
        assertTrue(html.contains("<img src=\"image.png\" alt=\"alt\">"))
    }

    @Test
    fun `converts fenced code blocks`() {
        val md = "```kotlin\nfun main() {}\n```"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(html.contains("<pre><code class=\"language-kotlin\">"))
        assertTrue(html.contains("fun main()"))
    }

    @Test
    fun `converts unordered lists`() {
        val md = "- item 1\n- item 2"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>item 1</li>"))
        assertTrue(html.contains("<li>item 2</li>"))
    }

    @Test
    fun `converts ordered lists`() {
        val md = "1. first\n2. second"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>first</li>"))
    }

    @Test
    fun `converts task lists`() {
        val md = "- [ ] todo\n- [x] done"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(html.contains("type=\"checkbox\""))
        assertTrue(html.contains("checked"))
    }

    @Test
    fun `converts blockquotes`() {
        val md = "> quoted text"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("quoted text"))
    }

    @Test
    fun `converts horizontal rules`() {
        val html = MarkdownHtmlConverter.convert("---")
        assertTrue(html.contains("<hr>"))
    }

    @Test
    fun `converts tables`() {
        val md = "| A | B |\n| --- | --- |\n| 1 | 2 |"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>"))
        assertTrue(html.contains("<td>"))
    }

    @Test
    fun `preserves table alignment`() {
        val md = "| Left | Right |\n| :--- | ---: |\n| a | b |"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(html.contains("text-align:left"))
        assertTrue(html.contains("text-align:right"))
    }

    @Test
    fun `skips front matter`() {
        val md = "---\ntitle: Test\n---\n# Hello"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(!html.contains("title: Test"))
        assertTrue(html.contains("Hello"))
    }

    @Test
    fun `escapes HTML in text`() {
        assertEquals("&lt;script&gt;", MarkdownHtmlConverter.escapeHtml("<script>"))
        assertEquals("&amp;amp;", MarkdownHtmlConverter.escapeHtml("&amp;"))
    }

    @Test
    fun `wrapInDocument produces full HTML`() {
        val doc = MarkdownHtmlConverter.wrapInDocument("<p>test</p>", "body{}", "p{color:red}")
        assertTrue(doc.contains("<!DOCTYPE html>"))
        assertTrue(doc.contains("body{}"))
        assertTrue(doc.contains("p{color:red}"))
        assertTrue(doc.contains("<p>test</p>"))
        assertTrue(doc.contains("class=\"markdown-body\""))
    }

    @Test
    fun `wrapInDocument omits custom CSS when empty`() {
        val doc = MarkdownHtmlConverter.wrapInDocument("<p>test</p>", "body{}", "")
        assertTrue(!doc.contains("color:red"))
    }

    @Test
    fun `converts paragraphs`() {
        val html = MarkdownHtmlConverter.convert("Hello world")
        assertTrue(html.contains("<p>Hello world</p>"))
    }

    @Test
    fun `converts setext headings`() {
        val md = "Title\n=====\n\nSubtitle\n--------"
        val html = MarkdownHtmlConverter.convert(md)
        assertTrue(html.contains("<h1"))
        assertTrue(html.contains("Title"))
        assertTrue(html.contains("<h2"))
        assertTrue(html.contains("Subtitle"))
    }

    // ── Source-line annotation tests ─────────────────────────────────

    @Test
    fun `annotateSourceLines adds data-source-line to headings`() {
        val html = MarkdownHtmlConverter.convert("# Title\n\nText\n\n## Sub", annotateSourceLines = true)
        assertTrue(html.contains("data-source-line=\"0\""))  // # Title on line 0
        assertTrue(html.contains("data-source-line=\"4\""))  // ## Sub on line 4
    }

    @Test
    fun `annotateSourceLines adds data-source-line to paragraphs`() {
        val html = MarkdownHtmlConverter.convert("Hello\n\nWorld", annotateSourceLines = true)
        assertTrue(html.contains("<p data-source-line=\"0\">Hello</p>"))
        assertTrue(html.contains("<p data-source-line=\"2\">World</p>"))
    }

    @Test
    fun `annotateSourceLines adds data-source-line to code blocks`() {
        val md = "text\n\n```kotlin\nval x = 1\n```"
        val html = MarkdownHtmlConverter.convert(md, annotateSourceLines = true)
        assertTrue(html.contains("<pre data-source-line=\"2\">"))
    }

    @Test
    fun `annotateSourceLines adds data-source-line to lists`() {
        val md = "- item 1\n- item 2"
        val html = MarkdownHtmlConverter.convert(md, annotateSourceLines = true)
        assertTrue(html.contains("<ul data-source-line=\"0\">"))
        assertTrue(html.contains("<li data-source-line=\"0\">"))
        assertTrue(html.contains("<li data-source-line=\"1\">"))
    }

    @Test
    fun `annotateSourceLines adds data-source-line to tables`() {
        val md = "| A | B |\n| --- | --- |\n| 1 | 2 |"
        val html = MarkdownHtmlConverter.convert(md, annotateSourceLines = true)
        assertTrue(html.contains("<table data-source-line=\"0\">"))
    }

    @Test
    fun `annotateSourceLines adds data-source-line to blockquotes`() {
        val html = MarkdownHtmlConverter.convert("> quote", annotateSourceLines = true)
        assertTrue(html.contains("<blockquote data-source-line=\"0\">"))
    }

    @Test
    fun `annotateSourceLines adds data-source-line to hr`() {
        val html = MarkdownHtmlConverter.convert("---", annotateSourceLines = true)
        assertTrue(html.contains("<hr data-source-line=\"0\">"))
    }

    @Test
    fun `default convert does not include data-source-line`() {
        val html = MarkdownHtmlConverter.convert("# Title\n\nHello\n\n- item")
        assertFalse(html.contains("data-source-line"))
    }

    @Test
    fun `wrapInDocument includes extra JS when provided`() {
        val doc = MarkdownHtmlConverter.wrapInDocument("<p>test</p>", "body{}", extraJs = "console.log('hi');")
        assertTrue(doc.contains("console.log('hi');"))
        assertTrue(doc.contains("<script>"))
    }

    @Test
    fun `wrapInDocument omits extra JS when empty`() {
        val doc = MarkdownHtmlConverter.wrapInDocument("<p>test</p>", "body{}")
        assertFalse(doc.contains("console.log"))
    }

    // ── Backslash escape tests ──────────────────────────────────────

    @Test
    fun `links with escaped brackets in text render correctly`() {
        val html = MarkdownHtmlConverter.convertInline("[Heading (`Ctrl+Shift+\\]` / `\\[`)](#slug)")
        assertTrue(html.contains("<a href=\"#slug\">"))
        assertTrue(html.contains("Heading"))
    }

    @Test
    fun `backslash escapes render literal characters`() {
        val html = MarkdownHtmlConverter.convertInline("\\[ \\] \\* \\_ \\` \\# \\!")
        assertTrue(html.contains("[ ] * _ ` # !"))
        assertFalse(html.contains("\\["))
    }

    @Test
    fun `escaped brackets inside code spans in links`() {
        val html = MarkdownHtmlConverter.convertInline("[task lists (`- \\[ \\]`, `- \\[x\\]`)](#slug)")
        assertTrue(html.contains("<a href=\"#slug\">"))
    }

    // ── Loose list tests (blank lines between items) ─────────────────

    @Test
    fun `ordered list with blank lines between items stays one list`() {
        val md = "1. first\n\n2. second\n\n3. third"
        val html = MarkdownHtmlConverter.convert(md)
        // Should be a single <ol> with 3 items, not three separate <ol>s
        assertEquals(1, Regex("<ol>").findAll(html).count(), "Expected exactly one <ol> tag")
        assertTrue(html.contains("<li>first</li>"))
        assertTrue(html.contains("<li>second</li>"))
        assertTrue(html.contains("<li>third</li>"))
    }

    @Test
    fun `unordered list with blank lines between items stays one list`() {
        val md = "- alpha\n\n- beta\n\n- gamma"
        val html = MarkdownHtmlConverter.convert(md)
        assertEquals(1, Regex("<ul>").findAll(html).count(), "Expected exactly one <ul> tag")
        assertTrue(html.contains("<li>alpha</li>"))
        assertTrue(html.contains("<li>beta</li>"))
        assertTrue(html.contains("<li>gamma</li>"))
    }

    @Test
    fun `ordered list with bold items and blank lines stays one list`() {
        val md = "1. **Modular JAR packaging:** Details here\n\n2. **Extension point-driven preview:** More details\n\n3. **JCEF-based preview:** Even more"
        val html = MarkdownHtmlConverter.convert(md)
        assertEquals(1, Regex("<ol>").findAll(html).count(), "Expected exactly one <ol> tag")
        assertTrue(html.contains("<strong>Modular JAR packaging:</strong>"))
        assertTrue(html.contains("<strong>Extension point-driven preview:</strong>"))
        assertTrue(html.contains("<strong>JCEF-based preview:</strong>"))
    }

    @Test
    fun `list ends when non-list content follows blank line`() {
        val md = "1. first\n\n2. second\n\nSome paragraph"
        val html = MarkdownHtmlConverter.convert(md)
        assertEquals(1, Regex("<ol>").findAll(html).count())
        assertTrue(html.contains("</ol>"))
        assertTrue(html.contains("<p>Some paragraph</p>"))
    }

    @Test
    fun `multiple blank lines between list items still keeps one list`() {
        val md = "- one\n\n\n- two\n\n\n- three"
        val html = MarkdownHtmlConverter.convert(md)
        assertEquals(1, Regex("<ul>").findAll(html).count(), "Expected exactly one <ul> tag")
    }
}
