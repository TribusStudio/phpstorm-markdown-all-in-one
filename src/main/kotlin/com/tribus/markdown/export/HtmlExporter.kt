package com.tribus.markdown.export

import com.tribus.markdown.preview.MarkdownHtmlConverter
import com.tribus.markdown.preview.PreviewTheme
import com.tribus.markdown.settings.MarkdownSettings
import java.io.File
import java.nio.file.Path
import java.util.Base64

/**
 * Exports markdown files to styled HTML documents.
 * Handles image path resolution, theme application, and link validation.
 */
object HtmlExporter {

    data class ExportResult(
        val outputFile: File,
        val warnings: List<String>
    )

    data class ExportOptions(
        val theme: PreviewTheme.Theme = PreviewTheme.Theme.GITHUB,
        val customCssPath: String = "",
        val resolveImages: Boolean = true,
        val embedImages: Boolean = false,
        val validateLinks: Boolean = true,
        val convertMdLinks: Boolean = true,
        val pureCss: Boolean = false
    )

    /**
     * Export a single markdown file to HTML.
     */
    fun exportFile(
        markdownFile: File,
        outputFile: File,
        options: ExportOptions = defaultOptions()
    ): ExportResult {
        val markdown = markdownFile.readText()
        val baseDir = markdownFile.parentFile

        val warnings = mutableListOf<String>()

        // Convert markdown to HTML body
        var bodyHtml = MarkdownHtmlConverter.convert(markdown)

        // Resolve image paths
        if (options.resolveImages || options.embedImages) {
            bodyHtml = resolveImagePaths(bodyHtml, baseDir, options.embedImages, warnings)
        }

        // Validate links
        if (options.validateLinks) {
            warnings.addAll(validateLinks(bodyHtml, baseDir, markdown))
        }

        // Convert .md links to .html in exported output
        if (options.convertMdLinks) {
            bodyHtml = convertMdLinksToHtml(bodyHtml)
        }

        // Build styled document
        val themeCss = if (options.pureCss) "" else loadThemeCss(options.theme)
        val customCss = if (options.pureCss) "" else PreviewTheme.loadCustomCss(options.customCssPath)
        val title = extractTitle(markdown, markdownFile.nameWithoutExtension)
        val fullHtml = wrapInExportDocument(bodyHtml, themeCss, customCss, title)

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(fullHtml)

        return ExportResult(outputFile, warnings)
    }

    /**
     * Batch export all markdown files in a directory.
     */
    fun exportDirectory(
        inputDir: File,
        outputDir: File,
        options: ExportOptions = defaultOptions(),
        recursive: Boolean = false
    ): List<ExportResult> {
        val results = mutableListOf<ExportResult>()
        val mdFiles = if (recursive) {
            inputDir.walkTopDown().filter { isMarkdownFile(it) }.toList()
        } else {
            inputDir.listFiles()?.filter { isMarkdownFile(it) }?.toList() ?: emptyList()
        }

        for (mdFile in mdFiles) {
            val relativePath = inputDir.toPath().relativize(mdFile.toPath())
            val outputFile = outputDir.resolve(
                relativePath.toString().replaceAfterLast('.', "html")
            )
            results.add(exportFile(mdFile, outputFile, options))
        }

        return results
    }

    /**
     * Resolve relative image paths to absolute paths or embed as base64.
     */
    fun resolveImagePaths(
        html: String,
        baseDir: File,
        embedBase64: Boolean,
        warnings: MutableList<String>
    ): String {
        val imgPattern = Regex("""<img\s+src="([^"]+)"([^>]*)>""")

        return imgPattern.replace(html) { match ->
            val src = match.groupValues[1]
            val rest = match.groupValues[2]

            // Skip URLs (http://, https://, data:)
            if (src.startsWith("http://") || src.startsWith("https://") || src.startsWith("data:")) {
                match.value
            } else {
                val imageFile = resolveRelativePath(src, baseDir)
                if (imageFile != null && imageFile.exists()) {
                    if (embedBase64) {
                        val mimeType = guessMimeType(imageFile.name)
                        val encoded = Base64.getEncoder().encodeToString(imageFile.readBytes())
                        """<img src="data:$mimeType;base64,$encoded"$rest>"""
                    } else {
                        val absolutePath = imageFile.absolutePath.replace("\\", "/")
                        """<img src="file://$absolutePath"$rest>"""
                    }
                } else {
                    warnings.add("Image not found: $src")
                    match.value
                }
            }
        }
    }

    /**
     * Validate internal links and references in the document.
     */
    fun validateLinks(html: String, baseDir: File, markdown: String): List<String> {
        val warnings = mutableListOf<String>()

        // Validate anchor links (#heading-slug)
        val anchorPattern = Regex("""<a\s+href="#([^"]+)"[^>]*>""")
        val headingIds = Regex("""<h[1-6]\s+id="([^"]+)"[^>]*>""").findAll(html)
            .map { it.groupValues[1] }.toSet()

        for (match in anchorPattern.findAll(html)) {
            val anchor = match.groupValues[1]
            if (anchor !in headingIds) {
                warnings.add("Broken anchor link: #$anchor")
            }
        }

        // Validate relative file links
        val linkPattern = Regex("""<a\s+href="([^"#][^"]*)"[^>]*>""")
        for (match in linkPattern.findAll(html)) {
            val href = match.groupValues[1]
            if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("mailto:")) {
                continue
            }
            val linkedFile = resolveRelativePath(href, baseDir)
            if (linkedFile == null || !linkedFile.exists()) {
                warnings.add("Broken file link: $href")
            }
        }

