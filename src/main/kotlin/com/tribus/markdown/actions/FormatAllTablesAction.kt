package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.table.TableFormatter
import com.tribus.markdown.table.TableParser
import com.tribus.markdown.util.MarkdownFileUtil

class FormatAllTablesAction : AnAction(), MarkdownAction {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val formatted = TableFormatter.formatAll(document.text) ?: return

        WriteCommandAction.runWriteCommandAction(project, "Format All Tables", null, {
            document.setText(formatted)
        })
    }

    override fun update(e: AnActionEvent) {
        val hasFile = MarkdownFileUtil.isMarkdownFile(e)
        e.presentation.isEnabled = hasFile
        if (hasFile) {
            val editor = e.getData(CommonDataKeys.EDITOR)
            if (editor != null) {
                val hasTables = TableParser.findAll(editor.document.text).isNotEmpty()
                e.presentation.isEnabled = hasTables
            }
        }
    }
}
