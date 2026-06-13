package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val sender: String, // "user" or "assistant"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "device_status")
data class DeviceStatus(
    @PrimaryKey val id: Int = 1,
    val batteryPercentage: Int,
    val connectionType: String,
    val isRinging: Boolean = false,
    val isLocked: Boolean = false,
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: Long = System.currentTimeMillis(),
    val micActivityActive: Boolean = false,
    val listeningStatus: String = "Inactive", // "Listening", "Paused (Low Battery)", "Paused (Call)", "Paused (Battery Saver)", "Paused (Thermal)", "Push To Talk", "Disabled"
    val estimatedHourlyDrain: Float = 0.0f, // percentage per hour
    val cpuSavingsPercent: Int = 0,
    val deviceTemperature: Float = 28.0f, // in Celsius
    val thermalStatus: String = "Normal", // "Normal", "Warm", "Critical"
    val isBatterySaverActive: Boolean = false
)

@Entity(tableName = "system_settings")
data class SystemSettings(
    @PrimaryKey val id: Int = 1,
    val pinCode: String = "1234",
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val remoteWakeKeyword: String = "Siri Finder", // Secret find command
    val preferredLanguage: String = "en", // "en", "ar", "dz", "fr"
    val accessibilityEnabled: Boolean = false,
    val flashlightBlinkOnRemote: Boolean = true,
    val sirenOnRemote: Boolean = true,
    val backgroundVoiceEnabled: Boolean = true,
    val listeningMode: String = "smart", // "always", "smart", "ptt"
    val adaptiveListeningEnabled: Boolean = true
)
