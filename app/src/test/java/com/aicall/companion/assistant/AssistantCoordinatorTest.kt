package com.aicall.companion.assistant

import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantCoordinatorTest {
    @Test
    fun `demo reply references local fallback`() {
        val reply = AssistantCoordinator.buildDemoReply("Can you take a message?")

        assertTrue(reply.contains("Can you take a message?"))
        assertTrue(reply.contains("데모 엔진 응답"))
    }
}
