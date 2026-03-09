package com.tribus.markdown.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.JBUI
import com.tribus.markdown.lang.MarkdownIcons
import com.tribus.markdown.preview.MarkdownPreviewFileEditor
import com.tribus.markdown.preview.MarkdownSplitEditor
import com.tribus.markdown.settings.MarkdownSettings
import com.tribus.markdown.util.MarkdownFileUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.HierarchyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.function.Function
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

/**
 * Provides a toolbar at the top of markdown editors with quick-access buttons
 * for formatting, structure, lists, tables/TOC, a tools popup menu, and settings.
 *
 * The toolbar re-creates itself automatically when the editor hierarchy changes
 * (window move, split, combine) because we listen for SHOWING_CHANGED events
 * on the editor component and re-trigger EditorNotifications.
 */
class EditorToolbarProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function { fileEditor ->
            if (!MarkdownFileUtil.isMarkdownFile(file)) return@Function null
            if (fileEditor is MarkdownPreviewFileEditor) return@Function null

            val editor = when (fileEditor) {
                is TextEditor -> fileEditor.editor
                is MarkdownSplitEditor -> (fileEditor.textEditor as? TextEditor)?.editor
                else -> null
            } ?: return@Function null

            val displayMode = try {
                MarkdownSettings.getInstance().state.toolbarDisplayMode
            } catch (_: Exception) {
                "icons"
            }

            val actionButtons = mutableListOf<JButton>()

            val toolbar = JPanel(BorderLayout())
            toolbar.border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.Editor.BORDER_COLOR)

            // ── Left side: grouped action buttons ──────────────────────
            val leftPanel = JPanel()
            leftPanel.layout = BoxLayout(leftPanel, BoxLayout.X_AXIS)
            leftPanel.isOpaque = false
            leftPanel.border = JBUI.Borders.empty(1, 2)

            // Group 1: Text formatting
            actionButtons.add(addButton(leftPanel, "Bold", MarkdownIcons.TOOLBAR_BOLD, displayMode,
                "com.tribus.markdown.actions.ToggleBold", editor))
            actionButtons.add(addButton(leftPanel, "Italic", MarkdownIcons.TOOLBAR_ITALIC, displayMode,
                "com.tribus.markdown.actions.ToggleItalic", editor))
            actionButtons.add(addButton(leftPanel, "Strikethrough", MarkdownIcons.TOOLBAR_STRIKETHROUGH, displayMode,
                "com.tribus.markdown.actions.ToggleStrikethrough", editor))
            actionButtons.add(addButton(leftPanel, "Code", MarkdownIcons.TOOLBAR_CODE, displayMode,
                "com.tribus.markdown.actions.ToggleCodeSpan", editor))
            actionButtons.add(addButton(leftPanel, "Code Block", MarkdownIcons.TOOLBAR_CODE, displayMode,
                "com.tribus.markdown.actions.ToggleCodeBlock", editor))

            addSeparator(leftPanel)

            // Group 2: Structure (headings)
            actionButtons.add(addButton(leftPanel, "H+", MarkdownIcons.TOOLBAR_HEADING_UP, displayMode,
                "com.tribus.markdown.actions.HeadingUp", editor))
            actionButtons.add(addButton(leftPanel, "H-", MarkdownIcons.TOOLBAR_HEADING_DOWN, displayMode,
                "com.tribus.markdown.actions.HeadingDown", editor))

            addSeparator(leftPanel)

            // Group 3: Lists
            actionButtons.add(addButton(leftPanel, "Indent", MarkdownIcons.TOOLBAR_INDENT, displayMode,
                "com.tribus.markdown.actions.ListIndent", editor))
            actionButtons.add(addButton(leftPanel, "Outdent", MarkdownIcons.TOOLBAR_OUTDENT, displayMode,
                "com.tribus.markdown.actions.ListOutdent", editor))
            actionButtons.add(addButton(leftPanel, "Task", MarkdownIcons.TOOLBAR_TASK, displayMode,
                "com.tribus.markdown.actions.ToggleTaskList", editor))

            addSeparator(leftPanel)

            // Group 4: Tables & TOC
            actionButtons.add(addButton(leftPanel, "Table", MarkdownIcons.TOOLBAR_TABLE, displayMode,
                "com.tribus.markdown.actions.FormatTable", editor))
            actionButtons.add(addButton(leftPanel, "TOC", MarkdownIcons.TOOLBAR_TOC, displayMode,
                "com.tribus.markdown.actions.CreateToc", editor))

            toolbar.add(leftPanel, BorderLayout.WEST)

            // ── Right side: tools menu + settings gear ─────────────────
            val rightPanel = JPanel()
            rightPanel.layout = BoxLayout(rightPanel, BoxLayout.X_AXIS)
            rightPanel.isOpaque = false
            rightPanel.border = JBUI.Borders.empty(1, 2)

            // Tools dropdown
            val toolsButton = createHoverButton(AllIcons.Actions.More, null, "icons")
            toolsButton.toolTipText = "Markdown Tools"
            toolsButton.addActionListener {
                showToolsMenu(toolsButton, editor, project)
            }
            rightPanel.add(toolsButton)

            rightPanel.add(Box.createRigidArea(Dimension(2, 0)))

            // Settings gear
            val settingsButton = createHoverButton(AllIcons.General.GearPlain, null, "icons")
            settingsButton.toolTipText = "Markdown All-in-One Settings"
            settingsButton.addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Markdown All-in-One")
            }
            rightPanel.add(settingsButton)

            toolbar.add(rightPanel, BorderLayout.EAST)

            // ── Focus tracking ─────────────────────────────────────────
            actionButtons.forEach { it.isEnabled = true }

            val editorComponent = editor.contentComponent
            editorComponent.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) {
                    actionButtons.forEach { it.isEnabled = true }
                }
                override fun focusLost(e: FocusEvent?) {
                    actionButtons.forEach { it.isEnabled = false }
                }
            })

            // ── Hierarchy listener: re-create toolbar after window operations ──
            // When the editor is moved, split, or combined, its component hierarchy
            // changes. The toolbar panel may be detached and not re-attached.
            // Listening for SHOWING_CHANGED lets us re-trigger notification updates
            // so the toolbar is recreated.
            editorComponent.addHierarchyListener { e ->
                if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                    if (editorComponent.isShowing) {
                        SwingUtilities.invokeLater {
                            if (!project.isDisposed) {
                                EditorNotifications.getInstance(project).updateNotifications(file)
                            }
                        }
                    }
                }
            }

            toolbar
        }
    }

    private fun addSeparator(panel: JPanel) {
        panel.add(Box.createRigidArea(Dimension(4, 0)))
        val sep = JSeparator(SwingConstants.VERTICAL)
        sep.maximumSize = Dimension(1, 22)
        panel.add(sep)
        panel.add(Box.createRigidArea(Dimension(4, 0)))
    }

    private fun showToolsMenu(anchor: JComponent, editor: Editor, project: Project) {
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
    ): JButton {
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
        return button
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
