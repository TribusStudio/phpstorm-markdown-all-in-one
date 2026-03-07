package com.tribus.markdown.toc

/**
 * Extracts headings from markdown document text, skipping code blocks,
 * front matter, and HTML comments. Supports omit markers.
 */
object HeadingExtractor {

    data class Heading(
        val level: Int,
        val rawText: String,
        val lineNumber: Int,
        val canInToc: Boolean = true
    )

    // ATX heading: 0-3 leading spaces, 1-6 hashes, then space/tab or EOL
    private val ATX_HEADING = Regex("^\\s{0,3}(#{1,6})(\\s.*|$)")

    // Setext underline: = or - repeated, optionally indented 0-3 spaces
    private val SETEXT_UNDERLINE = Regex("^\\s{0,3}(=+|-+)\\s*$")

    // Fenced code block opener: 0-3 spaces, then 3+ backticks or tildes
    private val FENCE_OPEN = Regex("^\\s{0,3}(`{3,}|~{3,})")

    // YAML front matter start/end
    private val FRONT_MATTER_FENCE = Regex("^---\\s*$")

    // HTML comment (single line)
    private val HTML_COMMENT_SINGLE = Regex("^\\s{0,3}<!--(.*)-->\\s*$")

    // HTML comment block open (no closing on same line)
    private val HTML_COMMENT_OPEN = Regex("^\\s{0,3}<!--")
    private val HTML_COMMENT_CLOSE = Regex("-->")

    // Omit from TOC marker
    private val OMIT_MARKER = Regex("<!--\\s*omit\\s+(in|from)\\s+toc\\s*-->", RegexOption.IGNORE_CASE)

    /**
     * Extract all headings from the given markdown text.
     */
    fun extract(text: String): List<Heading> {
        val lines = text.lines()
        val headings = mutableListOf<Heading>()

        var inFrontMatter = false
        var frontMatterDone = false
        var inCodeFence = false
        var fenceChar = ' '
        var fenceLength = 0
        var inHtmlComment = false
        var prevLineOmit = false
        var prevLineText: String? = null
        var prevLineNumber = -1

        for ((index, line) in lines.withIndex()) {
            // Handle front matter (only at start of document)
            if (index == 0 && !frontMatterDone && FRONT_MATTER_FENCE.matches(line)) {
                inFrontMatter = true
                continue
            }
            if (inFrontMatter) {
                if (FRONT_MATTER_FENCE.matches(line)) {
                    inFrontMatter = false
                    frontMatterDone = true
                }
                continue
            }
            frontMatterDone = true

            // Handle HTML comment blocks
            if (inHtmlComment) {
                if (HTML_COMMENT_CLOSE.containsMatchIn(line)) {
                    inHtmlComment = false
                }
                continue
            }

            // Check for single-line HTML comment
            val singleComment = HTML_COMMENT_SINGLE.matchEntire(line)
            if (singleComment != null) {
                prevLineOmit = OMIT_MARKER.containsMatchIn(line)
                prevLineText = null
                prevLineNumber = -1
                continue
            }

            // Check for multi-line HTML comment open
            if (HTML_COMMENT_OPEN.containsMatchIn(line) && !HTML_COMMENT_CLOSE.containsMatchIn(line)) {
                inHtmlComment = true
                continue
            }

            // Handle fenced code blocks
            if (inCodeFence) {
                val closeMatch = FENCE_OPEN.find(line)
                if (closeMatch != null) {
                    val matchChar = closeMatch.groupValues[1][0]
                    val matchLength = closeMatch.groupValues[1].length
                    if (matchChar == fenceChar && matchLength >= fenceLength) {
                        inCodeFence = false
                    }
                }
                prevLineText = null
                prevLineNumber = -1
                continue
            }

            val fenceMatch = FENCE_OPEN.find(line)
            if (fenceMatch != null) {
                inCodeFence = true
                fenceChar = fenceMatch.groupValues[1][0]
                fenceLength = fenceMatch.groupValues[1].length
                prevLineText = null
                prevLineNumber = -1
                continue
            }

            // Check for ATX heading
            val atxMatch = ATX_HEADING.matchEntire(line)
            if (atxMatch != null) {
                val level = atxMatch.groupValues[1].length
                val rawText = atxMatch.groupValues[2].trim()
                    .replace(Regex("\\s+#+\\s*$"), "") // Remove trailing hashes
                    .trim()

                // Check for inline omit marker
                val inlineOmit = OMIT_MARKER.containsMatchIn(line)
                val canInToc = !prevLineOmit && !inlineOmit

                headings.add(Heading(
                    level = level,
                    rawText = rawText.replace(OMIT_MARKER, "").trim(),
                    lineNumber = index,
                    canInToc = canInToc
                ))
                prevLineOmit = false
                prevLineText = null
                prevLineNumber = -1
                continue
            }

            // Check for Setext heading (underline on current line, heading text on previous)
            if (prevLineText != null && SETEXT_UNDERLINE.matches(line)) {
                val underlineChar = line.trim()[0]
                val level = if (underlineChar == '=') 1 else 2
                val rawText = prevLineText.trim()

                // Don't treat list items or blockquotes as setext headings
                if (!rawText.startsWith("-") && !rawText.startsWith(">") &&
                    !rawText.startsWith("*") && !rawText.startsWith("+") &&
                    !rawText.matches(Regex("^\\d+[.)].+"))
                ) {
                    val inlineOmit = OMIT_MARKER.containsMatchIn(rawText)
                    val canInToc = !prevLineOmit && !inlineOmit

                    headings.add(Heading(
                        level = level,
                        rawText = rawText.replace(OMIT_MARKER, "").trim(),
                        lineNumber = prevLineNumber,
                        canInToc = canInToc
                    ))
                }
                prevLineOmit = false
                prevLineText = null
                prevLineNumber = -1
                continue
            }

            // Track previous line for setext detection
            prevLineOmit = OMIT_MARKER.containsMatchIn(line)
            prevLineText = if (line.isNotBlank()) line else null
            prevLineNumber = index
        }

        return headings
    }
}
