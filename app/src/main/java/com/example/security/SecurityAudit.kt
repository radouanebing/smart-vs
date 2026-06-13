package com.example.security

import android.util.Log

data class SecurityLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,      // e.g. "PIN_VERIFIED", "BIOMETRIC_FAIL", "REMOTE_WIPE_MOCK"
    val description: String,
    val severity: String        // "INFO", "WARNING", "CRITICAL"
)

object SecurityAudit {
    private val auditLogs = mutableListOf<SecurityLogEntry>()

    fun logEvent(eventType: String, description: String, severity: String = "INFO") {
        val entry = SecurityLogEntry(
            eventType = eventType,
            description = description,
            severity = severity
        )
        auditLogs.add(0, entry) // Prepend to show newest first
        if (auditLogs.size > 100) {
            auditLogs.removeLast()
        }
        Log.i("SecurityAudit", "AUDIT LOG [$severity]: $eventType - $description")
    }

    fun retrieveAuditLogs(): List<SecurityLogEntry> {
        return auditLogs.toList()
    }

    fun clearLogs() {
        auditLogs.clear()
    }
}
