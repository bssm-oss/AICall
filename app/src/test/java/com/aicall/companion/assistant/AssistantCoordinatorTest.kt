package com.aicall.companion.assistant

import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantCoordinatorTest {
    @Test
    fun `demo reply references local fallback`() {
        val reply = AssistantCoordinator.buildDemoReply("Can you take a message?")

        assertTrue(reply.contains("Can you take a message?"))
        assertTrue(reply.contains("Codex access token이 연결되지 않아"))
    }
}
