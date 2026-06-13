package com.example.launcher

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.util.Log
import com.example.data.model.AutomationRule
import java.util.Locale

class IntentRouter(
    private val context: Context,
    private val onTriggerSiren: () -> Unit,
    private val onToggleFlashlight: (Boolean) -> Unit,
    private val onSpeakBack: (String) -> Unit
) {
    private val appLauncher = AppLauncher(context)
    private val shortcutManager = ShortcutManager(context)

    /**
     * Tries to execute the command matching offline lists OR custom DB automation rules.
     * Returns true if handled, false if it must fallback to online Gemini.
     */
    fun routeIntent(command: String, dbRules: List<AutomationRule>, preferredLanguage: String): Boolean {
        val query = command.lowercase(Locale.getDefault()).trim()
        Log.d("IntentRouter", "Intercepting intent offline for: '$query'")

        // 1. Check Custom DB Automation Rules first!
        val matchedRule = dbRules.firstOrNull { rule ->
            query.contains(rule.triggerPhrase.lowercase(Locale.getDefault()).trim())
        }
        if (matchedRule != null) {
            executeCustomAutomationRule(matchedRule)
            val ackMessage = when (preferredLanguage) {
                "ar", "dz" -> "تم تفعيل سيناريو التشغيل السريع: '${matchedRule.triggerPhrase}' بنجاح."
                "fr" -> "Automatisation activée: '${matchedRule.triggerPhrase}' avec succès."
                else -> "Custom automated scenario '${matchedRule.triggerPhrase}' triggered successfully."
            }
            onSpeakBack(ackMessage)
            return true
        }

        // 2. Offline command checks
        val isAr = preferredLanguage == "ar" || preferredLanguage == "dz"
        val isFr = preferredLanguage == "fr"

        // Find my phone
        if (query.contains("find my phone") || query.contains("أين أنت") || query.contains("وين كاين") || query.contains("où est mon téléphone")) {
            onTriggerSiren()
            return true
        }

        // Flashlight commands
        if (query.contains("enable flashlight") || query.contains("تشغيل الفلاش") || query.contains("أشعل الضوء") || query.contains("activer le flash")) {
            onToggleFlashlight(true)
            val text = if (isAr) "تم تشغيل كشاف الفلاش الضوئي." else if (isFr) "Flash activé." else "Flashlight turned on."
            onSpeakBack(text)
            return true
        }
        if (query.contains("disable flashlight") || query.contains("إطفاء الفلاش") || query.contains("أطفئ الضوء") || query.contains("désactiver le flash")) {
            onToggleFlashlight(false)
            val text = if (isAr) "تم إيقاف فلاش الكاميرا." else if (isFr) "Flash désactivé." else "Flashlight turned off."
            onSpeakBack(text)
            return true
        }

        // Open WhatsApp
        if (query.contains("whatsapp") || query.contains("واتساب")) {
            appLauncher.launchApplication("com.whatsapp")
            return true
        }

        // Open Camera
        if (query.contains("camera") || query.contains("كاميرا") || query.contains("appareil photo")) {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                appLauncher.launchApplication("com.android.camera")
            }
            return true
        }

        // Open settings
        if (query.contains("open settings") || query.contains("الإعدادات") || query.contains("إعدادات الهاتف") || query.contains("ouvrir les paramètres")) {
            appLauncher.openSystemSettings()
            return true
        }

        // Call client contacts
        if (query.contains("call") || query.contains("اتصل") || query.contains("تلفون") || query.contains("appelle contact")) {
            val contactNumber = query.filter { it.isDigit() }.ifBlank { "" }
            shortcutManager.triggerDialCall(contactNumber)
            return true
        }

        // Send SMS
        if (query.contains("send sms") || query.contains("رسالة قصيرة") || query.contains("رساله") || query.contains("envoyer sms")) {
            shortcutManager.triggerSmsMessage("", "NHN Workflow Quick Template")
            return true
        }

        // Open Browser
        if (query.contains("browser") || query.contains("متصفح") || query.contains("الويب") || query.contains("navigateur")) {
            appLauncher.openWebSearch("")
            return true
        }

        // Check Battery level
        if (query.contains("battery") || query.contains("البطارية") || query.contains("حالة الشحن") || query.contains("batterie")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val levelReply = when (preferredLanguage) {
                "ar", "dz" -> "مستوى طاقة بطاريتك هو $percentage بالمائة حالياً."
                "fr" -> "Le niveau actuel de votre batterie est de $percentage%."
                else -> "Your current battery status is at $percentage percent."
            }
            onSpeakBack(levelReply)
            return true
        }

        // Increase / Decrease Volume
        if (query.contains("increase volume") || query.contains("رفع الصوت") || query.contains("صوت أعلى") || query.contains("monter le volume")) {
            adjustDeviceVolume(true)
            val reply = if (isAr) "تم رفع مستوى الصوت." else if (isFr) "Volume augmenté." else "Volume increased."
            onSpeakBack(reply)
            return true
        }
        if (query.contains("decrease volume") || query.contains("خفض الصوت") || query.contains("صوت أقل") || query.contains("baisser le volume")) {
            adjustDeviceVolume(false)
            val reply = if (isAr) "تم خفض مستوى الصوت." else if (isFr) "Volume diminué." else "Volume decreased."
            onSpeakBack(reply)
            return true
        }

        return false // If no local match, proceed to online cloud processing
    }

    private fun adjustDeviceVolume(increase: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val increment = max / 8
        if (increase) {
            val target = (current + increment).coerceAtMost(max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        } else {
            val target = (current - increment).coerceAtLeast(0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        }
    }

    private fun executeCustomAutomationRule(rule: AutomationRule) {
        Log.i("IntentRouter", "Executing Custom Automation Rule: triggerPhrase='${rule.triggerPhrase}'")

        // 1. App Launch
        if (rule.appToLaunch.isNotBlank()) {
            val packageToLaunch = when (rule.appToLaunch.lowercase(Locale.getDefault())) {
                "maps", "maps navigation" -> "com.google.android.apps.maps"
                "whatsapp" -> "com.whatsapp"
                "camera" -> "com.android.camera"
                "youtube" -> "com.google.android.youtube"
                else -> rule.appToLaunch
            }
            appLauncher.launchApplication(packageToLaunch)
        }

        // 2. Custom system actions (Flashlight, siren, list locator)
        when (rule.customSystemAction) {
            "FLASHLIGHT_ON" -> onToggleFlashlight(true)
            "FLASHLIGHT_OFF" -> onToggleFlashlight(false)
            "FIND_MY_PHONE" -> onTriggerSiren()
        }

        // 3. Audio volume level adjustment
        if (rule.changeVolumeLevel in 0f..1f) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetLevel = (rule.changeVolumeLevel * max).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetLevel, AudioManager.FLAG_SHOW_UI)
        }

        // 4. Predefined SMS trigger
        if (rule.smsRecipient.isNotBlank() && rule.smsText.isNotBlank()) {
            shortcutManager.triggerSmsMessage(rule.smsRecipient, rule.smsText)
        }

        // 5. Silent/Hospital Mode trigger
        if (rule.silentMode) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            } catch (e: Exception) {
                Log.e("IntentRouter", "Failed to set silent ringer mode due to permissions", e)
            }
        }
    }
}
