package com.tribus.markdown.preview

import org.junit.jupiter.api.Assertions.assertEquals
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
}
