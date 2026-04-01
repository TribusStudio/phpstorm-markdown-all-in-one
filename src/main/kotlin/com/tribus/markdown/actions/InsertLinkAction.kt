package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.tribus.markdown.util.MarkdownFileUtil
import javax.swing.JComponent

/**
 * Opens a dialog to insert a markdown link [text](url) at the cursor.
 * If text is selected, it's used as the link text; the dialog pre-fills it.
 */
class InsertLinkAction : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val selectedText = editor.selectionModel.selectedText ?: ""

        val dialog = LinkDialog(selectedText)
        if (!dialog.showAndGet()) return

        val linkText = dialog.linkText.ifBlank { dialog.url }
        val url = dialog.url
        val title = dialog.titleText
        val markdown = if (title.isNotBlank()) {
            "[$linkText]($url \"$title\")"
        } else {
            "[$linkText]($url)"
        }

        WriteCommandAction.runWriteCommandAction(project) {
            val offset = if (editor.selectionModel.hasSelection()) {
                val start = editor.selectionModel.selectionStart
                val end = editor.selectionModel.selectionEnd
                editor.document.replaceString(start, end, markdown)
                start + markdown.length
            } else {
                editor.document.insertString(editor.caretModel.offset, markdown)
                editor.caretModel.offset + markdown.length
            }
            editor.caretModel.moveToOffset(offset)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }

    private class LinkDialog(initialText: String) : DialogWrapper(true) {
        var linkText: String = initialText
        var url: String = ""
        var titleText: String = ""

        init {
            title = "Insert Link"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row("Text:") {
                    textField()
                        .bindText(::linkText)
                        .focused()
                        .comment("Link display text")
                }
                row("URL:") {
                    textField()
                        .bindText(::url)
                        .comment("https://example.com or relative/path.md")
                }
                row("Title (optional):") {
                    textField()
                        .bindText(::titleText)
                        .comment("Tooltip shown on hover")
                }
            }
        }
    }
}
