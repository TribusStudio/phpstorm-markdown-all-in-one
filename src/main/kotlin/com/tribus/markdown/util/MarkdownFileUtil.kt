package com.tribus.markdown.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.vfs.VirtualFile

object MarkdownFileUtil {

    private val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd", "mkdn")

    fun isMarkdownFile(e: AnActionEvent): Boolean {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return false
        return isMarkdownFile(file)
    }

    fun isMarkdownFile(file: VirtualFile): Boolean {
        return file.extension?.lowercase() in MARKDOWN_EXTENSIONS
    }

    fun isMarkdownEditor(editor: Editor): Boolean {
        val file = editor.virtualFile ?: return false
        return isMarkdownFile(file)
    }
}
