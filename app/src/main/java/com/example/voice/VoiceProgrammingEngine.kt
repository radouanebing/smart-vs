package com.example.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.AutomationRule
import com.example.data.model.VoiceLearningLog
import com.example.data.model.VoiceProfile
import com.example.data.model.WakeWord
import com.example.security.SecurityAudit
import java.util.Locale

class VoiceProgrammingEngine(private val context: Context) {

    private val biometricRecognizer = VoiceBiometricRecognizer(context)

    /**
     * Inspects a command, checks security levels, matches speaker biometric profiles, and triggers appropriate actions.
     * Returns a string response indicating the outcome.
     */
    fun processVoiceCommand(
        transcript: String,
        activeLanguage: String,
        currentPitch: Float,
        speakerVector: List<Float>,
        profiles: List<VoiceProfile>,
        wakeWords: List<WakeWord>,
        automationRules: List<AutomationRule>,
        onExecuteAction: (AutomationRule) -> Unit,
        onAddLearningLog: (VoiceLearningLog) -> Unit,
        onUpdateProfilePermission: (targetName: String, allowApps: Boolean, allowYoutube: Boolean) -> Unit
    ): VoiceCommandResult {
        val query = transcript.trim()
        val lowercaseQuery = query.lowercase(Locale.getDefault())

        // 1. Confirm Wake Word usage
        var matchedWakeWord: WakeWord? = null
        for (w in wakeWords) {
            if (w.isActive && lowercaseQuery.contains(w.word.lowercase(Locale.getDefault()))) {
                matchedWakeWord = w
                break
            }
        }

        // Clean query from wake words if match found
        var actionQuery = lowercaseQuery
        if (matchedWakeWord != null) {
            actionQuery = actionQuery.replace(matchedWakeWord.word.lowercase(Locale.getDefault()), "").trim()
        }

        // 2. Identify the speaker biomterically
        val speakerResult = biometricRecognizer.identifySpeaker(currentPitch, speakerVector, profiles)
        val speakerName = speakerResult.matchedProfile?.name ?: "Unknown Speaker"

        // Live Voice Intercept for Granting Permissions (e.g. Fatima or "him" application / YouTube access)
        val isPermissionCommand = (lowercaseQuery.contains("permission") || lowercaseQuery.contains("صلاحية") || lowercaseQuery.contains("إذن") || lowercaseQuery.contains("allow") || lowercaseQuery.contains("منح") || lowercaseQuery.contains("تمكين")) &&
                (lowercaseQuery.contains("fatima") || lowercaseQuery.contains("him") || lowercaseQuery.contains("فاطمة") || lowercaseQuery.contains("هو") || lowercaseQuery.contains("staff") || lowercaseQuery.contains("family"))

        if (isPermissionCommand) {
            if (speakerResult.speakerRole == "Owner") {
                onUpdateProfilePermission("Fatima (Staff)", true, true)
                val reply = when (activeLanguage) {
                    "ar", "dz" -> "تم منح الصلاحيات بنجاح! تم تمكين فاطمة (Staff) من تشغيل التطبيقات والوصول لليوتيوب بأمر من المالك $speakerName."
                    "fr" -> "Autorisation accordée avec succès ! Fatima (Staff) est désormais autorisée à lancer les applications et YouTube par ordre de $speakerName."
                    else -> "Permissions granted successfully! Fatima (Staff) is now authorized to launch applications and access YouTube by order of the owner $speakerName."
                }
                return VoiceCommandResult(
                    reply = reply,
                    wasExecuted = true,
                    detectedSpeakerName = speakerName,
                    confidence = speakerResult.confidence,
                    speakerRole = speakerResult.speakerRole
                )
            } else {
                val reply = when (activeLanguage) {
                    "ar", "dz" -> "عذراً $speakerName، تم رفض العملية. فقط المالك يمكنه تعديل تراخيص التطبيقات واليوتيوب."
                    "fr" -> "Opération refusée $speakerName. Seul le propriétaire peut modifier les autorisations d'applications et de YouTube."
                    else -> "Sorry $speakerName, operation denied. Only the Device Owner can modify application or YouTube permissions."
                }
                return VoiceCommandResult(
                    reply = reply,
                    wasExecuted = false,
                    detectedSpeakerName = speakerName,
                    confidence = speakerResult.confidence,
                    speakerRole = speakerResult.speakerRole
                )
            }
        }
        
        // 3. Determine command security level
        val requiredLevel = getRequiredSecurityLevel(actionQuery)
        
        Log.i("VoiceProgrammingEngine", "Command: '$query' | Cleaned: '$actionQuery' | Raw Speaker: $speakerName (${speakerResult.speakerRole}) | Required Level: $requiredLevel | Match Conf: ${speakerResult.confidence}%")

        val isRtl = activeLanguage == "ar" || activeLanguage == "dz"

        // 4. Verify authority under critical OWNER mode requirements
        if (requiredLevel == VoiceSecurityLevel.OWNER_ONLY && speakerResult.speakerRole != "Owner") {
            // Breach detected!
            val errorMessage = "SECURITY RETROMAP BLOCKED: Unauthorized voice access denied."
            Log.e("VoiceProgrammingEngine", "Access breach: $speakerName attempted sensitive command: '$query' without Owner biometrics")
            
            // Log security event
            SecurityAudit.logEvent(
                "VOICE_BIOMETRIC_BREACH",
                "Unauthorized speaker ($speakerName, Conf: ${speakerResult.confidence}%) tried to invoke: '$query'",
                "CRITICAL"
            )

            // Trigger notification
            triggerBreachAlertNotification(speakerName, query, speakerResult.confidence)

            val reply = when (activeLanguage) {
                "ar", "dz" -> "تم رفض الأمر الأمني: صوت المستخدم غير مصرح له بتفعيل هذا المستوى من النظام."
                "fr" -> "Accès Refusé: Empreinte vocale non autorisée pour cette action sécurisée."
                else -> "Access Denied: Voice biometric verification failed for sensitive operation."
            }
            return VoiceCommandResult(
                reply = reply,
                wasExecuted = false,
                detectedSpeakerName = speakerName,
                confidence = speakerResult.confidence,
                speakerRole = speakerResult.speakerRole
            )
        }

        if (requiredLevel == VoiceSecurityLevel.AUTHORIZED_USER && !speakerResult.isAuthorized) {
            val reply = when (activeLanguage) {
                "ar", "dz" -> "عذراً، هذا الأمر يتطلب مستخدم معتمد بالبصمة الصوتية."
                "fr" -> "Commande refusée. Vous devez être un utilisateur enregistré."
                else -> "Sorry, that command requires an authorized speaker profile."
            }
            return VoiceCommandResult(
                reply = reply,
                wasExecuted = false,
                detectedSpeakerName = speakerName,
                confidence = speakerResult.confidence,
                speakerRole = speakerResult.speakerRole
            )
        }

        // 5. Look up custom Voice Programmable commands inside automation rules
        val matchedRule = automationRules.firstOrNull { rule ->
            actionQuery.contains(rule.triggerPhrase.lowercase(Locale.getDefault()).trim()) ||
            lowercaseQuery.contains(rule.triggerPhrase.lowercase(Locale.getDefault()).trim())
        }

        if (matchedRule != null) {
            // Execute the custom workflow actions (launch apps, change volume, dial SMS, toggle flashlight)
            onExecuteAction(matchedRule)

            // Register dynamic learning log (Dialect variation patterns check)
            val dialectVar = detectDialectPatterns(query)
            onAddLearningLog(
                VoiceLearningLog(
                    phrase = query,
                    matchedCommand = "Automation Rule: ${matchedRule.triggerPhrase}",
                    dialectVariance = dialectVar,
                    accuracyScore = speakerResult.confidence
                )
            )

            val successReply = when (activeLanguage) {
                "ar", "dz" -> "أهلاً $speakerName. تم تشغيل سيناريوهات الأتمتة الجاهزة لـ '${matchedRule.triggerPhrase}'."
                "fr" -> "Bonjour $speakerName. Scénario personnalisé '${matchedRule.triggerPhrase}' exécuté."
                else -> "Hello $speakerName. Executed programmable voice workflow: '${matchedRule.triggerPhrase}'."
            }

            return VoiceCommandResult(
                reply = successReply,
                wasExecuted = true,
                detectedSpeakerName = speakerName,
                confidence = speakerResult.confidence,
                speakerRole = speakerResult.speakerRole
            )
        }

        // Local Fallback defaults or Standard workflows
        val dialectVar = detectDialectPatterns(query)
        onAddLearningLog(
            VoiceLearningLog(
                phrase = query,
                matchedCommand = if (actionQuery.isNotBlank()) actionQuery else "General Inquiry",
                dialectVariance = dialectVar,
                accuracyScore = speakerResult.confidence
            )
        )

        return VoiceCommandResult(
            reply = "", // Proceed to default routing / Gemini API online analysis
            wasExecuted = false,
            detectedSpeakerName = speakerName,
            confidence = speakerResult.confidence,
            speakerRole = speakerResult.speakerRole,
            shouldDoStandardRoute = true,
            extractedCleanQuery = if (actionQuery.isNotBlank()) actionQuery else query
        )
    }