        // Validate reference-style links [label]: url
        val refPattern = Regex("""\[([^\]]+)\]\[([^\]]*)\]""")
        val refDefs = Regex("""^\[([^\]]+)\]:\s+""", RegexOption.MULTILINE)
            .findAll(markdown).map { it.groupValues[1].lowercase() }.toSet()

        for (match in refPattern.findAll(markdown)) {
            val label = (match.groupValues[2].ifEmpty { match.groupValues[1] }).lowercase()
            if (label !in refDefs) {
                warnings.add("Undefined reference link: [$label]")
            }
        }

        return warnings
    }

    /**
     * Extract document title from first heading or filename.
     */
    fun extractTitle(markdown: String, fallback: String): String {
        // Priority 1: <!-- title: Your Title --> comment
        val titleComment = Regex("<!--\\s*title:\\s*(.+?)\\s*-->").find(markdown)
        if (titleComment != null) {
            return titleComment.groupValues[1].trim()
        }
        // Priority 2: First ATX heading
        val headingMatch = Regex("^#{1,6}\\s+(.+)$", RegexOption.MULTILINE).find(markdown)
        if (headingMatch != null) {
            return headingMatch.groupValues[1].replace(Regex("\\s+#+\\s*$"), "").trim()
        }
        // Priority 3: Setext heading
        val lines = markdown.lines()
        for (i in 0 until lines.size - 1) {
            if (lines[i].isNotBlank() && lines[i + 1].trim().matches(Regex("^[=]+$"))) {
                return lines[i].trim()
            }
        }
        return fallback
    }

    /**
     * Load theme CSS, falling back to GitHub if the theme resource isn't available.
     */
    fun loadThemeCss(theme: PreviewTheme.Theme): String {
        return try {
            PreviewTheme.loadThemeCss(theme)
        } catch (_: Exception) {
            // Fallback: inline minimal CSS
            FALLBACK_CSS
        }
    }

    fun defaultOptions(): ExportOptions {
        return try {
            val state = MarkdownSettings.getInstance().state
            val theme = PreviewTheme.Theme.fromName(state.previewTheme)
            ExportOptions(
                theme = theme,
                customCssPath = state.previewCustomCssPath,
                embedImages = state.exportEmbedImages,
                validateLinks = state.exportValidateLinks,
                convertMdLinks = state.exportConvertMdLinks,
                pureCss = state.exportPureCss
            )
        } catch (_: Exception) {
            ExportOptions()
        }
    }

    /**
     * Rewrite internal .md links to .html in exported HTML output.
     * Preserves anchor fragments: file.md#heading → file.html#heading
     */
    fun convertMdLinksToHtml(html: String): String {
        return html.replace(Regex("""<a\s+href="([^"]+\.md)(#[^"]*)?"""")) { match ->
            val path = match.groupValues[1]
            // Skip external URLs
            if (path.startsWith("http://") || path.startsWith("https://")) {
                match.value
            } else {
                val htmlPath = path.replaceAfterLast('.', "html")
                val anchor = match.groupValues[2]
                """<a href="$htmlPath$anchor""""
            }
        }
    }

    private fun wrapInExportDocument(
        bodyHtml: String,
        themeCss: String,
        customCss: String,
        title: String
    ): String {
        val escapedTitle = MarkdownHtmlConverter.escapeHtml(title)
        return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="generator" content="Markdown All-in-One for PHPStorm">
<title>$escapedTitle</title>
<style>
$themeCss
</style>
${if (customCss.isNotEmpty()) "<style>\n$customCss\n</style>" else ""}
</head>
<body class="markdown-body">
$bodyHtml
</body>
</html>"""
    }

    private fun resolveRelativePath(path: String, baseDir: File): File? {
        return try {
            val decoded = java.net.URLDecoder.decode(path, "UTF-8")
            val resolved = baseDir.resolve(decoded).canonicalFile
            resolved
        } catch (_: Exception) {
            null
        }
    }

    private fun guessMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            "bmp" -> "image/bmp"
            "ico" -> "image/x-icon"
            else -> "application/octet-stream"
        }
    }

    private fun isMarkdownFile(file: File): Boolean {
        return file.isFile && file.extension.lowercase() in setOf("md", "markdown", "mdown", "mkd", "mkdn")
    }

    private const val FALLBACK_CSS = """
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
    font-size: 16px;
    line-height: 1.5;
    max-width: 980px;
    margin: 0 auto;
    padding: 45px;
    color: #24292e;
}
h1, h2, h3, h4, h5, h6 { margin-top: 24px; margin-bottom: 16px; font-weight: 600; }
h1 { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: .3em; }
h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: .3em; }
code { padding: .2em .4em; background-color: rgba(27,31,35,.05); border-radius: 3px; }
pre { padding: 16px; overflow: auto; background-color: #f6f8fa; border-radius: 3px; }
pre code { padding: 0; background: transparent; }
table { border-collapse: collapse; }
table th, table td { padding: 6px 13px; border: 1px solid #dfe2e5; }
table tr:nth-child(2n) { background-color: #f6f8fa; }
blockquote { padding: 0 1em; color: #6a737d; border-left: .25em solid #dfe2e5; }
img { max-width: 100%; }
"""
}
