package com.tribus.markdown.preview

/**
 * Converts markdown text to HTML for preview rendering.
 * Handles common GFM elements without external dependencies.
 *
 * When [annotateSourceLines] is true, block-level elements include
 * `data-source-line` attributes for editor–preview scroll synchronization.
 */
object MarkdownHtmlConverter {

    fun convert(markdown: String, annotateSourceLines: Boolean = false): String {
        val lines = markdown.lines()
        val html = StringBuilder()
        var i = 0
        var inCodeBlock = false
        var codeBlockLang = ""
        var codeBlockStartLine = 0
        val codeBlockContent = StringBuilder()
        var inFrontMatter = false
        var frontMatterDone = false
        val listStack = mutableListOf<String>() // stack of "ol" or "ul"
        val listIndents = mutableListOf<Int>() // stack of indentation levels
        var inBlockquote = false

        while (i < lines.size) {
            val line = lines[i]

            // Front matter — only if there's a closing --- later
            if (i == 0 && !frontMatterDone && line.trim() == "---") {
                val hasClosing = lines.drop(1).any { it.trim() == "---" }
                if (hasClosing) {
                    inFrontMatter = true
                    i++
                    continue
                }
                frontMatterDone = true
            }
            if (inFrontMatter) {
                if (line.trim() == "---") {
                    inFrontMatter = false
                    frontMatterDone = true
                }
                i++
                continue
            }
            frontMatterDone = true

            // Fenced code blocks
            if (!inCodeBlock && line.trimStart().matches(Regex("^(`{3,}|~{3,})(.*)"))) {
                val match = Regex("^\\s*(`{3,}|~{3,})(.*)").find(line)!!
                codeBlockLang = match.groupValues[2].trim()
                codeBlockStartLine = i
                inCodeBlock = true
                codeBlockContent.clear()
                i++
                continue
            }
            if (inCodeBlock) {
                if (line.trimStart().matches(Regex("^(`{3,}|~{3,})\\s*$"))) {
                    val langAttr = if (codeBlockLang.isNotEmpty()) " class=\"language-$codeBlockLang\"" else ""
                    html.append("<pre${sl(codeBlockStartLine, annotateSourceLines)}><code$langAttr>${escapeHtml(codeBlockContent.toString().trimEnd())}</code></pre>\n")
                    inCodeBlock = false
                } else {
                    codeBlockContent.appendLine(line)
                }
                i++
                continue
            }

            // Close open lists if we hit a non-list line
            if (listStack.isNotEmpty() && !isListItem(line) && line.isNotBlank()) {
                closeAllLists(html, listStack, listIndents)
            }

            // Close blockquote
            if (inBlockquote && !line.trimStart().startsWith(">")) {
                html.append("</blockquote>\n")
                inBlockquote = false
            }

            // Blank line
            if (line.isBlank()) {
                // Look ahead: if the next non-blank line is a list item, keep the list open
                // (CommonMark "loose list" — blank lines between items don't end the list)
                if (listStack.isNotEmpty()) {
                    val nextContentLine = lines.drop(i + 1).firstOrNull { it.isNotBlank() }
                    if (nextContentLine == null || !isListItem(nextContentLine)) {
                        closeAllLists(html, listStack, listIndents)
                    }
                    // else: keep list open, blank line is just loose-list spacing
                }
                if (inBlockquote) {
                    html.append("</blockquote>\n")
                    inBlockquote = false
                }
                i++
                continue
            }

            // Horizontal rule
            if (line.trim().matches(Regex("^([-*_])\\s*\\1\\s*\\1[\\s\\-*_]*$"))) {
                html.append("<hr${sl(i, annotateSourceLines)}>\n")
                i++
                continue
            }

            // ATX Heading
            val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(line.trim())
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2].replace(Regex("\\s+#+\\s*$"), "").trim()
                val id = slugify(text)
                html.append("<h$level${sl(i, annotateSourceLines)} id=\"$id\">${convertInline(text)}</h$level>\n")
                i++
                continue
            }

            // Setext heading (= or -)
            if (i + 1 < lines.size) {
                val nextLine = lines[i + 1].trim()
                if (nextLine.matches(Regex("^=+\\s*$"))) {
                    val id = slugify(line.trim())
                    html.append("<h1${sl(i, annotateSourceLines)} id=\"$id\">${convertInline(line.trim())}</h1>\n")
                    i += 2
                    continue
                }
                if (nextLine.matches(Regex("^-+\\s*$")) && !isListItem(line)) {
                    val id = slugify(line.trim())
                    html.append("<h2${sl(i, annotateSourceLines)} id=\"$id\">${convertInline(line.trim())}</h2>\n")
                    i += 2
                    continue
                }
            }

