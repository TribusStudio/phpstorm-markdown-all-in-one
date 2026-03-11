package com.tribus.markdown.toc

import com.tribus.markdown.settings.MarkdownSettings

/**
 * Generates and updates Table of Contents from document headings.
 *
 * Supports multiple TOCs per document:
 * - Generic: `<!-- TOC -->` ... `<!-- /TOC -->` — includes all document headings
 * - Named with overrides: `<!-- TOC name="api" type="ordered" level="2..4" -->` ... `<!-- /TOC -->`
 *
 * Named TOCs can be scoped to content ranges:
 * - `<!-- toc range name="api" start -->` ... `<!-- toc range end -->`
 */
object TocGenerator {

    const val TOC_END = "<!-- /TOC -->"

    // Matches TOC start markers with optional attributes
    private val TOC_START_PATTERN = Regex(
        """^\s{0,3}<!--\s*TOC(?!\s+range\b)(\s[^>]*)?\s*-->""",
        RegexOption.IGNORE_CASE
    )

    // Matches <!-- /TOC -->
    private val TOC_END_PATTERN = Regex(
        """^\s{0,3}<!--\s*/TOC\s*-->""",
        RegexOption.IGNORE_CASE
    )

    // Matches toc range start: <!-- toc range name="x" start -->
    private val TOC_RANGE_START_PATTERN = Regex(
        """^\s{0,3}<!--\s*toc\s+range\s+name\s*=\s*"([^"]+)"\s+start\s*-->""",
        RegexOption.IGNORE_CASE
    )

    // Matches toc range end: <!-- toc range end -->
    private val TOC_RANGE_END_PATTERN = Regex(
        """^\s{0,3}<!--\s*toc\s+range\s+end\s*-->""",
        RegexOption.IGNORE_CASE
    )

    // Parses key="value" pairs from TOC start marker attributes
    private val ATTR_PATTERN = Regex("(\\w+)\\s*=\\s*\"([^\"]*)\"")

    /**
     * Parsed attributes from a TOC start marker.
     */
    data class TocAttributes(
        val name: String? = null,
        val type: String? = null,   // "bullet" or "ordered"
        val level: String? = null   // e.g. "1..2"
    )

    /**
     * A TOC block found in the document.
     */
    data class TocBlock(
        val startLine: Int,
        val endLine: Int,
        val startOffset: Int,
        val endOffset: Int,
        val attributes: TocAttributes,
        val rawStartLine: String
    )

    /**
     * A content range associated with a named TOC.
     */
    data class ContentRange(
        val name: String,
        val startLine: Int,
        val endLine: Int
    )

    // --- Legacy compatibility: TocRange used by actions ---

    data class TocRange(
        val startLine: Int,
        val endLine: Int,
        val startOffset: Int,
        val endOffset: Int
    )

    /**
     * Parse attributes from a TOC start marker line.
     */
    fun parseAttributes(line: String): TocAttributes {
        val match = TOC_START_PATTERN.find(line.trim())
        val attrString = match?.groupValues?.getOrNull(1) ?: return TocAttributes()

        val attrs = mutableMapOf<String, String>()
        ATTR_PATTERN.findAll(attrString).forEach {
            attrs[it.groupValues[1].lowercase()] = it.groupValues[2]
        }

        return TocAttributes(
            name = attrs["name"],
            type = attrs["type"],
            level = attrs["level"]
        )
    }

    /**
     * Build the TOC start marker line from attributes.
     */
    fun buildStartMarker(attrs: TocAttributes): String {
        val parts = mutableListOf<String>()
        attrs.name?.let { parts.add("name=\"$it\"") }
        attrs.type?.let { parts.add("type=\"$it\"") }
        attrs.level?.let { parts.add("level=\"$it\"") }
        return if (parts.isEmpty()) "<!-- TOC -->" else "<!-- TOC ${parts.joinToString(" ")} -->"
    }

    // Matches fenced code block opening/closing (same pattern as HeadingExtractor)
    private val FENCE_PATTERN = Regex("^\\s{0,3}(`{3,}|~{3,})")

