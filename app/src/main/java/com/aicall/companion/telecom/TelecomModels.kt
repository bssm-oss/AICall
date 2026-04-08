package com.aicall.companion.telecom

import kotlinx.serialization.Serializable

@Serializable
data class TelecomHistoryEntry(
    val timestampLabel: String,
    val message: String,
)

data class TelecomSnapshot(
    val latestCallSummary: String = "No Telecom events yet.",
    val screeningSummary: String = "No screening decisions yet.",
    val hasActiveCall: Boolean = false,
    val recentEvents: List<TelecomHistoryEntry> = emptyList(),
    val testLabState: TelecomTestLabState = TelecomTestLabState(),
)

enum class ScreeningDecision {
    Allow,
    Silence,
}
