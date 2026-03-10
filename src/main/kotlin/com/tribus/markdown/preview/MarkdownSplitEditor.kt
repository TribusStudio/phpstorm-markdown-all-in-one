package com.tribus.markdown.preview

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.tribus.markdown.lang.MarkdownIcons
import com.tribus.markdown.settings.MarkdownSettings
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Split editor combining a text editor with a markdown preview.
 * Embeds the formatting toolbar directly — no EditorNotificationProvider —
 * so the toolbar appears instantly when the editor opens.
 * Also wires up bidirectional scroll synchronization between editor and preview.
 */
class MarkdownSplitEditor(
    private val project: Project,
    editor: TextEditor,
    private val preview: MarkdownPreviewFileEditor
) : TextEditorWithPreview(editor, preview, "Markdown Editor", Layout.SHOW_EDITOR_AND_PREVIEW) {

    private var toolbarPanel: JPanel? = null
    private var settingsListener: MarkdownSettings.ChangeListener? = null

    // Scroll sync state
    @Volatile private var scrollingFromEditor = false
    @Volatile private var scrollingFromPreview = false
    private var scrollTimer: Timer? = null

    private val wrapperPanel: JPanel by lazy {
        val wrapper = JPanel(BorderLayout())

        val settings = try { MarkdownSettings.getInstance().state } catch (_: Exception) { null }
        val tp = createToolbar((textEditor as TextEditor).editor, settings)
        toolbarPanel = tp
        wrapper.add(tp, BorderLayout.NORTH)
        wrapper.add(super.getComponent(), BorderLayout.CENTER)

        // Listen for settings changes to toggle toolbar visibility
        settingsListener = MarkdownSettings.ChangeListener { state ->
            toolbarPanel?.isVisible = state.toolbarEnabled
        }
        try {
            MarkdownSettings.getInstance().addChangeListener(settingsListener!!)
        } catch (_: Exception) {}

        // Wire up scroll sync
        setupScrollSync((textEditor as TextEditor).editor)

        wrapper
    }

    override fun getComponent(): JComponent = wrapperPanel

    fun getPreviewEditor(): MarkdownPreviewFileEditor = preview

    // ── Toolbar ──────────────────────────────────────────────────────────

    private fun createToolbar(editor: Editor, settings: MarkdownSettings.State?): JPanel {
        val toolbar = JPanel(BorderLayout())

        if (settings?.toolbarEnabled == false) {
            toolbar.isVisible = false
            return toolbar
        }

        val displayMode = settings?.toolbarDisplayMode ?: "icons"
        toolbar.border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.Editor.BORDER_COLOR)

        // ── Left side: grouped action buttons ──────────────────────
        val leftPanel = JPanel()
        leftPanel.layout = BoxLayout(leftPanel, BoxLayout.X_AXIS)
        leftPanel.isOpaque = false
        leftPanel.border = JBUI.Borders.empty(1, 2)

        // Group 1: Text formatting
        addButton(leftPanel, "Bold", MarkdownIcons.TOOLBAR_BOLD, displayMode,
            "com.tribus.markdown.actions.ToggleBold", editor)
        addButton(leftPanel, "Italic", MarkdownIcons.TOOLBAR_ITALIC, displayMode,
            "com.tribus.markdown.actions.ToggleItalic", editor)
        addButton(leftPanel, "Strikethrough", MarkdownIcons.TOOLBAR_STRIKETHROUGH, displayMode,
            "com.tribus.markdown.actions.ToggleStrikethrough", editor)
        addButton(leftPanel, "Code", MarkdownIcons.TOOLBAR_CODE, displayMode,
            "com.tribus.markdown.actions.ToggleCodeSpan", editor)
        addButton(leftPanel, "Code Block", MarkdownIcons.TOOLBAR_CODE, displayMode,
            "com.tribus.markdown.actions.ToggleCodeBlock", editor)

        addSeparator(leftPanel)

        // Group 2: Structure (headings)
        addButton(leftPanel, "H+", MarkdownIcons.TOOLBAR_HEADING_UP, displayMode,
            "com.tribus.markdown.actions.HeadingUp", editor)
        addButton(leftPanel, "H-", MarkdownIcons.TOOLBAR_HEADING_DOWN, displayMode,
            "com.tribus.markdown.actions.HeadingDown", editor)

        addSeparator(leftPanel)

        // Group 3: Lists
        addButton(leftPanel, "Indent", MarkdownIcons.TOOLBAR_INDENT, displayMode,
            "com.tribus.markdown.actions.ListIndent", editor)
        addButton(leftPanel, "Outdent", MarkdownIcons.TOOLBAR_OUTDENT, displayMode,
            "com.tribus.markdown.actions.ListOutdent", editor)
        addButton(leftPanel, "Task", MarkdownIcons.TOOLBAR_TASK, displayMode,
            "com.tribus.markdown.actions.ToggleTaskList", editor)

        addSeparator(leftPanel)

        // Group 4: Tables & TOC
        addButton(leftPanel, "Format Table", MarkdownIcons.TOOLBAR_TABLE, displayMode,
            "com.tribus.markdown.actions.FormatTable", editor)
        addButton(leftPanel, "Update Table of Contents", MarkdownIcons.TOOLBAR_TOC, displayMode,
            "com.tribus.markdown.actions.UpdateToc", editor)

        toolbar.add(leftPanel, BorderLayout.WEST)

        // ── Right side: tools menu + settings gear ─────────────────
        val rightPanel = JPanel()
        rightPanel.layout = BoxLayout(rightPanel, BoxLayout.X_AXIS)
        rightPanel.isOpaque = false
        rightPanel.border = JBUI.Borders.empty(1, 2)

        val toolsButton = createHoverButton(AllIcons.Actions.More, null, "icons")
        toolsButton.toolTipText = "Markdown Tools"
        toolsButton.addActionListener { showToolsMenu(toolsButton, editor) }
        rightPanel.add(toolsButton)

        rightPanel.add(Box.createRigidArea(Dimension(2, 0)))

        val settingsButton = createHoverButton(AllIcons.General.GearPlain, null, "icons")
        settingsButton.toolTipText = "Markdown All-in-One Settings"
        settingsButton.addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Markdown All-in-One")
        }
        rightPanel.add(settingsButton)

        toolbar.add(rightPanel, BorderLayout.EAST)

        return toolbar
    }

    private fun addSeparator(panel: JPanel) {
        panel.add(Box.createRigidArea(Dimension(4, 0)))
        val sep = JSeparator(SwingConstants.VERTICAL)
        sep.maximumSize = Dimension(1, 22)
        panel.add(sep)
        panel.add(Box.createRigidArea(Dimension(4, 0)))
    }

    private fun showToolsMenu(anchor: JComponent, editor: Editor) {
        val popup = JPopupMenu()

        popup.add(createMenuItem("Update TOC", "com.tribus.markdown.actions.UpdateToc", editor))
        popup.add(createMenuItem("Create TOC", "com.tribus.markdown.actions.CreateToc", editor))
        popup.addSeparator()
        popup.add(createMenuItem("Format Table at Cursor", "com.tribus.markdown.actions.FormatTable", editor))
        popup.add(createMenuItem("Format All Tables", "com.tribus.markdown.actions.FormatAllTables", editor))
        popup.addSeparator()
        popup.add(createMenuItem("Add Section Numbers", "com.tribus.markdown.actions.AddSectionNumbers", editor))
        popup.add(createMenuItem("Remove Section Numbers", "com.tribus.markdown.actions.RemoveSectionNumbers", editor))
        popup.addSeparator()
        popup.add(createMenuItem("Export to HTML", "com.tribus.markdown.actions.ExportHtml", editor))
        popup.add(createMenuItem("Batch Export to HTML", "com.tribus.markdown.actions.BatchExportHtml", editor))

        popup.show(anchor, 0, anchor.height)
    }

    private fun createMenuItem(label: String, actionId: String, editor: Editor): JMenuItem {
        val item = JMenuItem(label)
        val actionManager = ActionManager.getInstance()
        val action = actionManager.getAction(actionId)
        item.isEnabled = action != null

        item.addActionListener {
            action ?: return@addActionListener
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
        return item
    }

    private fun addButton(
        panel: JPanel,
        label: String,
        icon: Icon,
        displayMode: String,
        actionId: String,
        editor: Editor
    ) {
        val button = createHoverButton(icon, label, displayMode)
        button.toolTipText = label

        button.addActionListener {
            val actionManager = ActionManager.getInstance()
            val action = actionManager.getAction(actionId) ?: return@addActionListener
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

        panel.add(button)
    }

    // ── Scroll Sync ──────────────────────────────────────────────────────

    private fun setupScrollSync(editor: Editor) {
        // Editor → Preview: listen for visible area changes
        editor.scrollingModel.addVisibleAreaListener { e ->
            if (scrollingFromPreview) return@addVisibleAreaListener

            val scrollEnabled = try {
                MarkdownSettings.getInstance().state.scrollSyncEnabled
            } catch (_: Exception) { true }
            if (!scrollEnabled) return@addVisibleAreaListener

            val topLine = editor.xyToLogicalPosition(Point(0, e.newRectangle.y)).line
            scrollingFromEditor = true
            preview.scrollToSourceLine(topLine)

            // Reset flag after the preview scroll settles
            scrollTimer?.stop()
            scrollTimer = Timer(200) { scrollingFromEditor = false }
            scrollTimer?.isRepeats = false
            scrollTimer?.start()
        }

        // Preview → Editor: receive source line from JS callback
        preview.setOnScrollCallback { line ->
            if (scrollingFromEditor) return@setOnScrollCallback

            scrollingFromPreview = true
            SwingUtilities.invokeLater {
                if (!editor.isDisposed) {
                    editor.scrollingModel.scrollTo(
                        LogicalPosition(line, 0),
                        ScrollType.MAKE_VISIBLE
                    )
                }
                // Reset flag after the editor scroll settles
                Timer(200) { scrollingFromPreview = false }.apply {
                    isRepeats = false
                    start()
                }
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun dispose() {
        settingsListener?.let { listener ->
            try {
                MarkdownSettings.getInstance().removeChangeListener(listener)
            } catch (_: Exception) {}
        }
        scrollTimer?.stop()
        super.dispose()
    }

    companion object {
        private val HOVER_BG = Color(128, 128, 128, 40)

        fun createHoverButton(icon: Icon, label: String?, displayMode: String): JButton {
            val button = when (displayMode) {
                "labels" -> JButton(label)
                "icons and labels" -> JButton(label, icon)
                else -> JButton(icon)
            }

            button.isFocusable = false
            button.isBorderPainted = false
            button.isContentAreaFilled = false
            button.isOpaque = false
            button.margin = JBUI.insetsLeft(0)
            button.border = JBUI.Borders.empty(2, 3)

            val size = if (displayMode == "icons") Dimension(24, 24) else null
            if (size != null) {
                button.preferredSize = size
                button.minimumSize = size
                button.maximumSize = size
            }

            button.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    if (button.isEnabled) {
                        button.isContentAreaFilled = true
                        button.background = HOVER_BG
                        button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    }
                }
                override fun mouseExited(e: MouseEvent) {
                    button.isContentAreaFilled = false
                    button.cursor = Cursor.getDefaultCursor()
                }
            })

            return button
        }
    }
}
