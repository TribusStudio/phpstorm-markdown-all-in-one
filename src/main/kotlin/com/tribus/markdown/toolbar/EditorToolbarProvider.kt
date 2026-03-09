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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.function.Function
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants

/**
 * Provides a toolbar at the top of markdown editors with quick-access buttons
 * for formatting, power tools, and settings.
 * Buttons show hover highlight when enabled and are grayed out when
 * the preview panel has focus.
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

            // Left side: formatting + power tool buttons
            val leftPanel = JPanel()
            leftPanel.layout = BoxLayout(leftPanel, BoxLayout.X_AXIS)
            leftPanel.isOpaque = false
            leftPanel.border = JBUI.Borders.empty(1, 2)

            // Formatting buttons
            actionButtons.add(addButton(leftPanel, "Bold", MarkdownIcons.TOOLBAR_BOLD, displayMode,
                "com.tribus.markdown.actions.ToggleBold", editor))
            actionButtons.add(addButton(leftPanel, "Italic", MarkdownIcons.TOOLBAR_ITALIC, displayMode,
                "com.tribus.markdown.actions.ToggleItalic", editor))
            actionButtons.add(addButton(leftPanel, "Strikethrough", MarkdownIcons.TOOLBAR_STRIKETHROUGH, displayMode,
                "com.tribus.markdown.actions.ToggleStrikethrough", editor))
            actionButtons.add(addButton(leftPanel, "Code", MarkdownIcons.TOOLBAR_CODE, displayMode,
                "com.tribus.markdown.actions.ToggleCodeSpan", editor))
            actionButtons.add(addButton(leftPanel, "H+", MarkdownIcons.TOOLBAR_HEADING_UP, displayMode,
                "com.tribus.markdown.actions.HeadingUp", editor))
            actionButtons.add(addButton(leftPanel, "H-", MarkdownIcons.TOOLBAR_HEADING_DOWN, displayMode,
                "com.tribus.markdown.actions.HeadingDown", editor))

            // Separator
            leftPanel.add(Box.createRigidArea(Dimension(4, 0)))
            val sep = JSeparator(SwingConstants.VERTICAL)
            sep.maximumSize = Dimension(1, 22)
            leftPanel.add(sep)
            leftPanel.add(Box.createRigidArea(Dimension(4, 0)))

            // Power tools
            actionButtons.add(addButton(leftPanel, "Table", MarkdownIcons.TOOLBAR_TABLE, displayMode,
                "com.tribus.markdown.actions.FormatTable", editor))
            actionButtons.add(addButton(leftPanel, "TOC", MarkdownIcons.TOOLBAR_TOC, displayMode,
                "com.tribus.markdown.actions.CreateToc", editor))

            toolbar.add(leftPanel, BorderLayout.WEST)

            // Right side: settings gear
            val rightPanel = JPanel()
            rightPanel.layout = BoxLayout(rightPanel, BoxLayout.X_AXIS)
            rightPanel.isOpaque = false
            rightPanel.border = JBUI.Borders.empty(1, 2)

            val settingsButton = createHoverButton(AllIcons.General.GearPlain, null, "icons")
            settingsButton.toolTipText = "Markdown All-in-One Settings"
            settingsButton.addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Markdown All-in-One")
            }
            rightPanel.add(settingsButton)

            toolbar.add(rightPanel, BorderLayout.EAST)

            // Track editor focus — start enabled, disable when focus leaves editor
            // Start enabled: toolbar appears when the file opens and editor naturally
            // receives focus. Checking hasFocus() at construction time is unreliable
            // because the editor component hasn't been shown yet.
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

            toolbar
        }
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
        /** Hover highlight color — translucent so it works on light and dark themes. */
        private val HOVER_BG = Color(128, 128, 128, 40)

        /**
         * Creates a flat button with hover highlight. When enabled, mousing over
         * paints a subtle background; when disabled, the cursor and highlight are
         * suppressed so the button appears inert.
         */
        fun createHoverButton(icon: Icon, label: String?, displayMode: String): JButton {
            val button = when (displayMode) {
                "labels" -> JButton(label)
                "icons and labels" -> JButton(label, icon)
                else -> JButton(icon) // "icons" (default)
            }

            button.isFocusable = false
            button.isBorderPainted = false
            button.isContentAreaFilled = false
            button.isOpaque = false
            button.margin = JBUI.insetsLeft(0)
            button.border = JBUI.Borders.empty(2, 3)

            // Fixed size for tight packing
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
