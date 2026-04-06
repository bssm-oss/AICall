package com.aicall.companion.telecom

import org.junit.Assert.assertEquals
import org.junit.Test

class TelecomHistoryRepositoryTest {
    @Test
    fun `appendEntry prepends newest event`() {
        val existing = listOf(
            TelecomHistoryEntry("2026-04-07 10:00:00", "Older"),
        )

        val updated = TelecomHistoryRepository.appendEntry(
            existing = existing,
            entry = TelecomHistoryEntry("2026-04-07 10:01:00", "Newest"),
            maxEvents = 5,
        )

        assertEquals("Newest", updated.first().message)
        assertEquals("Older", updated.last().message)
    }

    @Test
    fun `appendEntry trims to max size`() {
        val existing = (1..3).map {
            TelecomHistoryEntry("2026-04-07 10:0$it:00", "Entry $it")
        }

        val updated = TelecomHistoryRepository.appendEntry(
            existing = existing,
            entry = TelecomHistoryEntry("2026-04-07 10:04:00", "Newest"),
            maxEvents = 3,
        )

        assertEquals(3, updated.size)
        assertEquals("Newest", updated.first().message)
        assertEquals("Entry 2", updated.last().message)
    }
}
