package com.tribus.markdown.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * A floating toolbar that appears above text selections in the markdown editor.
 * Shows formatting buttons using the platform's ActionToolbar for consistent
 * styling and behavior.
 *
 * Uses a debounce timer to avoid flicker from rapid selection changes
 * (e.g., double-click word select, shift+arrow expansion).
 */
class FloatingToolbar(private val editor: Editor) : SelectionListener, CaretListener {

    private var popup: JBPopup? = null
    private var showTimer: Timer? = null

    override fun selectionChanged(e: SelectionEvent) {
        // Debounce: wait 200ms after last selection change before showing
        showTimer?.stop()

        if (!editor.selectionModel.hasSelection() || editor.selectionModel.selectedText.isNullOrBlank()) {
            hideToolbar()
            return
        }

        showTimer = Timer(200) {
            SwingUtilities.invokeLater {
                if (editor.selectionModel.hasSelection() && !editor.isDisposed) {
                    showToolbar()
                }
            }
        }.apply {
            isRepeats = false
            start()
        }
    }

    override fun caretPositionChanged(e: CaretEvent) {
        // Dismiss when caret moves without selection (user clicked elsewhere)
        if (!editor.selectionModel.hasSelection()) {
            hideToolbar()
        }
    }

    private fun showToolbar() {
        hideToolbar()

        if (!editor.selectionModel.hasSelection() || editor.isDisposed) return

        val actionManager = ActionManager.getInstance()
        val group = DefaultActionGroup()

        // Formatting actions
        addAction(group, actionManager, "com.tribus.markdown.actions.ToggleBold")
        addAction(group, actionManager, "com.tribus.markdown.actions.ToggleItalic")
        addAction(group, actionManager, "com.tribus.markdown.actions.ToggleStrikethrough")
        addAction(group, actionManager, "com.tribus.markdown.actions.ToggleCodeSpan")
        group.addSeparator()
        addAction(group, actionManager, "com.tribus.markdown.actions.HeadingUp")
        addAction(group, actionManager, "com.tribus.markdown.actions.HeadingDown")
        group.addSeparator()
        addAction(group, actionManager, "com.tribus.markdown.actions.InsertLink")
        addAction(group, actionManager, "com.tribus.markdown.actions.InsertImage")

        val toolbar = actionManager.createActionToolbar("MarkdownFloatingToolbar", group, true)
        toolbar.targetComponent = editor.contentComponent
        val toolbarComponent = toolbar.component

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(toolbarComponent, null)
            .setRequestFocus(false)
            .setFocusable(false)
            .setResizable(false)
            .setMovable(false)
            .setShowBorder(true)
            .setCancelOnClickOutside(false)  // We dismiss via selectionChanged when selection clears
            .setCancelOnOtherWindowOpen(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelKeyEnabled(true)
            .createPopup()

        // Position above the selection start
        val selectionStart = editor.selectionModel.selectionStart
        val visualPos = editor.offsetToVisualPosition(selectionStart)
        val point = editor.visualPositionToXY(visualPos)

        val screenPoint = Point(point.x, point.y - 40)
        if (screenPoint.y < 0) {
            // Not enough room above — show below
            screenPoint.y = point.y + editor.lineHeight + 5
        }

        try {
            popup?.show(RelativePoint(editor.contentComponent, screenPoint))
        } catch (_: Exception) {
            // Component not showing or hierarchy issue — silently skip
            popup = null
        }
    }

    fun hideToolbar() {
        showTimer?.stop()
        popup?.cancel()
        popup = null
    }

    private fun addAction(group: DefaultActionGroup, actionManager: ActionManager, actionId: String) {
        actionManager.getAction(actionId)?.let { group.add(it) }
    }

    fun dispose() {
        hideToolbar()
    }
}
