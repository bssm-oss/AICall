package com.aicall.companion.assistant

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class AssistantSettings(
    val selectedEngine: AssistantEngine = AssistantEngine.Codex,
    val codexAccessToken: String = "",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val silenceNumberSuffix: String = "0000",
    val autoSpeakReplies: Boolean = false,
    val localModelUri: String = "",
    val localModelLabel: String = "",
)

enum class AssistantEngine {
    Codex,
    Local,
    Demo,
}

@Serializable
data class AssistantExchange(
    val timestampLabel: String,
    val callerText: String,
    val replyText: String,
    val source: String,
)

data class AssistantResponse(
    val reply: String,
    val source: ResponseSource,
    val statusMessage: String,
)

enum class ResponseSource {
    Codex,
    Local,
    Demo,
}

@Serializable
data class AssistantRequestPayload(
    @SerialName("caller_text") val callerText: String,
    @SerialName("system_prompt") val systemPrompt: String,
)

@Serializable
data class AssistantResponsePayload(
    val reply: String,
)

const val DEFAULT_SYSTEM_PROMPT =
    "You are an AI call companion. Keep replies short, polite, and suitable for live phone assistance."
