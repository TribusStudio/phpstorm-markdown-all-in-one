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
 * Inserts a fenced code block at the cursor with an optional language identifier.
 */
class GenerateCodeBlockAction : AnAction("Code Block", "Insert a fenced code block", null), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val language = Messages.showInputDialog(
            project,
            "Language (leave empty for none):",
            "Generate Code Block",
            null,
            "",
            null
        ) ?: return

        val offset = editor.caretModel.offset
        val selectedText = editor.selectionModel.selectedText ?: ""
        val selStart = if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else offset
        val selEnd = if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionEnd else offset

        val block = "```${language}\n${selectedText}\n```"
        val cursorOffset = selStart + "```${language}\n".length + selectedText.length

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(selStart, selEnd, block)
            editor.caretModel.moveToOffset(cursorOffset)
            editor.selectionModel.removeSelection()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
