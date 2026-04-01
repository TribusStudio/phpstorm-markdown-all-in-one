package com.tribus.markdown.table

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TableOperationsTest {

    private fun table(
        header: List<String> = listOf("A", "B", "C"),
        alignments: List<TableParser.Alignment> = listOf(TableParser.Alignment.NONE, TableParser.Alignment.NONE, TableParser.Alignment.NONE),
        rows: List<List<String>> = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"))
    ) = TableParser.Table(header, alignments, rows, 0, 0, 0, 0)

    // ── Row operations ───────────────────────────────────────────────

    @Test
    fun `insertRow above first data row`() {
        val result = TableOperations.insertRow(table(), 0)
        assertEquals(3, result.dataRows.size)
        assertEquals(listOf("", "", ""), result.dataRows[0])
        assertEquals(listOf("1", "2", "3"), result.dataRows[1])
    }

    @Test
    fun `insertRow below last data row`() {
        val result = TableOperations.insertRow(table(), 2)
        assertEquals(3, result.dataRows.size)
        assertEquals(listOf("4", "5", "6"), result.dataRows[1])
        assertEquals(listOf("", "", ""), result.dataRows[2])
    }

    @Test
    fun `deleteRow removes data row`() {
        val result = TableOperations.deleteRow(table(), 0)
        assertEquals(1, result.dataRows.size)
        assertEquals(listOf("4", "5", "6"), result.dataRows[0])
    }

    @Test
    fun `deleteRow out of bounds returns unchanged`() {
        val t = table()
        val result = TableOperations.deleteRow(t, -1)
        assertEquals(t.dataRows, result.dataRows)
    }

    @Test
    fun `swapRows swaps adjacent rows`() {
        val result = TableOperations.swapRows(table(), 0)
        assertEquals(listOf("4", "5", "6"), result.dataRows[0])
        assertEquals(listOf("1", "2", "3"), result.dataRows[1])
    }

    @Test
    fun `swapRows at end returns unchanged`() {
        val t = table()
        val result = TableOperations.swapRows(t, 1)
        assertEquals(t.dataRows, result.dataRows)
    }

    // ── Column operations ────────────────────────────────────────────

    @Test
    fun `insertColumn before first`() {
        val result = TableOperations.insertColumn(table(), 0)
        assertEquals(4, result.columnCount)
        assertEquals("", result.headerCells[0])
        assertEquals("A", result.headerCells[1])
        assertEquals("", result.dataRows[0][0])
        assertEquals("1", result.dataRows[0][1])
    }

    @Test
    fun `insertColumn after last`() {
        val result = TableOperations.insertColumn(table(), 3)
        assertEquals(4, result.columnCount)
        assertEquals("C", result.headerCells[2])
        assertEquals("", result.headerCells[3])
    }

    @Test
    fun `deleteColumn removes column`() {
        val result = TableOperations.deleteColumn(table(), 1)
        assertEquals(2, result.columnCount)
        assertEquals(listOf("A", "C"), result.headerCells)
        assertEquals(listOf("1", "3"), result.dataRows[0])
    }

    @Test
    fun `deleteColumn on last column returns unchanged`() {
        val t = table(header = listOf("X"), alignments = listOf(TableParser.Alignment.NONE), rows = listOf(listOf("1")))
        val result = TableOperations.deleteColumn(t, 0)
        assertEquals(1, result.columnCount) // can't delete the only column
    }

    @Test
    fun `swapColumns swaps adjacent columns`() {
        val result = TableOperations.swapColumns(table(), 0)
        assertEquals(listOf("B", "A", "C"), result.headerCells)
        assertEquals(listOf("2", "1", "3"), result.dataRows[0])
    }

    @Test
    fun `swapColumns preserves alignment`() {
        val t = table(alignments = listOf(TableParser.Alignment.LEFT, TableParser.Alignment.CENTER, TableParser.Alignment.RIGHT))
        val result = TableOperations.swapColumns(t, 0)
        assertEquals(TableParser.Alignment.CENTER, result.alignments[0])
        assertEquals(TableParser.Alignment.LEFT, result.alignments[1])
    }

    // ── Alignment ────────────────────────────────────────────────────

    @Test
    fun `setAlignment changes column alignment`() {
        val result = TableOperations.setAlignment(table(), 1, TableParser.Alignment.RIGHT)
        assertEquals(TableParser.Alignment.RIGHT, result.alignments[1])
        assertEquals(TableParser.Alignment.NONE, result.alignments[0])
    }

    // ── Column index detection ───────────────────────────────────────

    @Test
    fun `findColumnIndex after first pipe`() {
        // "| A | B | C |" with cursor at position 3 (inside cell A)
        val col = com.tribus.markdown.actions.table.BaseTableAction.findColumnIndex("| A | B | C |", 3)
        assertEquals(0, col)
    }

    @Test
    fun `findColumnIndex in second cell`() {
        val col = com.tribus.markdown.actions.table.BaseTableAction.findColumnIndex("| A | B | C |", 7)
        assertEquals(1, col)
    }

    @Test
    fun `findColumnIndex in third cell`() {
        val col = com.tribus.markdown.actions.table.BaseTableAction.findColumnIndex("| A | B | C |", 11)
        assertEquals(2, col)
    }

    @Test
    fun `findColumnIndex at start returns 0`() {
        val col = com.tribus.markdown.actions.table.BaseTableAction.findColumnIndex("| A | B |", 0)
        assertEquals(0, col)
    }
}
