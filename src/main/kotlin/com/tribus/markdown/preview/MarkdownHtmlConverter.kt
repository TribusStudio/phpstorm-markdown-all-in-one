package com.tribus.markdown.preview

/**
 * Converts markdown text to HTML for preview rendering.
 * Handles common GFM elements without external dependencies.
 */
object MarkdownHtmlConverter {

    fun convert(markdown: String): String {
        val lines = markdown.lines()
        val html = StringBuilder()
        var i = 0
        var inCodeBlock = false
        var codeBlockLang = ""
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
                inCodeBlock = true
                codeBlockContent.clear()
                i++
                continue
            }
            if (inCodeBlock) {
                if (line.trimStart().matches(Regex("^(`{3,}|~{3,})\\s*$"))) {
                    val langAttr = if (codeBlockLang.isNotEmpty()) " class=\"language-$codeBlockLang\"" else ""
                    html.append("<pre><code$langAttr>${escapeHtml(codeBlockContent.toString().trimEnd())}</code></pre>\n")
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
                if (listStack.isNotEmpty()) {
                    closeAllLists(html, listStack, listIndents)
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
                html.append("<hr>\n")
                i++
                continue
            }

            // ATX Heading
            val headingMatch = Regex("^(#{1,6})\\s+(.+)$").find(line.trim())
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val text = headingMatch.groupValues[2].replace(Regex("\\s+#+\\s*$"), "").trim()
                val id = slugify(text)
                html.append("<h$level id=\"$id\">${convertInline(text)}</h$level>\n")
                i++
                continue
            }

            // Setext heading (= or -)
            if (i + 1 < lines.size) {
                val nextLine = lines[i + 1].trim()
                if (nextLine.matches(Regex("^=+\\s*$"))) {
                    val id = slugify(line.trim())
                    html.append("<h1 id=\"$id\">${convertInline(line.trim())}</h1>\n")
                    i += 2
                    continue
                }
                if (nextLine.matches(Regex("^-+\\s*$")) && !isListItem(line)) {
                    val id = slugify(line.trim())
                    html.append("<h2 id=\"$id\">${convertInline(line.trim())}</h2>\n")
                    i += 2
                    continue
                }
            }

            // Blockquote
            if (line.trimStart().startsWith(">")) {
                if (!inBlockquote) {
                    html.append("<blockquote>\n")
                    inBlockquote = true
                }
                val content = line.trimStart().removePrefix(">").trimStart()
                html.append("<p>${convertInline(content)}</p>\n")
                i++
                continue
            }

            // GFM Table
            if (i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                val tableHtml = convertTable(lines, i)
                html.append(tableHtml.first)
                i = tableHtml.second
                continue
            }

            // Unordered list
            val ulMatch = Regex("^(\\s*)([-*+])\\s+(.+)$").find(line)
            if (ulMatch != null) {
                val indent = ulMatch.groupValues[1].length
                adjustListNesting(html, listStack, listIndents, indent, "ul")
                val content = ulMatch.groupValues[3]
                val taskMatch = Regex("^\\[([ xX])\\]\\s+(.+)$").find(content)
                if (taskMatch != null) {
                    val checked = if (taskMatch.groupValues[1].lowercase() == "x") " checked" else ""
                    html.append("<li><input type=\"checkbox\" disabled$checked> ${convertInline(taskMatch.groupValues[2])}</li>\n")
                } else {
                    html.append("<li>${convertInline(content)}</li>\n")
                }
                i++
                continue
            }

            // Ordered list
            val olMatch = Regex("^(\\s*)(\\d+)[.)\\]]\\s+(.+)$").find(line)
            if (olMatch != null) {
                val indent = olMatch.groupValues[1].length
                adjustListNesting(html, listStack, listIndents, indent, "ol")
                html.append("<li>${convertInline(olMatch.groupValues[3])}</li>\n")
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
            html.append("<p>${convertInline(line.trim())}</p>\n")
            i++
        }

        // Close any open blocks
        if (inCodeBlock) {
            html.append("<pre><code>${escapeHtml(codeBlockContent.toString().trimEnd())}</code></pre>\n")
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
     */
    fun convertInline(text: String): String {
        var result = escapeHtml(text)

        // Images: ![alt](url)
        result = result.replace(Regex("!\\[([^\\]]*)\\]\\(([^)]+)\\)")) { m ->
            "<img src=\"${m.groupValues[2]}\" alt=\"${m.groupValues[1]}\">"
        }

        // Links: [text](url)
        result = result.replace(Regex("\\[([^\\]]*)\\]\\(([^)]+)\\)")) { m ->
            "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>"
        }

        // Inline code — handle multi-backtick delimiters (`` ` ``, ``` `` ```, etc.)
        // Per CommonMark: a backtick string of length N opens code that closes at the next
        // backtick string of exactly length N. Content is trimmed of one leading+trailing space
        // if the content both starts and ends with a space and is not all spaces.
        result = result.replace(Regex("(`{2,})(.+?)\\1")) { m ->
            val content = m.groupValues[2]
            val trimmed = if (content.startsWith(" ") && content.endsWith(" ") && content.trim().isNotEmpty()) {
                content.substring(1, content.length - 1)
            } else {
                content
            }
            "<code>$trimmed</code>"
        }
        result = result.replace(Regex("`([^`]+)`")) { m ->
            "<code>${m.groupValues[1]}</code>"
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

        return result
    }

    private fun convertTable(lines: List<String>, startIndex: Int): Pair<String, Int> {
        val html = StringBuilder("<table>\n")
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
        if (trimmed.endsWith("|")) trimmed = trimmed.substring(0, trimmed.length - 1)
        return trimmed.split("|")
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
        listType: String
    ) {
        if (listStack.isEmpty()) {
            // Start first list
            html.append(if (listType == "ol") "<ol>\n" else "<ul>\n")
            listStack.add(listType)
            listIndents.add(indent)
        } else if (indent > listIndents.last()) {
            // Deeper nesting — open a new sub-list
            html.append(if (listType == "ol") "<ol>\n" else "<ul>\n")
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
    fun wrapInDocument(bodyHtml: String, css: String, customCss: String = "", darkMode: Boolean = false): String {
        val highlightTheme = if (darkMode) "/preview/highlight/github-dark.min.css" else "/preview/highlight/github.min.css"
        val highlightCss = loadResource(highlightTheme)
        val highlightJs = loadResource("/preview/highlight/highlight.min.js")
        val hasCodeBlocks = bodyHtml.contains("<code class=\"language-")

        return """<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<style>
$css
</style>
${if (customCss.isNotEmpty()) "<style>\n$customCss\n</style>" else ""}
${if (hasCodeBlocks && highlightCss.isNotEmpty()) "<style>\n$highlightCss\n</style>" else ""}
</head>
<body class="markdown-body">
$bodyHtml
${if (hasCodeBlocks && highlightJs.isNotEmpty()) "<script>$highlightJs</script>\n<script>hljs.highlightAll();</script>" else ""}
</body>
</html>"""
    }

    private fun loadResource(path: String): String {
        return MarkdownHtmlConverter::class.java.getResourceAsStream(path)
            ?.bufferedReader()
            ?.readText()
            ?: ""
    }
}
