package com.tribus.markdown.actions.generate

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.tribus.markdown.actions.MarkdownAction
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Generates a GFM table skeleton at the cursor. Prompts for column count.
 */
class GenerateTableAction : AnAction("Table", "Insert a GFM table", null), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return

        val input = Messages.showInputDialog(
            project,
            "Number of columns:",
            "Generate Table",
            null,
            "3",
            null
        ) ?: return

        val cols = input.toIntOrNull()?.coerceIn(1, 20) ?: return

        val header = "| " + (1..cols).joinToString(" | ") { "Header $it" } + " |"
        val separator = "| " + (1..cols).joinToString(" | ") { "---" } + " |"
        val row = "| " + (1..cols).joinToString(" | ") { "   " } + " |"
        val table = "$header\n$separator\n$row\n"

        val offset = editor.caretModel.offset
        WriteCommandAction.runWriteCommandAction(project) {
            editor.document.insertString(offset, table)
            editor.caretModel.moveToOffset(offset + table.length)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
