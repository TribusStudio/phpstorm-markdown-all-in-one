package com.tribus.markdown.actions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MoveLineActionTest {

    @Test
    fun `renumberOrderedLists renumbers sequential items`() {
        val doc = TestDocument("1. first\n3. second\n5. third")
        MoveLineAction.renumberOrderedLists(doc, 0, 2)
        assertEquals("1. first\n2. second\n3. third", doc.currentText())
    }

    @Test
    fun `renumberOrderedLists preserves indent levels`() {
        // Inner items at different indent are renumbered independently
        val doc = TestDocument("   1. inner a\n   5. inner b")
        MoveLineAction.renumberOrderedLists(doc, 0, 1)
        assertEquals("   1. inner a\n   2. inner b", doc.currentText())
    }

    @Test
    fun `renumberOrderedLists skips unordered lists`() {
        val doc = TestDocument("- one\n- two\n- three")
        MoveLineAction.renumberOrderedLists(doc, 0, 2)
        assertEquals("- one\n- two\n- three", doc.currentText())
    }

    @Test
    fun `renumberOrderedLists handles parenthesis delimiter`() {
        val doc = TestDocument("1) first\n1) second\n1) third")
        MoveLineAction.renumberOrderedLists(doc, 0, 2)
        assertEquals("1) first\n2) second\n3) third", doc.currentText())
    }

    @Test
    fun `renumberOrderedLists handles blank lines in list`() {
        val doc = TestDocument("1. first\n\n3. second")
        MoveLineAction.renumberOrderedLists(doc, 0, 2)
        assertEquals("1. first\n\n2. second", doc.currentText())
    }
}

/**
 * Simple document mock for testing renumber logic without a full IntelliJ Document.
 */
class TestDocument(initialText: String) : com.intellij.openapi.editor.Document {
    private var _text: String = initialText

    fun currentText(): String = _text

    private val lines get() = _text.split("\n")

    override fun getText(): String = _text
    override fun getText(textRange: com.intellij.openapi.util.TextRange): String =
        _text.substring(textRange.startOffset, textRange.endOffset)

    override fun getLineCount(): Int = lines.size

    override fun getLineStartOffset(line: Int): Int {
        var offset = 0
        for (i in 0 until line) {
            offset += lines[i].length + 1
        }
        return offset
    }

    override fun getLineEndOffset(line: Int): Int =
        getLineStartOffset(line) + lines[line].length

    override fun replaceString(startOffset: Int, endOffset: Int, s: CharSequence) {
        _text = _text.substring(0, startOffset) + s + _text.substring(endOffset)
    }

    override fun getTextLength(): Int = _text.length
    override fun getLineNumber(offset: Int): Int {
        var o = 0
        for ((i, line) in lines.withIndex()) {
            if (offset <= o + line.length) return i
            o += line.length + 1
        }
        return lines.size - 1
    }

    // Unused stubs
    override fun getCharsSequence(): CharSequence = _text
    override fun getImmutableCharSequence(): CharSequence = _text
    override fun getModificationStamp(): Long = 0
    override fun isWritable(): Boolean = true
    override fun insertString(offset: Int, s: CharSequence) {
        _text = _text.substring(0, offset) + s + _text.substring(offset)
    }
    override fun deleteString(startOffset: Int, endOffset: Int) {
        _text = _text.substring(0, startOffset) + _text.substring(endOffset)
    }
    override fun createRangeMarker(startOffset: Int, endOffset: Int) = throw UnsupportedOperationException()
    override fun createRangeMarker(startOffset: Int, endOffset: Int, surviveOnExternalChange: Boolean) = throw UnsupportedOperationException()
    override fun createGuardedBlock(startOffset: Int, endOffset: Int) = throw UnsupportedOperationException()
    override fun removeGuardedBlock(block: com.intellij.openapi.editor.RangeMarker) {}
    override fun getOffsetGuard(offset: Int) = null
    override fun getRangeGuard(start: Int, end: Int) = null
    override fun startGuardedBlockChecking() {}
    override fun stopGuardedBlockChecking() {}
    override fun addDocumentListener(listener: com.intellij.openapi.editor.event.DocumentListener) {}
    override fun addDocumentListener(listener: com.intellij.openapi.editor.event.DocumentListener, parentDisposable: com.intellij.openapi.Disposable) {}
    override fun removeDocumentListener(listener: com.intellij.openapi.editor.event.DocumentListener) {}
    override fun setReadOnly(isReadOnly: Boolean) {}
    override fun fireReadOnlyModificationAttempt() {}
    override fun addPropertyChangeListener(listener: java.beans.PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: java.beans.PropertyChangeListener) {}
    override fun setText(text: CharSequence) { _text = text.toString() }
    override fun setCyclicBufferSize(bufferSize: Int) {}
    override fun <T : Any?> getUserData(key: com.intellij.openapi.util.Key<T>): T? = null
    override fun <T : Any?> putUserData(key: com.intellij.openapi.util.Key<T>, value: T?) {}
}
