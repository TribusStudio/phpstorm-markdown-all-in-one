package com.tribus.markdown.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.tribus.markdown.lang.MarkdownLanguage

/**
 * Registers completion providers for markdown-specific content:
 * - File/image path completion inside `()` and `[]()`
 * - Heading reference completion for `[text](#heading)`
 * - Reference link label completion for `[text][label]`
 */
class MarkdownCompletionContributor : CompletionContributor() {
    init {
        val markdownPattern = PlatformPatterns.psiElement().withLanguage(MarkdownLanguage)

        extend(
            CompletionType.BASIC,
            markdownPattern,
            HeadingReferenceCompletionProvider()
        )

        extend(
            CompletionType.BASIC,
            markdownPattern,
            ReferenceLinkCompletionProvider()
        )

        extend(
            CompletionType.BASIC,
            markdownPattern,
            FilePathCompletionProvider()
        )

        extend(
            CompletionType.BASIC,
            markdownPattern,
            MathCompletionProvider()
        )
    }
}
