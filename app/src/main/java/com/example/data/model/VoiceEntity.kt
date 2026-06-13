package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_profiles")
data class VoiceProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // "Owner", "Family", "Business Staff", "Administrator"
    val preferredLanguage: String = "en", // "en", "ar", "dz", "fr"
    val biometricPitch: Float, // Simulated key frequency
    val enrolledPhrases: String, // Pipe-separated string of phrases read during enrollment
    val voiceprintVector: String, // Comma-separated floating-point metrics for speaker biometric check
    val isOwnerModeRestricted: Boolean = false, // Protects sensitive operations
    val hasAppAccess: Boolean = false, // Authorization to launch applications
    val hasYoutubeAccess: Boolean = false, // Specific authorization to open YouTube
    val isActive: Boolean = true
)

@Entity(tableName = "wake_words")
data class WakeWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val isCustom: Boolean = true,
    val isActive: Boolean = true
)

@Entity(tableName = "voice_learning_logs")
data class VoiceLearningLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phrase: String,
    val matchedCommand: String,
    val dialectVariance: String, // e.g. "Algerian Dialect (Darja)" or "Eastern Dialect"
    val accuracyScore: Int, // 0-100 rating
    val timestamp: Long = System.currentTimeMillis()
)
