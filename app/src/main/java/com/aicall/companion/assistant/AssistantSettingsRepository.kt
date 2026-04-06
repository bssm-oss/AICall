package com.aicall.companion.assistant

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(readSettings())

    fun observe(): StateFlow<AssistantSettings> = state.asStateFlow()

    fun update(transform: (AssistantSettings) -> AssistantSettings) {
        val updated = transform(state.value)
        prefs.edit()
            .putString(KEY_BACKEND_BASE_URL, updated.backendBaseUrl)
            .putString(KEY_BACKEND_SESSION_TOKEN, updated.backendSessionToken)
            .putString(KEY_SYSTEM_PROMPT, updated.systemPrompt)
            .putString(KEY_SILENCE_SUFFIX, updated.silenceNumberSuffix)
            .apply()
        state.value = updated
    }

    private fun readSettings(): AssistantSettings = AssistantSettings(
        backendBaseUrl = prefs.getString(KEY_BACKEND_BASE_URL, "").orEmpty(),
        backendSessionToken = prefs.getString(KEY_BACKEND_SESSION_TOKEN, "").orEmpty(),
        systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT).orEmpty(),
        silenceNumberSuffix = prefs.getString(KEY_SILENCE_SUFFIX, "0000").orEmpty(),
    )

    private companion object {
        const val PREFS_NAME = "assistant_settings"
        const val KEY_BACKEND_BASE_URL = "backend_base_url"
        const val KEY_BACKEND_SESSION_TOKEN = "backend_session_token"
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_SILENCE_SUFFIX = "silence_suffix"
    }
}
