package com.tribus.markdown.table

/**
 * Parses GFM (GitHub Flavored Markdown) tables from text.
 *
 * A valid GFM table has:
 * - A header row with pipe-delimited cells
 * - A separator row with dashes (and optional colons for alignment)
 * - Zero or more data rows
 */
object TableParser {

    enum class Alignment { LEFT, CENTER, RIGHT, NONE }

    data class Table(
        val headerCells: List<String>,
        val alignments: List<Alignment>,
        val dataRows: List<List<String>>,
        val startLine: Int,
        val endLine: Int,
        val startOffset: Int,
        val endOffset: Int
    ) {
        val columnCount: Int get() = headerCells.size
    }

    private val SEPARATOR_CELL = Regex("""\s*:?-{1,}:?\s*""")
    private val SEPARATOR_ROW = Regex("""^\|?(\s*:?-{1,}:?\s*\|)+\s*:?-{1,}:?\s*\|?\s*$""")

    /**
     * Parse a table from text lines starting at the given line index.
     * Returns null if the lines don't form a valid GFM table.
     */
    fun parseAt(lines: List<String>, lineIndex: Int): Table? {
        if (lineIndex + 1 >= lines.size) return null

        val headerLine = lines[lineIndex]
        val separatorLine = lines[lineIndex + 1]

        if (!isSeparatorRow(separatorLine)) return null

        val headerCells = parseCells(headerLine)
        val separatorCells = parseCells(separatorLine)

        if (headerCells.isEmpty() || separatorCells.isEmpty()) return null
        if (headerCells.size != separatorCells.size) return null

        val alignments = separatorCells.map { parseAlignment(it.trim()) }

        val dataRows = mutableListOf<List<String>>()
        var endLine = lineIndex + 1

        for (i in lineIndex + 2 until lines.size) {
            val line = lines[i]
            if (!looksLikeTableRow(line)) break
            val cells = parseCells(line)
            dataRows.add(normalizeRow(cells, headerCells.size))
            endLine = i
        }

        return Table(
            headerCells = headerCells.map { it.trim() },
            alignments = alignments,
            dataRows = dataRows.map { row -> row.map { it.trim() } },
            startLine = lineIndex,
            endLine = endLine,
            startOffset = 0,
            endOffset = 0
        )
    }

    /**
     * Find the table surrounding the given line index in the document.
     */
    fun findTableAt(text: String, lineIndex: Int): Table? {
        val lines = text.lines()
        if (lineIndex < 0 || lineIndex >= lines.size) return null

        // Search upward to find the header row
        var headerLine = lineIndex

        // If we're on the separator row, header is one line above
        if (isSeparatorRow(lines[lineIndex]) && lineIndex > 0) {
            headerLine = lineIndex - 1
        } else if (lineIndex > 0 && lineIndex + 1 < lines.size && !isSeparatorRow(lines.getOrElse(lineIndex + 1) { "" })) {
            // We might be on a data row — search upward for the separator
            var sep = lineIndex - 1
            while (sep >= 1) {
                if (isSeparatorRow(lines[sep])) {
                    headerLine = sep - 1
                    break
                }
                if (!looksLikeTableRow(lines[sep])) return null
                sep--
            }
            if (sep < 1 && !isSeparatorRow(lines.getOrElse(1) { "" })) return null
        }

        val table = parseAt(lines, headerLine) ?: return null

        // Calculate offsets
        var startOffset = 0
        for (i in 0 until headerLine) {
            startOffset += lines[i].length + 1
        }
        var endOffset = startOffset
        for (i in headerLine..table.endLine) {
            endOffset += lines[i].length + if (i < table.endLine) 1 else 0
        }

        return table.copy(startOffset = startOffset, endOffset = endOffset)
    }

    /**
     * Find all tables in the document text.
     */
    fun findAll(text: String): List<Table> {
        val lines = text.lines()
        val tables = mutableListOf<Table>()
        var i = 0
        var offset = 0

        while (i < lines.size - 1) {
            if (i + 1 < lines.size && isSeparatorRow(lines[i + 1])) {
                val table = parseAt(lines, i)
                if (table != null) {
                    var endOffset = offset
                    for (j in i..table.endLine) {
                        endOffset += lines[j].length + if (j < table.endLine) 1 else 0
                    }
                    tables.add(table.copy(startOffset = offset, endOffset = endOffset))
                    // Skip past this table
                    for (j in i..table.endLine) {
                        offset += lines[j].length + 1
                    }
                    i = table.endLine + 1
                    continue
                }
            }
            offset += lines[i].length + 1
            i++
        }

        return tables
    }

    fun isSeparatorRow(line: String): Boolean = SEPARATOR_ROW.matches(line.trim())

    fun parseAlignment(cell: String): Alignment {
        val trimmed = cell.trim()
        val startsColon = trimmed.startsWith(":")
        val endsColon = trimmed.endsWith(":")
        return when {
            startsColon && endsColon -> Alignment.CENTER
            endsColon -> Alignment.RIGHT
            startsColon -> Alignment.LEFT
            else -> Alignment.NONE
        }
    }

    fun parseCells(line: String): List<String> {
        var trimmed = line.trim()

        // Remove leading and trailing pipes
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1)
        if (trimmed.endsWith("|")) trimmed = trimmed.substring(0, trimmed.length - 1)

        if (trimmed.isEmpty()) return emptyList()

        // Split on unescaped pipes
        return splitOnPipes(trimmed)
    }

    private fun splitOnPipes(text: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0

        while (i < text.length) {
            if (text[i] == '\\' && i + 1 < text.length && text[i + 1] == '|') {
                current.append("\\|")
                i += 2
            } else if (text[i] == '|') {
                cells.add(current.toString())
                current.clear()
                i++
            } else {
                current.append(text[i])
                i++
            }
        }
        cells.add(current.toString())

        return cells
    }

    fun looksLikeTableRow(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.contains("|") && trimmed.isNotEmpty()
    }

    private fun normalizeRow(cells: List<String>, expectedSize: Int): List<String> {
        return when {
            cells.size == expectedSize -> cells
            cells.size > expectedSize -> cells.take(expectedSize)
            else -> cells + List(expectedSize - cells.size) { "" }
        }
    }
}