            // Blockquote
            if (line.trimStart().startsWith(">")) {
                if (!inBlockquote) {
                    html.append("<blockquote${sl(i, annotateSourceLines)}>\n")
                    inBlockquote = true
                }
                val content = line.trimStart().removePrefix(">").trimStart()
                html.append("<p${sl(i, annotateSourceLines)}>${convertInline(content)}</p>\n")
                i++
                continue
            }

            // GFM Table
            if (i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                val tableHtml = convertTable(lines, i, annotateSourceLines)
                html.append(tableHtml.first)
                i = tableHtml.second
                continue
            }

            // Unordered list
            val ulMatch = Regex("^(\\s*)([-*+])\\s+(.+)$").find(line)
            if (ulMatch != null) {
                val indent = ulMatch.groupValues[1].length
                adjustListNesting(html, listStack, listIndents, indent, "ul", i, annotateSourceLines)
                val content = ulMatch.groupValues[3]
                val taskMatch = Regex("^\\[([ xX])\\]\\s+(.+)$").find(content)
                if (taskMatch != null) {
                    val checked = if (taskMatch.groupValues[1].lowercase() == "x") " checked" else ""
                    html.append("<li${sl(i, annotateSourceLines)}><input type=\"checkbox\" disabled$checked> ${convertInline(taskMatch.groupValues[2])}</li>\n")
                } else {
                    html.append("<li${sl(i, annotateSourceLines)}>${convertInline(content)}</li>\n")
                }
                i++
                continue
            }

            // Ordered list
            val olMatch = Regex("^(\\s*)(\\d+)[.)\\]]\\s+(.+)$").find(line)
            if (olMatch != null) {
                val indent = olMatch.groupValues[1].length
                adjustListNesting(html, listStack, listIndents, indent, "ol", i, annotateSourceLines)
                html.append("<li${sl(i, annotateSourceLines)}>${convertInline(olMatch.groupValues[3])}</li>\n")
                i++
                continue
            }

            // HTML comment — pass through
            if (line.trim().startsWith("<!--")) {
                html.append("$line\n")
                i++
                continue
            }

