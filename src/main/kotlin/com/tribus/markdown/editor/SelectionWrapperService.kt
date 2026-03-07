package com.tribus.markdown.editor

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke

/**
 * Registers selection-aware character wrapping directly on the editor component
 * via Swing's InputMap/ActionMap. When text is selected and a trigger character
 * is typed, it wraps or transforms the selection instead of replacing it.
 *
 * Uses KEY_TYPED keystrokes which fire for plain characters (*, ~, _, |, -).
 * This is lower-level than IntelliJ's action system (which uses KEY_PRESSED)
 * and reliably intercepts typed characters before the editor processes them.
 *
 * When no text is selected, the binding is a no-op and the character types
 * normally via the WHEN_ANCESTOR_OF_FOCUSED_COMPONENT fallback.
 */
object SelectionWrapperService {

    private val WRAPPER_CHARS = mapOf(
        '*' to WrapSpec.Symmetric("*"),
        '~' to WrapSpec.Symmetric("~"),
        '_' to WrapSpec.Symmetric("_"),
        '`' to WrapSpec.Symmetric("`"),
        '|' to WrapSpec.Pipe,
        '-' to WrapSpec.Dash,
    )

    /**
     * Registers KEY_TYPED bindings on the editor component for all wrapper characters.
     * Must be called when a markdown editor is created.
     */
    fun registerOn(editor: Editor) {
        val component = editor.contentComponent
        val inputMap = component.getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = component.actionMap

        for ((char, spec) in WRAPPER_CHARS) {
            val keyStroke = KeyStroke.getKeyStroke(char)
            val actionKey = "markdown-wrap-selection-$char"

            inputMap.put(keyStroke, actionKey)
            actionMap.put(actionKey, WrapAction(editor, char, spec))
        }
    }

    /**
     * Swing action that wraps the selection when triggered by a KEY_TYPED event.
     * If no selection exists, does nothing — Swing falls through to default typing.
     */
    private class WrapAction(
        private val editor: Editor,
        private val char: Char,
        private val spec: WrapSpec
    ) : AbstractAction() {

        override fun actionPerformed(e: ActionEvent) {
            val selectionModel = editor.selectionModel
            if (!selectionModel.hasSelection()) {
                // No selection — insert the character normally
                val project = editor.project
                WriteCommandAction.runWriteCommandAction(project) {
                    editor.document.insertString(editor.caretModel.offset, char.toString())
                    editor.caretModel.moveToOffset(editor.caretModel.offset + 1)
                }
                return
            }

            val project = editor.project ?: return
            WriteCommandAction.runWriteCommandAction(project) {
                val selectedText = selectionModel.selectedText ?: return@runWriteCommandAction
                val start = selectionModel.selectionStart
                val end = selectionModel.selectionEnd
                val docText = editor.document.charsSequence

                val replacement = spec.transform(selectedText, docText, start, end)
                editor.document.replaceString(start, end, replacement)
                editor.caretModel.moveToOffset(start + replacement.length)
                selectionModel.removeSelection()
            }
        }
    }

    sealed class WrapSpec {
        abstract fun transform(selected: String, docText: CharSequence, start: Int, end: Int): String

        /**
         * Wraps selection symmetrically: marker + selection + marker
         */
        class Symmetric(private val marker: String) : WrapSpec() {
            override fun transform(selected: String, docText: CharSequence, start: Int, end: Int): String {
                return "$marker$selected$marker"
            }
        }

        /**
         * Pipe wrapper for table cell creation:
         * - If no pipe before selection: wraps as "| selection |"
         * - If pipe already exists before selection: only adds closing " |"
         */
        object Pipe : WrapSpec() {
            override fun transform(selected: String, docText: CharSequence, start: Int, end: Int): String {
                return if (hasPrecedingPipe(docText, start)) {
                    "$selected |"
                } else {
                    "| $selected |"
                }
            }

            private fun hasPrecedingPipe(text: CharSequence, selectionStart: Int): Boolean {
                var i = selectionStart - 1
                while (i >= 0 && text[i] == ' ') i--
                return i >= 0 && text[i] == '|'
            }
        }

        /**
         * Dash wrapper for table header borders:
         * - Inside a table cell (between pipes): fills with dashes of same length
         * - Outside table context: wraps symmetrically with dashes
         */
        object Dash : WrapSpec() {
            override fun transform(selected: String, docText: CharSequence, start: Int, end: Int): String {
                return if (isInsideTableCell(docText, start, end)) {
                    "-".repeat(selected.length)
                } else {
                    "-$selected-"
                }
            }

            private fun isInsideTableCell(text: CharSequence, selStart: Int, selEnd: Int): Boolean {
                var lineStart = selStart
                while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
                var lineEnd = selEnd
                while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++
                val beforeSel = text.subSequence(lineStart, selStart)
                val afterSel = text.subSequence(selEnd, lineEnd)
                return beforeSel.contains('|') && afterSel.contains('|')
            }
        }
    }
}
