package com.aicall.companion.assistant

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AssistantSessionRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<AssistantExchange> {
        val serialized = prefs.getString(KEY_HISTORY, null).orEmpty()
        if (serialized.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<AssistantExchange>>(serialized)
        }.getOrDefault(emptyList())
    }

    fun append(entry: AssistantExchange): List<AssistantExchange> {
        val updated = appendEntry(load(), entry)
        save(updated)
        return updated
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun save(history: List<AssistantExchange>) {
        prefs.edit().putString(KEY_HISTORY, json.encodeToString(history)).apply()
    }

    companion object {
        private const val PREFS_NAME = "assistant_session"
        private const val KEY_HISTORY = "history"
        private const val MAX_ENTRIES = 20

        internal fun appendEntry(
            existing: List<AssistantExchange>,
            entry: AssistantExchange,
            maxEntries: Int = MAX_ENTRIES,
        ): List<AssistantExchange> = (listOf(entry) + existing).take(maxEntries)
    }
}
