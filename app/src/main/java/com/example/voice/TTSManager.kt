package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLocale = Locale.ENGLISH

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = currentLocale
            } else {
                Log.e("TTSManager", "Failed to initialize TextToSpeech engine")
            }
        }
    }

    fun setLanguage(locale: Locale) {
        currentLocale = locale
        if (isInitialized) {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TTSManager", "Language $locale is not supported or missing voice data")
            }
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isInitialized) {
            tts?.speak(text, queueMode, null, "NHN_TTS_UTTERANCE_ID")
        } else {
            Log.w("TTSManager", "TTS not initialized yet. Message ignored: $text")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
