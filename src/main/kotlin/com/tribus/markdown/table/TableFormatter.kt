package com.tribus.markdown.table

import com.tribus.markdown.settings.MarkdownSettings

/**
 * Formats GFM tables with consistent padding and alignment.
 *
 * Features:
 * - Pads cells to uniform column widths
 * - Preserves column alignment markers
 * - Generates properly formatted separator rows
 * - Handles tables with varying cell counts
 * - Width-limited mode: when the padded table exceeds a column limit,
 *   cells use minimal spacing (1 space padding) instead of full padding
 */
object TableFormatter {

    /**
     * Format a parsed table into a nicely aligned string.
     */
    fun format(table: TableParser.Table, maxWidth: Int = 0): String {
        val colCount = table.columnCount

        // Calculate max width for each column (content only)
        val colWidths = IntArray(colCount) { col ->
            val headerWidth = table.headerCells.getOrElse(col) { "" }.length
            val maxDataWidth = table.dataRows.maxOfOrNull { row ->
                row.getOrElse(col) { "" }.length
            } ?: 0
            maxOf(headerWidth, maxDataWidth, 3) // minimum 3 for separator "---"
        }

        // Check if padded table fits within the width limit
        val effectiveMaxWidth = if (maxWidth > 0) {
            maxWidth
        } else {
            try {
                val settingsWidth = MarkdownSettings.getInstance().state.tableMaxWidth
                if (settingsWidth > 0) settingsWidth else 0
            } catch (_: Exception) {
                0
            }
        }

        val paddedWidth = calculateTableWidth(colWidths)
        val useCompact = effectiveMaxWidth > 0 && paddedWidth > effectiveMaxWidth

        if (useCompact) {
            // In compact mode, use minimum column widths (content width, min 3)
            // but don't pad beyond that
            return formatCompact(table, colWidths)
        }

        return formatPadded(table, colWidths)
    }

    private fun formatPadded(table: TableParser.Table, colWidths: IntArray): String {
        val lines = mutableListOf<String>()
        val colCount = table.columnCount

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

    private fun formatCompact(table: TableParser.Table, colWidths: IntArray): String {
        val lines = mutableListOf<String>()
        val colCount = table.columnCount

        // In compact mode, each cell gets exactly its content + 1 space on each side
        lines.add(formatCompactRow(table.headerCells, colCount))

        // Separator dashes match header cell widths (not max data widths)
        val headerWidths = IntArray(colCount) { col ->
            maxOf(table.headerCells.getOrElse(col) { "" }.length, 3)
        }
        lines.add(formatSeparator(headerWidths, table.alignments))

        // Data rows
        for (row in table.dataRows) {
            val normalizedRow = normalizeRow(row, colCount)
            lines.add(formatCompactRow(normalizedRow, colCount))
        }

        return lines.joinToString("\n")
    }

    private fun formatCompactRow(cells: List<String>, colCount: Int): String {
        val parts = (0 until colCount).map { i ->
            cells.getOrElse(i) { "" }
        }
        return "| ${parts.joinToString(" | ")} |"
    }

    /**
     * Format a single table from raw text.
     */
    fun formatTableInText(text: String, lineIndex: Int): String? {
        val table = TableParser.findTableAt(text, lineIndex) ?: return null
        val formatted = format(table)

        val lines = text.lines().toMutableList()
        val newLines = formatted.lines()

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
     */
    fun formatAll(text: String): String? {
        val tables = TableParser.findAll(text)
        if (tables.isEmpty()) return null

        var result = text
        var changed = false

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
            TableParser.Alignment.CENTER -> ":${"-".repeat(maxOf(width - 2, 1))}:"
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

    /**
     * Calculate the total width of a formatted table line given column widths.
     * Format: "| cell1 | cell2 | cell3 |"
     * Width = 2 (leading "| ") + sum(colWidths) + 3*(colCount-1) (" | " between) + 2 (trailing " |")
     */
    private fun calculateTableWidth(colWidths: IntArray): Int {
        if (colWidths.isEmpty()) return 0
        return 2 + colWidths.sum() + 3 * (colWidths.size - 1) + 2
    }

    private fun normalizeRow(cells: List<String>, expectedSize: Int): List<String> {
        return when {
            cells.size == expectedSize -> cells
            cells.size > expectedSize -> cells.take(expectedSize)
            else -> cells + List(expectedSize - cells.size) { "" }
        }
    }
}
