package com.tribus.markdown.table

/**
 * Operations for inserting, deleting, and rearranging rows and columns
 * in a parsed GFM table. Each operation returns a new Table instance.
 */
object TableOperations {

    /**
     * Insert a new empty row at [rowIndex] (0-based into dataRows).
     * rowIndex=0 inserts before the first data row.
     */
    fun insertRow(table: TableParser.Table, rowIndex: Int): TableParser.Table {
        val emptyRow = List(table.columnCount) { "" }
        val newRows = table.dataRows.toMutableList()
        newRows.add(rowIndex.coerceIn(0, newRows.size), emptyRow)
        return table.copy(dataRows = newRows)
    }

    /**
     * Delete the data row at [rowIndex] (0-based into dataRows).
     */
    fun deleteRow(table: TableParser.Table, rowIndex: Int): TableParser.Table {
        if (rowIndex < 0 || rowIndex >= table.dataRows.size) return table
        val newRows = table.dataRows.toMutableList()
        newRows.removeAt(rowIndex)
        return table.copy(dataRows = newRows)
    }

    /**
     * Swap two adjacent data rows. [rowIndex] is the upper row (0-based).
     */
    fun swapRows(table: TableParser.Table, rowIndex: Int): TableParser.Table {
        if (rowIndex < 0 || rowIndex + 1 >= table.dataRows.size) return table
        val newRows = table.dataRows.toMutableList()
        val temp = newRows[rowIndex]
        newRows[rowIndex] = newRows[rowIndex + 1]
        newRows[rowIndex + 1] = temp
        return table.copy(dataRows = newRows)
    }

    /**
     * Insert an empty column at [colIndex] (0-based).
     */
    fun insertColumn(table: TableParser.Table, colIndex: Int): TableParser.Table {
        val idx = colIndex.coerceIn(0, table.columnCount)
        val newHeader = table.headerCells.toMutableList().apply { add(idx, "") }
        val newAlignments = table.alignments.toMutableList().apply { add(idx, TableParser.Alignment.NONE) }
        val newRows = table.dataRows.map { row ->
            val padded = row.toMutableList()
            // Pad short rows to match current column count before inserting
            while (padded.size < table.columnCount) padded.add("")
            padded.add(idx, "")
            padded
        }
        return table.copy(headerCells = newHeader, alignments = newAlignments, dataRows = newRows)
    }

    /**
     * Delete the column at [colIndex] (0-based).
     */
    fun deleteColumn(table: TableParser.Table, colIndex: Int): TableParser.Table {
        if (colIndex < 0 || colIndex >= table.columnCount) return table
        if (table.columnCount <= 1) return table // can't delete the last column
        val newHeader = table.headerCells.toMutableList().apply { removeAt(colIndex) }
        val newAlignments = table.alignments.toMutableList().apply { removeAt(colIndex) }
        val newRows = table.dataRows.map { row ->
            if (colIndex < row.size) {
                row.toMutableList().apply { removeAt(colIndex) }
            } else row
        }
        return table.copy(headerCells = newHeader, alignments = newAlignments, dataRows = newRows)
    }

    /**
     * Swap two adjacent columns. [colIndex] is the left column (0-based).
     */
    fun swapColumns(table: TableParser.Table, colIndex: Int): TableParser.Table {
        if (colIndex < 0 || colIndex + 1 >= table.columnCount) return table
        val newHeader = table.headerCells.toMutableList().apply {
            val temp = this[colIndex]; this[colIndex] = this[colIndex + 1]; this[colIndex + 1] = temp
        }
        val newAlignments = table.alignments.toMutableList().apply {
            val temp = this[colIndex]; this[colIndex] = this[colIndex + 1]; this[colIndex + 1] = temp
        }
        val newRows = table.dataRows.map { row ->
            if (colIndex + 1 < row.size) {
                row.toMutableList().apply {
                    val temp = this[colIndex]; this[colIndex] = this[colIndex + 1]; this[colIndex + 1] = temp
                }
            } else row
        }
        return table.copy(headerCells = newHeader, alignments = newAlignments, dataRows = newRows)
    }

    /**
     * Set the alignment of column [colIndex].
     */
    fun setAlignment(table: TableParser.Table, colIndex: Int, alignment: TableParser.Alignment): TableParser.Table {
        if (colIndex < 0 || colIndex >= table.columnCount) return table
        val newAlignments = table.alignments.toMutableList()
        newAlignments[colIndex] = alignment
        return table.copy(alignments = newAlignments)
    }
}
