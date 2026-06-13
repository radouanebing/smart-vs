package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.local.SmartDao
import com.example.data.model.ChatMessage
import com.example.data.model.DeviceStatus
import com.example.data.model.SystemSettings
import com.example.data.model.AutomationRule
import com.example.data.model.VoiceProfile
import com.example.data.model.WakeWord
import com.example.data.model.VoiceLearningLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SmartRepository(private val smartDao: SmartDao) {

    val chatHistory: Flow<List<ChatMessage>> = smartDao.getRecentHistory()
    val deviceStatus: Flow<DeviceStatus?> = smartDao.getDeviceStatus()
    val systemSettings: Flow<SystemSettings?> = smartDao.getSettings()
    val activeAutomationRules: Flow<List<AutomationRule>> = smartDao.getActiveAutomationRules()
    val allAutomationRules: Flow<List<AutomationRule>> = smartDao.getAllAutomationRules()
    val allVoiceProfiles: Flow<List<VoiceProfile>> = smartDao.getAllVoiceProfiles()
    val allWakeWords: Flow<List<WakeWord>> = smartDao.getAllWakeWords()
    val voiceLearningLogs: Flow<List<VoiceLearningLog>> = smartDao.getVoiceLearningLogs()

    suspend fun saveMessage(message: ChatMessage) {
        smartDao.insertMessage(message)
    }

    suspend fun insertAutomationRule(rule: AutomationRule) {
        smartDao.insertAutomationRule(rule)
    }

    suspend fun deleteAutomationRule(rule: AutomationRule) {
        smartDao.deleteAutomationRule(rule)
    }

    suspend fun insertVoiceProfile(profile: VoiceProfile) {
        smartDao.insertVoiceProfile(profile)
    }

    suspend fun deleteVoiceProfile(profile: VoiceProfile) {
        smartDao.deleteVoiceProfile(profile)
    }

    suspend fun insertWakeWord(wakeWord: WakeWord) {
        smartDao.insertWakeWord(wakeWord)
    }

    suspend fun deleteWakeWord(wakeWord: WakeWord) {
        smartDao.deleteWakeWord(wakeWord)
    }

    suspend fun insertVoiceLearningLog(log: VoiceLearningLog) {
        smartDao.insertVoiceLearningLog(log)
    }

    suspend fun clearChatHistory() {
        smartDao.clearHistory()
    }

    suspend fun updateDeviceStatus(status: DeviceStatus) {
        smartDao.updateDeviceStatus(status)
    }

    suspend fun updateSystemSettings(settings: SystemSettings) {
        smartDao.updateSettings(settings)
    }

    suspend fun initializeDefaultSettings() {
        val current = smartDao.getSettings().firstOrNull()
        if (current == null) {
            smartDao.updateSettings(SystemSettings())
        }
        val currentStatus = smartDao.getDeviceStatus().firstOrNull()
        if (currentStatus == null) {
            smartDao.updateDeviceStatus(
                DeviceStatus(
                    id = 1,
                    batteryPercentage = 85,
                    connectionType = "5G Carrier",
                    isRinging = false,
                    isLocked = false,
                    latitude = 36.7538, // Algiers latitude
                    longitude = 3.0588   // Algiers longitude
                )
            )
        }
        val currentWakes = smartDao.getAllWakeWords().firstOrNull()
        if (currentWakes.isNullOrEmpty()) {
            smartDao.insertWakeWord(WakeWord(word = "Hey Assistant", isCustom = false))
            smartDao.insertWakeWord(WakeWord(word = "Arif", isCustom = false))
            smartDao.insertWakeWord(WakeWord(word = "Rafiq", isCustom = false))
            smartDao.insertWakeWord(WakeWord(word = "NHN", isCustom = false))
        }
        val currentProfiles = smartDao.getAllVoiceProfiles().firstOrNull()
        if (currentProfiles.isNullOrEmpty()) {
            smartDao.insertVoiceProfile(
                VoiceProfile(
                    name = "Youcef (Owner)",
                    role = "Owner",
                    preferredLanguage = "dz",
                    biometricPitch = 124.5f,
                    enrolledPhrases = "Open the workflow | Find my phone | Run backup",
                    voiceprintVector = "0.22,0.58,0.84,0.12,-0.41",
                    isOwnerModeRestricted = true,
                    hasAppAccess = true,
                    hasYoutubeAccess = true,
                    isActive = true
                )
            )
            smartDao.insertVoiceProfile(
                VoiceProfile(
                    name = "Fatima (Staff)",
                    role = "Family",
                    preferredLanguage = "ar",
                    biometricPitch = 210.3f,
                    enrolledPhrases = "Check battery | Tell weather",
                    voiceprintVector = "0.41,0.22,0.12,0.84,0.35",
                    isOwnerModeRestricted = false,
                    hasAppAccess = false,
                    hasYoutubeAccess = false,
                    isActive = true
                )
            )
        }
        val currentRules = smartDao.getActiveAutomationRules().firstOrNull()
        if (currentRules.isNullOrEmpty()) {
            smartDao.insertAutomationRule(
                AutomationRule(
                    triggerPhrase = "I am going to work",
                    appToLaunch = "Maps",
                    enableBluetooth = true,
                    smsRecipient = "+213555012345",
                    smsText = "I am starting my commute now! See you soon.",
                    isActive = true
                )
            )
            smartDao.insertAutomationRule(
                AutomationRule(
                    triggerPhrase = "Good night",
                    changeVolumeLevel = 0.1f,
                    customSystemAction = "FLASHLIGHT_OFF",
                    isActive = true
                )
            )
            smartDao.insertAutomationRule(
                AutomationRule(
                    triggerPhrase = "Hospital Mode",
                    appToLaunch = "Maps",
                    changeVolumeLevel = 0.0f,
                    silentMode = true,
                    screenBrightness = 0.1f,
                    isActive = true
                )
            )
        }
    }

    /**
     * Calls Gemini API to get voice interaction response
     */
    suspend fun consultAI(prompt: String, lang: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("SmartRepository", "Gemini API key is not configured!")
            return getFallbackResponse(prompt, lang)
        }

        // Determine system instructions based on language
        val baseInstructionText = when (lang) {
            "ar" -> "أنت مساعد صوتي ذكي مدمج في تطبيق NHN Workflow. أجب باختصار شديد جداً وباللغة العربية الفصحى المبسطة بما تلائم الأوامر الصوتية المحمولة."
            "dz" -> "أنت المساعد الصوتي للجزائر بالدارجة الجزائرية. أجب باختصار وذكاء تام بالدارجة الجزائرية السهلة والمحببة للمستخدمين، ولا تستعمل كلمات صعبة."
            "fr" -> "Vous êtes l'assistant vocal intelligent NHN Workflow. Répondez de manière très concise en français algérien moderne, adaptée à des commandes vocales rapides et claires."
            else -> "You are the NHN Workflow Smart Voice Assistant. Respond very concisely in English for hands-free voice commands."
        }

        val actionStructureRules = """
            [CRITICAL ENGINE RULE]: If the user intent is to launch/open an application, trigger/open a settings panel, dial a phone number, send an SMS/message, open maps/navigate, set/create an alarm, or share content, you MUST generate and return ONLY a pure JSON block following this schema:
            {
              "actionType": "LAUNCH_APP" | "SYSTEM_SETTING" | "DIAL" | "SEND_SMS" | "NAVIGATE" | "SET_ALARM" | "SHARE",
              "target": "package_name_or_number_or_destination_or_setting_type",
              "title": "sms_body_or_alarm_label",
              "extraParams": { "hour": "optional_alarm_hour", "minutes": "optional_alarm_minutes" }
            }
            Do not wrap the JSON output in triple backticks (```) or code blocks or any introductory text. It must be valid parsable JSON. If none of these tool actions match, reply normally with natural conversational speech in the requested language ($lang).
            
            Multilingual Command Translation Examples:
            - User: "open settings" or "ouvrir les paramètres" or "افتح الإعدادات" or "حل الإعدادات" -> { "actionType": "SYSTEM_SETTING", "target": "wifi" }
            - User: "launch camera" or "ouvrir l'appareil photo" or "شغل الكاميرا" or "افتح الكاميرا" or "حل الكاميرا" -> { "actionType": "LAUNCH_APP", "target": "com.android.camera" }
            - User: "dial 0550123456" or "appeler 0550123456" or "اتصل بـ 0550123456" or "عيط لـ 0550123456" -> { "actionType": "DIAL", "target": "0550123456" }
            - User: "send sms to 0550123456 saying Hello" or "envoyer un message à 0550123456" or "ارسل رسالة لـ 0550123456" or "ابعث ميساج لـ 0550123456" -> { "actionType": "SEND_SMS", "target": "0550123456", "title": "Hello" }
            - User: "navigate to Algiers" or "aller à Alger" or "الذهاب إلى الجزائر" or "ديني للجزائر" -> { "actionType": "NAVIGATE", "target": "Algiers" }
            - User: "set alarm for 8:15 AM" or "régler l'alarme à 8h15" or "اضبط المنبه لـ 8:15" or "دير منبه على 8 وربع" -> { "actionType": "SET_ALARM", "target": "Wake Up", "extraParams": { "hour": "8", "minutes": "15" } }
        """.trimIndent()

        val instructionText = "$baseInstructionText\n\n$actionStructureRules"

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            systemInstruction = Content(parts = listOf(Part(text = instructionText)))
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: getFallbackResponse(prompt, lang)
        } catch (e: Exception) {
            Log.e("SmartRepository", "Gemini API failure, fallback applied", e)
            getFallbackResponse(prompt, lang)
        }
    }

    private fun getFallbackResponse(prompt: String, lang: String): String {
        val query = prompt.lowercase().trim()
        
        // Basic offline local heuristics for rapid, responsive offline voice control:
        if (lang == "ar" || lang == "dz") {
            return when {
                query.contains("واتساب") || query.contains("whatsapp") -> "مفهوم، جاري تشغيل تطبيق واتساب للتواصل."
                query.contains("اتصل") || query.contains("تلفون") || query.contains("call") -> "جاري الاتصال بجهة الاتصال المطلوبة."
                query.contains("كاميرا") || query.contains("camera") -> "حاضر، جاري فتح الكاميرا الآن لالتقاط الصور."
                query.contains("رسالة") || query.contains("sms") -> "جاري تحضير رسالة قصيرة وإرسالها."
                query.contains("أين أنت") || query.contains("وينك") -> "أنا هنا! سأقوم بتشغيل الرنين والوميض فوراً."
                query.contains("ابحث") || query.contains("البحث") || query.contains("search") -> "جاري البحث على غوغل عن " + prompt.substringAfter("البحث")
                else -> "لقد تلقيت أمرك: \"$prompt\". سأقوم بمعالجته فوراً."
            }
        } else if (lang == "fr") {
            return when {
                query.contains("whatsapp") -> "Compris, j'ouvre WhatsApp tout de suite."
                query.contains("appelle") || query.contains("téléphone") || query.contains("appeler") -> "Lancement de l'appel pour le contact demandé."
                query.contains("camera") || query.contains("appareil photo") -> "D'accord, j'ouvre l'appareil photo."
                query.contains("message") || query.contains("sms") -> "Préparation et envoi du SMS en cours."
                query.contains("où es-tu") || query.contains("ou es tu") -> "Je suis ici ! J'active la sonnerie et le flash immédiatement."
                query.contains("recherche") || query.contains("chercher") -> "Je lance la recherche web pour " + prompt.substringAfter("recherche")
                else -> "J'ai bien entendu: \"$prompt\". J'exécute votre commande."
            }
        } else {
            return when {
                query.contains("whatsapp") -> "Understood, opening WhatsApp."
                query.contains("call") || query.contains("phone") -> "Calling the requested contact now."
                query.contains("camera") -> "Sure, opening the device camera."
                query.contains("message") || query.contains("sms") -> "Preparing and sending SMS."
                query.contains("where are you") || query.contains("find my phone") -> "I am here! Activating loud ring and flashlight blink."
                query.contains("search") -> "Searching the web for " + prompt.substringAfter("search")
                else -> "Received: \"$prompt\". I am performing that action."
            }
        }
    }
}
