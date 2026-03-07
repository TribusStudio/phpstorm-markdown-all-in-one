package com.tribus.markdown.actions.generate

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.actions.MarkdownAction
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Inserts a footnote reference at cursor and appends the definition at the end of the document.
 * Auto-increments the footnote number based on existing footnotes.
 */
class GenerateFootnoteAction : AnAction("Footnote", "Insert a footnote reference and definition", null), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        // Find the next footnote number
        val text = document.text
        val existingFootnotes = Regex("""\[\^(\d+)]""").findAll(text)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .maxOrNull() ?: 0
        val nextNum = existingFootnotes + 1

        val offset = editor.caretModel.offset
        val reference = "[^$nextNum]"
        val definition = "\n[^$nextNum]: "

        WriteCommandAction.runWriteCommandAction(project) {
            // Insert reference at cursor
            document.insertString(offset, reference)
            // Append definition at end of document
            val docEnd = document.textLength
            document.insertString(docEnd, definition)
            // Move caret to the definition to start typing
            editor.caretModel.moveToOffset(docEnd + definition.length)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
