package com.tribus.markdown.completion

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MathCompletionProviderTest {

    @Test
    fun `detects cursor inside inline math`() {
        // $x| where | is cursor at position 2
        assertTrue(MathCompletionProvider.isInsideMath("\$x", 2))
    }

    @Test
    fun `detects cursor inside display math`() {
        // $$x| where | is cursor at position 3
        assertTrue(MathCompletionProvider.isInsideMath("\$\$x", 3))
    }

    @Test
    fun `cursor outside math returns false`() {
        assertFalse(MathCompletionProvider.isInsideMath("hello world", 5))
    }

    @Test
    fun `cursor after closed inline math returns false`() {
        // $x$ hello| — cursor is outside math
        assertFalse(MathCompletionProvider.isInsideMath("\$x\$ hello", 9))
    }

    @Test
    fun `cursor after closed display math returns false`() {
        assertFalse(MathCompletionProvider.isInsideMath("\$\$x\$\$ hello", 11))
    }

    @Test
    fun `cursor between two inline math expressions returns false`() {
        // $a$ text $b$ — cursor at "text" (position 5)
        assertFalse(MathCompletionProvider.isInsideMath("\$a\$ text \$b\$", 5))
    }

    @Test
    fun `math commands list is not empty`() {
        assertTrue(MathCompletionProvider.MATH_COMMANDS.isNotEmpty())
    }

    @Test
    fun `math commands include common symbols`() {
        val commands = MathCompletionProvider.MATH_COMMANDS.map { it.first }
        assertTrue(commands.contains("alpha"))
        assertTrue(commands.contains("frac{}{}"))
        assertTrue(commands.contains("sum"))
        assertTrue(commands.contains("int"))
        assertTrue(commands.contains("infty"))
        assertTrue(commands.contains("rightarrow"))
    }
}
