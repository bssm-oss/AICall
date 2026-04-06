package com.aicall.companion.speech

import android.app.Application
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechManager(application: Application) : TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(application, this)
    @Volatile
    private var initialized = false

    override fun onInit(status: Int) {
        initialized = status == TextToSpeech.SUCCESS
        if (initialized) {
            textToSpeech.language = Locale.getDefault()
        }
    }

    fun speak(text: String) {
        if (!initialized || text.isBlank()) return
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant-reply")
    }

    fun shutdown() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
