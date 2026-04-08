package com.aicall.companion.telecom

import kotlinx.serialization.Serializable

@Serializable
data class TelecomHistoryEntry(
    val timestampLabel: String,
    val message: String,
)

data class TelecomSnapshot(
    val latestCallSummary: String = "아직 Telecom 이벤트가 없습니다.",
    val screeningSummary: String = "아직 스크리닝 판정이 없습니다.",
    val hasActiveCall: Boolean = false,
    val recentEvents: List<TelecomHistoryEntry> = emptyList(),
    val testLabState: TelecomTestLabState = TelecomTestLabState(),
)

enum class ScreeningDecision {
    Allow,
    Silence,
}
