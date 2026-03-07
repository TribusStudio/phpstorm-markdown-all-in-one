package com.tribus.markdown.toc

import com.tribus.markdown.settings.MarkdownSettings

/**
 * Generates and updates Table of Contents from document headings.
 */
object TocGenerator {

    // TOC region markers
    const val TOC_START = "<!-- TOC -->"
    const val TOC_END = "<!-- /TOC -->"

    data class TocRange(
        val startLine: Int,
        val endLine: Int,
        val startOffset: Int,
        val endOffset: Int
    )

    /**
     * Generate TOC text from document content.
     */
    fun generate(documentText: String): String {
        val settings = MarkdownSettings.getInstance()
        val state = settings.state

        val headings = HeadingExtractor.extract(documentText)
        return generateFromHeadings(headings, state)
    }

    /**
     * Generate TOC text from a list of headings.
     */
    fun generateFromHeadings(
        headings: List<HeadingExtractor.Heading>,
        state: MarkdownSettings.State = MarkdownSettings.State()
    ): String {
        val (startDepth, endDepth) = parseLevels(state.tocLevels)
        val slugMode = parseSlugMode(state.tocSlugifyMode)
        val ordered = state.tocOrderedList
        val marker = state.tocUnorderedListMarker

        val filtered = headings.filter { it.canInToc && it.level in startDepth..endDepth }
        if (filtered.isEmpty()) return ""

        // Calculate minimum level for indentation base
        val minLevel = filtered.minOf { it.level }

        val slugOccurrences = mutableMapOf<String, Int>()
        val lines = mutableListOf<String>()

        for ((index, heading) in filtered.withIndex()) {
            val indent = "    ".repeat(heading.level - minLevel)
            val slug = Slugify.slugify(heading.rawText, slugMode)
            val uniqueSlug = Slugify.makeUnique(slug, slugOccurrences)
            val linkText = escapeLinkText(heading.rawText)

            val listMarker = if (ordered) "${index + 1}." else marker
            lines.add("$indent$listMarker [$linkText](#$uniqueSlug)")
        }

        return lines.joinToString("\n")
    }

    /**
     * Generate TOC with surrounding markers.
     */
    fun generateWithMarkers(documentText: String): String {
        val toc = generate(documentText)
        if (toc.isEmpty()) return "$TOC_START\n$TOC_END"
        return "$TOC_START\n$toc\n$TOC_END"
    }

    /**
     * Find existing TOC region in document (between markers).
     */
    fun findTocRange(documentText: String): TocRange? {
        val lines = documentText.lines()
        var startLine = -1
        var startOffset = 0
        var currentOffset = 0

        for ((index, line) in lines.withIndex()) {
            if (line.trim() == TOC_START && startLine == -1) {
                startLine = index
                startOffset = currentOffset
            } else if (line.trim() == TOC_END && startLine != -1) {
                return TocRange(
                    startLine = startLine,
                    endLine = index,
                    startOffset = startOffset,
                    endOffset = currentOffset + line.length
                )
            }
            currentOffset += line.length + 1 // +1 for newline
        }

        return null
    }

    /**
     * Update existing TOC or return null if no TOC region found.
     * Returns the new document text if updated, null otherwise.
     */
    fun updateExistingToc(documentText: String): String? {
        val range = findTocRange(documentText) ?: return null
        val newToc = generateWithMarkers(documentText)
        val before = documentText.substring(0, range.startOffset)
        val after = if (range.endOffset < documentText.length) {
            documentText.substring(range.endOffset)
        } else {
            ""
        }
        return before + newToc + after
    }

    /**
     * Add section numbers to headings in document text.
     */
    fun addSectionNumbers(documentText: String): String {
        val settings = MarkdownSettings.getInstance()
        val (startDepth, endDepth) = parseLevels(settings.state.tocLevels)

        val lines = documentText.lines().toMutableList()
        val headings = HeadingExtractor.extract(documentText)
        val filtered = headings.filter { it.canInToc && it.level in startDepth..endDepth }

        if (filtered.isEmpty()) return documentText

        val minLevel = filtered.minOf { it.level }
        val counters = IntArray(7) // index 0 unused, 1-6 for heading levels

        // Process in reverse to preserve line numbers
        for (heading in filtered.reversed()) {
            // Reset deeper counters
            for (i in heading.level + 1..6) counters[i] = 0
            counters[heading.level]++

            // Build number prefix
            val numberParts = (minLevel..heading.level).map { counters[it] }
            val numberPrefix = numberParts.joinToString(".") + "."

            // Replace heading text
            val line = lines[heading.lineNumber]
            val atxMatch = Regex("^(\\s{0,3}#{1,6}\\s+)((?:\\d+\\.)+\\s)?(.*)$").matchEntire(line)
            if (atxMatch != null) {
                lines[heading.lineNumber] = "${atxMatch.groupValues[1]}$numberPrefix ${atxMatch.groupValues[3]}"
            }
        }

        // Re-run forward pass to get correct counters (reverse was wrong for numbering)
        val lines2 = documentText.lines().toMutableList()
        val counters2 = IntArray(7)
        for (heading in filtered) {
            for (i in heading.level + 1..6) counters2[i] = 0
            counters2[heading.level]++

            val numberParts = (minLevel..heading.level).map { counters2[it] }
            val numberPrefix = numberParts.joinToString(".") + "."

            val line = lines2[heading.lineNumber]
            val atxMatch = Regex("^(\\s{0,3}#{1,6}\\s+)((?:\\d+\\.)+\\s)?(.*)$").matchEntire(line)
            if (atxMatch != null) {
                lines2[heading.lineNumber] = "${atxMatch.groupValues[1]}$numberPrefix ${atxMatch.groupValues[3]}"
            }
        }

        return lines2.joinToString("\n")
    }

    /**
     * Remove section numbers from headings in document text.
     */
    fun removeSectionNumbers(documentText: String): String {
        val lines = documentText.lines().toMutableList()
        val numberPattern = Regex("^(\\s{0,3}#{1,6}\\s+)(\\d+\\.(?:\\d+\\.)*\\s)(.*)")

        for (i in lines.indices) {
            val match = numberPattern.matchEntire(lines[i])
            if (match != null) {
                lines[i] = "${match.groupValues[1]}${match.groupValues[3]}"
            }
        }

        return lines.joinToString("\n")
    }

    /**
     * Parse "1..6" style level range string.
     */
    fun parseLevels(levels: String): Pair<Int, Int> {
        val match = Regex("^([1-6])\\.\\.([1-6])$").matchEntire(levels.trim())
        if (match != null) {
            val start = match.groupValues[1].toInt()
            val end = match.groupValues[2].toInt()
            return Pair(minOf(start, end), maxOf(start, end))
        }
        return Pair(1, 6) // default
    }

    fun parseSlugMode(mode: String): Slugify.Mode {
        return when (mode.lowercase()) {
            "github" -> Slugify.Mode.GITHUB
            "gitlab" -> Slugify.Mode.GITLAB
            "gitea" -> Slugify.Mode.GITEA
            "azure_devops", "azuredevops", "azure" -> Slugify.Mode.AZURE_DEVOPS
            "bitbucket", "bitbucket_cloud" -> Slugify.Mode.BITBUCKET_CLOUD
            else -> Slugify.Mode.GITHUB
        }
    }

    /**
     * Escape special characters in link text for markdown.
     */
    private fun escapeLinkText(text: String): String {
        // In TOC link text, we need to escape brackets and backslashes
        return text
            .replace("\\", "\\\\")
            .replace("[", "\\[")
            .replace("]", "\\]")
    }
}
