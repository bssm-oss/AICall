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
            .putString(KEY_SELECTED_ENGINE, updated.selectedEngine.name)
            .putString(KEY_SYSTEM_PROMPT, updated.systemPrompt)
            .putString(KEY_SILENCE_SUFFIX, updated.silenceNumberSuffix)
            .putBoolean(KEY_AUTO_SPEAK_REPLIES, updated.autoSpeakReplies)
            .putString(KEY_LOCAL_MODEL_URI, updated.localModelUri)
            .putString(KEY_LOCAL_MODEL_LABEL, updated.localModelLabel)
            .apply()
        state.value = updated
    }

    private fun readSettings(): AssistantSettings = AssistantSettings(
        selectedEngine = prefs.getString(KEY_SELECTED_ENGINE, AssistantEngine.Local.name)
            ?.let { runCatching { AssistantEngine.valueOf(it) }.getOrDefault(AssistantEngine.Local) }
            ?: AssistantEngine.Local,
        systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT).orEmpty(),
        silenceNumberSuffix = prefs.getString(KEY_SILENCE_SUFFIX, "0000").orEmpty(),
        autoSpeakReplies = prefs.getBoolean(KEY_AUTO_SPEAK_REPLIES, false),
        localModelUri = prefs.getString(KEY_LOCAL_MODEL_URI, "").orEmpty(),
        localModelLabel = prefs.getString(KEY_LOCAL_MODEL_LABEL, "").orEmpty(),
    )

    private companion object {
        const val PREFS_NAME = "assistant_settings"
        const val KEY_SELECTED_ENGINE = "selected_engine"
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_SILENCE_SUFFIX = "silence_suffix"
        const val KEY_AUTO_SPEAK_REPLIES = "auto_speak_replies"
        const val KEY_LOCAL_MODEL_URI = "local_model_uri"
        const val KEY_LOCAL_MODEL_LABEL = "local_model_label"
    }
}
