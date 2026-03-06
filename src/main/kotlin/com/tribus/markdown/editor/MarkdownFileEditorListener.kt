package com.tribus.markdown.editor

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.vfs.VirtualFile
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Registers plugin shortcuts directly on the editor component when a markdown
 * file is opened. Component-level shortcuts always take priority over global
 * keymap shortcuts, which guarantees our actions fire instead of IDE builtins
 * like GotoDeclaration (Cmd+B) or Go to Implementation (Cmd+I).
 *
 * Shortcuts are read from the active keymap, so user customizations in
 * Settings > Keymap are respected.
 */
class MarkdownFileEditorListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (!MarkdownFileUtil.isMarkdownFile(file)) return

        val actionManager = ActionManager.getInstance()
        val keymap = KeymapManager.getInstance()?.activeKeymap ?: return

        for (fileEditor in source.getEditors(file)) {
            val textEditor = fileEditor as? TextEditor ?: continue
            val component = textEditor.editor.contentComponent

            for (actionId in SHORTCUT_ACTION_IDS) {
                val action = actionManager.getAction(actionId) ?: continue
                val shortcuts = keymap.getShortcuts(actionId)
                if (shortcuts.isNotEmpty()) {
                    action.registerCustomShortcutSet(CustomShortcutSet(*shortcuts), component)
                }
            }
        }
    }

    companion object {
        private val SHORTCUT_ACTION_IDS = listOf(
            "com.tribus.markdown.actions.ToggleBold",
            "com.tribus.markdown.actions.ToggleItalic",
            "com.tribus.markdown.actions.ToggleStrikethrough",
            "com.tribus.markdown.actions.ToggleCodeSpan",
            "com.tribus.markdown.actions.ToggleCodeBlock",
            "com.tribus.markdown.actions.HeadingUp",
            "com.tribus.markdown.actions.HeadingDown",
            "com.tribus.markdown.actions.ToggleTaskList",
        )
    }
}
