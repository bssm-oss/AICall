package com.aicall.companion.telecom

import android.telecom.Call
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TelecomEventStore {
    private val state = MutableStateFlow(TelecomSnapshot())
    private var activeCall: Call? = null
    private var historyRepository: TelecomHistoryRepository? = null

    fun observe(): StateFlow<TelecomSnapshot> = state.asStateFlow()

    fun initialize(repository: TelecomHistoryRepository) {
        historyRepository = repository
        state.value = state.value.copy(recentEvents = repository.load())
    }

    fun onCallAdded(call: Call) {
        activeCall = call
        state.value = state.value.copy(
            latestCallSummary = "실제 통화 상태: ${call.state.toHumanReadableState()}",
            hasActiveCall = true,
        )
        appendHistory("실제 통화가 추가되었습니다: ${call.state.toHumanReadableState()}")
    }

    fun onCallUpdated(call: Call) {
        if (activeCall == call) {
            state.value = state.value.copy(
                latestCallSummary = "실제 통화 상태: ${call.state.toHumanReadableState()}",
                hasActiveCall = true,
            )
            appendHistory("실제 통화 상태가 ${call.state.toHumanReadableState()}(으)로 바뀌었습니다.")
        }
    }

    fun onCallRemoved() {
        activeCall = null
        state.value = state.value.copy(
            latestCallSummary = "실제 통화가 종료되었거나 제거되었습니다.",
            hasActiveCall = false,
        )
        appendHistory("실제 Telecom 통화가 세션에서 제거되었습니다.")
    }

    fun onScreeningDecision(number: String, decision: ScreeningDecision) {
        state.value = state.value.copy(
            screeningSummary = "$number 에 대한 스크리닝 판정: $decision",
        )
        appendHistory("$number 에 대한 스크리닝 판정: $decision")
    }

    fun applyTestLabAction(action: TelecomTestLabAction) {
        state.value = state.value.copy(
            testLabState = state.value.testLabState.apply(action),
        )
        appendHistory(action.toHistoryMessage())
    }

    fun answerActiveCall() {
        activeCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        appendHistory("실제 active 통화에 대해 받기를 시도했습니다.")
    }

    fun rejectActiveCall() {
        activeCall?.reject(false, null)
        appendHistory("실제 active 통화에 대해 거절을 시도했습니다.")
    }

    fun disconnectActiveCall() {
        activeCall?.disconnect()
        appendHistory("실제 active 통화에 대해 끊기를 시도했습니다.")
    }

    fun clearHistory() {
        historyRepository?.clear()
        state.value = state.value.copy(recentEvents = emptyList())
    }

    private fun appendHistory(message: String) {
        val repository = historyRepository ?: return
        val updated = repository.append(
            TelecomHistoryEntry(
                timestampLabel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                message = message,
            ),
        )
        state.value = state.value.copy(recentEvents = updated)
    }

    private fun Int.toHumanReadableState(): String = when (this) {
        Call.STATE_ACTIVE -> "ACTIVE"
        Call.STATE_CONNECTING -> "CONNECTING"
        Call.STATE_DIALING -> "DIALING"
        Call.STATE_DISCONNECTING -> "DISCONNECTING"
        Call.STATE_DISCONNECTED -> "DISCONNECTED"
        Call.STATE_HOLDING -> "HOLDING"
        Call.STATE_NEW -> "NEW"
        Call.STATE_PULLING_CALL -> "PULLING"
        Call.STATE_RINGING -> "RINGING"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
        else -> "UNKNOWN($this)"
    }
}