    /**
     * Find all TOC blocks in the document, skipping markers inside fenced code blocks.
     */
    fun findAllTocBlocks(documentText: String): List<TocBlock> {
        val lines = documentText.lines()
        val blocks = mutableListOf<TocBlock>()
        var currentOffset = 0
        var pendingStart: Triple<Int, Int, String>? = null // (line, offset, rawLine)
        var inCodeFence = false
        var fenceChar = ' '
        var fenceLength = 0

        for ((index, line) in lines.withIndex()) {
            // Track fenced code blocks so we skip TOC markers inside them
            if (inCodeFence) {
                val closeMatch = FENCE_PATTERN.find(line)
                if (closeMatch != null) {
                    val matchChar = closeMatch.groupValues[1][0]
                    val matchLength = closeMatch.groupValues[1].length
                    if (matchChar == fenceChar && matchLength >= fenceLength) {
                        inCodeFence = false
                    }
                }
                currentOffset += line.length + 1
                continue
            }

            val fenceMatch = FENCE_PATTERN.find(line)
            if (fenceMatch != null) {
                inCodeFence = true
                fenceChar = fenceMatch.groupValues[1][0]
                fenceLength = fenceMatch.groupValues[1].length
                currentOffset += line.length + 1
                continue
            }

            val trimmed = line.trim()
            if (TOC_START_PATTERN.matches(trimmed) && pendingStart == null) {
                pendingStart = Triple(index, currentOffset, line)
            } else if (TOC_END_PATTERN.matches(trimmed) && pendingStart != null) {
                val (startLine, startOffset, rawStartLine) = pendingStart
                blocks.add(TocBlock(
                    startLine = startLine,
                    endLine = index,
                    startOffset = startOffset,
                    endOffset = currentOffset + line.length,
                    attributes = parseAttributes(rawStartLine),
                    rawStartLine = rawStartLine.trim()
                ))
                pendingStart = null
            }
            currentOffset += line.length + 1
        }

        return blocks
    }

    /**
     * Find all content ranges in the document, skipping markers inside fenced code blocks.
     */
    fun findAllContentRanges(documentText: String): List<ContentRange> {
        val lines = documentText.lines()
        val ranges = mutableListOf<ContentRange>()
        var pendingName: String? = null
        var pendingStartLine = -1
        var inCodeFence = false
        var fenceChar = ' '
        var fenceLength = 0

        for ((index, line) in lines.withIndex()) {
            // Track fenced code blocks
            if (inCodeFence) {
                val closeMatch = FENCE_PATTERN.find(line)
                if (closeMatch != null) {
                    val matchChar = closeMatch.groupValues[1][0]
                    val matchLength = closeMatch.groupValues[1].length
                    if (matchChar == fenceChar && matchLength >= fenceLength) {
                        inCodeFence = false
                    }
                }
                continue
            }

            val fenceMatch = FENCE_PATTERN.find(line)
            if (fenceMatch != null) {
                inCodeFence = true
                fenceChar = fenceMatch.groupValues[1][0]
                fenceLength = fenceMatch.groupValues[1].length
                continue
            }

            val startMatch = TOC_RANGE_START_PATTERN.find(line.trim())
            if (startMatch != null && pendingName == null) {
                pendingName = startMatch.groupValues[1]
                pendingStartLine = index
                continue
            }
            if (pendingName != null && TOC_RANGE_END_PATTERN.matches(line.trim())) {
                ranges.add(ContentRange(
                    name = pendingName,
                    startLine = pendingStartLine,
                    endLine = index
                ))
                pendingName = null
            }
        }

        return ranges
    }

    /**
     * Find first TOC block (legacy compatibility for actions).
     */
    fun findTocRange(documentText: String): TocRange? {
        val block = findAllTocBlocks(documentText).firstOrNull() ?: return null
        return TocRange(
            startLine = block.startLine,
            endLine = block.endLine,
            startOffset = block.startOffset,
            endOffset = block.endOffset
        )
    }

    /**
     * Generate TOC text from document content using default or provided settings.
     */
    fun generate(documentText: String, state: MarkdownSettings.State? = null): String {
        val resolvedState = state ?: MarkdownSettings.getInstance().state
        val headings = HeadingExtractor.extract(documentText)
        return generateFromHeadings(headings, resolvedState)
    }

