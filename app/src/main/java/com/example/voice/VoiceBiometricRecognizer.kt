package com.example.voice

import android.content.Context
import android.util.Log
import com.example.data.model.VoiceProfile
import kotlin.math.abs

data class BiometricIdentifyResult(
    val matchedProfile: VoiceProfile?,
    val confidence: Int, // 0 - 100
    val isAuthorized: Boolean,
    val speakerRole: String, // "Owner", "Family", "Staff", "Guest"
    val preferredLanguage: String
)

class VoiceBiometricRecognizer(private val context: Context) {

    /**
     * Compares a simulated voice print (pitch + signal vector) against registered database profiles.
     * Computes biometric similarity scores.
         */
    fun identifySpeaker(
        currentPitch: Float,
        voiceVector: List<Float>,
        profiles: List<VoiceProfile>
    ): BiometricIdentifyResult {
        Log.d("VoiceBiometricRecognizer", "Initiating biometric comparisons for active speaker...")
        
        if (profiles.isEmpty()) {
            return BiometricIdentifyResult(
                matchedProfile = null,
                confidence = 0,
                isAuthorized = false,
                speakerRole = "Guest",
                preferredLanguage = "en"
            )
        }

        var bestMatch: VoiceProfile? = null
        var maxScore = 0

        for (profile in profiles) {
            // Calculate pitch delta relative difference
            val pitchDelta = abs(profile.biometricPitch - currentPitch) / profile.biometricPitch
            val pitchScore = ((1f - pitchDelta.coerceIn(0f, 1f)) * 100).toInt()

            // Calculate vector cosine similarity
            val enrolledVector = profile.voiceprintVector.split(",").mapNotNull { it.trim().toFloatOrNull() }
            var similarityScore = 0
            if (enrolledVector.isNotEmpty() && voiceVector.isNotEmpty()) {
                val size = minOf(enrolledVector.size, voiceVector.size)
                var dotProduct = 0f
                var normA = 0f
                var normB = 0f
                for (i in 0 until size) {
                    dotProduct += enrolledVector[i] * voiceVector[i]
                    normA += enrolledVector[i] * enrolledVector[i]
                    normB += voiceVector[i] * voiceVector[i]
                }
                val similarity = if (normA > 0 && normB > 0) {
                    dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
                } else 0f
                
                similarityScore = ((similarity.coerceIn(-1f, 1f) + 1f) / 2f * 100).toInt()
            } else {
                similarityScore = 80 // fallback simulation
            }

            // Weighted score: 40% Pitch, 60% Voice Envelope Vector
            val totalScore = ((pitchScore * 0.4f) + (similarityScore * 0.6f)).toInt().coerceIn(0, 100)
            Log.d("VoiceBiometricRecognizer", "Comparing against ${profile.name}: PitchScore=$pitchScore, VectorScore=$similarityScore, FinalScore=$totalScore")

            if (totalScore > maxScore) {
                maxScore = totalScore
                bestMatch = profile
            }
        }

        val finalProfile = if (maxScore >= 50) bestMatch else null
        val matchedRole = finalProfile?.role ?: "Guest"
        val isAuth = maxScore >= 70 || (finalProfile != null && finalProfile.role != "Guest")

        return BiometricIdentifyResult(
            matchedProfile = finalProfile,
            confidence = if (finalProfile != null) maxScore else 28, // base noise floor
            isAuthorized = isAuth,
            speakerRole = matchedRole,
            preferredLanguage = finalProfile?.preferredLanguage ?: "en"
        )
    }

    /**
     * Helper to generate a standardized biometric feature print for simulated voice profiles.
     */
    fun createVoiceprintVector(rawString: String): List<Float> {
        val hash = rawString.hashCode()
        val random = java.util.Random(hash.toLong())
        return List(5) { random.nextFloat() * 2f - 1f }
    }
}
