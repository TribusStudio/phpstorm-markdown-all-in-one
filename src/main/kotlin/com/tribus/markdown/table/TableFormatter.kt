package com.tribus.markdown.table

/**
 * Formats GFM tables with consistent padding and alignment.
 *
 * Features:
 * - Pads cells to uniform column widths
 * - Preserves column alignment markers
 * - Generates properly formatted separator rows
 * - Handles tables with varying cell counts
 */
object TableFormatter {

    /**
     * Format a parsed table into a nicely aligned string.
     */
    fun format(table: TableParser.Table): String {
        val colCount = table.columnCount

        // Calculate max width for each column
        val colWidths = IntArray(colCount) { col ->
            val headerWidth = table.headerCells.getOrElse(col) { "" }.length
            val maxDataWidth = table.dataRows.maxOfOrNull { row ->
                row.getOrElse(col) { "" }.length
            } ?: 0
            maxOf(headerWidth, maxDataWidth, 3) // minimum 3 for separator "---"
        }

        val lines = mutableListOf<String>()

        // Header row
        lines.add(formatRow(table.headerCells, colWidths, table.alignments))

        // Separator row
        lines.add(formatSeparator(colWidths, table.alignments))

        // Data rows
        for (row in table.dataRows) {
            val normalizedRow = normalizeRow(row, colCount)
            lines.add(formatRow(normalizedRow, colWidths, table.alignments))
        }

        return lines.joinToString("\n")
    }

    /**
     * Format a single table from raw text.
     * Returns the formatted text, or null if the text doesn't contain a valid table at the given line.
     */
    fun formatTableInText(text: String, lineIndex: Int): String? {
        val table = TableParser.findTableAt(text, lineIndex) ?: return null
        val formatted = format(table)

        val lines = text.lines().toMutableList()
        val newLines = formatted.lines()

        // Replace the table lines
        val tableLineCount = table.endLine - table.startLine + 1
        for (i in 0 until tableLineCount) {
            lines.removeAt(table.startLine)
        }
        for ((i, line) in newLines.withIndex()) {
            lines.add(table.startLine + i, line)
        }

        return lines.joinToString("\n")
    }

    /**
     * Format all tables in a document.
     * Returns the formatted text, or null if no tables were found or nothing changed.
     */
    fun formatAll(text: String): String? {
        val tables = TableParser.findAll(text)
        if (tables.isEmpty()) return null

        var result = text
        var changed = false

        // Process tables in reverse order to preserve offsets
        for (table in tables.reversed()) {
            val formatted = format(table)
            val lines = result.lines().toMutableList()
            val newLines = formatted.lines()

            val tableLineCount = table.endLine - table.startLine + 1
            val originalLines = lines.subList(table.startLine, table.startLine + tableLineCount).joinToString("\n")

            if (originalLines != formatted) {
                for (i in 0 until tableLineCount) {
                    lines.removeAt(table.startLine)
                }
                for ((i, line) in newLines.withIndex()) {
                    lines.add(table.startLine + i, line)
                }
                result = lines.joinToString("\n")
                changed = true
            }
        }

        return if (changed) result else null
    }

    private fun formatRow(
        cells: List<String>,
        colWidths: IntArray,
        alignments: List<TableParser.Alignment>
    ): String {
        val formatted = cells.mapIndexed { i, cell ->
            val width = colWidths.getOrElse(i) { 3 }
            val alignment = alignments.getOrElse(i) { TableParser.Alignment.NONE }
            padCell(cell, width, alignment)
        }
        return "| ${formatted.joinToString(" | ")} |"
    }

    private fun formatSeparator(
        colWidths: IntArray,
        alignments: List<TableParser.Alignment>
    ): String {
        val cells = colWidths.mapIndexed { i, width ->
            val alignment = alignments.getOrElse(i) { TableParser.Alignment.NONE }
            buildSeparatorCell(width, alignment)
        }
        return "| ${cells.joinToString(" | ")} |"
    }

    private fun buildSeparatorCell(width: Int, alignment: TableParser.Alignment): String {
        return when (alignment) {
            TableParser.Alignment.LEFT -> ":${"-".repeat(width - 1)}"
            TableParser.Alignment.RIGHT -> "${"-".repeat(width - 1)}:"
            TableParser.Alignment.CENTER -> ":${"-".repeat(width - 2)}:"
            TableParser.Alignment.NONE -> "-".repeat(width)
        }
    }

    private fun padCell(
        text: String,
        width: Int,
        alignment: TableParser.Alignment
    ): String {
        return when (alignment) {
            TableParser.Alignment.RIGHT -> text.padStart(width)
            TableParser.Alignment.CENTER -> {
                val totalPad = width - text.length
                val leftPad = totalPad / 2
                val rightPad = totalPad - leftPad
                " ".repeat(leftPad) + text + " ".repeat(rightPad)
            }
            else -> text.padEnd(width)
        }
    }

    private fun normalizeRow(cells: List<String>, expectedSize: Int): List<String> {
        return when {
            cells.size == expectedSize -> cells
            cells.size > expectedSize -> cells.take(expectedSize)
            else -> cells + List(expectedSize - cells.size) { "" }
        }
    }
}
