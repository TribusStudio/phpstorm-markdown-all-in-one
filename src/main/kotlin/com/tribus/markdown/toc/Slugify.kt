package com.tribus.markdown.toc

/**
 * Slug generation for markdown heading anchors.
 * Supports multiple platform modes matching their respective anchor generation rules.
 */
object Slugify {

    enum class Mode {
        GITHUB,
        GITLAB,
        GITEA,
        AZURE_DEVOPS,
        BITBUCKET_CLOUD,
        ZOLA
    }

    // GitHub: keeps letters, marks, decimal digits, letter numbers, connector punctuation, hyphens, spaces
    // Approximation in JVM regex — \p{L}\p{M}\p{Nd}\p{Nl}\p{Pc} plus hyphen and space
    private val GITHUB_PUNCTUATION = Regex("[^\\p{L}\\p{M}\\p{Nd}\\p{Nl}\\p{Pc}\\- ]")

    fun slugify(text: String, mode: Mode = Mode.GITHUB): String {
        val plainText = stripInlineMarkdown(text)
        return when (mode) {
            Mode.GITHUB -> slugifyGitHub(plainText)
            Mode.GITLAB -> slugifyGitLab(plainText)
            Mode.GITEA -> slugifyGitea(plainText)
            Mode.AZURE_DEVOPS -> slugifyAzureDevOps(plainText)
            Mode.BITBUCKET_CLOUD -> slugifyBitbucketCloud(plainText)
            Mode.ZOLA -> slugifyZola(plainText)
        }
    }

    private fun slugifyGitHub(text: String): String {
        return text
            .trim()
            .lowercase()
            .replace(GITHUB_PUNCTUATION, "")
            .replace(' ', '-')
    }

    private fun slugifyGitLab(text: String): String {
        var slug = text
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
        // Remove trailing (CORE), (STARTER), (PREMIUM), (ULTIMATE)
        slug = slug.replace(Regex("\\s*\\((core|starter|premium|ultimate)\\)\\s*$"), "")
        slug = slug.replace(GITHUB_PUNCTUATION, "")
        slug = slug.replace(' ', '-')
        slug = slug.replace(Regex("-{2,}"), "-")
        slug = slug.trimStart('-').trimEnd('-')
        // Prefix digit-only slugs
        if (slug.matches(Regex("^\\d+$"))) {
            slug = "anchor-$slug"
        }
        return slug
    }

    private fun slugifyGitea(text: String): String {
        return text
            .replace(Regex("^[^\\w]+"), "")
            .replace(Regex("[^\\w]+$"), "")
            .replace(Regex("[^\\w]+"), "-")
            .lowercase()
    }

    private fun slugifyAzureDevOps(text: String): String {
        val trimmed = text.trim().lowercase()
        val slug = trimmed.replace(Regex("\\s+"), "-")
        if (slug.isNotEmpty() && slug[0].isDigit()) {
            // Encode as UTF-8 percent encoding
            val sb = StringBuilder()
            for (byte in slug.toByteArray(Charsets.UTF_8)) {
                sb.append("%" + String.format("%02x", byte.toInt() and 0xFF))
            }
            return sb.toString()
        }
        return slug
    }

    /**
     * Zola static site generator slug rules:
     * - Lowercase, trim whitespace
     * - Replace whitespace with hyphens
     * - Remove anything that isn't alphanumeric, hyphen, or underscore
     * - Collapse multiple hyphens
     * - Trim leading/trailing hyphens
     */
    private fun slugifyZola(text: String): String {
        return text
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), "-")
            .replace(Regex("[^a-z0-9\\-_]"), "")
            .replace(Regex("-{2,}"), "-")
            .trimStart('-')
            .trimEnd('-')
    }

    private fun slugifyBitbucketCloud(text: String): String {
        val githubSlug = slugifyGitHub(text)
        val collapsed = githubSlug.replace(Regex("-{2,}"), "-")
        return "markdown-header-$collapsed"
    }

    /**
     * Strip inline markdown formatting to get plain text for slugification.
     * Removes: images, links (keeps text), bold/italic markers, code spans, HTML tags.
     */
    fun stripInlineMarkdown(text: String): String {
        var result = text

        // Remove images: ![alt](url) -> alt
        result = result.replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")

        // Remove links: [text](url) -> text
        result = result.replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")

        // Remove reference links: [text][ref] -> text
        result = result.replace(Regex("\\[([^\\]]*)\\]\\[[^\\]]*\\]"), "$1")

        // Remove autolinks: <url> -> url
        result = result.replace(Regex("<(https?://[^>]+)>"), "$1")

        // Remove HTML tags
        result = result.replace(Regex("<[^>]+>"), "")

        // Remove bold/italic markers (order matters: ** before *)
        result = result.replace("**", "")
        result = result.replace("__", "")
        result = result.replace("~~", "")
        result = result.replace(Regex("(?<=\\s|^)\\*(?=\\S)"), "")
        result = result.replace(Regex("(?<=\\S)\\*(?=\\s|$)"), "")
        result = result.replace(Regex("(?<=\\s|^)_(?=\\S)"), "")
        result = result.replace(Regex("(?<=\\S)_(?=\\s|$)"), "")

        // Remove inline code spans (keep content)
        result = result.replace(Regex("``([^`]+)``"), "$1")
        result = result.replace(Regex("`([^`]+)`"), "$1")

        // Remove trailing closing hashes (ATX heading)
        result = result.replace(Regex("\\s+#+\\s*$"), "")

        // Remove backslash escapes: \[ -> [, \] -> ], etc.
        result = result.replace(Regex("\\\\([\\\\`*_{}\\[\\]()#+\\-.!|~])")) { m ->
            m.groupValues[1]
        }

        return result.trim()
    }

    /**
     * Make slugs unique by appending -1, -2, etc. for duplicates.
     */
    fun makeUnique(slug: String, occurrences: MutableMap<String, Int>): String {
        val count = occurrences.getOrDefault(slug, 0)
        occurrences[slug] = count + 1
        return if (count == 0) slug else "$slug-$count"
    }
}
