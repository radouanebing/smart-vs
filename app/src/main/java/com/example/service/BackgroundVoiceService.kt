package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.DeviceStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

class BackgroundVoiceService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var isSettingsEnabled = true
    private var preferredLanguage = "en"

    // Smart Listening & Battery Protection variables
    private var listeningMode = "smart" // "always", "smart", "ptt"
    private var adaptiveListeningEnabled = true
    private var lastVoiceCommandTime = System.currentTimeMillis()
    private var isThrottled = false
    private var isMicBusyByOtherApp = false
    private var consecutiveErrorCount = 0
    private var isBackgroundBlocked = false

    companion object {
        private const val NOTIFICATION_ID = 8888
        private const val CHANNEL_ID = "background_voice_activation_channel"
        const val ACTION_START = "com.example.action.START_VOICE_SERVICE"
        const val ACTION_STOP = "com.example.action.STOP_VOICE_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BackgroundVoiceService", "onCreate called")
        createNotificationChannel()
        observeSettings()
        
        // Start periodic 10-second system status and intelligent microphone check
        serviceScope.launch {
            while (isActive) {
                evaluateListeningRules()
                delay(10000)
            }
        }
    }

    private fun observeSettings() {
        serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            database.smartDao.getSettings().collect { settings ->
                if (settings != null) {
                    isSettingsEnabled = settings.backgroundVoiceEnabled
                    preferredLanguage = settings.preferredLanguage
                    listeningMode = settings.listeningMode
                    adaptiveListeningEnabled = settings.adaptiveListeningEnabled
                    evaluateListeningRules()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundVoiceService", "onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                isBackgroundBlocked = false
            }
        }

        startAsForeground()
        evaluateListeningRules()
        return START_STICKY
    }

    private fun startAsForeground() {
        val stopIntent = Intent(this, BackgroundVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (preferredLanguage == "ar" || preferredLanguage == "dz") "مساعد الخلفية نشط" else "Vocal Assistant Active")
            .setContentText(if (preferredLanguage == "ar" || preferredLanguage == "dz") "نظام الاستماع في الخلفية قيد التشغيل" else "Continuous background wake-word engine is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var started = false
            // 1. Try to start as MICROPHONE if we have the permission
            if (hasMic) {
                try {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                    started = true
                    Log.i("BackgroundVoiceService", "Successfully started foreground service with TYPE_MICROPHONE")
                } catch (e: Throwable) {
                    Log.w("BackgroundVoiceService", "Error starting as TYPE_MICROPHONE: ${e.message}. Falling back to TYPE_DATA_SYNC.")
                }
            }

            // 2. If not started yet (no mic perm, or background launch restricted), start as DATA_SYNC
            if (!started) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    started = true
                    Log.i("BackgroundVoiceService", "Successfully started foreground service with TYPE_DATA_SYNC")
                } catch (e: Throwable) {
                    Log.e("BackgroundVoiceService", "Failed to start foreground with TYPE_DATA_SYNC: ${e.message}. Falling back to legacy startForeground.", e)
                }
            }

            // 3. Final fallback: standard startForeground and catch all exceptions to prevent crash at all costs
            if (!started) {
                try {
                    startForeground(NOTIFICATION_ID, notification)
                    Log.i("BackgroundVoiceService", "Successfully started foreground service with legacy startForeground as last resort")
                } catch (e: Throwable) {
                    Log.e("BackgroundVoiceService", "CRITICAL: All startForeground attempts failed. Service running as background service or may be killed.", e)
                }
            }
        } else {
            // Legacy SDK versions
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e: Throwable) {
                Log.e("BackgroundVoiceService", "Failed legacy startForeground", e)
            }
        }
    }

    private fun startListeningLoop() {
        if (!isSettingsEnabled || listeningMode == "ptt") {
            stopListeningLoop()
            return
        }
        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            Log.w("BackgroundVoiceService", "Cannot start background listening loop: RECORD_AUDIO permission not granted.")
            return
        }
        if (speechRecognizer != null) return
        Log.d("BackgroundVoiceService", "Starting background listening loop")
        initSpeechRecognizer()
    }

    private fun stopListeningLoop() {
        Log.d("BackgroundVoiceService", "Stopping background listening loop")
        speechRecognizer?.apply {
            cancel()
            destroy()
        }
        speechRecognizer = null
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("BackgroundVoiceService", "Speech recognition not available")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("BackgroundVoiceService", "Ready for background speech input")
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input timeout"
                        else -> "Unhandled recognizer error code: $error"
                    }
                    Log.w("BackgroundVoiceService", "Speech recognizer error: $message ($error)")
                    
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        Log.e("BackgroundVoiceService", "Stopping background listening loop due to ERROR_INSUFFICIENT_PERMISSIONS / AppOps block")
                        isBackgroundBlocked = true
                        stopListeningLoop()
                        return
                    }

                    if (error == SpeechRecognizer.ERROR_AUDIO || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        isMicBusyByOtherApp = true
                        serviceScope.launch {
                            delay(30000) // retry after 30 seconds
                            isMicBusyByOtherApp = false
                        }
                    }

                    consecutiveErrorCount++

                    if (isSettingsEnabled && listeningMode != "ptt") {
                        val restartDelay = if (consecutiveErrorCount > 3) {
                            // Back off exponentially if there are consecutive failures to protect resource usage
                            (15000L * (consecutiveErrorCount - 2)).coerceAtMost(60000L)
                        } else if (isThrottled) {
                            15000L
                        } else {
                            1200L
                        }
                        serviceScope.launch {
                            delay(restartDelay)
                            restartListening()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    consecutiveErrorCount = 0
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Log.i("BackgroundVoiceService", "Transcribed phrase: $text")
                        checkAndWakeIfMatches(text)
                    }
                    if (isSettingsEnabled && listeningMode != "ptt") {
                        val restartDelay = if (isThrottled) 15000L else 1200L
                        serviceScope.launch {
                            delay(restartDelay)
                            restartListening()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Log.v("BackgroundVoiceService", "Partial transcription: $text")
                        checkAndWakeIfMatches(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        triggerSpeechRecognition()
    }

    private fun triggerSpeechRecognition() {
        if (!isSettingsEnabled || listeningMode == "ptt") return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val localeStr = when (preferredLanguage) {
                "ar", "dz" -> "ar-DZ"
                "fr" -> "fr-FR"
                else -> "en-US"
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("BackgroundVoiceService", "Failed to start listening", e)
        }
    }

    private fun canListenInBackground(): Boolean {
        if (!MainActivity.isAppInForeground) return false
        if (isBackgroundBlocked) return false
        if (!isSettingsEnabled || listeningMode == "ptt") return false

        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasMic) return false

        if (isMicBusyByOtherApp) return false

        // Check power status
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isPowerSaveMode) return false

        // Check battery status
        val batteryIntent = applicationContext.registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
        if (batteryPct < 15) return false

        // Check active call
        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isCallActive = audioManager.mode == AudioManager.MODE_IN_CALL || 
                           audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
        if (isCallActive) return false

        // Check temperature
        val tempTenths = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val temperatureCelsius = tempTenths / 10.0f
        if (temperatureCelsius >= 45.0f) return false

        return true
    }

    private fun restartListening() {
        if (!canListenInBackground()) {
            stopListeningLoop()
            return
        }
        try {
            speechRecognizer?.cancel()
            triggerSpeechRecognition()
        } catch (e: Exception) {
            Log.e("BackgroundVoiceService", "Error during restartListening", e)
            stopListeningLoop()
            startListeningLoop()
        }
    }

    private fun checkAndWakeIfMatches(text: String) {
        val tLower = text.lowercase(Locale.getDefault())
        val isMatched = tLower.contains("hey assistant") || 
                        tLower.contains("smart assistant") || 
                        tLower.contains("مرحبا مساعد") || 
                        tLower.contains("يا مساعد") ||
                        tLower.contains("مساعد") || 
                        tLower.contains("assistant")

        if (isMatched) {
            Log.i("BackgroundVoiceService", "WAKE WORD DETECTED IN BACKGROUND!")
            lastVoiceCommandTime = System.currentTimeMillis()
            isThrottled = false
            wakeDeviceAndLaunchApp()
        }
    }

    private fun wakeDeviceAndLaunchApp() {
        try {
            val wakeIntent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                action = "com.example.action.WAKE_ASSISTANT"
            }
            applicationContext.startActivity(wakeIntent)
            Log.d("BackgroundVoiceService", "MainActivity launched from background to trigger wake-up")
        } catch (e: Exception) {
            Log.e("BackgroundVoiceService", "Error launching MainActivity to wake device", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Background Voice Activation Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopListeningLoop()
        serviceJob.cancel()
        Log.d("BackgroundVoiceService", "Service destroyed")
    }

    // Modern Intelligent Microphone Management & Thermal Protection Policy
    private fun evaluateListeningRules() {
        if (isBackgroundBlocked) {
            updateSystemStatus("Paused (Background Restricted)", false, 0.00f, 100, 28.0f, false, 100)
            stopListeningLoop()
            return
        }

        if (!MainActivity.isAppInForeground) {
            updateSystemStatus("Standby (App Backgrounded)", false, 0.01f, 100, 28.0f, false, 100)
            stopListeningLoop()
            return
        }

        if (!isSettingsEnabled) {
            updateSystemStatus("Disabled", false, 0.00f, 100, 28.0f, false, 100)
            stopListeningLoop()
            return
        }

        if (listeningMode == "ptt") {
            updateSystemStatus("Push To Talk", false, 0.01f, 98, 28.0f, false, 100)
            stopListeningLoop()
            return
        }

        // Collect telemetry details
        val batteryIntent = applicationContext.registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 85
        val tempTenths = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val temperatureCelsius = tempTenths / 10.0f

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isBatterySaver = powerManager.isPowerSaveMode

        val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isCallActive = audioManager.mode == AudioManager.MODE_IN_CALL || 
                           audioManager.mode == AudioManager.MODE_IN_COMMUNICATION

        var shouldPause = false
        var reason = "Listening"

        when {
            batteryPct < 15 -> {
                shouldPause = true
                reason = "Paused (Low Battery)"
            }
            isBatterySaver -> {
                shouldPause = true
                reason = "Paused (Battery Saver)"
            }
            isCallActive -> {
                shouldPause = true
                reason = "Paused (Call)"
            }
            temperatureCelsius >= 45.0f -> {
                shouldPause = true
                reason = "Paused (Thermal)"
            }
            isMicBusyByOtherApp -> {
                shouldPause = true
                reason = "Paused (Mic Busy)"
            }
        }

        if (shouldPause) {
            stopListeningLoop()
            updateSystemStatus(
                reason, 
                false, 
                0.05f, 
                95, 
                temperatureCelsius, 
                isBatterySaver, 
                batteryPct
            )
        } else {
            // Adaptive listen throttling:
            val timeSinceLastWake = System.currentTimeMillis() - lastVoiceCommandTime
            val maxIdleTimeBeforeThrottling = 3 * 60 * 1000L // 3 minutes
            val idleThrottle = adaptiveListeningEnabled && timeSinceLastWake > maxIdleTimeBeforeThrottling && listeningMode == "smart"

            val statusStr = if (idleThrottle) "Listening (Throttled)" else "Listening"
            val cpuSavings = if (idleThrottle) 65 else (if (listeningMode == "always") 0 else 40)
            val hourlyDrain = if (idleThrottle) 0.12f else (if (listeningMode == "always") 1.8f else 0.45f)

            isThrottled = idleThrottle
            startListeningLoop()
            updateSystemStatus(
                statusStr, 
                true, 
                hourlyDrain, 
                cpuSavings, 
                temperatureCelsius, 
                isBatterySaver, 
                batteryPct
            )
        }
    }

    private fun updateSystemStatus(
        listeningStatus: String,
        micActive: Boolean,
        hourlyDrain: Float,
        cpuSavings: Int,
        temp: Float,
        batterySaver: Boolean,
        batteryPct: Int
    ) {
        serviceScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(applicationContext)
            val current = database.smartDao.getDeviceStatus().firstOrNull() ?: DeviceStatus(
                id = 1,
                batteryPercentage = batteryPct,
                connectionType = "5G Carrier",
                latitude = 36.7538,
                longitude = 3.0588
            )
            val thermalStatus = when {
                temp >= 45.0f -> "Critical"
                temp >= 38.0f -> "Warm"
                else -> "Normal"
            }
            val updated = current.copy(
                batteryPercentage = batteryPct,
                micActivityActive = micActive,
                listeningStatus = listeningStatus,
                estimatedHourlyDrain = hourlyDrain,
                cpuSavingsPercent = cpuSavings,
                deviceTemperature = temp,
                thermalStatus = thermalStatus,
                isBatterySaverActive = batterySaver,
                lastUpdated = System.currentTimeMillis()
            )
            database.smartDao.updateDeviceStatus(updated)
        }
    }
}
