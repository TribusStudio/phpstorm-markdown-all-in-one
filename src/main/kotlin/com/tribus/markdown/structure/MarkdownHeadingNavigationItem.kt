package com.tribus.markdown.structure

import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiFile
import com.tribus.markdown.toc.HeadingExtractor

/**
 * Navigation item for a markdown heading, used by Go To Symbol.
 */
class MarkdownHeadingNavigationItem(
    private val heading: HeadingExtractor.Heading,
    private val psiFile: PsiFile,
    private val documentText: String
) : NavigationItem {

    override fun getName(): String = heading.rawText

    override fun getPresentation(): ItemPresentation {
        val icon = when (heading.level) {
            1, 2 -> AllIcons.Nodes.Tag
            3, 4 -> AllIcons.Nodes.Property
            else -> AllIcons.Nodes.PropertyRead
        }
        val levelPrefix = "#".repeat(heading.level)
        return object : ItemPresentation {
            override fun getPresentableText(): String = "$levelPrefix $name"
            override fun getLocationString(): String = psiFile.name
            override fun getIcon(unused: Boolean) = icon
        }
    }

    override fun navigate(requestFocus: Boolean) {
        val project = psiFile.project
        val virtualFile = psiFile.virtualFile ?: return
        val offset = calculateOffset()
        val descriptor = OpenFileDescriptor(project, virtualFile, offset)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, requestFocus)
    }

    override fun canNavigate(): Boolean = psiFile.virtualFile != null

    override fun canNavigateToSource(): Boolean = canNavigate()

    private fun calculateOffset(): Int {
        val lines = documentText.lines()
        var offset = 0
        for (i in 0 until heading.lineNumber.coerceAtMost(lines.size)) {
            offset += lines[i].length + 1
        }
        return offset
    }
}
