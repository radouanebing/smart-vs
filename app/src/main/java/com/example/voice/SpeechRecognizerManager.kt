package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognizerManager(
    private val context: Context,
    private val onReady: () -> Unit = {},
    private val onPartialResult: (String) -> Unit = {},
    private val onFinalResult: (String) -> Unit = {},
    private val onErrorState: (Int) -> Unit = {}
) {
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    val isAvailable: Boolean
        get() = recognizer != null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        onReady()
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        Log.e("SpeechRecognizer", "Recognizer error status code: $error")
                        isListening = false
                        onErrorState(error)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onFinalResult(matches[0])
                        }
                        isListening = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            onPartialResult(matches[0])
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startListening(langCode: String) {
        if (recognizer == null) {
            Log.w("SpeechRecognizer", "Speech recognition service is unavailable on this hardware")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val localeStr = when (langCode) {
                "ar" -> "ar-DZ"
                "dz" -> "ar-DZ"
                "fr" -> "fr-FR"
                else -> "en-US"
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            recognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error starting listening", e)
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        isListening = false
    }

    fun cancel() {
        recognizer?.cancel()
        isListening = false
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}
