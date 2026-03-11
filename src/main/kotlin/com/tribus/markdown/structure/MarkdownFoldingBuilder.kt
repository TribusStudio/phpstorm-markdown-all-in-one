package com.tribus.markdown.structure

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.tribus.markdown.toc.HeadingExtractor

/**
 * Provides code folding for markdown files.
 *
 * Foldable regions:
 * - Headings (fold from heading line to end of section)
 * - Fenced code blocks
 * - YAML front matter
 * - Blockquote blocks (consecutive lines starting with >)
 */
class MarkdownFoldingBuilder : FoldingBuilderEx() {

    private val FENCE_PATTERN = Regex("^\\s{0,3}(`{3,}|~{3,})")
    private val FRONT_MATTER = Regex("^---\\s*$")
    private val BLOCKQUOTE = Regex("^\\s{0,3}>")
    private val ATX_HEADING = Regex("^\\s{0,3}(#{1,6})(\\s.*|$)")

    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val text = document.text
        val lines = text.lines()
        val descriptors = mutableListOf<FoldingDescriptor>()

        buildCodeFenceFolds(lines, document, root.node, descriptors)
        buildFrontMatterFolds(lines, document, root.node, descriptors)
        buildBlockquoteFolds(lines, document, root.node, descriptors)
        buildHeadingFolds(text, lines, document, root.node, descriptors)

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun buildCodeFenceFolds(
        lines: List<String>,
        document: Document,
        node: ASTNode,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        var i = 0
        while (i < lines.size) {
            val fenceMatch = FENCE_PATTERN.find(lines[i])
            if (fenceMatch != null) {
                val fenceChar = fenceMatch.groupValues[1][0]
                val fenceLength = fenceMatch.groupValues[1].length
                val startLine = i
                i++
                while (i < lines.size) {
                    val closeMatch = FENCE_PATTERN.find(lines[i])
                    if (closeMatch != null) {
                        val closeChar = closeMatch.groupValues[1][0]
                        val closeLength = closeMatch.groupValues[1].length
                        if (closeChar == fenceChar && closeLength >= fenceLength) {
                            val startOffset = document.getLineStartOffset(startLine)
                            val endOffset = document.getLineEndOffset(i)
                            if (endOffset > startOffset) {
                                val lang = lines[startLine].trim().removePrefix(fenceMatch.groupValues[1]).trim()
                                val placeholder = if (lang.isNotEmpty()) "```$lang..." else "```..."
                                descriptors.add(FoldingDescriptor(
                                    node, TextRange(startOffset, endOffset),
                                    FoldingGroup.newGroup("code-fence-$startLine"),
                                    placeholder
                                ))
                            }
                            break
                        }
                    }
                    i++
                }
            }
            i++
        }
    }

    private fun buildFrontMatterFolds(
        lines: List<String>,
        document: Document,
        node: ASTNode,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        if (lines.isEmpty() || !FRONT_MATTER.matches(lines[0])) return

        for (i in 1 until lines.size) {
            if (FRONT_MATTER.matches(lines[i])) {
                val startOffset = document.getLineStartOffset(0)
                val endOffset = document.getLineEndOffset(i)
                if (endOffset > startOffset) {
                    descriptors.add(FoldingDescriptor(
                        node, TextRange(startOffset, endOffset),
                        FoldingGroup.newGroup("front-matter"),
                        "---..."
                    ))
                }
                return
            }
        }
    }

    private fun buildBlockquoteFolds(
        lines: List<String>,
        document: Document,
        node: ASTNode,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        var i = 0
        while (i < lines.size) {
            if (BLOCKQUOTE.matches(lines[i])) {
                val startLine = i
                while (i + 1 < lines.size && BLOCKQUOTE.matches(lines[i + 1])) {
                    i++
                }
                // Only fold if blockquote spans multiple lines
                if (i > startLine) {
                    val startOffset = document.getLineStartOffset(startLine)
                    val endOffset = document.getLineEndOffset(i)
                    if (endOffset > startOffset) {
                        descriptors.add(FoldingDescriptor(
                            node, TextRange(startOffset, endOffset),
                            FoldingGroup.newGroup("blockquote-$startLine"),
                            "> ..."
                        ))
                    }
                }
            }
            i++
        }
    }

    private fun buildHeadingFolds(
        text: String,
        lines: List<String>,
        document: Document,
        node: ASTNode,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        val headings = HeadingExtractor.extract(text)
        if (headings.isEmpty()) return

        for ((idx, heading) in headings.withIndex()) {
            val startLine = heading.lineNumber
            // Section ends at the next heading of same or higher level, or end of document
            val endLine = findSectionEnd(headings, idx, lines.size)

            // Trim trailing blank lines
            var actualEnd = endLine - 1
            while (actualEnd > startLine && lines.getOrNull(actualEnd)?.isBlank() == true) {
                actualEnd--
            }

            if (actualEnd > startLine) {
                val startOffset = document.getLineEndOffset(startLine) // fold from after heading text
                val endOffset = document.getLineEndOffset(actualEnd)
                if (endOffset > startOffset) {
                    descriptors.add(FoldingDescriptor(
                        node, TextRange(startOffset, endOffset),
                        FoldingGroup.newGroup("heading-$startLine"),
                        "..."
                    ))
                }
            }
        }
    }

    private fun findSectionEnd(
        headings: List<HeadingExtractor.Heading>,
        currentIndex: Int,
        totalLines: Int
    ): Int {
        val currentLevel = headings[currentIndex].level
        for (i in currentIndex + 1 until headings.size) {
            if (headings[i].level <= currentLevel) {
                return headings[i].lineNumber
            }
        }
        return totalLines
    }
}
