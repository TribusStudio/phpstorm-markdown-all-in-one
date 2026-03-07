package com.tribus.markdown.toc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SlugifyTest {

    // GitHub mode tests

    @Test
    fun `github - simple heading`() {
        assertEquals("hello-world", Slugify.slugify("Hello World", Slugify.Mode.GITHUB))
    }

    @Test
    fun `github - removes punctuation`() {
        assertEquals("hello-world", Slugify.slugify("Hello, World!", Slugify.Mode.GITHUB))
    }

    @Test
    fun `github - preserves hyphens`() {
        assertEquals("well-known-fact", Slugify.slugify("Well-Known Fact", Slugify.Mode.GITHUB))
    }

    @Test
    fun `github - unicode letters preserved`() {
        assertEquals("über-cool", Slugify.slugify("Über Cool", Slugify.Mode.GITHUB))
    }

    @Test
    fun `github - multiple spaces become single dash`() {
        assertEquals("a--b", Slugify.slugify("A  B", Slugify.Mode.GITHUB))
    }

    @Test
    fun `github - strips inline markdown`() {
        assertEquals("bold-text", Slugify.slugify("**Bold** Text", Slugify.Mode.GITHUB))
    }

    @Test
    fun `github - strips code spans`() {
        assertEquals("code-example", Slugify.slugify("`code` example", Slugify.Mode.GITHUB))
    }

    @Test
    fun `github - strips links`() {
        assertEquals("click-here", Slugify.slugify("[Click Here](http://example.com)", Slugify.Mode.GITHUB))
    }

    @Test
    fun `github - strips images`() {
        assertEquals("alt-text", Slugify.slugify("![Alt Text](image.png)", Slugify.Mode.GITHUB))
    }

    // GitLab mode tests

    @Test
    fun `gitlab - simple heading`() {
        assertEquals("hello-world", Slugify.slugify("Hello World", Slugify.Mode.GITLAB))
    }

    @Test
    fun `gitlab - collapses multiple dashes`() {
        assertEquals("a-b", Slugify.slugify("A & B", Slugify.Mode.GITLAB))
    }

    @Test
    fun `gitlab - trims leading trailing dashes`() {
        assertEquals("hello", Slugify.slugify("!Hello!", Slugify.Mode.GITLAB))
    }

    @Test
    fun `gitlab - digit-only gets anchor prefix`() {
        assertEquals("anchor-123", Slugify.slugify("123", Slugify.Mode.GITLAB))
    }

    // Gitea mode tests

    @Test
    fun `gitea - simple heading`() {
        assertEquals("hello-world", Slugify.slugify("Hello World", Slugify.Mode.GITEA))
    }

    @Test
    fun `gitea - strips leading non-word chars`() {
        assertEquals("hello", Slugify.slugify("!Hello", Slugify.Mode.GITEA))
    }

    // Azure DevOps tests

    @Test
    fun `azure - simple heading`() {
        assertEquals("hello-world", Slugify.slugify("Hello World", Slugify.Mode.AZURE_DEVOPS))
    }

    @Test
    fun `azure - digit-start encodes`() {
        val slug = Slugify.slugify("1st heading", Slugify.Mode.AZURE_DEVOPS)
        assert(slug.startsWith("%")) { "Expected percent-encoded slug, got: $slug" }
    }

    // Bitbucket Cloud tests

    @Test
    fun `bitbucket - prefixes with markdown-header`() {
        assertEquals("markdown-header-hello-world", Slugify.slugify("Hello World", Slugify.Mode.BITBUCKET_CLOUD))
    }

    // Inline markdown stripping

    @Test
    fun `strip - bold markers`() {
        assertEquals("bold", Slugify.stripInlineMarkdown("**bold**"))
    }

    @Test
    fun `strip - italic markers`() {
        assertEquals("italic text", Slugify.stripInlineMarkdown("*italic* text"))
    }

    @Test
    fun `strip - nested formatting`() {
        val result = Slugify.stripInlineMarkdown("**bold and *italic***")
        assert("bold" in result && "italic" in result)
    }

    @Test
    fun `strip - HTML tags`() {
        assertEquals("hello", Slugify.stripInlineMarkdown("<em>hello</em>"))
    }

    @Test
    fun `strip - trailing hashes`() {
        assertEquals("Hello", Slugify.stripInlineMarkdown("Hello ##"))
    }

    // Unique slugs

    @Test
    fun `makeUnique - first occurrence unchanged`() {
        val occurrences = mutableMapOf<String, Int>()
        assertEquals("hello", Slugify.makeUnique("hello", occurrences))
    }

    @Test
    fun `makeUnique - second occurrence gets -1`() {
        val occurrences = mutableMapOf<String, Int>()
        Slugify.makeUnique("hello", occurrences)
        assertEquals("hello-1", Slugify.makeUnique("hello", occurrences))
    }

    @Test
    fun `makeUnique - third occurrence gets -2`() {
        val occurrences = mutableMapOf<String, Int>()
        Slugify.makeUnique("hello", occurrences)
        Slugify.makeUnique("hello", occurrences)
        assertEquals("hello-2", Slugify.makeUnique("hello", occurrences))
    }
}
