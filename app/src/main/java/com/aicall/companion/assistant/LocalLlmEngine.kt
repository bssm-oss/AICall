package com.aicall.companion.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

interface LocalLlmEngine {
    suspend fun generateReply(
        callerText: String,
        settings: AssistantSettings,
    ): AssistantResponse

    fun getEngineStatus(settings: AssistantSettings): String
}

class PlaceholderLocalLlmEngine(
    private val nativeStatusProvider: () -> String = {
        NativeLocalLlmBridge().getNativeEngineStatus()
    },
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val ollamaUrl: String = DEFAULT_OLLAMA_URL,
    private val defaultModel: String = DEFAULT_OLLAMA_MODEL,
) : LocalLlmEngine {

    override fun getEngineStatus(settings: AssistantSettings): String {
        val nativeStatus = runCatching { nativeStatusProvider() }
            .getOrDefault("llama.cpp native bridge unavailable")
        val modelStatus = if (settings.localModelUri.isBlank()) {
            "기본 로컬 모델: $defaultModel"
        } else {
            "선택된 GGUF 참고값: ${settings.localModelLabel.ifBlank { "이름 없는 GGUF 모델" }} / 기본 로컬 모델: $defaultModel"
        }
        return "$modelStatus / Native: $nativeStatus"
    }

    override suspend fun generateReply(
        callerText: String,
        settings: AssistantSettings,
    ): AssistantResponse {
        val trimmedCallerText = callerText.trim()
        val nativeStatus = getEngineStatus(settings)
        return withContext(Dispatchers.IO) {
            val requestJson = """
                {
                  "model": ${jsonString(defaultModel)},
                  "prompt": ${jsonString(buildLocalPrompt(settings.systemPrompt, trimmedCallerText))},
                  "stream": false
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(ollamaUrl)
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .build()

            runCatching {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@use AssistantResponse(
                            reply = "로컬 모델 요청이 HTTP ${response.code}로 실패했습니다.",
                            source = ResponseSource.Local,
                            statusMessage = "Ollama 로컬 모델($defaultModel) 요청이 실패했습니다. 상태: $nativeStatus",
                        )
                    }

                    val generated = extractOllamaResponse(body)
                    AssistantResponse(
                        reply = generated,
                        source = ResponseSource.Local,
                        statusMessage = "로컬 모델($defaultModel) 응답을 받았습니다. 상태: $nativeStatus",
                    )
                }
            }.getOrElse { error ->
                AssistantResponse(
                    reply = "로컬 모델 연결에 실패했습니다: ${error.message}",
                    source = ResponseSource.Local,
                    statusMessage = "Ollama 로컬 모델($defaultModel) 연결에 실패했습니다. 상태: $nativeStatus",
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_OLLAMA_URL = "http://10.0.2.2:11434/api/generate"
        private const val DEFAULT_OLLAMA_MODEL = "qwen2.5:1.5b"

        internal fun buildLocalPrompt(systemPrompt: String, callerText: String): String {
            return "$systemPrompt\n\nCaller text:\n$callerText"
        }

        internal fun extractOllamaResponse(body: String): String {
            val marker = "\"response\":"
            val markerIndex = body.indexOf(marker)
            if (markerIndex == -1) {
                return "로컬 모델 응답 본문을 읽을 수 없습니다."
            }
            val startQuote = body.indexOf('"', markerIndex + marker.length)
            if (startQuote == -1) return "로컬 모델 응답 본문을 읽을 수 없습니다."
            var i = startQuote + 1
            val out = StringBuilder()
            var escaping = false
            while (i < body.length) {
                val c = body[i]
                if (escaping) {
                    out.append(
                        when (c) {
                            'n' -> '\n'
                            't' -> '\t'
                            'r' -> '\r'
                            '"' -> '"'
                            '\\' -> '\\'
                            else -> c
                        }
                    )
                    escaping = false
                } else if (c == '\\') {
                    escaping = true
                } else if (c == '"') {
                    break
                } else {
                    out.append(c)
                }
                i += 1
            }
            return out.toString().ifBlank { "로컬 모델이 빈 응답을 반환했습니다." }
        }

        private fun jsonString(value: String): String {
            return buildString {
                append('"')
                value.forEach { ch ->
                    when (ch) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(ch)
                    }
                }
                append('"')
            }
        }
    }
}
