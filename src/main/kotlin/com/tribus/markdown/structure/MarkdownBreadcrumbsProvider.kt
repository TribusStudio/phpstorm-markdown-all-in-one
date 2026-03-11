package com.tribus.markdown.structure

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider
import com.tribus.markdown.lang.MarkdownLanguage
import com.tribus.markdown.toc.HeadingExtractor

/**
 * Provides breadcrumb navigation showing the heading hierarchy path
 * at the current cursor position in the editor.
 */
class MarkdownBreadcrumbsProvider : BreadcrumbsProvider {

    override fun getLanguages(): Array<Language> = arrayOf(MarkdownLanguage)

    override fun acceptElement(element: PsiElement): Boolean {
        // We handle breadcrumbs at the file level via getElementInfo
        return false
    }

    override fun getElementInfo(element: PsiElement): String = element.text

    /**
     * Get the heading breadcrumb path for a given offset in the document.
     * Returns the chain of ancestor headings from outermost to innermost.
     */
    companion object {
        fun getBreadcrumbPath(documentText: String, offset: Int): List<HeadingExtractor.Heading> {
            val headings = HeadingExtractor.extract(documentText)
            if (headings.isEmpty()) return emptyList()

            // Find which line the offset is on
            val targetLine = documentText.substring(0, offset.coerceAtMost(documentText.length))
                .count { it == '\n' }

            // Build the breadcrumb path: find the heading chain that contains this line
            val path = mutableListOf<HeadingExtractor.Heading>()

            for (heading in headings) {
                if (heading.lineNumber > targetLine) break

                // Pop headings of same or deeper level (we're entering a new section)
                while (path.isNotEmpty() && path.last().level >= heading.level) {
                    path.removeAt(path.size - 1)
                }
                path.add(heading)
            }

            return path
        }
    }
}
