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
import com.tribus.markdown.settings.MarkdownSettings
import java.awt.Point
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * A floating toolbar that appears above text selections in the markdown editor.
 * Shows context-sensitive actions based on what the selection contains.
 *
 * Uses JBPopup with a 200ms debounce. Registered via the editorFactoryListener
 * extension point. Dismissed when the selection clears.
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
        if (!editor.selectionModel.hasSelection()) {
            hideToolbar()
        }
    }

    private fun showToolbar() {
        hideToolbar()

        if (!editor.selectionModel.hasSelection() || editor.isDisposed) return
        val contentComponent = editor.contentComponent
        if (!contentComponent.isShowing) return

        val contextSensitive = try {
            MarkdownSettings.getInstance().state.contextSensitiveToolbar
        } catch (_: Exception) { true }

        val context = if (contextSensitive) SelectionContext.detect(editor) else SelectionContext.Context.DEFAULT
        val group = buildActionGroup(context)

        val actionManager = ActionManager.getInstance()
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

    private fun buildActionGroup(context: SelectionContext.Context): DefaultActionGroup {
        val am = ActionManager.getInstance()
        val group = DefaultActionGroup()

        when (context) {
            SelectionContext.Context.TOC -> {
                add(group, am, "com.tribus.markdown.actions.UpdateToc")
                add(group, am, "com.tribus.markdown.actions.AddSectionNumbers")
                add(group, am, "com.tribus.markdown.actions.RemoveSectionNumbers")
            }

            SelectionContext.Context.TABLE -> {
                add(group, am, "com.tribus.markdown.actions.FormatTable")
                group.addSeparator()
                add(group, am, "com.tribus.markdown.table.InsertRowAbove")
                add(group, am, "com.tribus.markdown.table.InsertRowBelow")
                add(group, am, "com.tribus.markdown.table.DeleteRow")
                group.addSeparator()
                add(group, am, "com.tribus.markdown.table.InsertColumnBefore")
                add(group, am, "com.tribus.markdown.table.InsertColumnAfter")
                add(group, am, "com.tribus.markdown.table.DeleteColumn")
            }

            SelectionContext.Context.CODE_BLOCK -> {
                add(group, am, "com.tribus.markdown.actions.ToggleCodeBlock")
            }

            SelectionContext.Context.MATH -> {
                add(group, am, "com.tribus.markdown.actions.ToggleMath")
                add(group, am, "com.tribus.markdown.actions.ToggleMathReverse")
            }

            SelectionContext.Context.DEFAULT -> {
                add(group, am, "com.tribus.markdown.actions.ToggleBold")
                add(group, am, "com.tribus.markdown.actions.ToggleItalic")
                add(group, am, "com.tribus.markdown.actions.ToggleStrikethrough")
                add(group, am, "com.tribus.markdown.actions.ToggleCodeSpan")
                group.addSeparator()
                add(group, am, "com.tribus.markdown.actions.HeadingUp")
                add(group, am, "com.tribus.markdown.actions.HeadingDown")
                add(group, am, "com.tribus.markdown.actions.ToggleBlockquote")
                group.addSeparator()
                add(group, am, "com.tribus.markdown.actions.InsertLink")
                add(group, am, "com.tribus.markdown.actions.InsertImage")
            }
        }

        return group
    }

    private fun add(group: DefaultActionGroup, actionManager: ActionManager, actionId: String) {
        actionManager.getAction(actionId)?.let { group.add(it) }
    }

    fun dispose() {
        hideToolbar()
    }
}
