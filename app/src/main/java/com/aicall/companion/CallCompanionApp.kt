package com.aicall.companion

import android.app.Application
import com.aicall.companion.assistant.AssistantCoordinator
import com.aicall.companion.assistant.AssistantSessionRepository
import com.aicall.companion.assistant.AssistantSettingsRepository
import com.aicall.companion.speech.SpeechRecognizerManager
import com.aicall.companion.speech.TextToSpeechManager
import com.aicall.companion.telecom.TelecomEventStore
import com.aicall.companion.telecom.TelecomHistoryRepository

class CallCompanionApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val settingsRepository = AssistantSettingsRepository(application)
    val assistantSessionRepository = AssistantSessionRepository(application)
    val speechRecognizerManager = SpeechRecognizerManager(application)
    val textToSpeechManager = TextToSpeechManager(application)
    val telecomHistoryRepository = TelecomHistoryRepository(application)
    val assistantCoordinator = AssistantCoordinator(settingsRepository)

    init {
        TelecomEventStore.initialize(telecomHistoryRepository)
    }
}
