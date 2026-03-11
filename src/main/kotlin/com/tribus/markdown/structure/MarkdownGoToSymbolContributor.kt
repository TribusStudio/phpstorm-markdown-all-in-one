package com.tribus.markdown.structure

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import com.tribus.markdown.lang.MarkdownFileType
import com.tribus.markdown.toc.HeadingExtractor

/**
 * Contributes markdown headings to Go To Symbol (Ctrl+Shift+Alt+N).
 * Each heading in every markdown file is searchable as a symbol.
 */
class MarkdownGoToSymbolContributor : ChooseByNameContributorEx {

    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        val project = scope.project ?: return
        val psiManager = PsiManager.getInstance(project)

        FileTypeIndex.processFiles(MarkdownFileType.INSTANCE, { virtualFile ->
            val psiFile = psiManager.findFile(virtualFile) ?: return@processFiles true
            val headings = HeadingExtractor.extract(psiFile.text)
            for (heading in headings) {
                if (!processor.process(heading.rawText)) return@processFiles false
            }
            true
        }, scope)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        val project = parameters.project
        val psiManager = PsiManager.getInstance(project)
        val scope = parameters.searchScope

        FileTypeIndex.processFiles(MarkdownFileType.INSTANCE, { virtualFile ->
            val psiFile = psiManager.findFile(virtualFile) ?: return@processFiles true
            val text = psiFile.text
            val headings = HeadingExtractor.extract(text)

            for (heading in headings) {
                if (heading.rawText == name) {
                    val item = MarkdownHeadingNavigationItem(heading, psiFile, text)
                    if (!processor.process(item)) return@processFiles false
                }
            }
            true
        }, scope)
    }
}
