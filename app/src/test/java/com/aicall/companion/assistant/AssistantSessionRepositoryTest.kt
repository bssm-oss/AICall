package com.aicall.companion.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantSessionRepositoryTest {
    @Test
    fun `appendEntry prepends newest exchange`() {
        val existing = listOf(
            AssistantExchange("2026-04-07 12:00:00", "Older", "Old reply", "Demo"),
        )

        val updated = AssistantSessionRepository.appendEntry(
            existing = existing,
            entry = AssistantExchange("2026-04-07 12:01:00", "Newest", "New reply", "Backend"),
            maxEntries = 5,
        )

        assertEquals("Newest", updated.first().callerText)
        assertEquals("Older", updated.last().callerText)
    }

    @Test
    fun `appendEntry trims to max entries`() {
        val existing = listOf(
            AssistantExchange("1", "one", "reply1", "Demo"),
            AssistantExchange("2", "two", "reply2", "Demo"),
            AssistantExchange("3", "three", "reply3", "Demo"),
        )

        val updated = AssistantSessionRepository.appendEntry(
            existing = existing,
            entry = AssistantExchange("4", "four", "reply4", "Backend"),
            maxEntries = 3,
        )

        assertEquals(3, updated.size)
        assertEquals("four", updated.first().callerText)
        assertEquals("two", updated.last().callerText)
    }
}
