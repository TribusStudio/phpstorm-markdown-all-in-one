package com.tribus.markdown.actions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToggleMathActionTest {

    @Test
    fun `plain text wraps in inline math`() {
        assertEquals("\$x^2\$", ToggleMathAction.toggleMath("x^2"))
    }

    @Test
    fun `inline math upgrades to display math`() {
        assertEquals("\$\$x^2\$\$", ToggleMathAction.toggleMath("\$x^2\$"))
    }

    @Test
    fun `display math unwraps to plain text`() {
        assertEquals("x^2", ToggleMathAction.toggleMath("\$\$x^2\$\$"))
    }

    @Test
    fun `reverse plain text wraps in display math`() {
        assertEquals("\$\$x^2\$\$", ToggleMathAction.toggleMath("x^2", reverse = true))
    }

    @Test
    fun `reverse display math downgrades to inline math`() {
        assertEquals("\$x^2\$", ToggleMathAction.toggleMath("\$\$x^2\$\$", reverse = true))
    }

    @Test
    fun `reverse inline math unwraps to plain text`() {
        assertEquals("x^2", ToggleMathAction.toggleMath("\$x^2\$", reverse = true))
    }

    @Test
    fun `complex expression cycles correctly`() {
        val expr = "\\frac{a}{b}"
        val inline = "\$\\frac{a}{b}\$"
        val display = "\$\$\\frac{a}{b}\$\$"

        assertEquals(inline, ToggleMathAction.toggleMath(expr))
        assertEquals(display, ToggleMathAction.toggleMath(inline))
        assertEquals(expr, ToggleMathAction.toggleMath(display))
    }

    @Test
    fun `empty delimiters are not treated as math`() {
        // $$ is just two dollar signs, not display math wrapping empty content
        assertEquals("\$\$\$", ToggleMathAction.toggleMath("\$"))
    }
}
