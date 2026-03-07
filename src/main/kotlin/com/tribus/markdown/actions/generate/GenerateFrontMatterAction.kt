package com.tribus.markdown.actions.generate

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.tribus.markdown.actions.MarkdownAction
import com.tribus.markdown.util.MarkdownFileUtil
import java.time.LocalDate

/**
 * Inserts YAML front matter at the top of the document.
 */
class GenerateFrontMatterAction : AnAction("Front Matter", "Insert YAML front matter", null), MarkdownAction {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val document = editor.document

        val today = LocalDate.now().toString()
        val frontMatter = """---
title: "${document.getText(com.intellij.openapi.util.TextRange(0, minOf(document.textLength, 100))).lines().firstOrNull()?.removePrefix("# ")?.trim() ?: "Untitled"}"
date: $today
tags: []
---

"""

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(0, frontMatter)
            editor.caretModel.moveToOffset(frontMatter.length)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = MarkdownFileUtil.isMarkdownFile(e)
    }
}
