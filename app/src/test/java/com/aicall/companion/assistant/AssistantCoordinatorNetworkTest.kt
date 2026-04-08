package com.aicall.companion.assistant

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantCoordinatorNetworkTest {
    @Test
    fun `pasted token is used in real codex request path`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"output_text\":\"실제 Codex 응답\"}"))
        server.start()

        val settings = AssistantSettings(
            selectedEngine = AssistantEngine.Codex,
            codexAccessToken = "oauth-token",
            systemPrompt = "테스트 시스템 프롬프트",
        )

        val coordinator = AssistantCoordinator(
            client = OkHttpClient(),
            responsesUrl = server.url("/v1/responses").toString(),
            settingsProvider = { settings },
        )

        val response = runBlocking {
            coordinator.generateReply("안녕하세요")
        }

        val request = server.takeRequest()
        assertEquals("Bearer oauth-token", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("안녕하세요"))
        assertTrue(response.reply.contains("실제 Codex 응답"))
        assertEquals(ResponseSource.Codex, response.source)

        server.shutdown()
    }
}
