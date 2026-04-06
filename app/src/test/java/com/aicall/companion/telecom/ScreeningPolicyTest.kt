package com.aicall.companion.telecom

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreeningPolicyTest {
    @Test
    fun `returns silence when number ends with configured suffix`() {
        val decision = ScreeningPolicy.evaluate("01012340000", "0000")

        assertEquals(ScreeningDecision.Silence, decision)
    }

    @Test
    fun `returns allow when suffix does not match`() {
        val decision = ScreeningPolicy.evaluate("01012345678", "0000")

        assertEquals(ScreeningDecision.Allow, decision)
    }
}
