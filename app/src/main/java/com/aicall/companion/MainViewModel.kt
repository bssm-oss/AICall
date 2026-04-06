package com.aicall.companion

import android.app.Application
import android.app.role.RoleManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aicall.companion.assistant.AssistantCoordinator
import com.aicall.companion.assistant.AssistantSettings
import com.aicall.companion.assistant.AssistantSettingsRepository
import com.aicall.companion.speech.SpeechRecognizerManager
import com.aicall.companion.speech.SpeechUiState
import com.aicall.companion.speech.TextToSpeechManager
import com.aicall.companion.telecom.TelecomEventStore
import com.aicall.companion.telecom.TelecomSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: AssistantSettings,
    val telecomSnapshot: TelecomSnapshot,
    val speechState: SpeechUiState,
    val draftCallerText: String = "",
    val latestReply: String = "",
    val latestReplySource: String = "No reply generated yet.",
    val backendStatus: String = "Configure a backend URL and session token for Codex-backed replies, or use the local fallback.",
    val hasDialerRole: Boolean,
    val canRequestDialerRole: Boolean,
    val canAnswerCalls: Boolean,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CallCompanionApp
    private val settingsRepository: AssistantSettingsRepository = app.container.settingsRepository
    private val assistantCoordinator: AssistantCoordinator = app.container.assistantCoordinator
    private val speechRecognizerManager: SpeechRecognizerManager = app.container.speechRecognizerManager
    private val textToSpeechManager: TextToSpeechManager = app.container.textToSpeechManager

    private val draftCallerText = MutableStateFlow("")
    private val latestReply = MutableStateFlow("")
    private val latestReplySource = MutableStateFlow("No reply generated yet.")
    private val backendStatus = MutableStateFlow("Configure a backend URL and session token for Codex-backed replies, or use the local fallback.")

    private val baseState = combine(
        settingsRepository.observe(),
        TelecomEventStore.observe(),
        speechRecognizerManager.observe(),
    ) { settings, telecom, speech ->
        Triple(settings, telecom, speech)
    }

    val uiState: StateFlow<MainUiState> = combine(
        baseState,
        draftCallerText,
        latestReply,
        latestReplySource,
        backendStatus,
    ) { base, draft, reply, source, backendMessage ->
        val (settings, telecom, speech) = base
        MainUiState(
            settings = settings,
            telecomSnapshot = telecom,
            speechState = speech,
            draftCallerText = if (draft.isBlank()) speech.transcript else draft,
            latestReply = reply,
            latestReplySource = source,
            backendStatus = backendMessage,
            hasDialerRole = hasDialerRole(),
            canRequestDialerRole = canRequestDialerRole(),
            canAnswerCalls = telecom.hasActiveCall,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    fun refreshCapabilities() {
        speechRecognizerManager.refreshPermissionState()
    }

    fun updateDraftCallerText(value: String) {
        draftCallerText.value = value
    }

    fun updateBackendBaseUrl(value: String) {
        settingsRepository.update { it.copy(backendBaseUrl = value) }
    }

    fun updateBackendToken(value: String) {
        settingsRepository.update { it.copy(backendSessionToken = value) }
    }

    fun updateSystemPrompt(value: String) {
        settingsRepository.update { it.copy(systemPrompt = value) }
    }

    fun updateSilenceSuffix(value: String) {
        settingsRepository.update { it.copy(silenceNumberSuffix = value) }
    }

    fun startSpeechRecognition() {
        speechRecognizerManager.startListening()
    }

    fun stopSpeechRecognition() {
        speechRecognizerManager.stopListening()
    }

    fun answerCall() {
        TelecomEventStore.answerActiveCall()
    }

    fun rejectCall() {
        TelecomEventStore.rejectActiveCall()
    }

    fun endCall() {
        TelecomEventStore.disconnectActiveCall()
    }

    fun clearTelecomHistory() {
        TelecomEventStore.clearHistory()
    }

    fun speakLatestReply() {
        textToSpeechManager.speak(latestReply.value)
    }

    fun generateReply() {
        viewModelScope.launch {
            val response = assistantCoordinator.generateReply(uiState.value.draftCallerText)
            latestReply.value = response.reply
            latestReplySource.value = "Reply source: ${response.source}"
            backendStatus.value = response.statusMessage
        }
    }

    override fun onCleared() {
        speechRecognizerManager.release()
        textToSpeechManager.shutdown()
        super.onCleared()
    }

    private fun initialState(): MainUiState {
        return MainUiState(
            settings = settingsRepository.observe().value,
            telecomSnapshot = TelecomEventStore.observe().value,
            speechState = speechRecognizerManager.observe().value,
            hasDialerRole = hasDialerRole(),
            canRequestDialerRole = canRequestDialerRole(),
            canAnswerCalls = false,
        )
    }

    private fun canRequestDialerRole(): Boolean {
        val roleManager = getApplication<Application>().getSystemService(RoleManager::class.java)
        return roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true
    }

    private fun hasDialerRole(): Boolean {
        val roleManager = getApplication<Application>().getSystemService(RoleManager::class.java)
        return roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
    }
}
