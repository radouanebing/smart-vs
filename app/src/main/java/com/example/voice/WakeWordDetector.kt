package com.example.voice

import android.content.Context
import android.util.Log

class WakeWordDetector(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit
) {
    private var isMonitoring = false
    private var configuredKeyword = "Siri Finder"

    fun setWakeWord(keyword: String) {
        configuredKeyword = keyword.trim()
    }

    fun startContinuousMonitoring() {
        isMonitoring = true
        Log.i("WakeWordDetector", "Continuous local wake command monitoring active for: '$configuredKeyword'")
    }

    fun stopContinuousMonitoring() {
        isMonitoring = false
        Log.i("WakeWordDetector", "Continuous local wake command monitoring paused")
    }

    /**
     * Checks if a transcript text starts with or contains the wake phrase
     */
    fun checkPhrase(text: String): Boolean {
        if (!isMonitoring) return false
        val command = text.lowercase()
        val keyword = configuredKeyword.lowercase()
        return command.contains(keyword) || command.contains("أين أنت") || command.contains("وينك") || command.contains("où es tu")
    }
}
