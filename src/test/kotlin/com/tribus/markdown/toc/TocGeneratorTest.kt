package com.tribus.markdown.toc

import com.tribus.markdown.settings.MarkdownSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class TocGeneratorTest {

    @Test
    fun `generates simple TOC`() {
        val headings = listOf(
            HeadingExtractor.Heading(1, "Introduction", 0),
            HeadingExtractor.Heading(2, "Getting Started", 2),
            HeadingExtractor.Heading(2, "Usage", 5),
            HeadingExtractor.Heading(3, "Advanced", 8)
        )
        val toc = TocGenerator.generateFromHeadings(headings)
        val expected = """
            - [Introduction](#introduction)
                - [Getting Started](#getting-started)
                - [Usage](#usage)
                    - [Advanced](#advanced)
        """.trimIndent()
        assertEquals(expected, toc)
    }

    @Test
    fun `generates ordered TOC`() {
        val headings = listOf(
            HeadingExtractor.Heading(1, "First", 0),
            HeadingExtractor.Heading(1, "Second", 2)
        )
        val state = MarkdownSettings.State(tocOrderedList = true)
        val toc = TocGenerator.generateFromHeadings(headings, state)
        assert(toc.contains("1. [First]"))
        assert(toc.contains("2. [Second]"))
    }

    @Test
    fun `ordered TOC uses per-level numbering`() {
        val headings = listOf(
            HeadingExtractor.Heading(1, "Heading A", 0),
            HeadingExtractor.Heading(2, "Child A.1", 1),
            HeadingExtractor.Heading(2, "Child A.2", 2),
            HeadingExtractor.Heading(3, "Grandchild A.2.1", 3),
            HeadingExtractor.Heading(2, "Child A.3", 4),
            HeadingExtractor.Heading(1, "Heading B", 5),
            HeadingExtractor.Heading(2, "Child B.1", 6)
        )
        val state = MarkdownSettings.State(tocOrderedList = true)
        val toc = TocGenerator.generateFromHeadings(headings, state)
        val lines = toc.lines()
        assertEquals("1. [Heading A](#heading-a)", lines[0])
        assertEquals("    1. [Child A.1](#child-a1)", lines[1])
        assertEquals("    2. [Child A.2](#child-a2)", lines[2])
        assertEquals("        1. [Grandchild A.2.1](#grandchild-a21)", lines[3])
        assertEquals("    3. [Child A.3](#child-a3)", lines[4])
        assertEquals("2. [Heading B](#heading-b)", lines[5])
        assertEquals("    1. [Child B.1](#child-b1)", lines[6])
    }

    @Test
    fun `respects level filtering`() {
        val headings = listOf(
            HeadingExtractor.Heading(1, "H1", 0),
            HeadingExtractor.Heading(2, "H2", 2),
            HeadingExtractor.Heading(3, "H3", 4),
            HeadingExtractor.Heading(4, "H4", 6)
        )
        val state = MarkdownSettings.State(tocLevels = "2..3")
        val toc = TocGenerator.generateFromHeadings(headings, state)
        assert(!toc.contains("H1"))
        assert(toc.contains("H2"))
        assert(toc.contains("H3"))
        assert(!toc.contains("H4"))
    }

    @Test
    fun `skips omitted headings`() {
        val headings = listOf(
            HeadingExtractor.Heading(1, "Visible", 0, canInToc = true),
            HeadingExtractor.Heading(1, "Hidden", 2, canInToc = false),
            HeadingExtractor.Heading(1, "Also Visible", 4, canInToc = true)
        )
        val toc = TocGenerator.generateFromHeadings(headings)
        assert(toc.contains("Visible"))
        assert(!toc.contains("Hidden"))
        assert(toc.contains("Also Visible"))
    }

    @Test
    fun `handles duplicate heading text`() {
        val headings = listOf(
            HeadingExtractor.Heading(2, "Section", 0),
            HeadingExtractor.Heading(2, "Section", 3),
            HeadingExtractor.Heading(2, "Section", 6)
        )
        val toc = TocGenerator.generateFromHeadings(headings)
        assert(toc.contains("#section)"))
        assert(toc.contains("#section-1)"))
        assert(toc.contains("#section-2)"))
    }

    @Test
    fun `uses custom list marker`() {
        val headings = listOf(
            HeadingExtractor.Heading(1, "Hello", 0)
        )
        val state = MarkdownSettings.State(tocUnorderedListMarker = "*")
        val toc = TocGenerator.generateFromHeadings(headings, state)
        assert(toc.startsWith("* "))
    }

    @Test
    fun `empty headings returns empty string`() {
        val toc = TocGenerator.generateFromHeadings(emptyList())
        assertEquals("", toc)
    }

    // TOC block detection

    @Test
    fun `findTocRange finds markers`() {
        val text = """
            # Title

            <!-- TOC -->
            - [Section 1](#section-1)
            - [Section 2](#section-2)
            <!-- /TOC -->

            ## Section 1
        """.trimIndent()
        val range = TocGenerator.findTocRange(text)
        assertNotNull(range)
        assertEquals(2, range!!.startLine)
        assertEquals(5, range.endLine)
    }

    @Test
    fun `findTocRange returns null when no markers`() {
        val text = "# Title\n\nSome text"
        assertNull(TocGenerator.findTocRange(text))
    }

    @Test
    fun `findTocRange returns null with only start marker`() {
        val text = "<!-- TOC -->\n# Title"
        assertNull(TocGenerator.findTocRange(text))
    }

    // Attribute parsing

    @Test
    fun `parseAttributes extracts name`() {
        val attrs = TocGenerator.parseAttributes("<!-- TOC name=\"api\" -->")
        assertEquals("api", attrs.name)
    }

    @Test
    fun `parseAttributes extracts all attributes`() {
        val attrs = TocGenerator.parseAttributes("""<!-- TOC name="api" type="ordered" level="2..4" -->""")
        assertEquals("api", attrs.name)
        assertEquals("ordered", attrs.type)
        assertEquals("2..4", attrs.level)
    }

    @Test
    fun `parseAttributes returns empty for generic TOC`() {
        val attrs = TocGenerator.parseAttributes("<!-- TOC -->")
        assertNull(attrs.name)
        assertNull(attrs.type)
        assertNull(attrs.level)
    }

    @Test
    fun `buildStartMarker reconstructs marker`() {
        val attrs = TocGenerator.TocAttributes(name = "api", type = "ordered", level = "2..4")
        assertEquals("""<!-- TOC name="api" type="ordered" level="2..4" -->""", TocGenerator.buildStartMarker(attrs))
    }

    @Test
    fun `buildStartMarker generic`() {
        assertEquals("<!-- TOC -->", TocGenerator.buildStartMarker(TocGenerator.TocAttributes()))
    }

    // Multiple TOC blocks

    @Test
    fun `findAllTocBlocks finds multiple blocks`() {
        val text = """
            <!-- TOC -->
            <!-- /TOC -->
            some text
            <!-- TOC name="api" type="ordered" -->
            <!-- /TOC -->
        """.trimIndent()
        val blocks = TocGenerator.findAllTocBlocks(text)
        assertEquals(2, blocks.size)
        assertNull(blocks[0].attributes.name)
        assertEquals("api", blocks[1].attributes.name)
        assertEquals("ordered", blocks[1].attributes.type)
    }

    // Content ranges

    @Test
    fun `findAllContentRanges finds named ranges`() {
        val text = """
            # Doc
            <!-- toc range name="section-a" start -->
            ## Heading A
            <!-- toc range end -->
            <!-- toc range name="section-b" start -->
            ## Heading B
            <!-- toc range end -->
        """.trimIndent()
        val ranges = TocGenerator.findAllContentRanges(text)
        assertEquals(2, ranges.size)
        assertEquals("section-a", ranges[0].name)
        assertEquals("section-b", ranges[1].name)
    }

    // Named TOC with content range

    @Test
    fun `named TOC only includes headings from its range`() {
        val text = """
            # Full Doc Title
            ## Introduction

            <!-- toc range name="api" start -->
            ## Endpoints
            ### GET users
            ### POST users
            <!-- toc range end -->

            ## Conclusion
        """.trimIndent()
        val blocks = TocGenerator.findAllTocBlocks("<!-- TOC name=\"api\" level=\"2..3\" -->\n<!-- /TOC -->")
        val block = blocks[0]
        val contentRanges = TocGenerator.findAllContentRanges(text)
        val toc = TocGenerator.generateForBlock(text, block, contentRanges, MarkdownSettings.State())
        assertTrue(toc.contains("Endpoints"))
        assertTrue(toc.contains("GET users"))
        assertTrue(toc.contains("POST users"))
        assertFalse(toc.contains("Full Doc Title"))
        assertFalse(toc.contains("Introduction"))
        assertFalse(toc.contains("Conclusion"))
    }

    @Test
    fun `unnamed TOC includes all headings`() {
        val text = """
            # Title
            ## Section A

            <!-- toc range name="api" start -->
            ## API Section
            <!-- toc range end -->

            ## Section B
        """.trimIndent()
        val blocks = TocGenerator.findAllTocBlocks("<!-- TOC -->\n<!-- /TOC -->")
        val block = blocks[0]
        val contentRanges = TocGenerator.findAllContentRanges(text)
        val toc = TocGenerator.generateForBlock(text, block, contentRanges, MarkdownSettings.State())
        assertTrue(toc.contains("Title"))
        assertTrue(toc.contains("Section A"))
        assertTrue(toc.contains("API Section"))
        assertTrue(toc.contains("Section B"))
    }

    @Test
    fun `named TOC respects type override`() {
        val text = """
            <!-- toc range name="ordered-section" start -->
            ## First
            ## Second
            <!-- toc range end -->
        """.trimIndent()
        val tocMarker = """<!-- TOC name="ordered-section" type="ordered" -->"""
        val blocks = TocGenerator.findAllTocBlocks("$tocMarker\n<!-- /TOC -->")
        val block = blocks[0]
        val contentRanges = TocGenerator.findAllContentRanges(text)
        val toc = TocGenerator.generateForBlock(text, block, contentRanges, MarkdownSettings.State())
        assertTrue(toc.contains("1. [First]"))
        assertTrue(toc.contains("2. [Second]"))
    }

    @Test
    fun `named TOC respects level override`() {
        val text = """
            <!-- toc range name="shallow" start -->
            # H1
            ## H2
            ### H3
            <!-- toc range end -->
        """.trimIndent()
        val tocMarker = """<!-- TOC name="shallow" level="1..2" -->"""
        val blocks = TocGenerator.findAllTocBlocks("$tocMarker\n<!-- /TOC -->")
        val block = blocks[0]
        val contentRanges = TocGenerator.findAllContentRanges(text)
        val toc = TocGenerator.generateForBlock(text, block, contentRanges, MarkdownSettings.State())
        assertTrue(toc.contains("H1"))
        assertTrue(toc.contains("H2"))
        assertFalse(toc.contains("H3"))
    }

    @Test
    fun `named TOC with no matching range produces empty TOC`() {
        val text = "# Some Heading"
        val tocMarker = """<!-- TOC name="nonexistent" -->"""
        val blocks = TocGenerator.findAllTocBlocks("$tocMarker\n<!-- /TOC -->")
        val block = blocks[0]
        val contentRanges = TocGenerator.findAllContentRanges(text)
        val toc = TocGenerator.generateForBlock(text, block, contentRanges, MarkdownSettings.State())
        assertEquals("", toc)
    }

    // Update all TOCs

    @Test
    fun `updateAllTocs updates multiple TOC blocks`() {
        val text = """
            <!-- TOC -->
            - old
            <!-- /TOC -->

            # Title
            ## Section

            <!-- toc range name="api" start -->
            ## Endpoints
            <!-- toc range end -->

            <!-- TOC name="api" type="ordered" -->
            - old api
            <!-- /TOC -->
        """.trimIndent()
        val updated = TocGenerator.updateAllTocs(text, MarkdownSettings.State())
        assertNotNull(updated)
        // Generic TOC should have all headings
        assertTrue(updated!!.contains("[Title]"))
        assertTrue(updated.contains("[Section]"))
        assertTrue(updated.contains("[Endpoints]"))
        // Named TOC should only have api range headings
        // Check it appears with ordered numbering
        assertTrue(updated.contains("1. [Endpoints]"))
    }

    // Level parsing

    @Test
    fun `parseLevels parses valid range`() {
        assertEquals(Pair(2, 4), TocGenerator.parseLevels("2..4"))
    }

    @Test
    fun `parseLevels handles reversed range`() {
        assertEquals(Pair(2, 4), TocGenerator.parseLevels("4..2"))
    }

    @Test
    fun `parseLevels defaults on invalid input`() {
        assertEquals(Pair(1, 6), TocGenerator.parseLevels("invalid"))
    }

    @Test
    fun `parseLevels defaults on empty input`() {
        assertEquals(Pair(1, 6), TocGenerator.parseLevels(""))
    }

    // Slug mode parsing

    @Test
    fun `parseSlugMode github`() {
        assertEquals(Slugify.Mode.GITHUB, TocGenerator.parseSlugMode("github"))
    }

    @Test
    fun `parseSlugMode gitlab`() {
        assertEquals(Slugify.Mode.GITLAB, TocGenerator.parseSlugMode("gitlab"))
    }

    @Test
    fun `parseSlugMode case insensitive`() {
        assertEquals(Slugify.Mode.GITHUB, TocGenerator.parseSlugMode("GitHub"))
    }

    @Test
    fun `parseSlugMode unknown defaults to github`() {
        assertEquals(Slugify.Mode.GITHUB, TocGenerator.parseSlugMode("unknown"))
    }

    // Update existing TOC

    @Test
    fun `updateExistingToc replaces TOC content`() {
        val text = """
            # My Doc

            <!-- TOC -->
            - [Old Heading](#old-heading)
            <!-- /TOC -->

            ## New Heading
        """.trimIndent()
        val updated = TocGenerator.updateExistingToc(text, MarkdownSettings.State())
        assertNotNull(updated)
        assert(updated!!.contains("New Heading"))
        assert(updated.contains("<!-- TOC -->"))
        assert(updated.contains("<!-- /TOC -->"))
    }

    @Test
    fun `updateExistingToc returns null when no TOC`() {
        assertNull(TocGenerator.updateExistingToc("# Title\n\nNo TOC here", MarkdownSettings.State()))
    }

    // Section numbers

    @Test
    fun `removeSectionNumbers strips number prefixes`() {
        val text = """
            # 1. Introduction
            ## 1.1. Background
            ## 1.2. Goals
            # 2. Design
        """.trimIndent()
        val cleaned = TocGenerator.removeSectionNumbers(text)
        assert(cleaned.contains("# Introduction"))
        assert(cleaned.contains("## Background"))
        assert(cleaned.contains("## Goals"))
        assert(cleaned.contains("# Design"))
    }

    // Generate with markers

    @Test
    fun `generateWithMarkers wraps TOC`() {
        val text = "# Hello\n## World"
        val result = TocGenerator.generateWithMarkers(text, MarkdownSettings.State())
        assert(result.startsWith("<!-- TOC -->"))
        assert(result.endsWith("<!-- /TOC -->"))
        assert(result.contains("[Hello]"))
        assert(result.contains("[World]"))
    }

    @Test
    fun `generateWithMarkers empty document`() {
        val result = TocGenerator.generateWithMarkers("No headings here", MarkdownSettings.State())
        assertEquals("<!-- TOC -->\n<!-- /TOC -->", result)
    }

    // Escaping

    @Test
    fun `escapes brackets in link text`() {
        val headings = listOf(
            HeadingExtractor.Heading(1, "Array [0] access", 0)
        )
        val toc = TocGenerator.generateFromHeadings(headings)
        assert(toc.contains("\\[0\\]"))
    }

    // ── Code fence awareness tests ───────────────────────────────────

    @Test
    fun `TOC markers inside fenced code blocks are ignored`() {
        val md = """
            # Real Heading
            ```markdown
            <!-- TOC -->
            - fake toc content
            <!-- /TOC -->
            ```
            ## Another Heading
        """.trimIndent()
        val blocks = TocGenerator.findAllTocBlocks(md)
        assertEquals(0, blocks.size, "TOC markers inside code fence should be ignored")
    }

    @Test
    fun `TOC markers outside code blocks are found normally`() {
        val md = """
            # Heading
            ```
            some code
            ```
            <!-- TOC -->
            <!-- /TOC -->
        """.trimIndent()
        val blocks = TocGenerator.findAllTocBlocks(md)
        assertEquals(1, blocks.size)
    }

    @Test
    fun `TOC markers inside tilde code blocks are ignored`() {
        val md = """
            # Heading
            ~~~
            <!-- TOC -->
            <!-- /TOC -->
            ~~~
        """.trimIndent()
        val blocks = TocGenerator.findAllTocBlocks(md)
        assertEquals(0, blocks.size)
    }

    @Test
    fun `content range markers inside code blocks are ignored`() {
        val md = """
            # Heading
            ```
            <!-- toc range name="api" start -->
            ## Fake
            <!-- toc range end -->
            ```
        """.trimIndent()
        val ranges = TocGenerator.findAllContentRanges(md)
        assertEquals(0, ranges.size)
    }

    @Test
    fun `updateAllTocs ignores TOC markers in code blocks`() {
        val md = """
            # Title
            <!-- TOC -->
            <!-- /TOC -->
            ```markdown
            <!-- TOC -->
            - this should not be touched
            <!-- /TOC -->
            ```
        """.trimIndent()
        val result = TocGenerator.updateAllTocs(md, MarkdownSettings.State())
        // Should only update the real TOC (first one), not the one in the code block
        assertNotNull(result)
        assertTrue(result!!.contains("- [Title](#title)"))
        assertTrue(result.contains("- this should not be touched"), "Code block content must not be modified")
    }
}
