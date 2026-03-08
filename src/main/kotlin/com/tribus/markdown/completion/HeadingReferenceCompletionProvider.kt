package com.tribus.markdown.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.util.ProcessingContext
import com.tribus.markdown.toc.HeadingExtractor
import com.tribus.markdown.toc.Slugify

/**
 * Provides heading reference completion for `[text](#heading)` patterns.
 * Triggers after `(#` in markdown link contexts.
 */
class HeadingReferenceCompletionProvider : CompletionProvider<CompletionParameters>() {

    // Matches `[...](#` pattern before cursor to detect heading reference context
    private val HEADING_REF_BEFORE = Regex("""\[[^\]]*\]\(#[^)]*$""")

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val editor = parameters.editor
        val document = editor.document
        val offset = parameters.offset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val textBefore = document.getText(com.intellij.openapi.util.TextRange(lineStart, offset))

        if (!HEADING_REF_BEFORE.containsMatchIn(textBefore)) return

        // Find the start of the fragment (after `#`)
        val hashIndex = textBefore.lastIndexOf("#")
        if (hashIndex < 0) return
        val prefix = textBefore.substring(hashIndex + 1)

        val headings = HeadingExtractor.extract(document.text)
        val slugOccurrences = mutableMapOf<String, Int>()

        val resultWithPrefix = result.withPrefixMatcher(prefix)

        for (heading in headings) {
            val slug = Slugify.slugify(heading.rawText)
            val uniqueSlug = Slugify.makeUnique(slug, slugOccurrences)

            resultWithPrefix.addElement(
                LookupElementBuilder.create(uniqueSlug)
                    .withPresentableText(heading.rawText)
                    .withTailText("  #$uniqueSlug", true)
                    .withTypeText("H${heading.level}")
                    .withIcon(AllIcons.Nodes.Tag)
                    .withInsertHandler { ctx: InsertionContext, _: LookupElement ->
                        // Append closing ) if not already present
                        val doc = ctx.document
                        val tail = ctx.tailOffset
                        if (tail >= doc.textLength || doc.charsSequence[tail] != ')') {
                            doc.insertString(tail, ")")
                            ctx.editor.caretModel.moveToOffset(tail + 1)
                        }
                    }
            )
        }
    }
}
