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
            latestCallSummary = "Active call state: ${call.state.toHumanReadableState()}",
            hasActiveCall = true,
        )
        appendHistory("Call added with state ${call.state.toHumanReadableState()}.")
    }

    fun onCallUpdated(call: Call) {
        if (activeCall == call) {
            state.value = state.value.copy(
                latestCallSummary = "Active call state: ${call.state.toHumanReadableState()}",
                hasActiveCall = true,
            )
            appendHistory("Call updated to ${call.state.toHumanReadableState()}.")
        }
    }

    fun onCallRemoved() {
        activeCall = null
        state.value = state.value.copy(
            latestCallSummary = "Active call ended or was removed.",
            hasActiveCall = false,
        )
        appendHistory("Call removed from the active Telecom session.")
    }

    fun onScreeningDecision(number: String, decision: ScreeningDecision) {
        state.value = state.value.copy(
            screeningSummary = "Screening decision for $number: $decision",
        )
        appendHistory("Screening decision for $number: $decision")
    }

    fun applyTestLabAction(action: TelecomTestLabAction) {
        state.value = state.value.copy(
            testLabState = state.value.testLabState.apply(action),
        )
        appendHistory(action.toHistoryMessage())
    }

    fun answerActiveCall() {
        activeCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        appendHistory("Attempted to answer the active call.")
    }

    fun rejectActiveCall() {
        activeCall?.reject(false, null)
        appendHistory("Attempted to reject the active call.")
    }

    fun disconnectActiveCall() {
        activeCall?.disconnect()
        appendHistory("Attempted to disconnect the active call.")
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
