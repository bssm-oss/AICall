package com.aicall.companion.assistant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AssistantCoordinator(
    private val settingsRepository: AssistantSettingsRepository,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun generateReply(callerText: String): AssistantResponse {
        val normalizedText = callerText.trim()
        if (normalizedText.isBlank()) {
            return AssistantResponse(
                reply = "Please say or enter something before asking the assistant to reply.",
                source = ResponseSource.Demo,
                statusMessage = "No caller text was provided, so the assistant did not contact the backend.",
            )
        }

        val settings = settingsRepository.observe().value
        val backendUrl = settings.backendBaseUrl.trim().trimEnd('/')
        val backendToken = settings.backendSessionToken.trim()

        if (backendUrl.isBlank() || backendToken.isBlank()) {
            return AssistantResponse(
                reply = buildDemoReply(normalizedText),
                source = ResponseSource.Demo,
                statusMessage = "Using the local fallback reply because the Codex backend URL or session token is still empty.",
            )
        }

        return withContext(Dispatchers.IO) {
            val body = json.encodeToString(
                AssistantRequestPayload(
                    callerText = normalizedText,
                    systemPrompt = settings.systemPrompt,
                )
            )
            val request = Request.Builder()
                .url("$backendUrl/assistant/respond")
                .header("Authorization", "Bearer $backendToken")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use AssistantResponse(
                        reply = "Backend request failed with HTTP ${response.code}. Falling back is recommended until the Codex backend is configured.",
                        source = ResponseSource.Demo,
                        statusMessage = "The Codex-compatible backend returned HTTP ${response.code}, so the app stayed in a safe local-only state.",
                    )
                }
                val payload = response.body?.string().orEmpty()
                val parsed = json.decodeFromString<AssistantResponsePayload>(payload)
                AssistantResponse(
                    reply = parsed.reply,
                    source = ResponseSource.Backend,
                    statusMessage = "Using Codex-compatible backend token flow. Long-lived provider secrets stay off-device.",
                )
            }
        }
    }

    companion object {
        fun buildDemoReply(callerText: String): String {
            return "Hello, this is the AI call companion. I heard: '$callerText'. The Codex-backed server token is not configured, so this is the local fallback reply."
        }
    }
}
