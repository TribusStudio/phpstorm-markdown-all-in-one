package com.tribus.markdown.structure

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiFile
import com.tribus.markdown.lang.MarkdownIcons
import com.tribus.markdown.toc.HeadingExtractor

/**
 * Root structure view element for a markdown file.
 * Parses headings from document text and builds a hierarchical tree.
 */
class MarkdownStructureViewElement(
    private val psiFile: PsiFile
) : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = psiFile

    override fun getAlphaSortKey(): String = psiFile.name

    override fun getPresentation(): ItemPresentation {
        return PresentationData(psiFile.name, null, MarkdownIcons.FILE, null)
    }

    override fun getChildren(): Array<TreeElement> {
        val text = psiFile.text ?: return emptyArray()
        val headings = HeadingExtractor.extract(text)
        if (headings.isEmpty()) return emptyArray()

        return buildTree(headings, psiFile, text).toTypedArray()
    }

    override fun navigate(requestFocus: Boolean) {
        psiFile.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = psiFile.canNavigate()

    override fun canNavigateToSource(): Boolean = psiFile.canNavigateToSource()

    companion object {
        /**
         * Build a tree of [HeadingTreeElement] from a flat list of headings.
         * Uses a stack to track the hierarchy based on heading levels.
         */
        fun buildTree(headings: List<HeadingExtractor.Heading>, psiFile: PsiFile?, documentText: String): List<HeadingTreeElement> {
            val roots = mutableListOf<HeadingTreeElement>()
            val stack = mutableListOf<HeadingTreeElement>() // stack of ancestors

            for (heading in headings) {
                val element = HeadingTreeElement(heading, psiFile, documentText)

                // Pop stack until we find a parent with a lower heading level
                while (stack.isNotEmpty() && stack.last().heading.level >= heading.level) {
                    stack.removeAt(stack.size - 1)
                }

                if (stack.isEmpty()) {
                    roots.add(element)
                } else {
                    stack.last().addChild(element)
                }

                stack.add(element)
            }

            return roots
        }
    }
}
