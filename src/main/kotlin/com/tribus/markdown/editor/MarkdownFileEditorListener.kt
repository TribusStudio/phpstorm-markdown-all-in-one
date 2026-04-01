package com.tribus.markdown.editor

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.keymap.KeymapManager
import com.tribus.markdown.toolbar.FloatingToolbar
import com.tribus.markdown.util.MarkdownFileUtil
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Registers plugin shortcuts directly on the editor component when a markdown
 * file is opened. Component-level shortcuts always take priority over global
 * keymap shortcuts, which guarantees our actions fire instead of IDE builtins
 * like GotoDeclaration (Cmd+B) or Go to Implementation (Cmd+I).
 *
 * Uses EditorFactoryListener (fires at editor creation) for reliable timing.
 *
 * If the user has customized shortcuts in Settings > Keymap, those are used.
 * Otherwise, falls back to platform-aware defaults (Cmd on macOS, Ctrl on
 * Windows/Linux).
 */
class MarkdownFileEditorListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return

        if (!MarkdownFileUtil.isMarkdownFile(file)) return

        val actionManager = ActionManager.getInstance()
        val keymap = KeymapManager.getInstance()?.activeKeymap
        val component = editor.contentComponent

        for ((actionId, defaultShortcut) in DEFAULT_SHORTCUTS) {
            val action = actionManager.getAction(actionId) ?: continue

            // Use keymap shortcuts if available (respects user customization),
            // otherwise fall back to our platform-aware defaults
            val keymapShortcuts = keymap?.getShortcuts(actionId)
            val shortcutSet = if (keymapShortcuts != null && keymapShortcuts.isNotEmpty()) {
                CustomShortcutSet(*keymapShortcuts)
            } else {
                defaultShortcut
            }

            action.registerCustomShortcutSet(shortcutSet, component)
        }

        // Register floating toolbar for text selections
        val floatingToolbar = FloatingToolbar(editor)
        editor.selectionModel.addSelectionListener(floatingToolbar)
    }

    companion object {
        // Cmd on macOS, Ctrl on Windows/Linux — lazy to avoid HeadlessException in tests
        private val MENU_MOD by lazy {
            try {
                Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
            } catch (_: java.awt.HeadlessException) {
                InputEvent.CTRL_DOWN_MASK
            }
        }

        private val DEFAULT_SHORTCUTS by lazy {
            mapOf(
                "com.tribus.markdown.actions.ToggleBold" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_B, MENU_MOD), null)
                ),
                "com.tribus.markdown.actions.ToggleItalic" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, MENU_MOD), null)
                ),
                "com.tribus.markdown.actions.ToggleStrikethrough" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.ToggleCodeSpan" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, MENU_MOD), null)
                ),
                "com.tribus.markdown.actions.ToggleCodeBlock" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, MENU_MOD or InputEvent.SHIFT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.HeadingUp" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.HeadingDown" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.ToggleTaskList" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.ListIndent" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, MENU_MOD), null)
                ),
                "com.tribus.markdown.actions.ListOutdent" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, MENU_MOD), null)
                ),
                "com.tribus.markdown.actions.FormatTable" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK or InputEvent.ALT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.ToggleMath" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.InsertLink" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.InsertImage" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.ToggleBlockquote" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.MoveLineUp" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.MoveLineDown" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.CopyLineUp" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK or InputEvent.ALT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.CopyLineDown" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK or InputEvent.ALT_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.ListExit" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), null)
                ),
                "com.tribus.markdown.actions.SoftBreak" to CustomShortcutSet(
                    KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), null)
                ),
            )
        }
    }
}
