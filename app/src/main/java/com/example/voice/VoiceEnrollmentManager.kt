package com.example.voice

import android.content.Context
import android.util.Log
import com.example.data.model.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class EnrollmentState {
    object Idle : EnrollmentState()
    data class PromptingPhrase(val step: Int, val phrase: String, val progress: Float) : EnrollmentState()
    data class AnalyzingBiometrics(val pitchCollected: Float) : EnrollmentState()
    data class Success(val profile: VoiceProfile) : EnrollmentState()
    data class Error(val reason: String) : EnrollmentState()
}

class VoiceEnrollmentManager(private val context: Context) {

    private val _enrollmentState = MutableStateFlow<EnrollmentState>(EnrollmentState.Idle)
    val enrollmentState: StateFlow<EnrollmentState> = _enrollmentState.asStateFlow()

    private var currentEnrollmentName = ""
    private var currentEnrollmentRole = "Family"
    private var currentEnrollmentLang = "dz"
    private var currentStep = 1
    private val totalSteps = 3

    private val enrollmentPhrases = mapOf(
        "en" to listOf(
            "NHN, launch work dashboard now.",
            "Hey Assistant, make a safety dial.",
            "Confirm voice authentication code zero nine."
        ),
        "ar" to listOf(
            "عارف، قم بتشغيل لوحة العمل ومزامنة الخادم.",
            "رفيق، اتصل بفرع الشركة في الجزائر.",
            "أوكل الصلاحيات الأمنية للمشرف المعتمد."
        ),
        "dz" to listOf(
            "عارف، افتح التطبيق وشوف الخدمة وين راهي لحقة.",
            "رفيق، دير رنين للهاتف التاعي و شعل الضو.",
            "أكد بصمة الصوت والتحقق الإضافي للمالك."
        ),
        "fr" to listOf(
            "Arif, lance l'application et vérifie le workflow.",
            "Rafiq, trouve mon téléphone s'il te plaît.",
            "NHN, active le mode sécurité de l'entreprise."
        )
    )

    fun startEnrollment(name: String, role: String, lang: String) {
        currentEnrollmentName = name.trim()
        currentEnrollmentRole = role
        currentEnrollmentLang = lang
        currentStep = 1
        
        val phrases = enrollmentPhrases[lang] ?: enrollmentPhrases["en"]!!
        _enrollmentState.value = EnrollmentState.PromptingPhrase(
            step = 1,
            phrase = phrases[0],
            progress = 1f / totalSteps
        )
        Log.i("VoiceEnrollmentManager", "Enrolling user: $name, Role: $role, Language: $lang")
    }

    fun submitSpokenInput(spokenText: String): Boolean {
        val state = _enrollmentState.value
        if (state !is EnrollmentState.PromptingPhrase) return false

        Log.d("VoiceEnrollmentManager", "Enrollment speaker input: '$spokenText'")
        
        // Simulating matching transcript matches or sounds like a part of it
        val phrases = enrollmentPhrases[currentEnrollmentLang] ?: enrollmentPhrases["en"]!!
        val currentTarget = phrases[currentStep - 1]

        // Advance to next step
        if (currentStep < totalSteps) {
            currentStep++
            _enrollmentState.value = EnrollmentState.PromptingPhrase(
                step = currentStep,
                phrase = phrases[currentStep - 1],
                progress = currentStep.toFloat() / totalSteps
            )
            return true
        } else {
            // Final step complete! Analyze biometrics
            _enrollmentState.value = EnrollmentState.AnalyzingBiometrics(pitchCollected = 145.2f)
            completeAndCreateProfile()
            return true
        }
    }

    private fun completeAndCreateProfile() {
        // Generate simulated robust acoustic properties
        val finalPitch = if (currentEnrollmentRole == "Owner") 130f else 185f // masculine vs feminine pitch ranges
        val random = java.util.Random()
        val vectorList = List(5) { random.nextFloat() * 1.8f - 0.9f }
        val vectorStr = vectorList.joinToString(",") { String.format("%.3f", it) }

        val phrases = enrollmentPhrases[currentEnrollmentLang] ?: enrollmentPhrases["en"]!!

        val finalProfile = VoiceProfile(
            name = currentEnrollmentName,
            role = currentEnrollmentRole,
            preferredLanguage = currentEnrollmentLang,
            biometricPitch = finalPitch,
            enrolledPhrases = phrases.joinToString(" | "),
            voiceprintVector = vectorStr,
            isOwnerModeRestricted = (currentEnrollmentRole == "Owner"),
            hasAppAccess = (currentEnrollmentRole == "Owner"),
            hasYoutubeAccess = (currentEnrollmentRole == "Owner"),
            isActive = true
        )

        _enrollmentState.value = EnrollmentState.Success(finalProfile)
        Log.i("VoiceEnrollmentManager", "Enrollment SUCCESS. Storing VoiceProfile for: $currentEnrollmentName")
    }

    fun cancelEnrollment() {
        _enrollmentState.value = EnrollmentState.Idle
        currentStep = 1
    }
}
