package com.tribus.markdown.toolbar

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * A floating toolbar that appears above text selections in the markdown editor.
 *
 * Adds the toolbar as a child of the editor's scroll pane, positioned
 * absolutely over the editor content. This avoids conflicts with IntelliJ's
 * popup/hint/intention systems which dismiss JBPopup instances.
 */
class FloatingToolbar(private val editor: Editor) : SelectionListener, CaretListener {

    private var overlayPanel: JPanel? = null
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

        val editorComponent = editor.contentComponent
        val scrollPane = editor.scrollingModel.visibleArea

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
        toolbar.targetComponent = editorComponent
        val toolbarComponent = toolbar.component

        // Wrap in a panel with a visible border and background
        val wrapper = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0))
        wrapper.isOpaque = true
        wrapper.border = JBUI.Borders.customLine(JBUI.CurrentTheme.Editor.BORDER_COLOR, 1)
        wrapper.add(toolbarComponent)

        // Calculate position: above the selection start
        val selectionStart = editor.selectionModel.selectionStart
        val visualPos = editor.offsetToVisualPosition(selectionStart)
        val editorPoint = editor.visualPositionToXY(visualPos)

        val prefSize = wrapper.preferredSize

        // Position in editor coordinate space (relative to content component)
        var x = editorPoint.x
        var y = editorPoint.y - prefSize.height - 6

        // If above would be out of the visible area, show below
        if (y < scrollPane.y) {
            y = editorPoint.y + editor.lineHeight + 6
        }

        // Clamp to visible area
        x = x.coerceIn(scrollPane.x, (scrollPane.x + scrollPane.width - prefSize.width).coerceAtLeast(scrollPane.x))

        wrapper.setBounds(x, y, prefSize.width, prefSize.height)

        // Add directly to the editor content component's parent (the JViewport/scroll pane)
        // Using the content component itself as the container with null layout overlay
        editorComponent.add(wrapper)
        editorComponent.revalidate()
        editorComponent.repaint()

        overlayPanel = wrapper
    }

    fun hideToolbar() {
        showTimer?.stop()
        val panel = overlayPanel ?: return
        val parent = panel.parent
        if (parent != null) {
            parent.remove(panel)
            parent.revalidate()
            parent.repaint()
        }
        overlayPanel = null
    }

    private fun addAction(group: DefaultActionGroup, actionManager: ActionManager, actionId: String) {
        actionManager.getAction(actionId)?.let { group.add(it) }
    }

    fun dispose() {
        hideToolbar()
    }
}
