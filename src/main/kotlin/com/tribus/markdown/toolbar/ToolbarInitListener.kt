package com.tribus.markdown.toolbar

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.tribus.markdown.util.MarkdownFileUtil

/**
 * Triggers EditorNotifications when a markdown file is opened in a FileEditor tab.
 *
 * EditorNotificationProvider is lazy — IntelliJ may not evaluate it immediately
 * when a file opens. This listener forces an update so the toolbar appears
 * without delay.
 *
 * This fires at the right abstraction level: after the FileEditor is fully
 * constructed and associated with a project, unlike EditorFactoryListener
 * which fires earlier when editor.project may still be null.
 */
class ToolbarInitListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (!MarkdownFileUtil.isMarkdownFile(file)) return
        EditorNotifications.getInstance(source.project).updateNotifications(file)
    }
}
