package com.aicall.companion.telecom

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TelecomHistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<TelecomHistoryEntry> {
        val serialized = prefs.getString(KEY_EVENTS, null).orEmpty()
        if (serialized.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<TelecomHistoryEntry>>(serialized)
        }.getOrDefault(emptyList())
    }

    fun append(entry: TelecomHistoryEntry): List<TelecomHistoryEntry> {
        val updated = appendEntry(load(), entry)
        save(updated)
        return updated
    }

    fun clear() {
        prefs.edit().remove(KEY_EVENTS).apply()
    }

    private fun save(events: List<TelecomHistoryEntry>) {
        prefs.edit()
            .putString(KEY_EVENTS, json.encodeToString(events))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "telecom_history"
        private const val KEY_EVENTS = "recent_events"
        private const val MAX_EVENTS = 20

        internal fun appendEntry(
            existing: List<TelecomHistoryEntry>,
            entry: TelecomHistoryEntry,
            maxEvents: Int = MAX_EVENTS,
        ): List<TelecomHistoryEntry> {
            return (listOf(entry) + existing).take(maxEvents)
        }
    }
}
