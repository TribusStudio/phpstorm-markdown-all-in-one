package com.tribus.markdown.editor

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.tribus.markdown.actions.MarkdownAction
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
 *
 * Everything registered here hangs off a per-editor [Disposable] stashed in the
 * editor's user data, and is torn down in [editorReleased]. Without that, the
 * floating toolbar (and its popup and debounce alarm) outlived every editor the
 * user ever opened.
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

        // Everything below is scoped to this editor's lifetime.
        // Must be a fresh instance per editor — a non-capturing `Disposable { }`
        // lambda is a single shared JVM instance, so the first editor released
        // would poison the scope for every editor opened afterwards.
        val editorScope = Disposer.newDisposable("MarkdownEditorScope")
        editor.putUserData(EDITOR_SCOPE_KEY, editorScope)

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

            // Register the shortcut on a per-editor wrapper, never on the
            // action instance ActionManager handed us.
            //
            // registerCustomShortcutSet mutates the shortcut set of whatever
            // instance it is called on, and ActionManager returns
            // application-wide singletons. Calling it directly rebound the
            // shortcut globally and made the platform log a stack trace per
            // call — 21 actions on every markdown editor opened, so 21 global
            // mutations and 21 logged traces each time a .md file was opened.
            EditorScopedAction(action).registerCustomShortcutSet(shortcutSet, component, editorScope)
        }

        // Register floating toolbar for text selections
        val floatingToolbar = FloatingToolbar(editor)
        Disposer.register(editorScope, floatingToolbar)
        editor.selectionModel.addSelectionListener(floatingToolbar, floatingToolbar)
        editor.caretModel.addCaretListener(floatingToolbar, floatingToolbar)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val scope = editor.getUserData(EDITOR_SCOPE_KEY) ?: return
        editor.putUserData(EDITOR_SCOPE_KEY, null)
        Disposer.dispose(scope)
    }

    /**
     * A per-editor delegating copy of a plugin action, so binding a shortcut to
     * it leaves the shared [ActionManager] instance untouched.
     *
     * Implements [MarkdownAction] so [com.tribus.markdown.actions.MarkdownActionPromoter]
     * still recognises it when resolving shortcut conflicts against IDE builtins —
     * without the marker, Cmd+B and friends would silently lose to GotoDeclaration.
     */
    private class EditorScopedAction(private val delegate: AnAction) : AnAction(), MarkdownAction {
        override fun getActionUpdateThread(): ActionUpdateThread = delegate.actionUpdateThread
        override fun update(e: AnActionEvent) = delegate.update(e)
        override fun actionPerformed(e: AnActionEvent) = delegate.actionPerformed(e)
        override fun isDumbAware(): Boolean = delegate.isDumbAware
    }

    companion object {
        private val EDITOR_SCOPE_KEY =
            Key.create<Disposable>("com.tribus.markdown.editorScope")

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
