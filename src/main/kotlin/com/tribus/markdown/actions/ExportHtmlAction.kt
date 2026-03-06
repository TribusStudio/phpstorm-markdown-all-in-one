package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.tribus.markdown.util.MarkdownFileUtil

class ExportHtmlAction : AnAction(), MarkdownAction {
    override fun actionPerformed(e: AnActionEvent) {
        // TODO: Phase 6 implementation
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = MarkdownFileUtil.isMarkdownFile(e)
    }
}
