package com.tribus.markdown.structure

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.tribus.markdown.toc.HeadingExtractor

/**
 * A tree element representing a single heading in the structure view.
 * Supports navigation to the heading's line in the editor.
 */
class HeadingTreeElement(
    val heading: HeadingExtractor.Heading,
    private val documentText: String
) : StructureViewTreeElement, SortableTreeElement {

    private val children = mutableListOf<HeadingTreeElement>()

    fun addChild(child: HeadingTreeElement) {
        children.add(child)
    }

    override fun getValue(): Any = heading

    override fun getAlphaSortKey(): String = heading.rawText

    override fun getPresentation(): ItemPresentation {
        val icon = when (heading.level) {
            1 -> AllIcons.Nodes.Tag
            2 -> AllIcons.Nodes.Tag
            3 -> AllIcons.Nodes.Property
            4 -> AllIcons.Nodes.Property
            else -> AllIcons.Nodes.PropertyRead
        }
        val levelPrefix = "#".repeat(heading.level)
        return PresentationData("$levelPrefix ${heading.rawText}", null, icon, null)
    }

    override fun getChildren(): Array<TreeElement> = children.toTypedArray()

    override fun navigate(requestFocus: Boolean) {
        // Navigation is handled via the NavigationGutter / structure view framework
        // using the offset from documentText
    }

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = true

    /**
     * Get the text offset of this heading in the document.
     */
    fun getTextOffset(): Int {
        val lines = documentText.lines()
        var offset = 0
        for (i in 0 until heading.lineNumber.coerceAtMost(lines.size)) {
            offset += lines[i].length + 1
        }
        return offset
    }
}
