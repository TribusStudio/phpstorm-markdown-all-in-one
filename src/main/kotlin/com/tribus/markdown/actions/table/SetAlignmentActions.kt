package com.tribus.markdown.actions.table

import com.tribus.markdown.table.TableOperations
import com.tribus.markdown.table.TableParser

class SetAlignLeftAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.setAlignment(table, colIndex, TableParser.Alignment.LEFT)
    }
}

class SetAlignCenterAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.setAlignment(table, colIndex, TableParser.Alignment.CENTER)
    }
}

class SetAlignRightAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.setAlignment(table, colIndex, TableParser.Alignment.RIGHT)
    }
}

class SetAlignNoneAction : BaseTableAction() {
    override fun transformTable(table: TableParser.Table, dataRowIndex: Int, colIndex: Int): TableParser.Table {
        return TableOperations.setAlignment(table, colIndex, TableParser.Alignment.NONE)
    }
}
