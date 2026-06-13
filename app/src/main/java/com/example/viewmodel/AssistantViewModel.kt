package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.DeviceStatus
import com.example.data.model.SystemSettings
import com.example.data.model.AutomationRule
import com.example.data.model.VoiceProfile
import com.example.data.model.WakeWord
import com.example.data.model.VoiceLearningLog
import com.example.data.repository.SmartRepository
import com.example.launcher.IntentRouter
import com.example.launcher.NativeExecutionLayer
import com.example.launcher.NativeExecutionAction
import com.example.launcher.ExecutionOutcome
import com.example.launcher.AppMetaDataDetail
import com.example.voice.VoiceSessionManager
import com.example.voice.VoiceEnrollmentManager
import com.example.voice.EnrollmentState
import com.example.voice.VoiceProgrammingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SmartRepository(database.smartDao)

    // UI States
    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceStatus: StateFlow<DeviceStatus?> = repository.deviceStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val systemSettings: StateFlow<SystemSettings?> = repository.systemSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Multi-User Voice & Wake Word configs
    val allVoiceProfiles: StateFlow<List<VoiceProfile>> = repository.allVoiceProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWakeWords: StateFlow<List<WakeWord>> = repository.allWakeWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceLearningLogs: StateFlow<List<VoiceLearningLog>> = repository.voiceLearningLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val enrollmentManager = VoiceEnrollmentManager(application)
    val enrollmentState = enrollmentManager.enrollmentState

    // Simulated speaker context for demonstration
    private val _simulatedSpeakerId = MutableStateFlow<Int?>(1) // Default pre-seeded Owner Profile
    val simulatedSpeakerId: StateFlow<Int?> = _simulatedSpeakerId.asStateFlow()

    fun setSimulatedSpeaker(profileId: Int?) {
        _simulatedSpeakerId.value = profileId
    }

    // Interactive enrollment triggers
    fun startEnrollment(name: String, role: String, lang: String) {
        enrollmentManager.startEnrollment(name, role, lang)
    }

    fun submitEnrollmentSpokenText(text: String) {
        viewModelScope.launch {
            val done = enrollmentManager.submitSpokenInput(text)
            if (done) {
                val state = enrollmentManager.enrollmentState.value
                if (state is EnrollmentState.Success) {
                    repository.insertVoiceProfile(state.profile)
                }
            }
        }
    }

    fun cancelEnrollment() {
        enrollmentManager.cancelEnrollment()
    }

    // Wake Words and Profile Management
    fun addWakeWord(word: String) {
        viewModelScope.launch {
            repository.insertWakeWord(WakeWord(word = word, isCustom = true, isActive = true))
        }
    }

    fun removeWakeWord(wakeWord: WakeWord) {
        viewModelScope.launch {
            repository.deleteWakeWord(wakeWord)
        }
    }

    fun deleteVoiceProfile(profile: VoiceProfile) {
        viewModelScope.launch {
            // Unset selected simulated speaker if deleted
            if (_simulatedSpeakerId.value == profile.id) {
                _simulatedSpeakerId.value = null
            }
            repository.deleteVoiceProfile(profile)
        }
    }

    fun updateVoiceProfilePermissions(profile: VoiceProfile, allowApps: Boolean, allowYoutube: Boolean) {
        viewModelScope.launch {
            repository.insertVoiceProfile(profile.copy(hasAppAccess = allowApps, hasYoutubeAccess = allowYoutube))
        }
    }

    // Interaction states
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialCommandText = MutableStateFlow("")
    val partialCommandText: StateFlow<String> = _partialCommandText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Find My Phone Action State
    private val _findMyPhoneActive = MutableStateFlow(false)
    val findMyPhoneActive: StateFlow<Boolean> = _findMyPhoneActive.asStateFlow()

    // Automation Rules Flow
    val automationRules: StateFlow<List<AutomationRule>> = repository.allAutomationRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Simulating Mic Voice amplitude for holographic visualizer pulses
    private val _voiceAmplitude = MutableStateFlow(1.0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    // Emergency Trigger Active Flag
    private val _emergencyModeActive = MutableStateFlow(false)
    val emergencyModeActive: StateFlow<Boolean> = _emergencyModeActive.asStateFlow()

    // Active context simulation (DRIVING, WORK, HOME, DEFAULT)
    private val _currentContext = MutableStateFlow("DEFAULT")
    val currentContext: StateFlow<String> = _currentContext.asStateFlow()

    fun updateCurrentContext(contextCode: String) {
        _currentContext.value = contextCode
        com.example.security.SecurityAudit.logEvent("ENVIRONMENT_CONTEXT", "Switched context to $contextCode. Auto learning dynamic suggestions applied.", "INFO")
    }

    fun triggerEmergencyMode() {
        _emergencyModeActive.value = true
        com.example.security.SecurityAudit.logEvent("EMERGENCY_MODE", "CRITICAL user panic trigger SOS warning issued!", "CRITICAL")
        triggerFindMyPhoneSiren()
    }

    fun dismissEmergencyMode() {
        _emergencyModeActive.value = false
        dismissFindMyPhoneSiren()
    }

    private val intentRouter by lazy {
        IntentRouter(
            context = getApplication(),
            onTriggerSiren = { triggerFindMyPhoneSiren() },
            onToggleFlashlight = { state -> toggleFlashlightDirect(state) },
            onSpeakBack = { text -> speakText(text) }
        )
    }

    val nativeExecutionLayer by lazy {
        NativeExecutionLayer(getApplication())
    }

    fun addAutomationRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.insertAutomationRule(rule)
        }
    }

    fun removeAutomationRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.deleteAutomationRule(rule)
        }
    }

    private fun toggleFlashlightDirect(state: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val cameraManager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull()
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, state)
                }
            } catch (e: Exception) {
                Log.e("SmartVoiceVM", "Toggle flashlight hardware error", e)
            }
        }
    }

    // Speech-To-Text / Text-To-Speech Handlers
    private var speechRecognizerManager: com.example.voice.SpeechRecognizerManager? = null
    private var textToSpeech: TextToSpeech? = null
    private var ringtone: Ringtone? = null
    private var flashlightJob = false

    init {
        viewModelScope.launch {
            repository.initializeDefaultSettings()
            monitorBatteryAndConnectivity()
        }
        viewModelScope.launch {
            isListening.collectLatest { listening ->
                if (listening) {
                    while (isListening.value) {
                        _voiceAmplitude.value = (0.7f + Math.random().toFloat() * 1.5f)
                        delay(120)
                    }
                } else {
                    _voiceAmplitude.value = 1.0f
                }
            }
        }
        initTTS(application)
        initSpeechRecognizer(application)

        viewModelScope.launch {
            systemSettings.collectLatest { settings ->
                if (settings != null) {
                    syncBackgroundVoiceService(settings.backgroundVoiceEnabled)
                }
            }
        }
    }

    private fun syncBackgroundVoiceService(enabled: Boolean) {
        val context = getApplication<Application>()
        val intent = Intent(context, com.example.service.BackgroundVoiceService::class.java).apply {
            action = com.example.service.BackgroundVoiceService.ACTION_START
        }
        try {
            if (enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        } catch (e: Exception) {
            Log.e("SmartVoiceVM", "Failed to sync BackgroundVoiceService status", e)
        }
    }

    fun refreshBackgroundVoiceService() {
        val enabled = systemSettings.value?.backgroundVoiceEnabled ?: true
        syncBackgroundVoiceService(enabled)
    }

    fun onWakeWordDetected() {
        val lang = systemSettings.value?.preferredLanguage ?: "en"
        val responseMsg = when (lang) {
            "ar", "dz" -> "نعم، أنا أستمع إليك."
            "fr" -> "Oui, je vous écoute."
            else -> "Yes, I am listening."
        }
        speakText(responseMsg)
        
        viewModelScope.launch {
            repository.saveMessage(ChatMessage(text = responseMsg, sender = "assistant"))
        }

        viewModelScope.launch {
            delay(1200)
            startListening()
        }
    }

    private fun initTTS(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set default language or detect setting language
                viewModelScope.launch {
                    val settings = systemSettings.firstOrNull() ?: SystemSettings()
                    val locale = when (settings.preferredLanguage) {
                        "ar", "dz" -> Locale("ar")
                        "fr" -> Locale.FRENCH
                        else -> Locale.ENGLISH
                    }
                    textToSpeech?.language = locale
                }
            } else {
                Log.e("SmartVoiceVM", "Text-To-Speech engine failed to initialize.")
            }
        }
    }

    private fun initSpeechRecognizer(context: Context) {
        speechRecognizerManager = com.example.voice.SpeechRecognizerManager(
            context = context,
            onReady = {
                _isListening.value = true
                _partialCommandText.value = ""
            },
            onPartialResult = { partial ->
                _partialCommandText.value = partial
            },
            onFinalResult = { text ->
                _partialCommandText.value = text
                executeVoiceCommand(text)
            },
            onErrorState = { error ->
                Log.e("SmartVoiceVM", "SpeechRecognizer error: $error")
                _isListening.value = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions (Microphone)"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown speech error ($error)"
                }
                viewModelScope.launch {
                    repository.saveMessage(ChatMessage(text = "⚠️ Speech Error: $errorMsg", sender = "assistant"))
                }
            }
        )
    }

    /**
     * Start speech input recording
     */
    fun startListening() {
        _partialCommandText.value = ""
        val lang = systemSettings.value?.preferredLanguage ?: "en"
        val manager = speechRecognizerManager
        if (manager != null && manager.isAvailable) {
            try {
                manager.startListening(lang)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e("SmartVoiceVM", "Start speech recognition error", e)
                simulateVoiceInputDemo()
            }
        } else {
            simulateVoiceInputDemo()
        }
    }

    /**
     * Stop speech recording
     */
    fun stopListening() {
        speechRecognizerManager?.stopListening()
        _isListening.value = false
    }

    /**
     * Simulation mode for quick testing and sandboxed models
     */
    private fun simulateVoiceInputDemo() {
        _isListening.value = true
        _partialCommandText.value = ""
        viewModelScope.launch {
            delay(1500)
            _isListening.value = false
            val lang = systemSettings.value?.preferredLanguage ?: "en"
            
            // Generate some random simulated scenarios based on language
            val simulatedText = when (lang) {
                "ar", "dz" -> listOf(
                    "افتح واتساب وتواصل",
                    "أين الهاتف ابحث عنه",
                    "اتصل برقم الأمان",
                    "افتح الكاميرا لالتقاط صورة",
                    "ابحث في الويب عن طقس الجزائر"
                ).random()
                "fr" -> listOf(
                    "ouvrir whatsapp",
                    "où est mon téléphone",
                    "appeler le contact",
                    "ouvrir l'appareil photo",
                    "recherche météo algérie"
                ).random()
                else -> listOf(
                    "open whatsapp",
                    "find my phone",
                    "call emergency number",
                    "open camera",
                    "search web for neural networks"
                ).random()
            }
            _partialCommandText.value = simulatedText
            executeVoiceCommand(simulatedText)
        }
    }

    /**
     * Process voice command text
     */
    fun executeVoiceCommand(commandText: String) {
        if (commandText.isBlank()) return

        _isProcessing.value = true
        viewModelScope.launch {
            // Save command to history
            repository.saveMessage(ChatMessage(text = commandText, sender = "user"))

            val lang = systemSettings.value?.preferredLanguage ?: "en"

            // Resolve active simulated biometric profile details
            val profilesList = repository.allVoiceProfiles.firstOrNull() ?: emptyList()
            val simulatedProfile = profilesList.find { it.id == _simulatedSpeakerId.value }

            val pitch = simulatedProfile?.biometricPitch ?: 160f
            val vector = simulatedProfile?.voiceprintVector?.split(",")?.mapNotNull { it.trim().toFloatOrNull() }
                ?: listOf(0.12f, 0.45f, -0.18f, 0.5f, 0.1f)

            val wakeWordsList = repository.allWakeWords.firstOrNull() ?: emptyList()
            val dbRules = repository.allAutomationRules.firstOrNull() ?: emptyList()

            // Run programming engine
            val engine = VoiceProgrammingEngine(getApplication())
            val engineResult = engine.processVoiceCommand(
                transcript = commandText,
                activeLanguage = lang,
                currentPitch = pitch,
                speakerVector = vector,
                profiles = profilesList,
                wakeWords = wakeWordsList,
                automationRules = dbRules,
                onExecuteAction = { rule ->
                    intentRouter.routeIntent(rule.triggerPhrase, dbRules, lang)
                },
                onAddLearningLog = { log ->
                    viewModelScope.launch {
                        repository.insertVoiceLearningLog(log)
                    }
                },
                onUpdateProfilePermission = { targetName, allowApps, allowYoutube ->
                    viewModelScope.launch {
                        val match = profilesList.find { it.name.lowercase(Locale.getDefault()).contains(targetName.substringBefore(" ").lowercase(Locale.getDefault())) }
                        if (match != null) {
                            repository.insertVoiceProfile(match.copy(hasAppAccess = allowApps, hasYoutubeAccess = allowYoutube))
                            Log.i("AssistantViewModel", "Permissions updated for speaker: ${match.name}")
                        }
                    }
                }
            )

            if (engineResult.reply.isNotEmpty()) {
                // Handled secure block or custom workflow execute
                repository.saveMessage(ChatMessage(text = engineResult.reply, sender = "assistant"))
                speakText(engineResult.reply)
                _isProcessing.value = false
                return@launch
            }

            val queryToProcess = if (engineResult.extractedCleanQuery.isNotEmpty()) engineResult.extractedCleanQuery else commandText

            val cmdLower = queryToProcess.lowercase(Locale.getDefault()).trim()
            if (cmdLower == "emergency" || cmdLower == "help me" || cmdLower == "sos" || cmdLower.contains("النجدة") || cmdLower.contains("ساعدني")) {
                triggerEmergencyMode()
                val responseMsg = when (lang) {
                    "ar", "dz" -> "تم تفعيل وضع الطوارئ الأقصى! تشغيل الإنذار والوميض وإرسال الموقع."
                    "fr" -> "MODE URGENCE ACTIVÉ! Sirène et flash en boucle, envoi de votre position."
                    else -> "EMERGENCY MODE ACTIVATED! Loud alarm siren is looping, flashlight blinking, sending GPS location."
                }
                repository.saveMessage(ChatMessage(text = responseMsg, sender = "assistant"))
                speakText(responseMsg)
                _isProcessing.value = false
                return@launch
            }

            val activeProfile = profilesList.find { it.id == _simulatedSpeakerId.value }
            val isOwner = activeProfile?.role == "Owner"
            
            if (!isOwner) {
                val isYoutubeRequest = queryToProcess.lowercase(Locale.getDefault()).contains("youtube") || queryToProcess.lowercase(Locale.getDefault()).contains("يوتيوب")
                val isAppLaunchRequest = queryToProcess.lowercase(Locale.getDefault()).contains("open") || queryToProcess.lowercase(Locale.getDefault()).contains("launch") || queryToProcess.lowercase(Locale.getDefault()).contains("افتح") || queryToProcess.lowercase(Locale.getDefault()).contains("démarrer") || queryToProcess.lowercase(Locale.getDefault()).contains("whatsapp") || queryToProcess.lowercase(Locale.getDefault()).contains("camera") || queryToProcess.lowercase(Locale.getDefault()).contains("settings") || queryToProcess.lowercase(Locale.getDefault()).contains("إعدادات") || queryToProcess.lowercase(Locale.getDefault()).contains("كاميرا") || queryToProcess.lowercase(Locale.getDefault()).contains("تطبيق")
                
                if (isYoutubeRequest && activeProfile?.hasYoutubeAccess == false) {
                    val failReply = when (lang) {
                        "ar", "dz" -> "تم رفض الطلب: لا تملك صلاحية الوصول إلى تطبيق YouTube على هذا النظام."
                        "fr" -> "Accès refusé: Votre empreinte vocale n'est pas autorisée à accéder à l'application YouTube."
                        else -> "Access Denied: Voice profile '${activeProfile?.name}' is not authorized to access YouTube."
                    }
                    repository.saveMessage(ChatMessage(text = failReply, sender = "assistant"))
                    speakText(failReply)
                    _isProcessing.value = false
                    return@launch
                } else if (isAppLaunchRequest && activeProfile?.hasAppAccess == false) {
                    val failReply = when (lang) {
                        "ar", "dz" -> "تم رفض الطلب: لا تملك صلاحية الدخول إلى تطبيقات الهاتف."
                        "fr" -> "Accès refusé: Votre empreinte vocale n'est pas autorisée à accéder aux applications."
                        else -> "Access Denied: Voice profile '${activeProfile?.name}' does not have permission to access applications."
                    }
                    repository.saveMessage(ChatMessage(text = failReply, sender = "assistant"))
                    speakText(failReply)
                    _isProcessing.value = false
                    return@launch
                }
            }

            val activeRules = dbRules.filter { it.isActive }
            val offlineHandled = intentRouter.routeIntent(queryToProcess, activeRules, lang)

            if (!offlineHandled) {
                // Forward unknown commands to Gemini Core (which can generate action JSON payloads)
                val response = repository.consultAI(queryToProcess, lang)

                val trimmedResponse = response.trim()
                if (trimmedResponse.startsWith("{") && trimmedResponse.endsWith("}")) {
                    Log.i("AssistantViewModel", "Detected structured action payload JSON. Routing to Native Android Execution Layer...")
                    val outcome = nativeExecutionLayer.executeFromJson(trimmedResponse)

                    val speakableConfirmation = if (outcome.success) {
                        when (lang) {
                            "ar", "dz" -> "تم تفعيل الإجراء بنجاح: ${outcome.message}"
                            "fr" -> "Action lancée avec succès: ${outcome.message}"
                            else -> "Action executed successfully: ${outcome.message}"
                        }
                    } else {
                        when (lang) {
                            "ar", "dz" -> "فشل تشغيل الإجراء: ${outcome.message}"
                            "fr" -> "Échec de l'action native: ${outcome.message}"
                            else -> "Action execution failed: ${outcome.message}"
                        }
                    }

                    repository.saveMessage(ChatMessage(text = "[Native Execution Layer]: $speakableConfirmation", sender = "assistant"))
                    speakText(speakableConfirmation)
                } else {
                    // Save standard AI response to history
                    repository.saveMessage(ChatMessage(text = response, sender = "assistant"))

                    // Speak reply
                    speakText(response)

                    // Fallback to legacy local keyword matched triggers
                    evaluateLocalSystemActions(queryToProcess)
                }
            } else {
                Log.d("AssistantViewModel", "Command executed locally offline! bypassed Gemini.")
            }

            _isProcessing.value = false
        }
    }

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * Executing deep system automations and standard packages
     */
    private fun evaluateLocalSystemActions(command: String) {
        val query = command.lowercase(Locale.getDefault())
        val context = getApplication<Application>()

        // 1. Silent alert Finder / Find my phone voice command
        val wakeWord = systemSettings.value?.remoteWakeKeyword?.lowercase(Locale.getDefault()) ?: "siri finder"
        if (query.contains(wakeWord) || query.contains("أين أنت") || query.contains("وين كاين تليفون") || query.contains("où est mon téléphone") || query.contains("find my phone")) {
            triggerFindMyPhoneSiren()
            return
        }

        // 2. Open Camera
        if (query.contains("camera") || query.contains("كاميرا") || query.contains("appareil photo")) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Secondary check for standard camera packages
                openAppByPackage("com.android.camera")
            }
            return
        }

        // 3. Open WhatsApp
        if (query.contains("whatsapp") || query.contains("واتساب")) {
            openAppByPackage("com.whatsapp")
            return
        }

        // 4. Call Contacts dialer
        if (query.contains("call") || query.contains("اتصل") || query.contains("appelle") || query.contains("تلفون")) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.parse("tel:")
            }
            context.startActivity(intent)
            return
        }

        // 5. Open Web browser search engine
        if (query.contains("search") || query.contains("ابحث") || query.contains("recherche") || query.contains("google")) {
            val searchTerm = query.replace("search", "")
                .replace("ابحث", "")
                .replace("recherche", "")
                .trim()
            try {
                val encoded = URLEncoder.encode(searchTerm, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: UnsupportedEncodingException) {
                Log.e("SmartVoiceVM", "URL Encoder exception during web search.", e)
            }
            return
        }
    }

    private fun openAppByPackage(packageName: String) {
        val context = getApplication<Application>()
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                // Open play store as fallback
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(storeIntent)
            }
        } catch (e: Exception) {
            Log.e("SmartVoiceVM", "Failed to launch package: $packageName", e)
        }
    }

    /**
     * Triggers max volume buzzer, speaks alert and blinks LED flashlight
     */
    fun triggerFindMyPhoneSiren() {
        _findMyPhoneActive.value = true
        val context = getApplication<Application>()

        viewModelScope.launch(Dispatchers.IO) {
            val settings = systemSettings.value ?: SystemSettings()

            // 1. Force maximum volume on Music Stream
            if (settings.sirenOnRemote) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

                // 2. Play Siren sound
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ringtone = RingtoneManager.getRingtone(context, alarmUri)
                ringtone?.play()
            }

            // Speak: "I am here."/"أنا هنا!"/"Je suis là!"
            val speechResponse = when (settings.preferredLanguage) {
                "ar", "dz" -> "أنا هنا! الهاتف في هذا المكان."
                "fr" -> "Je suis ici ! Votre téléphone est localisé."
                else -> "I am here! The device is located here."
            }
            speakText(speechResponse)

            // 3. Blink Flashlight
            if (settings.flashlightBlinkOnRemote) {
                flashlightJob = true
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                try {
                    val cameraId = cameraManager.cameraIdList.firstOrNull()
                    if (cameraId != null) {
                        var state = true
                        for (i in 0 until 15) {
                            if (!flashlightJob) break
                            cameraManager.setTorchMode(cameraId, state)
                            state = !state
                            delay(400)
                        }
                        cameraManager.setTorchMode(cameraId, false)
                    }
                } catch (e: Exception) {
                    Log.e("SmartVoiceVM", "Flashlight camera control failed/missing hardware", e)
                }
            }
        }
    }

    /**
     * Cancels the Find My Phone ringtone alert and flashlight activity
     */
    fun dismissFindMyPhoneSiren() {
        _findMyPhoneActive.value = false
        flashlightJob = false
        ringtone?.stop()
        
        // Turn off flashlight
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull()
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, false)
                }
            } catch (e: Exception) {
                Log.e("SmartVoiceVM", "Dismiss Flashlight failure", e)
            }
        }
    }

    // Settings adjustments
    fun setPreferredLanguage(langCode: String) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(preferredLanguage = langCode)
            repository.updateSystemSettings(updated)
            
            // Sync TTS Locale
            val locale = when (langCode) {
                "ar", "dz" -> Locale("ar")
                "fr" -> Locale.FRENCH
                else -> Locale.ENGLISH
            }
            textToSpeech?.language = locale
        }
    }

    fun updatePinSettings(pin: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(pinCode = pin, pinEnabled = enabled)
            repository.updateSystemSettings(updated)
        }
    }

    fun updateBiometricSettings(enabled: Boolean) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(biometricEnabled = enabled)
            repository.updateSystemSettings(updated)
        }
    }

    fun updateRemoteWakeWord(keyword: String) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(remoteWakeKeyword = keyword)
            repository.updateSystemSettings(updated)
        }
    }

    fun updateFlashlightBlinkOnRemote(enabled: Boolean) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(flashlightBlinkOnRemote = enabled)
            repository.updateSystemSettings(updated)
        }
    }

    fun updateSirenOnRemote(enabled: Boolean) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(sirenOnRemote = enabled)
            repository.updateSystemSettings(updated)
        }
    }

    fun updateBackgroundVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(backgroundVoiceEnabled = enabled)
            repository.updateSystemSettings(updated)
        }
    }

    fun updateListeningMode(mode: String) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(listeningMode = mode)
            repository.updateSystemSettings(updated)
        }
    }

    fun updateAdaptiveListeningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = systemSettings.value ?: SystemSettings()
            val updated = current.copy(adaptiveListeningEnabled = enabled)
            repository.updateSystemSettings(updated)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    // Remote simulated control commands
    fun simulateLockDevice() {
        viewModelScope.launch {
            val currentStatus = deviceStatus.value
            if (currentStatus != null) {
                repository.updateDeviceStatus(currentStatus.copy(isLocked = true, lastUpdated = System.currentTimeMillis()))
            }
        }
    }

    fun simulateUnlockDevice() {
        viewModelScope.launch {
            val currentStatus = deviceStatus.value
            if (currentStatus != null) {
                repository.updateDeviceStatus(currentStatus.copy(isLocked = false, lastUpdated = System.currentTimeMillis()))
            }
        }
    }

    // Telemetry Sync simulation details
    private fun monitorBatteryAndConnectivity() {
        viewModelScope.launch {
            while (true) {
                val currentStatus = deviceStatus.value
                if (currentStatus != null) {
                    // Update battery dynamically with slight fluctuations
                    var newBattery = currentStatus.batteryPercentage - 1
                    if (newBattery < 5) newBattery = 100
                    
                    repository.updateDeviceStatus(
                        currentStatus.copy(
                            batteryPercentage = newBattery,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }
                delay(30000) // Poll updates moderately
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager?.destroy()
        textToSpeech?.shutdown()
        ringtone?.stop()
    }
}