    /**
     * Generate TOC for a specific named TOC block, scoped to its content range(s).
     */
    fun generateForBlock(
        documentText: String,
        block: TocBlock,
        contentRanges: List<ContentRange>,
        state: MarkdownSettings.State? = null
    ): String {
        val resolvedState = state ?: MarkdownSettings.getInstance().state

        // Build effective state by merging block attributes over defaults
        val effectiveState = resolvedState.copy(
            tocOrderedList = when (block.attributes.type?.lowercase()) {
                "ordered" -> true
                "bullet" -> false
                else -> resolvedState.tocOrderedList
            },
            tocLevels = block.attributes.level ?: resolvedState.tocLevels
        )

        val allHeadings = HeadingExtractor.extract(documentText)

        // If named, filter headings to those within matching content ranges
        val headings = if (block.attributes.name != null) {
            val matchingRanges = contentRanges.filter {
                it.name.equals(block.attributes.name, ignoreCase = true)
            }
            if (matchingRanges.isEmpty()) {
                emptyList()
            } else {
                allHeadings.filter { heading ->
                    matchingRanges.any { range ->
                        heading.lineNumber in (range.startLine + 1) until range.endLine
                    }
                }
            }
        } else {
            // Unnamed TOC: all headings in the document
            allHeadings
        }

        return generateFromHeadings(headings, effectiveState)
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
        // Per-level counters for ordered lists (index 0-6 for heading levels 0-6)
        val levelCounters = IntArray(7)

        for (heading in filtered) {
            if (ordered) {
                // Reset counters for all deeper levels when we return to a shallower level
                for (i in heading.level + 1..6) levelCounters[i] = 0
                levelCounters[heading.level]++
            }

            val indent = "    ".repeat(heading.level - minLevel)
            val slug = Slugify.slugify(heading.rawText, slugMode)
            val uniqueSlug = Slugify.makeUnique(slug, slugOccurrences)
            val linkText = escapeLinkText(heading.rawText)

            val listMarker = if (ordered) "${levelCounters[heading.level]}." else marker
            lines.add("$indent$listMarker [$linkText](#$uniqueSlug)")
        }

        return lines.joinToString("\n")
    }

    /**
     * Generate TOC with surrounding markers (for unnamed/generic TOC insertion).
     */
    fun generateWithMarkers(documentText: String, state: MarkdownSettings.State? = null): String {
        val toc = generate(documentText, state)
        return if (toc.isEmpty()) "<!-- TOC -->\n$TOC_END" else "<!-- TOC -->\n$toc\n$TOC_END"
    }

    /**
     * Generate TOC with surrounding markers for a specific block.
     */
    fun generateBlockWithMarkers(
        documentText: String,
        block: TocBlock,
        contentRanges: List<ContentRange>,
        state: MarkdownSettings.State? = null
    ): String {
        val toc = generateForBlock(documentText, block, contentRanges, state)
        val startMarker = buildStartMarker(block.attributes)
        return if (toc.isEmpty()) "$startMarker\n$TOC_END" else "$startMarker\n$toc\n$TOC_END"
    }

    /**
     * Update all TOC blocks in the document.
     * Returns the new document text if any TOC was updated, null otherwise.
     */
    fun updateAllTocs(documentText: String, state: MarkdownSettings.State? = null): String? {
        val blocks = findAllTocBlocks(documentText)
        if (blocks.isEmpty()) return null

        val contentRanges = findAllContentRanges(documentText)
        var result = documentText
        var changed = false

        // Pre-generate all TOCs from the original document (line numbers are consistent)
        val newTocs = blocks.map { block ->
            generateBlockWithMarkers(documentText, block, contentRanges, state)
        }

        // Apply replacements in reverse order so offsets remain valid
        for ((i, block) in blocks.withIndex().reversed()) {
            val newToc = newTocs[i]
            val currentToc = result.substring(block.startOffset, block.endOffset)

            if (currentToc != newToc) {
                result = result.substring(0, block.startOffset) + newToc +
                    if (block.endOffset < result.length) result.substring(block.endOffset) else ""
                changed = true
            }
        }

        return if (changed) result else null
    }

    /**
     * Update existing TOC or return null if no TOC region found.
     * Updates all TOC blocks in the document.
     */
    fun updateExistingToc(documentText: String, state: MarkdownSettings.State? = null): String? {
        return updateAllTocs(documentText, state)
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
        return text
            .replace("\\", "\\\\")
            .replace("[", "\\[")
            .replace("]", "\\]")
    }
}
