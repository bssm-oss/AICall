package com.aicall.companion.speech

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class SpeechUiState(
    val isRecognitionAvailable: Boolean,
    val hasRecordAudioPermission: Boolean,
    val isListening: Boolean = false,
    val transcript: String = "",
    val lastError: String = "",
)

class SpeechRecognizerManager(
    private val application: Application,
) : RecognitionListener {
    private val state = MutableStateFlow(
        SpeechUiState(
            isRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(application),
            hasRecordAudioPermission = hasRecordAudioPermission(),
        ),
    )
    private var speechRecognizer: SpeechRecognizer? = null

    fun observe(): StateFlow<SpeechUiState> = state.asStateFlow()

    fun refreshPermissionState() {
        state.value = state.value.copy(hasRecordAudioPermission = hasRecordAudioPermission())
    }

    fun startListening() {
        if (!state.value.isRecognitionAvailable || !hasRecordAudioPermission()) {
            state.value = state.value.copy(
                hasRecordAudioPermission = hasRecordAudioPermission(),
                lastError = "Speech recognition is unavailable until microphone permission is granted and the recognizer exists on-device.",
            )
            return
        }
        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(application).also {
            it.setRecognitionListener(this)
            speechRecognizer = it
        }
        state.value = state.value.copy(isListening = true, lastError = "")
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            },
        )
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        state.value = state.value.copy(isListening = false)
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        state.value = state.value.copy(isListening = false)
    }

    override fun onError(error: Int) {
        state.value = state.value.copy(
            isListening = false,
            lastError = "Speech recognition failed with code $error.",
        )
    }

    override fun onResults(results: Bundle?) {
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        state.value = state.value.copy(isListening = false, transcript = transcript)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val transcript = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        if (transcript.isNotBlank()) {
            state.value = state.value.copy(transcript = transcript)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(application, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
}
