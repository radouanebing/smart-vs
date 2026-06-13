package com.example.voice

import android.content.Context
import android.util.Log

class VoiceSessionManager(
    private val context: Context,
    private val onTranscriptReady: (String) -> Unit
) {
    private var isSessionActive = false
    private val languageManager = LanguageManager()
    private val ttsManager = TTSManager(context)
    private var recognizerManager: SpeechRecognizerManager? = null

    init {
        recognizerManager = SpeechRecognizerManager(
            context = context,
            onReady = { Log.d("VoiceSessionManager", "Microphone listening is active") },
            onPartialResult = { partial -> Log.v("VoiceSessionManager", "Partial voice input: $partial") },
            onFinalResult = { text -> onTranscriptReady(text) },
            onErrorState = { err -> Log.e("VoiceSessionManager", "Voice recognition error status code $err") }
        )
    }

    fun startSession(langCode: String) {
        isSessionActive = true
        ttsManager.stop() // Interrupt speech-to-text feedback on fresh record request
        val locale = languageManager.getLocale(langCode)
        ttsManager.setLanguage(locale)
        recognizerManager?.startListening(langCode)
    }

    fun stopSession() {
        recognizerManager?.stopListening()
        isSessionActive = false
    }

    fun speak(text: String) {
        ttsManager.speak(text)
    }

    fun release() {
        recognizerManager?.destroy()
        ttsManager.shutdown()
        recognizerManager = null
    }
}
