package com.tribus.markdown.actions

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Promotes Markdown All-in-One actions above built-in IDE actions when editing
 * a markdown file. This allows us to use familiar shortcuts (Cmd+B for bold,
 * Cmd+I for italic, etc.) that would otherwise conflict with IDE actions like
 * "Go to Declaration" or "Find Implementations".
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
}
