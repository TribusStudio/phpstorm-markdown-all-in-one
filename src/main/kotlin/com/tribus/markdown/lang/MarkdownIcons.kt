package com.tribus.markdown.lang

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MarkdownIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/markdown.svg", MarkdownIcons::class.java)

    // Toolbar icons
    @JvmField
    val TOOLBAR_BOLD: Icon = IconLoader.getIcon("/icons/toolbar-bold.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_ITALIC: Icon = IconLoader.getIcon("/icons/toolbar-italic.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_STRIKETHROUGH: Icon = IconLoader.getIcon("/icons/toolbar-strikethrough.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_CODE: Icon = IconLoader.getIcon("/icons/toolbar-code.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_HEADING_UP: Icon = IconLoader.getIcon("/icons/toolbar-heading-up.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_HEADING_DOWN: Icon = IconLoader.getIcon("/icons/toolbar-heading-down.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_TASK: Icon = IconLoader.getIcon("/icons/toolbar-task.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_TABLE: Icon = IconLoader.getIcon("/icons/toolbar-table.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_TOC: Icon = IconLoader.getIcon("/icons/toolbar-toc.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_SETTINGS: Icon = IconLoader.getIcon("/icons/toolbar-settings.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_INDENT: Icon = IconLoader.getIcon("/icons/toolbar-indent.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_OUTDENT: Icon = IconLoader.getIcon("/icons/toolbar-outdent.svg", MarkdownIcons::class.java)
    @JvmField
    val TOOLBAR_MATH: Icon = IconLoader.getIcon("/icons/toolbar-math.svg", MarkdownIcons::class.java)
}
