package com.tribus.markdown.toolbar

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import java.awt.Point
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * A floating toolbar that appears above text selections in the markdown editor.
 *
 * Uses JBPopup with a longer debounce (500ms) to avoid conflicting with
 * IntelliJ's intention lightbulb system, which appears ~300ms after caret
 * movement. The popup uses setCancelOnClickOutside(false) so it doesn't
 * get dismissed by the lightbulb's appearance; instead, it's dismissed
 * explicitly when the selection clears.
 */
class FloatingToolbar(private val editor: Editor) : SelectionListener, CaretListener {

    private var popup: JBPopup? = null
    private var showTimer: Timer? = null

    override fun selectionChanged(e: SelectionEvent) {
        showTimer?.stop()

        if (!editor.selectionModel.hasSelection() || editor.selectionModel.selectedText.isNullOrBlank()) {
            hideToolbar()
            return
        }

        // 500ms debounce — longer than the intention lightbulb (~300ms)
        showTimer = Timer(500) {
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
        if (!editor.selectionModel.hasSelection()) {
            hideToolbar()
        }
    }

    private fun showToolbar() {
        hideToolbar()

        if (!editor.selectionModel.hasSelection() || editor.isDisposed) return
        val contentComponent = editor.contentComponent
        if (!contentComponent.isShowing) return

        val actionManager = ActionManager.getInstance()
        val group = DefaultActionGroup()

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
        toolbar.targetComponent = contentComponent
        val toolbarComponent = toolbar.component
        toolbarComponent.border = JBUI.Borders.customLine(JBUI.CurrentTheme.Editor.BORDER_COLOR, 1)

        val newPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(toolbarComponent, null)
            .setRequestFocus(false)
            .setFocusable(false)
            .setResizable(false)
            .setMovable(false)
            .setShowBorder(false)
            .setCancelOnClickOutside(false)
            .setCancelOnOtherWindowOpen(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelKeyEnabled(false)
            .createPopup()

        // Position above the selection start
        val selectionStart = editor.selectionModel.selectionStart
        val visualPos = editor.offsetToVisualPosition(selectionStart)
        val editorPoint = editor.visualPositionToXY(visualPos)

        val screenPoint = Point(editorPoint.x, editorPoint.y - 40)
        if (screenPoint.y < editor.scrollingModel.visibleArea.y) {
            screenPoint.y = editorPoint.y + editor.lineHeight + 5
        }

        try {
            newPopup.show(RelativePoint(contentComponent, screenPoint))
            popup = newPopup
        } catch (_: Exception) {
            newPopup.cancel()
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
