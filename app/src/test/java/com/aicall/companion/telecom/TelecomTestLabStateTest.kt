package com.aicall.companion.telecom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelecomTestLabStateTest {
    @Test
    fun `incoming ringing updates only test call summary`() {
        val updated = TelecomTestLabState().apply(TelecomTestLabAction.IncomingRinging)

        assertTrue(updated.latestCallSummary.contains("RINGING"))
        assertEquals("테스트 전용 스크리닝 이벤트 없음.", updated.latestScreeningSummary)
    }

    @Test
    fun `screening silence keeps call summary and marks screening as test only`() {
        val updated = TelecomTestLabState(
            latestCallSummary = "기존 테스트 통화 상태",
        ).apply(TelecomTestLabAction.ScreeningSilence)

        assertEquals("기존 테스트 통화 상태", updated.latestCallSummary)
        assertTrue(updated.latestScreeningSummary.contains("Silence"))
        assertTrue(TelecomTestLabAction.ScreeningSilence.toHistoryMessage().contains("[TEST ONLY]"))
    }

    @Test
    fun `reset returns default test lab state`() {
        val updated = TelecomTestLabState(
            latestCallSummary = "가짜 연결",
            latestScreeningSummary = "가짜 허용",
        ).apply(TelecomTestLabAction.Reset)

        assertEquals(TelecomTestLabState(), updated)
    }
}
