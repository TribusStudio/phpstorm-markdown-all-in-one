package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Promotes Markdown All-in-One actions above built-in IDE actions when editing
 * a markdown file, and suppresses conflicting IDE actions (e.g. GotoDeclaration
 * for Cmd+B) so our action is the sole handler.
 *
 * When NOT in a markdown file, our actions are disabled via update() and the
 * built-in IDE actions take precedence as normal.
 */
class MarkdownActionPromoter : ActionPromoter {

    override fun promote(actions: List<AnAction>, context: DataContext): List<AnAction>? {
        val file = context.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        if (!MarkdownFileUtil.isMarkdownFile(file)) return null

        val markdownActions = actions.filter { it is MarkdownAction }
        if (markdownActions.isEmpty()) return null

        return markdownActions + (actions - markdownActions.toSet())
    }

    override fun suppress(actions: List<AnAction>, context: DataContext): List<AnAction> {
        val file = context.getData(CommonDataKeys.VIRTUAL_FILE) ?: return emptyList()
        if (!MarkdownFileUtil.isMarkdownFile(file)) return emptyList()

        val hasMarkdownActions = actions.any { it is MarkdownAction }
        if (!hasMarkdownActions) return emptyList()

        // Suppress all non-markdown actions that conflict with our shortcuts
        return actions.filter { it !is MarkdownAction }
    }
}
