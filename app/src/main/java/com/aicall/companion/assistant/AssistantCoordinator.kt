package com.aicall.companion.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AssistantCoordinator(
    private val settingsRepository: AssistantSettingsRepository? = null,
    private val localLlmEngine: LocalLlmEngine = PlaceholderLocalLlmEngine(),
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val responsesUrl: String = CODEX_RESPONSES_URL,
    private val settingsProvider: (() -> AssistantSettings)? = null,
) {
    fun getLocalEngineStatus(settings: AssistantSettings): String {
        return localLlmEngine.getEngineStatus(settings)
    }

    suspend fun generateReply(callerText: String): AssistantResponse {
        val normalizedText = callerText.trim()
        if (normalizedText.isBlank()) {
            return AssistantResponse(
                reply = "응답을 생성하기 전에 먼저 말하거나 텍스트를 입력해 주세요.",
                source = ResponseSource.Demo,
                statusMessage = "입력이 비어 있어 Codex 요청을 시작하지 않았습니다.",
            )
        }

        val settings = settingsProvider?.invoke() ?: requireNotNull(settingsRepository).observe().value
        if (settings.selectedEngine == AssistantEngine.Demo) {
            return AssistantResponse(
                reply = buildDemoReply(normalizedText),
                source = ResponseSource.Demo,
                statusMessage = "데모 엔진이 선택되어 로컬 fallback 응답을 사용했습니다.",
            )
        }
        if (settings.selectedEngine == AssistantEngine.Local) {
            return localLlmEngine.generateReply(normalizedText, settings)
        }
        val codexAccessToken = settings.codexAccessToken.trim()

        if (codexAccessToken.isBlank()) {
            return AssistantResponse(
                reply = "Codex access token이 아직 연결되지 않았습니다. Codex sign-in 후 access token을 붙여넣어 주세요.",
                source = ResponseSource.Demo,
                statusMessage = "Codex sign-in 후 access token을 붙여넣으면 Codex 요청 경로를 사용할 수 있습니다. backend URL 입력은 더 이상 필요하지 않습니다.",
            )
        }

        return withContext(Dispatchers.IO) {
            val requestBody = buildCodexRequestBody(
                callerText = normalizedText,
                settings = settings,
            )
            val request = Request.Builder()
                .url(responsesUrl)
                .header("Authorization", "Bearer $codexAccessToken")
                .header("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use AssistantResponse(
                        reply = "Codex 요청이 HTTP ${response.code}로 실패했습니다. access token이 만료되었거나, 붙여넣은 값이 Codex용 access token이 아닐 수 있습니다.",
                        source = ResponseSource.Demo,
                        statusMessage = "브라우저에서 Codex sign-in을 다시 진행한 뒤 최신 access token을 다시 붙여넣어 주세요.",
                    )
                }

                AssistantResponse(
                    reply = extractReplyText(body),
                    source = ResponseSource.Codex,
                    statusMessage = "붙여넣은 Codex access token으로 OpenAI Responses 요청을 보냈고, 이 경로를 통해 응답을 받았습니다.",
                )
            }
        }
    }

    companion object {
        private const val CODEX_RESPONSES_URL = "https://api.openai.com/v1/responses"

        fun buildDemoReply(callerText: String): String {
            return "안녕하세요, AI 전화 도우미입니다. 현재 들은 내용은 '$callerText' 입니다. Codex access token이 연결되지 않아 로컬 fallback 응답을 사용합니다."
        }

        internal fun buildCodexRequestBody(
            callerText: String,
            settings: AssistantSettings,
        ): String {
            return """
                {
                  "model": "gpt-5.4",
                  "input": [
                    {
                      "role": "system",
                      "content": [
                        {
                          "type": "input_text",
                          "text": ${jsonString(settings.systemPrompt)}
                        }
                      ]
                    },
                    {
                      "role": "user",
                      "content": [
                        {
                          "type": "input_text",
                          "text": ${jsonString(callerText)}
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
        }

        internal fun extractReplyText(responseBody: String): String {
            val root = Json.parseToJsonElement(responseBody).jsonObject
            val outputText = runCatching {
                root["output_text"]?.jsonPrimitive?.content
            }.getOrNull()
            if (!outputText.isNullOrBlank()) {
                return outputText
            }

            val output = root["output"]?.jsonArray ?: JsonArray(emptyList())
            val firstText = findFirstText(output)

            return firstText ?: "Codex 응답 본문을 읽었지만 표시 가능한 text를 찾지 못했습니다."
        }

        private fun jsonString(value: String): String {
            return Json.encodeToString(String.serializer(), value)
        }

        private fun findFirstText(output: JsonArray): String? {
            for (outputItem in output) {
                val outputObject = outputItem.jsonObject
                val contents = outputObject["content"]?.jsonArray ?: continue
                for (contentItem in contents) {
                    val contentObject: JsonObject = contentItem.jsonObject
                    val text = runCatching { contentObject["text"]?.jsonPrimitive?.content }.getOrNull()
                    if (!text.isNullOrBlank()) {
                        return text
                    }
                }
            }
            return null
        }
    }
}
