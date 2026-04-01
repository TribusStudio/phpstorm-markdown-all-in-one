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
 * Opens a dialog to insert a markdown image ![alt](src "title") at the cursor.
 * If text is selected, it's used as the alt text; the dialog pre-fills it.
 */
class InsertImageAction : AnAction(), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val selectedText = editor.selectionModel.selectedText ?: ""

        val dialog = ImageDialog(selectedText)
        if (!dialog.showAndGet()) return

        val altText = dialog.altText.ifBlank { "image" }
        val src = dialog.imageSrc
        val title = dialog.titleText
        val markdown = if (title.isNotBlank()) {
            "![$altText]($src \"$title\")"
        } else {
            "![$altText]($src)"
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

    private class ImageDialog(initialAlt: String) : DialogWrapper(true) {
        var altText: String = initialAlt
        var imageSrc: String = ""
        var titleText: String = ""

        init {
            title = "Insert Image"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row("Alt text:") {
                    textField()
                        .bindText(::altText)
                        .focused()
                        .comment("Descriptive text for accessibility")
                }
                row("Image path/URL:") {
                    textField()
                        .bindText(::imageSrc)
                        .comment("Relative path (images/photo.png) or URL (https://...)")
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
