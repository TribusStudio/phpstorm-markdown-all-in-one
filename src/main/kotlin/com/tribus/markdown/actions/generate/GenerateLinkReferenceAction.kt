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
 * Inserts a reference-style link at cursor and appends the reference definition
 * at the end of the document.
 */
class GenerateLinkReferenceAction : AnAction("Link Reference", "Insert a reference-style link", null), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val label = Messages.showInputDialog(
            project,
            "Reference label:",
            "Generate Link Reference",
            null,
            "ref",
            null
        ) ?: return

        val offset = editor.caretModel.offset
        val selectedText = editor.selectionModel.selectedText
        val linkText = if (!selectedText.isNullOrEmpty()) selectedText else "link text"
        val selStart = if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionStart else offset
        val selEnd = if (editor.selectionModel.hasSelection()) editor.selectionModel.selectionEnd else offset

        val reference = "[$linkText][$label]"
        val definition = "\n[$label]: url \"title\""

        WriteCommandAction.runWriteCommandAction(project) {
            // Replace selection (or insert at cursor)
            document.replaceString(selStart, selEnd, reference)
            // Append definition at end of document
            val docEnd = document.textLength
            document.insertString(docEnd, definition)
            // Move caret to the URL in the definition
            val urlOffset = docEnd + definition.indexOf("url")
            editor.caretModel.moveToOffset(urlOffset)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
