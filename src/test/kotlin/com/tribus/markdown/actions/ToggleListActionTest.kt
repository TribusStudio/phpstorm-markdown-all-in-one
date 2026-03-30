package com.tribus.markdown.actions

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToggleListActionTest {

    @Test
    fun `parseCandidates splits comma-separated values`() {
        val result = ToggleListAction.parseCandidates("-, *, +, 1., 1)")
        assertEquals(listOf("-", "*", "+", "1.", "1)"), result)
    }

    @Test
    fun `parseCandidates returns defaults for empty string`() {
        val result = ToggleListAction.parseCandidates("")
        assertEquals(ToggleListAction.DEFAULT_CANDIDATES, result)
    }

    @Test
    fun `parseCandidates handles custom subset`() {
        val result = ToggleListAction.parseCandidates("-, 1.")
        assertEquals(listOf("-", "1."), result)
    }
}
