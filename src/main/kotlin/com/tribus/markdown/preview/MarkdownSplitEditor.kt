package com.tribus.markdown.preview

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.tribus.markdown.settings.MarkdownSettings
import java.awt.BorderLayout
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Split editor combining a text editor with a markdown preview.
 *
 * The formatting toolbar is embedded inside the editor pane (not spanning
 * the full split width). This ensures the toolbar is always associated with
 * the editor context — actions resolve correctly regardless of which pane
 * has focus, and the toolbar naturally hides in preview-only mode.
 *
 * The toolbar is injected by wrapping the TextEditor with [ToolbarTextEditor],
 * which overrides [getComponent] to return toolbar + editor. When
 * TextEditorWithPreview builds its splitter, it picks up the wrapped component.
 */
class MarkdownSplitEditor(
    private val project: Project,
    editor: TextEditor,
    private val preview: MarkdownPreviewFileEditor
) : TextEditorWithPreview(
    ToolbarTextEditor(editor, project),
    preview,
    "Markdown Editor",
    Layout.SHOW_EDITOR_AND_PREVIEW
) {

    private val wrappedEditor = textEditor as ToolbarTextEditor
    private var settingsListener: MarkdownSettings.ChangeListener? = null

    // Scroll sync state
    @Volatile private var scrollingFromEditor = false
    @Volatile private var scrollingFromPreview = false
    private var scrollTimer: Timer? = null

    init {
        // Listen for settings changes to toggle toolbar visibility
        settingsListener = MarkdownSettings.ChangeListener { state ->
            wrappedEditor.toolbarPanel.isVisible = state.toolbarEnabled
        }
        try {
            MarkdownSettings.getInstance().addChangeListener(settingsListener!!)
        } catch (_: Exception) {}

        // Wire up scroll sync
        setupScrollSync(wrappedEditor.editor)
    }

    fun getPreviewEditor(): MarkdownPreviewFileEditor = preview

    // ── Scroll Sync ──────────────────────────────────────────────────────

    private fun setupScrollSync(editor: Editor) {
        // Editor → Preview: listen for visible area changes
        editor.scrollingModel.addVisibleAreaListener { e ->
            if (scrollingFromPreview) return@addVisibleAreaListener

            val topLine = editor.xyToLogicalPosition(Point(0, e.newRectangle.y)).line

            // Always track the current visible line so preview updates can restore position
            preview.lastVisibleSourceLine = topLine

            val scrollEnabled = try {
                MarkdownSettings.getInstance().state.scrollSyncEnabled
            } catch (_: Exception) { true }
            if (!scrollEnabled) return@addVisibleAreaListener

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
}

/**
 * Wraps a [TextEditor] to inject the formatting toolbar above the editor component.
 *
 * Delegates all [TextEditor] methods to the original editor. Only [getComponent]
 * is overridden to return a panel with the toolbar at the top and the editor below.
 * This way, [TextEditorWithPreview]'s internal splitter places the toolbar inside
 * the editor pane — not spanning the full split width.
 */
class ToolbarTextEditor(
    private val delegate: TextEditor,
    private val project: Project
) : TextEditor by delegate {

    val toolbarPanel: JPanel

    private val wrappedComponent: JPanel = JPanel(BorderLayout())

    init {
        val settings = try { MarkdownSettings.getInstance().state } catch (_: Exception) { null }
        toolbarPanel = createToolbar(delegate.editor, settings)

        wrappedComponent.add(toolbarPanel, BorderLayout.NORTH)
        wrappedComponent.add(delegate.component, BorderLayout.CENTER)
    }

    override fun getComponent(): JComponent = wrappedComponent

    override fun getPreferredFocusedComponent(): JComponent? = delegate.preferredFocusedComponent

    // ── Explicit overrides for Java default methods ──────────────────────
    // Kotlin's `by` delegation only generates overrides for abstract methods.
    // FileEditor has several Java default methods that must be forwarded
    // explicitly, otherwise the wrapper inherits the defaults (which return
    // null / no-op) instead of delegating to the real editor.

    override fun getFile(): VirtualFile = delegate.file

    override fun getStructureViewBuilder() = delegate.structureViewBuilder

    override fun getState(level: FileEditorStateLevel): FileEditorState = delegate.getState(level)

    override fun setState(state: FileEditorState, exactState: Boolean) = delegate.setState(state, exactState)

    override fun getBackgroundHighlighter() = delegate.backgroundHighlighter

    override fun getCurrentLocation() = delegate.currentLocation

    override fun selectNotify() = delegate.selectNotify()

    override fun deselectNotify() = delegate.deselectNotify()

    // ── Toolbar ──────────────────────────────────────────────────────────

    private fun createToolbar(editor: Editor, settings: MarkdownSettings.State?): JPanel {
        val toolbar = JPanel(BorderLayout())

        if (settings?.toolbarEnabled == false) {
            toolbar.isVisible = false
            return toolbar
        }

        toolbar.border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.Editor.BORDER_COLOR)

        val actionManager = ActionManager.getInstance()

        // ── Left side: grouped action buttons using ActionToolbar ──
        val leftGroup = DefaultActionGroup()

        // Group 1: Text formatting
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleBold")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleItalic")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleStrikethrough")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleCodeSpan")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleCodeBlock")

        leftGroup.addSeparator()

        // Group 2: Structure (headings)
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.HeadingUp")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.HeadingDown")

        leftGroup.addSeparator()

        // Group 3: Blockquote & Lists
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleBlockquote")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleList")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ListIndent")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ListOutdent")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleTaskList")

        leftGroup.addSeparator()

        // Group 4: Math
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.ToggleMath")

        leftGroup.addSeparator()

        // Group 5: Tables & TOC
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.FormatTable")
        addRegisteredAction(leftGroup, actionManager, "com.tribus.markdown.actions.UpdateToc")

        val leftToolbar = actionManager.createActionToolbar(
            ActionPlaces.EDITOR_TOOLBAR, leftGroup, true
        )
        leftToolbar.targetComponent = editor.contentComponent
        toolbar.add(leftToolbar.component, BorderLayout.WEST)

        // ── Right side: tools popup + settings gear ────────────────
        val rightGroup = DefaultActionGroup()

        // Tools popup menu
        val toolsPopupGroup = DefaultActionGroup("Markdown Tools", true)
        toolsPopupGroup.templatePresentation.icon = AllIcons.Actions.More
        toolsPopupGroup.templatePresentation.description = "Markdown Tools"

        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.UpdateToc")
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.CreateToc")
        toolsPopupGroup.addSeparator()
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.FormatTable")
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.FormatAllTables")
        toolsPopupGroup.addSeparator()
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.AddSectionNumbers")
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.RemoveSectionNumbers")
        toolsPopupGroup.addSeparator()
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.ToggleMath")
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.ToggleMathReverse")
        toolsPopupGroup.addSeparator()
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.ExportHtml")
        addRegisteredAction(toolsPopupGroup, actionManager, "com.tribus.markdown.actions.BatchExportHtml")

        rightGroup.add(toolsPopupGroup)

        // Settings gear
        rightGroup.add(object : DumbAwareAction("Settings", "Markdown All-in-One Settings", AllIcons.General.GearPlain) {
            override fun actionPerformed(e: AnActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Markdown All-in-One")
            }
        })

        val rightToolbar = actionManager.createActionToolbar(
            ActionPlaces.EDITOR_TOOLBAR, rightGroup, true
        )
        rightToolbar.targetComponent = editor.contentComponent
        toolbar.add(rightToolbar.component, BorderLayout.EAST)

        return toolbar
    }

    private fun addRegisteredAction(group: DefaultActionGroup, actionManager: ActionManager, actionId: String) {
        val action = actionManager.getAction(actionId)
        if (action != null) {
            group.add(action)
        }
    }
}
