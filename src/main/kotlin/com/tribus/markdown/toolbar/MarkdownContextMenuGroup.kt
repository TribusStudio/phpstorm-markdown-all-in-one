package com.tribus.markdown.toolbar

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Separator
import com.tribus.markdown.table.TableParser
import com.tribus.markdown.toc.TocGenerator
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Context menu group that provides context-aware markdown actions on right-click.
 * Shows different actions depending on cursor position and selection state.
 */
class MarkdownContextMenuGroup : ActionGroup("Markdown", true) {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        if (e == null) return EMPTY_ARRAY
        if (!MarkdownFileUtil.isMarkdownFile(e)) return EMPTY_ARRAY

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return EMPTY_ARRAY
        val actionManager = ActionManager.getInstance()
        val actions = mutableListOf<AnAction>()

        val hasSelection = editor.selectionModel.hasSelection()
        val caretLine = editor.caretModel.logicalPosition.line
        val docText = editor.document.text

        // Always show formatting actions when there's a selection
        if (hasSelection) {
            actions.add(actionManager.getAction("com.tribus.markdown.actions.ToggleBold"))
            actions.add(actionManager.getAction("com.tribus.markdown.actions.ToggleItalic"))
            actions.add(actionManager.getAction("com.tribus.markdown.actions.ToggleStrikethrough"))
            actions.add(actionManager.getAction("com.tribus.markdown.actions.ToggleCodeSpan"))
            actions.add(actionManager.getAction("com.tribus.markdown.actions.ToggleCodeBlock"))
            actions.add(Separator.getInstance())
        }

        // Heading actions (always available)
        actions.add(actionManager.getAction("com.tribus.markdown.actions.HeadingUp"))
        actions.add(actionManager.getAction("com.tribus.markdown.actions.HeadingDown"))
        actions.add(Separator.getInstance())

        // List actions
        actions.add(actionManager.getAction("com.tribus.markdown.actions.ListIndent"))
        actions.add(actionManager.getAction("com.tribus.markdown.actions.ListOutdent"))
        actions.add(actionManager.getAction("com.tribus.markdown.actions.ToggleTaskList"))
        actions.add(Separator.getInstance())

        // Table actions — show if cursor is in a table
        val inTable = TableParser.findTableAt(docText, caretLine) != null
        if (inTable) {
            actions.add(actionManager.getAction("com.tribus.markdown.actions.FormatTable"))
            actions.add(Separator.getInstance())
        }
        actions.add(actionManager.getAction("com.tribus.markdown.actions.FormatAllTables"))

        // TOC actions
        val hasToc = TocGenerator.findAllTocBlocks(docText).isNotEmpty()
        actions.add(Separator.getInstance())
        actions.add(actionManager.getAction("com.tribus.markdown.actions.CreateToc"))
        if (hasToc) {
            actions.add(actionManager.getAction("com.tribus.markdown.actions.UpdateToc"))
        }

        return actions.filterNotNull().toTypedArray()
    }

}
