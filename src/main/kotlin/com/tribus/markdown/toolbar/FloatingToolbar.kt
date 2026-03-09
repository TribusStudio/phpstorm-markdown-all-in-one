package com.tribus.markdown.toolbar

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.tribus.markdown.lang.MarkdownIcons
import java.awt.FlowLayout
import java.awt.Dimension
import java.awt.Point
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * A floating toolbar that appears above text selections in the markdown editor.
 * Shows formatting buttons with icons in a compact horizontal popup.
 */
class FloatingToolbar(private val editor: Editor) : SelectionListener {

    private var popup: JBPopup? = null

    data class ToolbarAction(
        val icon: Icon,
        val tooltip: String,
        val actionId: String
    )

    private val actions = listOf(
        ToolbarAction(MarkdownIcons.TOOLBAR_BOLD, "Bold (Cmd+B)", "com.tribus.markdown.actions.ToggleBold"),
        ToolbarAction(MarkdownIcons.TOOLBAR_ITALIC, "Italic (Cmd+I)", "com.tribus.markdown.actions.ToggleItalic"),
        ToolbarAction(MarkdownIcons.TOOLBAR_STRIKETHROUGH, "Strikethrough (Alt+S)", "com.tribus.markdown.actions.ToggleStrikethrough"),
        ToolbarAction(MarkdownIcons.TOOLBAR_CODE, "Code Span", "com.tribus.markdown.actions.ToggleCodeSpan"),
        ToolbarAction(MarkdownIcons.TOOLBAR_HEADING_UP, "Heading Up", "com.tribus.markdown.actions.HeadingUp"),
        ToolbarAction(MarkdownIcons.TOOLBAR_HEADING_DOWN, "Heading Down", "com.tribus.markdown.actions.HeadingDown"),
        ToolbarAction(MarkdownIcons.TOOLBAR_TASK, "Toggle Task", "com.tribus.markdown.actions.ToggleTaskList"),
    )

    override fun selectionChanged(e: SelectionEvent) {
        val selectionModel = editor.selectionModel
        if (selectionModel.hasSelection() && selectionModel.selectedText?.isNotBlank() == true) {
            SwingUtilities.invokeLater { showToolbar() }
        } else {
            hideToolbar()
        }
    }

    private fun showToolbar() {
        hideToolbar()

        if (!editor.selectionModel.hasSelection()) return

        val panel = createToolbarPanel()
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, null)
            .setRequestFocus(false)
            .setFocusable(false)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setCancelOnWindowDeactivation(true)
            .createPopup()

        val selectionStart = editor.selectionModel.selectionStart
        val visualPos = editor.offsetToVisualPosition(selectionStart)
        val point = editor.visualPositionToXY(visualPos)
        val editorComponent = editor.contentComponent

        // Position above the selection
        val screenPoint = Point(point.x, point.y - 35)
        if (screenPoint.y < 0) screenPoint.y = point.y + editor.lineHeight + 5

        popup?.show(RelativePoint(editorComponent, screenPoint))
    }

    fun hideToolbar() {
        popup?.cancel()
        popup = null
    }

    private fun createToolbarPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.CENTER, 2, 2))
        panel.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
        panel.isOpaque = true

        for (action in actions) {
            val button = JButton(action.icon)
            button.toolTipText = action.tooltip
            button.preferredSize = Dimension(28, 28)
            button.isFocusable = false
            button.addActionListener { executeAction(action.actionId) }
            panel.add(button)
        }

        return panel
    }

    private fun executeAction(actionId: String) {
        val actionManager = ActionManager.getInstance()
        val action = actionManager.getAction(actionId) ?: return
        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.EDITOR, editor)
            .add(CommonDataKeys.PROJECT, editor.project)
            .build()
        val event = AnActionEvent(
            null,
            dataContext,
            ActionPlaces.EDITOR_TOOLBAR,
            Presentation(),
            actionManager,
            0
        )
        action.actionPerformed(event)
    }

    fun dispose() {
        hideToolbar()
    }
}
