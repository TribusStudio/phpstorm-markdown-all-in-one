package com.tribus.markdown.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.util.ProcessingContext

/**
 * Provides reference link label completion for `[text][label]` patterns.
 * Scans the document for `[label]: url` definitions and offers them as completions.
 */
class ReferenceLinkCompletionProvider : CompletionProvider<CompletionParameters>() {

    // Matches `[...][` before cursor to detect reference link context
    private val REF_LINK_BEFORE = Regex("""\[[^\]]*\]\[[^\]]*$""")

    // Matches reference link definitions: [label]: url
    private val REF_DEFINITION = Regex("""^\s{0,3}\[([^\]]+)\]:\s+(.+)$""", RegexOption.MULTILINE)

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

        if (!REF_LINK_BEFORE.containsMatchIn(textBefore)) return

        // Find prefix after the last `[`
        val bracketIndex = textBefore.lastIndexOf("[")
        if (bracketIndex < 0) return
        val prefix = textBefore.substring(bracketIndex + 1)

        val resultWithPrefix = result.withPrefixMatcher(prefix)

        // Find all reference definitions in the document
        val definitions = REF_DEFINITION.findAll(document.text)
        val seen = mutableSetOf<String>()

        for (match in definitions) {
            val label = match.groupValues[1]
            val url = match.groupValues[2].trim()

            if (label.lowercase() in seen) continue
            seen.add(label.lowercase())

            resultWithPrefix.addElement(
                LookupElementBuilder.create(label)
                    .withTailText("  $url", true)
                    .withTypeText("ref")
                    .withIcon(AllIcons.Nodes.PpWeb)
            )
        }
    }
}
