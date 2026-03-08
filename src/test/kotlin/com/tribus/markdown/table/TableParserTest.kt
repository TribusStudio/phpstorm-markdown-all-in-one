package com.tribus.markdown.table

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TableParserTest {

    @Test
    fun `parses simple table`() {
        val lines = listOf(
            "| Name | Age |",
            "| --- | --- |",
            "| Alice | 30 |",
            "| Bob | 25 |"
        )
        val table = TableParser.parseAt(lines, 0)
        assertNotNull(table)
        assertEquals(2, table!!.columnCount)
        assertEquals(listOf("Name", "Age"), table.headerCells)
        assertEquals(2, table.dataRows.size)
        assertEquals(listOf("Alice", "30"), table.dataRows[0])
        assertEquals(listOf("Bob", "25"), table.dataRows[1])
    }

    @Test
    fun `parses table without leading pipes`() {
        val lines = listOf(
            "Name | Age",
            "--- | ---",
            "Alice | 30"
        )
        val table = TableParser.parseAt(lines, 0)
        assertNotNull(table)
        assertEquals(2, table!!.columnCount)
        assertEquals(listOf("Name", "Age"), table.headerCells)
    }

    @Test
    fun `returns null for non-table lines`() {
        val lines = listOf("Just some text", "Another line")
        assertNull(TableParser.parseAt(lines, 0))
    }

    @Test
    fun `returns null when header and separator column count mismatch`() {
        val lines = listOf(
            "| A | B | C |",
            "| --- | --- |",
            "| 1 | 2 | 3 |"
        )
        assertNull(TableParser.parseAt(lines, 0))
    }

    @Test
    fun `detects left alignment`() {
        assertEquals(TableParser.Alignment.LEFT, TableParser.parseAlignment(":---"))
    }

    @Test
    fun `detects right alignment`() {
        assertEquals(TableParser.Alignment.RIGHT, TableParser.parseAlignment("---:"))
    }

    @Test
    fun `detects center alignment`() {
        assertEquals(TableParser.Alignment.CENTER, TableParser.parseAlignment(":---:"))
    }

    @Test
    fun `detects no alignment`() {
        assertEquals(TableParser.Alignment.NONE, TableParser.parseAlignment("---"))
    }

    @Test
    fun `parses alignment from separator row`() {
        val lines = listOf(
            "| Left | Center | Right | Default |",
            "| :--- | :---: | ---: | --- |",
            "| a | b | c | d |"
        )
        val table = TableParser.parseAt(lines, 0)
        assertNotNull(table)
        assertEquals(
            listOf(
                TableParser.Alignment.LEFT,
                TableParser.Alignment.CENTER,
                TableParser.Alignment.RIGHT,
                TableParser.Alignment.NONE
            ),
            table!!.alignments
        )
    }

    @Test
    fun `isSeparatorRow validates correctly`() {
        assertTrue(TableParser.isSeparatorRow("| --- | --- |"))
        assertTrue(TableParser.isSeparatorRow("| :--- | ---: | :---: |"))
        assertTrue(TableParser.isSeparatorRow("--- | ---"))
        assertTrue(TableParser.isSeparatorRow("|---|---|"))
        assertTrue(!TableParser.isSeparatorRow("| abc | def |"))
        assertTrue(!TableParser.isSeparatorRow("some text"))
    }

    @Test
    fun `parseCells handles escaped pipes`() {
        val cells = TableParser.parseCells("| a \\| b | c |")
        assertEquals(2, cells.size)
        assertEquals(" a \\| b ", cells[0])
        assertEquals(" c ", cells[1])
    }

    @Test
    fun `stops at non-table line`() {
        val lines = listOf(
            "| A | B |",
            "| --- | --- |",
            "| 1 | 2 |",
            "",
            "Not a table"
        )
        val table = TableParser.parseAt(lines, 0)
        assertNotNull(table)
        assertEquals(1, table!!.dataRows.size)
        assertEquals(2, table.endLine)
    }

    @Test
    fun `normalizes rows with fewer cells`() {
        val lines = listOf(
            "| A | B | C |",
            "| --- | --- | --- |",
            "| 1 |"
        )
        val table = TableParser.parseAt(lines, 0)
        assertNotNull(table)
        assertEquals(listOf("1", "", ""), table!!.dataRows[0])
    }

    @Test
    fun `findAll finds multiple tables`() {
        val text = """
            | A | B |
            | --- | --- |
            | 1 | 2 |

            Some text between tables

            | X | Y | Z |
            | --- | --- | --- |
            | a | b | c |
        """.trimIndent()
        val tables = TableParser.findAll(text)
        assertEquals(2, tables.size)
        assertEquals(2, tables[0].columnCount)
        assertEquals(3, tables[1].columnCount)
    }

    @Test
    fun `findTableAt finds table from data row`() {
        val text = """
            | A | B |
            | --- | --- |
            | 1 | 2 |
            | 3 | 4 |
        """.trimIndent()
        val table = TableParser.findTableAt(text, 2)
        assertNotNull(table)
        assertEquals(0, table!!.startLine)
        assertEquals(3, table.endLine)
    }

    @Test
    fun `findTableAt finds table from separator row`() {
        val text = """
            | A | B |
            | --- | --- |
            | 1 | 2 |
        """.trimIndent()
        val table = TableParser.findTableAt(text, 1)
        assertNotNull(table)
        assertEquals(0, table!!.startLine)
    }

    @Test
    fun `findTableAt returns null outside table`() {
        val text = """
            Some text

            | A | B |
            | --- | --- |
            | 1 | 2 |
        """.trimIndent()
        assertNull(TableParser.findTableAt(text, 0))
    }

    @Test
    fun `handles table with only header and separator`() {
        val lines = listOf(
            "| A | B |",
            "| --- | --- |"
        )
        val table = TableParser.parseAt(lines, 0)
        assertNotNull(table)
        assertEquals(0, table!!.dataRows.size)
    }

    @Test
    fun `looksLikeTableRow`() {
        assertTrue(TableParser.looksLikeTableRow("| a | b |"))
        assertTrue(TableParser.looksLikeTableRow("a | b"))
        assertTrue(!TableParser.looksLikeTableRow("just text"))
        assertTrue(!TableParser.looksLikeTableRow(""))
    }
}
