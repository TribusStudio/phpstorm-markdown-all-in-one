package com.tribus.markdown.structure

import com.tribus.markdown.toc.HeadingExtractor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StructureViewTest {

    @Test
    fun `buildTree creates flat list for same-level headings`() {
        val text = """
            # A
            # B
            # C
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        val tree = MarkdownStructureViewElement.buildTree(headings, text)

        assertEquals(3, tree.size)
        assertEquals("A", tree[0].heading.rawText)
        assertEquals("B", tree[1].heading.rawText)
        assertEquals("C", tree[2].heading.rawText)
        tree.forEach { assertEquals(0, it.getChildren().size) }
    }

    @Test
    fun `buildTree nests sub-headings under parents`() {
        val text = """
            # Parent
            ## Child 1
            ## Child 2
            ### Grandchild
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        val tree = MarkdownStructureViewElement.buildTree(headings, text)

        assertEquals(1, tree.size)
        val parent = tree[0]
        assertEquals("Parent", parent.heading.rawText)
        assertEquals(2, parent.getChildren().size)

        val child1 = parent.getChildren()[0] as HeadingTreeElement
        assertEquals("Child 1", child1.heading.rawText)
        assertEquals(0, child1.getChildren().size)

        val child2 = parent.getChildren()[1] as HeadingTreeElement
        assertEquals("Child 2", child2.heading.rawText)
        assertEquals(1, child2.getChildren().size)

        val grandchild = child2.getChildren()[0] as HeadingTreeElement
        assertEquals("Grandchild", grandchild.heading.rawText)
    }

    @Test
    fun `buildTree handles level jumps correctly`() {
        val text = """
            # H1
            ### H3 (skips H2)
            ## H2
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        val tree = MarkdownStructureViewElement.buildTree(headings, text)

        assertEquals(1, tree.size)
        val h1 = tree[0]
        assertEquals(2, h1.getChildren().size)

        val h3 = h1.getChildren()[0] as HeadingTreeElement
        assertEquals("H3 (skips H2)", h3.heading.rawText)
        assertEquals(3, h3.heading.level)

        val h2 = h1.getChildren()[1] as HeadingTreeElement
        assertEquals("H2", h2.heading.rawText)
        assertEquals(2, h2.heading.level)
    }

    @Test
    fun `buildTree handles empty headings list`() {
        val tree = MarkdownStructureViewElement.buildTree(emptyList(), "")
        assertTrue(tree.isEmpty())
    }

    @Test
    fun `buildTree handles multiple top-level sections`() {
        val text = """
            # Section 1
            ## Sub 1
            # Section 2
            ## Sub 2
        """.trimIndent()
        val headings = HeadingExtractor.extract(text)
        val tree = MarkdownStructureViewElement.buildTree(headings, text)

        assertEquals(2, tree.size)
        assertEquals("Section 1", tree[0].heading.rawText)
        assertEquals(1, tree[0].getChildren().size)
        assertEquals("Section 2", tree[1].heading.rawText)
        assertEquals(1, tree[1].getChildren().size)
    }

    @Test
    fun `HeadingTreeElement getTextOffset calculates correctly`() {
        val text = "# First\n## Second\n### Third"
        val headings = HeadingExtractor.extract(text)
        val tree = MarkdownStructureViewElement.buildTree(headings, text)

        assertEquals(0, tree[0].getTextOffset())

        val child = tree[0].getChildren()[0] as HeadingTreeElement
        assertEquals(8, child.getTextOffset())
    }

    @Test
    fun `HeadingTreeElement presentation includes level prefix`() {
        val text = "## My Heading"
        val headings = HeadingExtractor.extract(text)
        val element = HeadingTreeElement(headings[0], text)
        val presentation = element.presentation

        assertEquals("## My Heading", presentation.presentableText)
    }
}
