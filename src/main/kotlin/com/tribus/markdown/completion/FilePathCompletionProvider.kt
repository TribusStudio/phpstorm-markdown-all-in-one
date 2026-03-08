package com.tribus.markdown.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ProcessingContext

/**
 * Provides file and image path completion inside link/image parentheses.
 * Triggers after `](` or `(` in image/link contexts.
 *
 * Completes relative paths from the current file's directory.
 */
class FilePathCompletionProvider : CompletionProvider<CompletionParameters>() {

    // Matches `](` or `![...](` before cursor — link or image path context
    private val LINK_PATH_BEFORE = Regex("""!?\[[^\]]*\]\([^)]*$""")

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "svg", "webp", "bmp", "ico", "tiff")
    private val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd", "mkdn")

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

        if (!LINK_PATH_BEFORE.containsMatchIn(textBefore)) return

        // Find the start of the path (after the `(`)
        val parenIndex = textBefore.lastIndexOf("(")
        if (parenIndex < 0) return
        val typedPath = textBefore.substring(parenIndex + 1)

        // Skip if it looks like a URL
        if (typedPath.matches(Regex("^https?://.*"))) return

        val currentFile = parameters.originalFile.virtualFile ?: return
        val baseDir = currentFile.parent ?: return

        // Resolve the directory from the typed path
        val (searchDir, pathPrefix) = resolveSearchDir(baseDir, typedPath)
        if (searchDir == null || !searchDir.isValid || !searchDir.isDirectory) return

        val resultWithPrefix = result.withPrefixMatcher(typedPath)
        val children = searchDir.children ?: return

        for (child in children.sortedBy { it.name }) {
            if (child.name.startsWith(".")) continue

            val completionPath = if (pathPrefix.isEmpty()) child.name
            else "$pathPrefix${child.name}"

            val element = if (child.isDirectory) {
                LookupElementBuilder.create("$completionPath/")
                    .withPresentableText(child.name + "/")
                    .withIcon(AllIcons.Nodes.Folder)
                    .withTypeText("dir")
                    .withInsertHandler { ctx: InsertionContext, _: LookupElement ->
                        // Re-trigger completion to browse into the directory
                        com.intellij.codeInsight.AutoPopupController
                            .getInstance(ctx.project)
                            .scheduleAutoPopup(ctx.editor)
                    }
            } else {
                val ext = child.extension?.lowercase() ?: ""
                val icon = when {
                    ext in IMAGE_EXTENSIONS -> AllIcons.FileTypes.Any_type
                    ext in MARKDOWN_EXTENSIONS -> AllIcons.FileTypes.Text
                    else -> AllIcons.FileTypes.Any_type
                }
                LookupElementBuilder.create(completionPath)
                    .withPresentableText(child.name)
                    .withIcon(icon)
                    .withTypeText(ext)
                    .withInsertHandler { ctx: InsertionContext, _: LookupElement ->
                        // Append closing ) if not already present
                        val doc = ctx.document
                        val tail = ctx.tailOffset
                        if (tail >= doc.textLength || doc.charsSequence[tail] != ')') {
                            doc.insertString(tail, ")")
                            ctx.editor.caretModel.moveToOffset(tail + 1)
                        }
                    }
            }

            resultWithPrefix.addElement(element)
        }
    }

    /**
     * Resolve the directory to search in based on the typed path fragment.
     * Returns (directory, prefix) where prefix is the path components already typed.
     */
    private fun resolveSearchDir(baseDir: VirtualFile, typedPath: String): Pair<VirtualFile?, String> {
        if (typedPath.isEmpty()) return Pair(baseDir, "")

        val lastSlash = typedPath.lastIndexOf("/")
        if (lastSlash < 0) return Pair(baseDir, "")

        val dirPath = typedPath.substring(0, lastSlash + 1)
        var current = baseDir

        for (segment in dirPath.split("/").filter { it.isNotEmpty() }) {
            if (segment == "..") {
                current = current.parent ?: return Pair(null, "")
            } else if (segment != ".") {
                current = current.findChild(segment) ?: return Pair(null, "")
            }
        }

        return Pair(current, dirPath)
    }
}
