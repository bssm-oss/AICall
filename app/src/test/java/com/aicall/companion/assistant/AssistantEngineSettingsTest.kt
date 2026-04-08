package com.aicall.companion.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantEngineSettingsTest {
    @Test
    fun `assistant settings keep selected engine and local model fields`() {
        val settings = AssistantSettings(
            selectedEngine = AssistantEngine.Local,
            codexAccessToken = "token",
            localModelUri = "content://models/qwen.gguf",
            localModelLabel = "qwen.gguf",
        )

        assertEquals(AssistantEngine.Local, settings.selectedEngine)
        assertEquals("content://models/qwen.gguf", settings.localModelUri)
        assertEquals("qwen.gguf", settings.localModelLabel)
    }
}
