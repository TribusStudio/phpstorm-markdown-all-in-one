package com.tribus.markdown.actions.table

import com.tribus.markdown.table.TableOperations
import com.tribus.markdown.table.TableParser

class InsertRowAboveAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.insertRow(table, dataRowIndex.coerceAtLeast(0))
    }
}

class InsertRowBelowAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.insertRow(table, dataRowIndex + 1)
    }
}

class DeleteRowAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        if (dataRowIndex < 0) return table // can't delete header via this action
        return TableOperations.deleteRow(table, dataRowIndex)
    }
}

class SwapRowUpAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        if (dataRowIndex <= 0) return table
        return TableOperations.swapRows(table, dataRowIndex - 1)
    }
}

class SwapRowDownAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        if (dataRowIndex < 0) return table
        return TableOperations.swapRows(table, dataRowIndex)
    }
}
