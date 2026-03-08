package com.tribus.markdown.table

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TableFormatterTest {

    @Test
    fun `formats simple table with padding`() {
        val table = TableParser.Table(
            headerCells = listOf("Name", "Age"),
            alignments = listOf(TableParser.Alignment.NONE, TableParser.Alignment.NONE),
            dataRows = listOf(
                listOf("Alice", "30"),
                listOf("Bob", "25")
            ),
            startLine = 0, endLine = 3, startOffset = 0, endOffset = 0
        )
        val formatted = TableFormatter.format(table)
        val expected = """
            | Name  | Age |
            | ----- | --- |
            | Alice | 30  |
            | Bob   | 25  |
        """.trimIndent()
        assertEquals(expected, formatted)
    }

    @Test
    fun `preserves left alignment`() {
        val table = TableParser.Table(
            headerCells = listOf("Left"),
            alignments = listOf(TableParser.Alignment.LEFT),
            dataRows = listOf(listOf("data")),
            startLine = 0, endLine = 2, startOffset = 0, endOffset = 0
        )
        val formatted = TableFormatter.format(table)
        val lines = formatted.lines()
        assertEquals("| Left |", lines[0])
        assertEquals("| :--- |", lines[1])
        assertEquals("| data |", lines[2])
    }

    @Test
    fun `preserves right alignment`() {
        val table = TableParser.Table(
            headerCells = listOf("Right"),
            alignments = listOf(TableParser.Alignment.RIGHT),
            dataRows = listOf(listOf("data")),
            startLine = 0, endLine = 2, startOffset = 0, endOffset = 0
        )
        val formatted = TableFormatter.format(table)
        val lines = formatted.lines()
        assert(lines[1].contains("---:"))
        // Right-aligned content should be right-padded
        assert(lines[2].contains("data"))
    }

    @Test
    fun `preserves center alignment`() {
        val table = TableParser.Table(
            headerCells = listOf("Center"),
            alignments = listOf(TableParser.Alignment.CENTER),
            dataRows = listOf(listOf("data")),
            startLine = 0, endLine = 2, startOffset = 0, endOffset = 0
        )
        val formatted = TableFormatter.format(table)
        val lines = formatted.lines()
        assert(lines[1].contains(":---"))
        assert(lines[1].contains("---:"))
    }

    @Test
    fun `minimum column width is 3`() {
        val table = TableParser.Table(
            headerCells = listOf("A"),
            alignments = listOf(TableParser.Alignment.NONE),
            dataRows = listOf(listOf("B")),
            startLine = 0, endLine = 2, startOffset = 0, endOffset = 0
        )
        val formatted = TableFormatter.format(table)
        val lines = formatted.lines()
        assertEquals("| A   |", lines[0])
        assertEquals("| --- |", lines[1])
        assertEquals("| B   |", lines[2])
    }

    @Test
    fun `normalizes short rows with empty cells`() {
        val table = TableParser.Table(
            headerCells = listOf("A", "B", "C"),
            alignments = listOf(TableParser.Alignment.NONE, TableParser.Alignment.NONE, TableParser.Alignment.NONE),
            dataRows = listOf(listOf("1")),
            startLine = 0, endLine = 2, startOffset = 0, endOffset = 0
        )
        val formatted = TableFormatter.format(table)
        val lines = formatted.lines()
        assertEquals("| 1   |     |     |", lines[2])
    }

    @Test
    fun `formats unformatted table text`() {
        val text = """
            |Name|Age|City|
            |---|---|---|
            |Alice|30|NYC|
            |Bob|25|LA|
        """.trimIndent()
        val formatted = TableFormatter.formatTableInText(text, 0)
        assertNotNull(formatted)
        val expected = """
            | Name  | Age | City |
            | ----- | --- | ---- |
            | Alice | 30  | NYC  |
            | Bob   | 25  | LA   |
        """.trimIndent()
        assertEquals(expected, formatted)
    }

    @Test
    fun `formatTableInText returns null for non-table line`() {
        assertNull(TableFormatter.formatTableInText("Just some text", 0))
    }

    @Test
    fun `formatAll formats multiple tables`() {
        val text = """
            |A|B|
            |---|---|
            |1|2|

            Some text

            |X|Y|Z|
            |---|---|---|
            |a|b|c|
        """.trimIndent()
        val formatted = TableFormatter.formatAll(text)
        assertNotNull(formatted)
        assert(formatted!!.contains("| A   | B   |"))
        assert(formatted.contains("| X   | Y   | Z   |"))
    }

    @Test
    fun `formatAll returns null when no tables`() {
        assertNull(TableFormatter.formatAll("Just text with no tables"))
    }

    @Test
    fun `formatAll returns null when tables already formatted`() {
        val text = """
            | A   | B   |
            | --- | --- |
            | 1   | 2   |
        """.trimIndent()
        assertNull(TableFormatter.formatAll(text))
    }

    @Test
    fun `formats table with mixed alignment`() {
        val text = """
            |Left|Center|Right|
            |:---|:---:|---:|
            |a|b|c|
            |long text|x|y|
        """.trimIndent()
        val formatted = TableFormatter.formatTableInText(text, 0)
        assertNotNull(formatted)
        val lines = formatted!!.lines()
        // Check separator preserves alignment
        assert(lines[1].contains(":---"))
        assert(lines[1].contains(":---:") || (lines[1].contains(":-") && lines[1].contains("-:")))
        assert(lines[1].contains("---:"))
    }

    @Test
    fun `right alignment pads content left`() {
        val table = TableParser.Table(
            headerCells = listOf("Price"),
            alignments = listOf(TableParser.Alignment.RIGHT),
            dataRows = listOf(listOf("1"), listOf("100")),
            startLine = 0, endLine = 3, startOffset = 0, endOffset = 0
        )
        val formatted = TableFormatter.format(table)
        val lines = formatted.lines()
        assertEquals("| Price |", lines[0])
        assertEquals("| ----: |", lines[1])
        assertEquals("|     1 |", lines[2])
        assertEquals("|   100 |", lines[3])
    }

    @Test
    fun `center alignment centers content`() {
        val table = TableParser.Table(
            headerCells = listOf("Title"),
            alignments = listOf(TableParser.Alignment.CENTER),
            dataRows = listOf(listOf("ab"), listOf("abcde")),
            startLine = 0, endLine = 3, startOffset = 0, endOffset = 0
        )
        val formatted = TableFormatter.format(table)
        val lines = formatted.lines()
        assertEquals("| Title |", lines[0])
        assertEquals("| :---: |", lines[1])
        // "ab" centered in 5-wide field
        assert(lines[2].contains("ab"))
    }

    @Test
    fun `formatTableInText preserves surrounding text`() {
        val text = """
            # Heading

            |A|B|
            |---|---|
            |1|2|

            More text here.
        """.trimIndent()
        val formatted = TableFormatter.formatTableInText(text, 3)
        assertNotNull(formatted)
        assert(formatted!!.startsWith("# Heading"))
        assert(formatted.endsWith("More text here."))
        assert(formatted.contains("| A   | B   |"))
    }
}