    private fun getRequiredSecurityLevel(query: String): VoiceSecurityLevel {
        // Owner only files and secure setup items:
        if (query.contains("security") || query.contains("تأمين") || query.contains("أمان") || query.contains("administr") || query.contains("remote") || query.contains("backup") || query.contains("نسخة احتياطية")) {
            return VoiceSecurityLevel.OWNER_ONLY
        }
        // Authorized action items (calls, messages, automations):
        if (query.contains("call") || query.contains("اتصل") || query.contains("sms") || query.contains("رسالة") || query.contains("whatsapp") || query.contains("واتساب") || query.contains("open") || query.contains("افتح") || query.contains("démarrer") || query.contains("rule") || query.contains("أتمتة")) {
            return VoiceSecurityLevel.AUTHORIZED_USER
        }
        // Public items like: asking time, battery, weather, location:
        return VoiceSecurityLevel.PUBLIC
    }

    private fun detectDialectPatterns(query: String): String {
        return when {
            query.contains("وين راهي") || query.contains("وين كاين") || query.contains("واش كاين") || query.contains("ديرلي") || query.contains("شعل") || query.contains("طفي") -> "Algerian Arabic (Darja Dialect)"
            query.contains("عارف") || query.contains("رفيق") -> "Phonetic Assistant Keyword"
            query.contains("ouvre") || query.contains("appelle") || query.contains("trouve") -> "Algerian French Dialect Accent"
            else -> "Standard Pronunciation Pattern"
        }
    }

    private fun triggerBreachAlertNotification(speaker: String, text: String, conf: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "voice_breach_channel",
                "Voice Identity Breach Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "voice_assistant_control")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 9912, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "voice_breach_channel")
            .setContentTitle("🚨 NHN Voice Shield: Identity Breach Alert")
            .setContentText("Unauthorized voice ($speaker, Match: $conf%) tried executing: '$text'")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        manager.notify(9912, builder.build())
    }
}

enum class VoiceSecurityLevel {
    PUBLIC,
    AUTHORIZED_USER,
    OWNER_ONLY
}

data class VoiceCommandResult(
    val reply: String,
    val wasExecuted: Boolean,
    val detectedSpeakerName: String,
    val confidence: Int,
    val speakerRole: String,
    val shouldDoStandardRoute: Boolean = false,
    val extractedCleanQuery: String = ""
)
