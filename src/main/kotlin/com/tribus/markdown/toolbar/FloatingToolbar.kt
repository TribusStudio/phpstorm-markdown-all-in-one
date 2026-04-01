package com.tribus.markdown.toolbar

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * A floating toolbar that appears above text selections in the markdown editor.
 *
 * Instead of using JBPopup (which conflicts with IntelliJ's intention/lightbulb
 * system), this adds the toolbar directly to the editor's parent JLayeredPane
 * as an overlay. This avoids popup conflicts and provides reliable positioning.
 *
 * Uses a debounce timer to avoid flicker from rapid selection changes.
 */
class FloatingToolbar(private val editor: Editor) : SelectionListener, CaretListener {

    private var toolbarComponent: JComponent? = null
    private var showTimer: Timer? = null

    override fun selectionChanged(e: SelectionEvent) {
        showTimer?.stop()

        if (!editor.selectionModel.hasSelection() || editor.selectionModel.selectedText.isNullOrBlank()) {
            hideToolbar()
            return
        }

        showTimer = Timer(300) {
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

        val layeredPane = findLayeredPane() ?: return

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
        toolbar.targetComponent = editor.contentComponent
        val component = toolbar.component

        // Measure the toolbar's preferred size
        val prefSize = component.preferredSize

        // Position above the selection start, converted to layered pane coordinates
        val selectionStart = editor.selectionModel.selectionStart
        val visualPos = editor.offsetToVisualPosition(selectionStart)
        val editorPoint = editor.visualPositionToXY(visualPos)

        val editorComponent = editor.contentComponent
        val pointInLayered = SwingUtilities.convertPoint(editorComponent, editorPoint, layeredPane)

        var x = pointInLayered.x
        var y = pointInLayered.y - prefSize.height - 4

        // Keep within bounds
        if (y < 0) {
            y = pointInLayered.y + editor.lineHeight + 4
        }
        if (x + prefSize.width > layeredPane.width) {
            x = layeredPane.width - prefSize.width
        }
        x = x.coerceAtLeast(0)

        component.setBounds(x, y, prefSize.width, prefSize.height)
        layeredPane.add(component, JLayeredPane.POPUP_LAYER)
        layeredPane.revalidate()
        layeredPane.repaint()

        toolbarComponent = component
    }

    fun hideToolbar() {
        showTimer?.stop()
        val component = toolbarComponent ?: return
        val parent = component.parent
        if (parent != null) {
            parent.remove(component)
            parent.revalidate()
            parent.repaint()
        }
        toolbarComponent = null
    }

    private fun findLayeredPane(): JLayeredPane? {
        var comp: java.awt.Component? = editor.contentComponent
        while (comp != null) {
            if (comp is JLayeredPane) return comp
            comp = comp.parent
        }
        return null
    }

    private fun addAction(group: DefaultActionGroup, actionManager: ActionManager, actionId: String) {
        actionManager.getAction(actionId)?.let { group.add(it) }
    }

    fun dispose() {
        hideToolbar()
    }
}
