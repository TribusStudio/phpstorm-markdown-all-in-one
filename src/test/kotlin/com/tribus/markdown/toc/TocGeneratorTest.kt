package com.tribus.markdown.toc

import com.tribus.markdown.settings.MarkdownSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

    // TOC range detection

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
}