            // Paragraph
            html.append("<p${sl(i, annotateSourceLines)}>${convertInline(line.trim())}</p>\n")
            i++
        }

        // Close any open blocks
        if (inCodeBlock) {
            html.append("<pre${sl(codeBlockStartLine, annotateSourceLines)}><code>${escapeHtml(codeBlockContent.toString().trimEnd())}</code></pre>\n")
        }
        if (listStack.isNotEmpty()) {
            closeAllLists(html, listStack, listIndents)
        }
        if (inBlockquote) {
            html.append("</blockquote>\n")
        }

        return html.toString()
    }

    /**
     * Convert inline markdown formatting to HTML.
     *
     * Per CommonMark, code spans have the highest priority among inline constructs.
     * We extract them into placeholders first, process all other formatting on the
     * remaining text, then restore the code spans. This prevents markup inside
     * backticks (e.g., `![alt](`) from being interpreted as images/links.
     */
    fun convertInline(text: String): String {
        var result = escapeHtml(text)

        // ── Step 1: Extract code spans into placeholders ─────────────
        // Code spans take priority over all other inline formatting in CommonMark.
        val codeSpans = mutableListOf<String>()

        // Multi-backtick delimiters first (`` ` ``, ``` `` ```, etc.)
        result = result.replace(Regex("(`{2,})(.+?)\\1")) { m ->
            val content = m.groupValues[2]
            val trimmed = if (content.startsWith(" ") && content.endsWith(" ") && content.trim().isNotEmpty()) {
                content.substring(1, content.length - 1)
            } else {
                content
            }
            val idx = codeSpans.size
            codeSpans.add("<code>$trimmed</code>")
            "\u0000CODESPAN$idx\u0000"
        }
        // Single-backtick delimiters
        result = result.replace(Regex("`([^`]+)`")) { m ->
            val idx = codeSpans.size
            codeSpans.add("<code>${m.groupValues[1]}</code>")
            "\u0000CODESPAN$idx\u0000"
        }

        // ── Step 2: Process all other inline formatting ──────────────

        // Images: ![alt](url) — allow \] in alt text
        result = result.replace(Regex("!\\[((?:\\\\.|[^\\]])*)\\]\\(([^)]+)\\)")) { m ->
            "<img src=\"${m.groupValues[2]}\" alt=\"${m.groupValues[1]}\">"
        }

        // Links: [text](url) — allow \] in link text
        result = result.replace(Regex("\\[((?:\\\\.|[^\\]])*)\\]\\(([^)]+)\\)")) { m ->
            "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>"
        }

        // Bold + Italic: ***text*** or ___text___
        result = result.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*")) { m ->
            "<strong><em>${m.groupValues[1]}</em></strong>"
        }
        result = result.replace(Regex("___(.+?)___")) { m ->
            "<strong><em>${m.groupValues[1]}</em></strong>"
        }

        // Bold: **text** or __text__
        result = result.replace(Regex("\\*\\*(.+?)\\*\\*")) { m ->
            "<strong>${m.groupValues[1]}</strong>"
        }
        result = result.replace(Regex("__(.+?)__")) { m ->
            "<strong>${m.groupValues[1]}</strong>"
        }

        // Italic: *text* or _text_
        result = result.replace(Regex("\\*(.+?)\\*")) { m ->
            "<em>${m.groupValues[1]}</em>"
        }
        result = result.replace(Regex("(?<=\\s|^)_(.+?)_(?=\\s|$)")) { m ->
            "<em>${m.groupValues[1]}</em>"
        }

        // Strikethrough: ~~text~~
        result = result.replace(Regex("~~(.+?)~~")) { m ->
            "<del>${m.groupValues[1]}</del>"
        }

        // Line break (two trailing spaces)
        result = result.replace(Regex("  $"), "<br>")

        // Backslash escapes — any ASCII punctuation preceded by \ renders as the literal character
        result = result.replace(Regex("\\\\([\\\\`*_{}\\[\\]()#+\\-.!|~])")) { m ->
            m.groupValues[1]
        }

        // ── Step 3: Restore code spans ───────────────────────────────
        for (i in codeSpans.indices) {
            result = result.replace("\u0000CODESPAN$i\u0000", codeSpans[i])
        }

        return result
    }

    private fun convertTable(lines: List<String>, startIndex: Int, annotateSourceLines: Boolean = false): Pair<String, Int> {
        val html = StringBuilder("<table${sl(startIndex, annotateSourceLines)}>\n")
        val headerCells = parseTableCells(lines[startIndex])
        val separatorCells = parseTableCells(lines[startIndex + 1])
        val alignments = separatorCells.map { cell ->
            val trimmed = cell.trim()
            when {
                trimmed.startsWith(":") && trimmed.endsWith(":") -> "center"
                trimmed.endsWith(":") -> "right"
                trimmed.startsWith(":") -> "left"
                else -> ""
            }
        }

        // Header
        html.append("<thead><tr>\n")
        for ((j, cell) in headerCells.withIndex()) {
            val align = alignments.getOrElse(j) { "" }
            val style = if (align.isNotEmpty()) " style=\"text-align:$align\"" else ""
            html.append("<th$style>${convertInline(cell.trim())}</th>\n")
        }
        html.append("</tr></thead>\n<tbody>\n")

        // Data rows
        var i = startIndex + 2
        while (i < lines.size && lines[i].contains("|")) {
            val cells = parseTableCells(lines[i])
            html.append("<tr>\n")
            for ((j, cell) in cells.withIndex()) {
                val align = alignments.getOrElse(j) { "" }
                val style = if (align.isNotEmpty()) " style=\"text-align:$align\"" else ""
                html.append("<td$style>${convertInline(cell.trim())}</td>\n")
            }
            html.append("</tr>\n")
            i++
        }

        html.append("</tbody></table>\n")
        return Pair(html.toString(), i)
    }

    private fun parseTableCells(line: String): List<String> {
        var trimmed = line.trim()
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1)
        if (trimmed.endsWith("|") && !trimmed.endsWith("\\|")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        // Split on unescaped pipes only
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < trimmed.length) {
            if (trimmed[i] == '\\' && i + 1 < trimmed.length && trimmed[i + 1] == '|') {
                current.append("\\|")
                i += 2
            } else if (trimmed[i] == '|') {
                cells.add(current.toString())
                current.clear()
                i++
            } else {
                current.append(trimmed[i])
                i++
            }
        }
        cells.add(current.toString())
        return cells
    }

    private fun isTableSeparator(line: String): Boolean {
        return line.trim().matches(Regex("^\\|?(\\s*:?-{1,}:?\\s*\\|)+\\s*:?-{1,}:?\\s*\\|?\\s*$"))
    }

    private fun isListItem(line: String): Boolean {
        return line.matches(Regex("^\\s*[-*+]\\s+.*")) || line.matches(Regex("^\\s*\\d+[.)\\]]\\s+.*"))
    }

    private fun adjustListNesting(
        html: StringBuilder,
        listStack: MutableList<String>,
        listIndents: MutableList<Int>,
        indent: Int,
        listType: String,
        sourceLine: Int = -1,
        annotateSourceLines: Boolean = false
    ) {
        if (listStack.isEmpty()) {
            // Start first list
            val tag = if (listType == "ol") "ol" else "ul"
            html.append("<$tag${sl(sourceLine, annotateSourceLines)}>\n")
            listStack.add(listType)
            listIndents.add(indent)
        } else if (indent > listIndents.last()) {
            // Deeper nesting — open a new sub-list
            val tag = if (listType == "ol") "ol" else "ul"
            html.append("<$tag${sl(sourceLine, annotateSourceLines)}>\n")
            listStack.add(listType)
            listIndents.add(indent)
        } else if (indent < listIndents.last()) {
            // Outdent — close lists until we match
            while (listStack.size > 1 && listIndents.last() > indent) {
                html.append(if (listStack.removeLast() == "ol") "</ol>\n" else "</ul>\n")
                listIndents.removeLast()
            }
        }
        // Same indent level — continue current list (type change handled by just continuing)
    }

    private fun closeAllLists(
        html: StringBuilder,
        listStack: MutableList<String>,
        listIndents: MutableList<Int>
    ) {
        while (listStack.isNotEmpty()) {
            html.append(if (listStack.removeLast() == "ol") "</ol>\n" else "</ul>\n")
            listIndents.removeLast()
        }
    }

    private fun slugify(text: String): String {
        return com.tribus.markdown.toc.Slugify.slugify(text)
    }

    fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    /**
     * Wrap HTML body with full document structure including CSS theme.
     */
    fun wrapInDocument(
        bodyHtml: String,
        css: String,
        customCss: String = "",
        darkMode: Boolean = false,
        extraJs: String = "",
        mathEnabled: Boolean = false
    ): String {
        val highlightTheme = if (darkMode) "/preview/highlight/github-dark.min.css" else "/preview/highlight/github.min.css"
        val highlightCss = loadResource(highlightTheme)
        val highlightJs = loadResource("/preview/highlight/highlight.min.js")
        val hasCodeBlocks = bodyHtml.contains("<code class=\"language-")

        // KaTeX resources (loaded from bundled files)
        val katexCss = if (mathEnabled) loadResource("/preview/katex/katex.min.css") else ""
        val katexJs = if (mathEnabled) loadResource("/preview/katex/katex.min.js") else ""
        val autoRenderJs = if (mathEnabled) loadResource("/preview/katex/auto-render.min.js") else ""

        return """<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
$css
</style>
${if (customCss.isNotEmpty()) "<style>\n$customCss\n</style>" else ""}
${if (hasCodeBlocks && highlightCss.isNotEmpty()) "<style>\n$highlightCss\n</style>" else ""}
${if (katexCss.isNotEmpty()) "<style>\n$katexCss\n</style>" else ""}
</head>
<body class="markdown-body">
$bodyHtml
${if (hasCodeBlocks && highlightJs.isNotEmpty()) "<script>$highlightJs</script>\n<script>hljs.highlightAll();</script>" else ""}
${if (katexJs.isNotEmpty()) "<script>$katexJs</script>" else ""}
${if (autoRenderJs.isNotEmpty()) """<script>$autoRenderJs</script>
<script>
renderMathInElement(document.body, {
  delimiters: [
    {left: "$$", right: "$$", display: true},
    {left: "$", right: "$", display: false}
  ],
  throwOnError: false
});
</script>""" else ""}
${if (extraJs.isNotEmpty()) "<script>\n$extraJs\n</script>" else ""}
</body>
</html>"""
    }

    private fun loadResource(path: String): String {
        return MarkdownHtmlConverter::class.java.getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: ""
    }

    /**
     * Returns a `data-source-line` attribute string for scroll sync, or empty.
     */
    private fun sl(line: Int, annotate: Boolean): String =
        if (annotate) " data-source-line=\"$line\"" else ""
}
