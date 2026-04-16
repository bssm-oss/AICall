package com.aicall.companion.assistant

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLlmEngineTest {
    @Test
    fun `engine status includes default gemma model and native status`() {
        val engine = PlaceholderLocalLlmEngine(nativeStatusProvider = { "native bridge ready" })

        val status = engine.getEngineStatus(
            AssistantSettings(selectedEngine = AssistantEngine.Local),
        )

        assertTrue(status.contains("gemma3:4b"))
        assertTrue(status.contains("native bridge ready"))
    }

    @Test
    fun `local engine returns ollama response when host model succeeds`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"response\":\"로컬 응답\",\"done\":true}"))
        server.start()

        val engine = PlaceholderLocalLlmEngine(
            nativeStatusProvider = { "native bridge ready" },
            client = OkHttpClient(),
            ollamaUrl = server.url("/api/generate").toString(),
        )

        val response = runBlocking {
            engine.generateReply(
                callerText = "테스트 입력",
                settings = AssistantSettings(selectedEngine = AssistantEngine.Local),
            )
        }

        assertTrue(response.reply.contains("로컬 응답"))
        assertTrue(response.statusMessage.contains("gemma3:4b"))

        server.shutdown()
    }

    @Test
    fun `local engine returns failure guidance when ollama is unreachable`() {
        val engine = PlaceholderLocalLlmEngine(
            nativeStatusProvider = { "native bridge ready" },
            client = OkHttpClient(),
            ollamaUrl = "http://127.0.0.1:1/api/generate",
        )

        val response = runBlocking {
            engine.generateReply(
                callerText = "테스트 입력",
                settings = AssistantSettings(selectedEngine = AssistantEngine.Local),
            )
        }

        assertTrue(response.reply.contains("로컬 모델 연결에 실패했습니다"))
    }
}
