package com.aicall.companion

import android.app.Application
import android.app.role.RoleManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aicall.companion.assistant.AssistantCoordinator
import com.aicall.companion.assistant.AssistantEngine
import com.aicall.companion.assistant.AssistantExchange
import com.aicall.companion.assistant.AssistantSettings
import com.aicall.companion.assistant.AssistantSessionRepository
import com.aicall.companion.assistant.AssistantSettingsRepository
import com.aicall.companion.speech.SpeechRecognizerManager
import com.aicall.companion.speech.SpeechUiState
import com.aicall.companion.telecom.TelecomTestLabAction
import com.aicall.companion.speech.TextToSpeechManager
import com.aicall.companion.telecom.TelecomEventStore
import com.aicall.companion.telecom.TelecomSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MainUiState(
    val settings: AssistantSettings,
    val telecomSnapshot: TelecomSnapshot,
    val speechState: SpeechUiState,
    val draftCallerText: String = "",
    val latestReply: String = "",
    val latestReplySource: String = "아직 생성된 응답이 없습니다.",
    val localStatus: String = "로컬 Gemma 모델이 준비되면 이 경로를 통해 응답을 생성합니다.",
    val hasDialerRole: Boolean,
    val canRequestDialerRole: Boolean,
    val canAnswerCalls: Boolean,
    val assistantHistory: List<AssistantExchange> = emptyList(),
)

private data class CombinedUiInputs(
    val settings: AssistantSettings,
    val telecomSnapshot: TelecomSnapshot,
    val speechState: SpeechUiState,
    val assistantHistory: List<AssistantExchange>,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CallCompanionApp
    private val settingsRepository: AssistantSettingsRepository = app.container.settingsRepository
    private val assistantSessionRepository: AssistantSessionRepository = app.container.assistantSessionRepository
    private val assistantCoordinator: AssistantCoordinator = app.container.assistantCoordinator
    private val speechRecognizerManager: SpeechRecognizerManager = app.container.speechRecognizerManager
    private val textToSpeechManager: TextToSpeechManager = app.container.textToSpeechManager

    private val draftCallerText = MutableStateFlow("")
    private val latestReply = MutableStateFlow("")
    private val latestReplySource = MutableStateFlow("아직 생성된 응답이 없습니다.")
    private val localStatus = MutableStateFlow("로컬 Gemma 모델이 준비되면 이 경로를 통해 응답을 생성합니다.")
    private val assistantHistory = MutableStateFlow(assistantSessionRepository.load())

    private val baseState = combine(
        settingsRepository.observe(),
        TelecomEventStore.observe(),
        speechRecognizerManager.observe(),
        assistantHistory,
    ) { settings, telecom, speech, sessionHistory ->
        CombinedUiInputs(
            settings = settings,
            telecomSnapshot = telecom,
            speechState = speech,
            assistantHistory = sessionHistory,
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        baseState,
        draftCallerText,
        latestReply,
        latestReplySource,
        localStatus,
    ) { base, draft, reply, source, localMessage ->
        MainUiState(
            settings = base.settings,
            telecomSnapshot = base.telecomSnapshot,
            speechState = base.speechState,
            draftCallerText = if (draft.isBlank()) base.speechState.transcript else draft,
            latestReply = reply,
            latestReplySource = source,
            localStatus = localMessage,
            hasDialerRole = hasDialerRole(),
            canRequestDialerRole = canRequestDialerRole(),
            canAnswerCalls = base.telecomSnapshot.hasActiveCall,
            assistantHistory = base.assistantHistory,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    fun refreshCapabilities() {
        speechRecognizerManager.refreshPermissionState()
    }

    fun updateDraftCallerText(value: String) {
        draftCallerText.value = value
    }

    fun updateSelectedEngine(engine: AssistantEngine) {
        settingsRepository.update { it.copy(selectedEngine = engine) }
    }

    fun updateSystemPrompt(value: String) {
        settingsRepository.update { it.copy(systemPrompt = value) }
    }

    fun updateSilenceSuffix(value: String) {
        settingsRepository.update { it.copy(silenceNumberSuffix = value) }
    }

    fun updateAutoSpeakReplies(enabled: Boolean) {
        settingsRepository.update { it.copy(autoSpeakReplies = enabled) }
    }

    fun updateLocalModelSelection(uri: String, label: String) {
        settingsRepository.update {
            it.copy(
                localModelUri = uri,
                localModelLabel = label,
            )
        }
        localStatus.value = assistantCoordinator.getLocalEngineStatus(settingsRepository.observe().value)
    }

    fun inspectLocalEngineStatus() {
        localStatus.value = assistantCoordinator.getLocalEngineStatus(settingsRepository.observe().value)
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

    fun applyTelecomTestLabAction(action: TelecomTestLabAction) {
        TelecomEventStore.applyTestLabAction(action)
    }

    fun clearAssistantHistory() {
        assistantSessionRepository.clear()
        assistantHistory.value = emptyList()
    }

    fun speakLatestReply() {
        textToSpeechManager.speak(latestReply.value)
    }

    fun generateReply() {
        viewModelScope.launch {
            val callerText = uiState.value.draftCallerText.trim()
            val response = assistantCoordinator.generateReply(callerText)
            latestReply.value = response.reply
            latestReplySource.value = "응답 엔진: ${response.source}"
            localStatus.value = response.statusMessage
            assistantHistory.value = assistantSessionRepository.append(
                AssistantExchange(
                    timestampLabel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    callerText = callerText,
                    replyText = response.reply,
                    source = response.source.name,
                ),
            )
            if (settingsRepository.observe().value.autoSpeakReplies) {
                textToSpeechManager.speak(response.reply)
            }
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
            assistantHistory = assistantSessionRepository.load(),
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
