package com.tribus.markdown.actions.table

import com.tribus.markdown.table.TableOperations
import com.tribus.markdown.table.TableParser

class InsertColumnBeforeAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.insertColumn(table, colIndex)
    }
}

class InsertColumnAfterAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.insertColumn(table, colIndex + 1)
    }
}

class DeleteColumnAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.deleteColumn(table, colIndex)
    }
}

class SwapColumnLeftAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        if (colIndex <= 0) return table
        return TableOperations.swapColumns(table, colIndex - 1)
    }
}

class SwapColumnRightAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.swapColumns(table, colIndex)
    }
}
