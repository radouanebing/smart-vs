package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.SystemSettings
import com.example.data.model.VoiceProfile
import com.example.data.model.WakeWord
import com.example.data.model.VoiceLearningLog
import com.example.voice.EnrollmentState
import com.example.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@Composable
fun VoiceAssistantScreen(
    viewModel: AssistantViewModel,
    settings: SystemSettings,
    onNavigateToTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening by viewModel.isListening.collectAsState()
    val partialText by viewModel.partialCommandText.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val chatMessages by viewModel.chatHistory.collectAsState()
    val automationRules by viewModel.automationRules.collectAsState()
    val voiceAmplitude by viewModel.voiceAmplitude.collectAsState()
    
    // Voice Identity States
    val voiceProfiles by viewModel.allVoiceProfiles.collectAsState()
    val wakeWords by viewModel.allWakeWords.collectAsState()
    val learningLogs by viewModel.voiceLearningLogs.collectAsState()
    val simulatedSpeakerId by viewModel.simulatedSpeakerId.collectAsState()
    val enrollmentState by viewModel.enrollmentState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showInputTextRow by remember { mutableStateOf(false) }
    var textInputState by remember { mutableStateOf("") }
    var showAutomationBuilderDialog by remember { mutableStateOf(false) }

    // Tab Navigation State
    var selectedTab by remember { mutableStateOf(0) } // 0 = Assistant Chat, 1 = Voice Identity, 2 = Learning Logs

    // RTL check
    val isRtl = settings.preferredLanguage == "ar" || settings.preferredLanguage == "dz"

    // Theme color constants (Immersive UI)
    val backgroundBg = Color(0xFF0F1117)
    val cardBg = Color(0xFF161922)
    val accentBranded = Color(0xFF6366F1)
    val indicatorActive = Color(0xFF10B981)

    // Glowing sphere ripple animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )

    // Bind voice micro amplitude dynamically for real-time pulsing feel
    val finalScale1 = if (isListening) scale1 * (0.8f + voiceAmplitude * 0.3f) else scale1
    val finalScale2 = if (isListening) scale2 * (0.8f + voiceAmplitude * 0.3f) else scale2

    // Automatically navigate scroll history down
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBg)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding() // Keep safe area above navigation bar
    ) {
        // App Identity Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentBranded.copy(alpha = 0.2f))
                        .border(1.dp, accentBranded.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone Brand",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isRtl) "المساعد الذكي NHN" else "NHN Voice Control",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "LOCAL SHIELD ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = indicatorActive,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            // Quick Actions: Direct mode selector and keypad popup toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showInputTextRow = !showInputTextRow },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = if (showInputTextRow) Icons.Default.Mic else Icons.Default.Keyboard,
                        contentDescription = "Switch Input Type",
                        tint = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentBranded.copy(alpha = 0.15f))
                        .clickable { onNavigateToTracking() }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Tracking Portal",
                        tint = Color(0xFFC084FC)
                    )
                }
            }
        }

        LanguageSelectionBar(
            currentLanguage = settings.preferredLanguage,
            onLanguageSelected = { langCode -> viewModel.setPreferredLanguage(langCode) },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Custom Navigation Tab Bar (Material Design 3 custom aesthetic)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = accentBranded
                )
            },
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.08f)) },
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(if (isRtl) "مساعد المحادثة" else "Voice Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_voice_chat")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(if (isRtl) "بصمة الصوت" else "Voice Identity", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_voice_identity")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(if (isRtl) "رؤى اللهجات" else "Dialect Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_dialect_insights")
            )
        }

        // Display panel based on selected tab
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AssistantChatPanel(
                    isRtl = isRtl,
                    isListening = isListening,
                    isProcessing = isProcessing,
                    partialText = partialText,
                    finalScale1 = finalScale1,
                    finalScale2 = finalScale2,
                    voiceAmplitude = voiceAmplitude,
                    showInputTextRow = showInputTextRow,
                    textInputState = textInputState,
                    onTextInputStateChange = { textInputState = it },
                    chatMessages = chatMessages,
                    automationRules = automationRules,
                    listState = listState,
                    onTriggerMic = {
                        if (isListening) viewModel.stopListening() else viewModel.startListening()
                    },
                    onExecuteTextCommand = { txt ->
                        viewModel.executeVoiceCommand(txt)
                    },
                    onOpenAddAutomation = { showAutomationBuilderDialog = true }
                )
                1 -> VoiceIdentityPanel(
                    isRtl = isRtl,
                    voiceProfiles = voiceProfiles,
                    wakeWords = wakeWords,
                    simulatedSpeakerId = simulatedSpeakerId,
                    enrollmentState = enrollmentState,
                    onSelectSpeaker = { id -> viewModel.setSimulatedSpeaker(id) },
                    onDeleteProfile = { profile -> viewModel.deleteVoiceProfile(profile) },
                    onUpdateProfilePermissions = { profile, apps, youtube ->
                        viewModel.updateVoiceProfilePermissions(profile, apps, youtube)
                    },
                    onAddWakeWord = { word ->
                        if (word.isNotBlank()) {
                            viewModel.addWakeWord(word)
                        }
                    },
                    onDeleteWakeWord = { ww -> viewModel.removeWakeWord(ww) },
                    onStartEnrollment = { name, role, lang -> viewModel.startEnrollment(name, role, lang) },
                    onSubmitEnrollmentPhrase = { p -> viewModel.submitEnrollmentSpokenText(p) },
                    onCancelEnrollment = { viewModel.cancelEnrollment() }
                )
                2 -> DialectInsightsPanel(
                    isRtl = isRtl,
                    logs = learningLogs
                )
            }
        }
    }

    if (showAutomationBuilderDialog) {
        var trigger by remember { mutableStateOf("") }
        var appSelected by remember { mutableStateOf("Maps") }
        var volSelected by remember { mutableStateOf(50) }
        var sysActionSelected by remember { mutableStateOf("FLASHLIGHT_ON") }
        var silentModeSelected by remember { mutableStateOf(false) }
        var screenBrightnessPercent by remember { mutableStateOf(50) }

        AlertDialog(
            onDismissRequest = { showAutomationBuilderDialog = false },
            containerColor = Color(0xFF161922),
            title = {
                Text(
                    text = if (isRtl) "إنشاء قاعدة تشغيل ذكية" else "Add Custom Programmable Command",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isRtl) "عند نطق العبارة، سيقوم التطبيق بتشغيل السيناريو تلقائياً وبسرعة فائقة (دون إنترنت):" 
                                else "Define offline triggers to automate sequences bypass Gemini:",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = trigger,
                        onValueChange = { trigger = it },
                        label = { Text("Voice phrase (e.g. 'I am going to work')", color = Color.White.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedBorderColor = accentBranded,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("workflow_trigger_input")
                    )

                    Text("Executed Actions Hierarchy:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    // App selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Launch app:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        listOf("Maps", "Camera", "WhatsApp", "None").forEach { app ->
                            Box(
                                modifier = Modifier
                                    .background(if (appSelected == app) accentBranded else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { appSelected = app }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(app, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // Flashlight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Torch Control:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        listOf("ON", "OFF", "None").forEach { flashOpt ->
                            val valMap = when(flashOpt) {
                                "ON" -> "FLASHLIGHT_ON"
                                "OFF" -> "FLASHLIGHT_OFF"
                                else -> "None"
                            }
                            Box(
                                modifier = Modifier
                                    .background(if (sysActionSelected == valMap) indicatorActive else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { sysActionSelected = valMap }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(flashOpt, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // Silent switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hospital / Silent Mode:", color = Color.White, fontSize = 12.sp)
                            Text("Silent ringer & notifications on match", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        }
                        Switch(
                            checked = silentModeSelected,
                            onCheckedChange = { silentModeSelected = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = indicatorActive)
                        )
                    }

                    // Volume scale
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume level:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        listOf(0, 30, 80).forEach { vol ->
                            Box(
                                modifier = Modifier
                                    .background(if (volSelected == vol) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { volSelected = vol }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(if (vol == 0) "Muted" else "$vol%", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (trigger.isNotBlank()) {
                            viewModel.addAutomationRule(
                                com.example.data.model.AutomationRule(
                                    triggerPhrase = trigger,
                                    appToLaunch = if (appSelected == "None") "" else appSelected,
                                    changeVolumeLevel = volSelected / 100f,
                                    customSystemAction = if (sysActionSelected == "None") "" else sysActionSelected,
                                    silentMode = silentModeSelected,
                                    screenBrightness = screenBrightnessPercent / 100f,
                                    isActive = true
                                )
                            )
                            showAutomationBuilderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBranded),
                    modifier = Modifier.testTag("save_workflow_button")
                ) {
                    Text("Save Programmable Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutomationBuilderDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
fun AssistantChatPanel(
    isRtl: Boolean,
    isListening: Boolean,
    isProcessing: Boolean,
    partialText: String,
    finalScale1: Float,
    finalScale2: Float,
    voiceAmplitude: Float,
    showInputTextRow: Boolean,
    textInputState: String,
    onTextInputStateChange: (String) -> Unit,
    chatMessages: List<ChatMessage>,
    automationRules: List<com.example.data.model.AutomationRule>,
    listState: LazyListState,
    onTriggerMic: () -> Unit,
    onExecuteTextCommand: (String) -> Unit,
    onOpenAddAutomation: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Live Sphere breathing mic or active chats scroll list representation
        Box(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Background ambient aura
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(Color(0xFF6366F1).copy(alpha = 0.05f), CircleShape)
            )

            // Dynamic Sphere Animation
            if (isListening || isProcessing) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(finalScale2)
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.1f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .scale(finalScale1)
                        .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.2f), CircleShape)
                )
            }

            // Holographic center orb
            Box(
                modifier = Modifier
                    .size(75.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isProcessing) listOf(
                                Color(0xFF10B981),
                                Color(0xFF047857)
                            ) else if (isListening) listOf(
                                Color(0xFFEC4899),
                                Color(0xFF6366F1)
                            ) else listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
                        )
                    )
                    .clickable { onTriggerMic() }
                    .border(
                        2.dp,
                        if (isListening) Color(0xFFEC4899) else Color(0xFF818CF8).copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Trigger Speech Input",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Display partial voice transcripts live
        AnimatedVisibility(
            visible = partialText.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFEC4899), CircleShape)
                    )
                    Text(
                        text = partialText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Direct Text input command keyboard alternative
        AnimatedVisibility(visible = showInputTextRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInputState,
                    onValueChange = { onTextInputStateChange(it) },
                    placeholder = { Text(if (isRtl) "اكتب رسالة الأمر هنا..." else "Type secure voice instruction...", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (textInputState.isNotBlank()) {
                            onExecuteTextCommand(textInputState)
                            onTextInputStateChange("")
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF6366F1)),
                    modifier = Modifier.testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send text command",
                        tint = Color.White
                    )
                }
            }
        }

        // Quick Automation section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRtl) "أتمتة وسيناريوهات التشغيل المبرمجة" else "⚡ OFFLINE PROGRAMMABLE COMMANDS",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            IconButton(
                onClick = { onOpenAddAutomation() },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add custom rule",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Horizontal tag-row of active macros
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (automationRules.isEmpty()) {
                item {
                    Text(
                        text = if (isRtl) "اضغط + لبرمجة الأوامر المخصصة" else "No custom workflows. Click + to program.",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 11.sp
                    )
                }
            } else {
                items(automationRules) { rule ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (rule.isActive) Color(0xFF10B981).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable {
                                onExecuteTextCommand(rule.triggerPhrase)
                            }
                            .testTag("automation_rule_chip_${rule.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (rule.isActive) Color(0xFF34D399) else Color.Gray, CircleShape)
                            )
                            Text(
                                text = rule.triggerPhrase,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dialogue log terminal panel
        Text(
            text = if (isRtl) "سجل التفاعلات الذاتية" else "DECISIONS AUDIT TRAIL",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRtl) "لم تقم بأي أوامر بعد.\nسأرد فورا لتلقي توجيهاتك." else "No processed execution logs found.\nTry talking to trigger automations.",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            items(chatMessages) { message ->
                ChatBubble(message = message, isRtl = isRtl)
            }
        }
    }
}

@Composable
fun VoiceIdentityPanel(
    isRtl: Boolean,
    voiceProfiles: List<VoiceProfile>,
    wakeWords: List<WakeWord>,
    simulatedSpeakerId: Int?,
    enrollmentState: EnrollmentState,
    onSelectSpeaker: (Int?) -> Unit,
    onDeleteProfile: (VoiceProfile) -> Unit,
    onUpdateProfilePermissions: (VoiceProfile, Boolean, Boolean) -> Unit,
    onAddWakeWord: (String) -> Unit,
    onDeleteWakeWord: (WakeWord) -> Unit,
    onStartEnrollment: (String, String, String) -> Unit,
    onSubmitEnrollmentPhrase: (String) -> Unit,
    onCancelEnrollment: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ENROLLMENT PROCESS CAPABILITY
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111420)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isRtl) "بوابة تسجيل بصمة صوتية جديدة" else "🎙️ SECURE VOICE BIOMETRIC ENROLLMENT",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    when (enrollmentState) {
                        is EnrollmentState.Idle -> {
                            var newProfileName by remember { mutableStateOf("") }
                            var newProfileRole by remember { mutableStateOf("Family") }
                            var newProfileLang by remember { mutableStateOf("dz") }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Register individual voiceprints to authenticate local system workflows.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                OutlinedTextField(
                                    value = newProfileName,
                                    onValueChange = { newProfileName = it },
                                    label = { Text("User name", color = Color.White.copy(alpha = 0.4f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF6366F1)
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("enroll_username_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Role picker
                                    listOf("Owner", "Family", "Business Staff").forEach { role ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (newProfileRole == role) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                .clickable { newProfileRole = role }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(role, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Language:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    listOf("dz", "ar", "fr", "en").forEach { lang ->
                                        Box(
                                            modifier = Modifier
                                                .background(if (newProfileLang == lang) Color(0xFF10B981) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                .clickable { newProfileLang = lang }
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(lang.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (newProfileName.isNotBlank()) {
                                            onStartEnrollment(newProfileName, newProfileRole, newProfileLang)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    modifier = Modifier.fillMaxWidth().testTag("start_enrollment_button")
                                ) {
                                    Text("Start Step-By-Step Enrollment")
                                }
                            }
                        }
                        is EnrollmentState.PromptingPhrase -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Prompt Phrase: Step ${enrollmentState.step} of 3",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = onCancelEnrollment) {
                                        Text("Cancel", color = Color.Red, fontSize = 11.sp)
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { enrollmentState.progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF6366F1),
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = enrollmentState.phrase,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )
                                }

                                Button(
                                    onClick = { onSubmitEnrollmentPhrase(enrollmentState.phrase) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth().testTag("simulate_speech_button")
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("Simulate Speaking Phrase")
                                    }
                                }
                            }
                        }
                        is EnrollmentState.AnalyzingBiometrics -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFFEC4899))
                                Text("Analyzing spectral voicepitch vectors...", color = Color.White, fontSize = 13.sp)
                                Text("Collected pitch: ${enrollmentState.pitchCollected} Hz", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                        is EnrollmentState.Success -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(28.dp))
                                }
                                Text("BIOMETRIC PROFILE SECURED", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Voice print saved locally for ${enrollmentState.profile.name} (${enrollmentState.profile.role}). Raw vocals discarded safely.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                                
                                Button(
                                    onClick = onCancelEnrollment,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) {
                                    Text("Done", color = Color.White)
                                }
                            }
                        }
                        is EnrollmentState.Error -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Calibration failed! Noise floor too high.", color = Color.Red, fontSize = 13.sp)
                                Button(onClick = onCancelEnrollment) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }
        }

        // BIOMETRIC PROFILE SIMULATOR LIST
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isRtl) "بصمات المحاكاة للنشاط الصوتي" else "👤 REGISTERED VOICEPROFS (SIMULATION SELECTOR)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select a profile to simulate that specific user speaking into the microphone. Test how Owner Mode blocks other profiles from admin commands.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                if (voiceProfiles.isEmpty()) {
                    Text("No profiles enrolled.", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                }

                voiceProfiles.forEach { profile ->
                    val isSimulated = simulatedSpeakerId == profile.id
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isSimulated) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF161922)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                1.dp,
                                if (isSimulated) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(if (profile.role == "Owner") Color(0xFFE0E7FF) else Color(0xFFF3E8FF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (profile.role == "Owner") Icons.Default.Lock else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (profile.role == "Owner") Color(0xFF4338CA) else Color(0xFF7E22CE),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(profile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Box(
                                                modifier = Modifier
                                                    .background(if (profile.role == "Owner") Color(0xFF4338CA).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(profile.role, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                                            }
                                        }
                                        Text("Biometrics: ${profile.biometricPitch}Hz | Lang: ${profile.preferredLanguage.uppercase()}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { onSelectSpeaker(if (isSimulated) null else profile.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isSimulated) Color(0xFF10B981) else Color.White.copy(alpha = 0.08f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(if (isSimulated) "Simulating" else "Simulate Speaking", fontSize = 10.sp, color = Color.White)
                                    }
                                    if (profile.name != "Youcef (Owner)") {
                                        IconButton(
                                            onClick = { onDeleteProfile(profile) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Permissions:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable(enabled = profile.role != "Owner") {
                                        onUpdateProfilePermissions(profile, !profile.hasAppAccess, profile.hasYoutubeAccess)
                                    }
                                ) {
                                    Checkbox(
                                        checked = profile.hasAppAccess,
                                        onCheckedChange = { chk ->
                                            onUpdateProfilePermissions(profile, chk, profile.hasYoutubeAccess)
                                        },
                                        enabled = profile.role != "Owner",
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF6366F1),
                                            uncheckedColor = Color.White.copy(alpha = 0.3f),
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Applications", color = if (profile.hasAppAccess) Color.White else Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable(enabled = profile.role != "Owner") {
                                        onUpdateProfilePermissions(profile, profile.hasAppAccess, !profile.hasYoutubeAccess)
                                    }
                                ) {
                                    Checkbox(
                                        checked = profile.hasYoutubeAccess,
                                        onCheckedChange = { chk ->
                                            onUpdateProfilePermissions(profile, profile.hasAppAccess, chk)
                                        },
                                        enabled = profile.role != "Owner",
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFFEF4444),
                                            uncheckedColor = Color.White.copy(alpha = 0.3f),
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("YouTube", color = if (profile.hasYoutubeAccess) Color.White else Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // WAKE WORD MANAGEMENT CONTROL
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "🗣️ WAKE WORDS MANAGEMENT MANAGER",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enable, customize, or delete multiple active system wake phrases synchronously. Any listed wake word triggers the assistant.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    var newWakeText by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newWakeText,
                            onValueChange = { newWakeText = it },
                            placeholder = { Text("E.g. Arif, Rafiq, Smart Assistant", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        Button(
                            onClick = {
                                if (newWakeText.isNotBlank()) {
                                    onAddWakeWord(newWakeText)
                                    newWakeWakeCompleted()
                                    newWakeText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Add", fontSize = 12.sp)
                        }
                    }

                    // Wake words chips list
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        wakeWords.forEach { ww ->
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                    Text(ww.word, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    if (ww.isCustom) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = Color.Red.copy(alpha = 0.8f),
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onDeleteWakeWord(ww) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // VOICE SECURITY LEVELS MATRIX
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0F14)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🛡️ SYSTEM PROTECTED VOICE SECURITY TARGETS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    SecurityLevelRow(level = "PUBLIC COMMANDS", desc = "Time, Weather, Battery Level, Settings status (Usable by anyone/guest speakers)", color = Color.White.copy(alpha = 0.5f))
                    SecurityLevelRow(level = "AUTHORIZED COMMANDS", desc = "Open Camera/WhatsApp, Make phone calls, Send SMS templates, Custom automation workflows (Requires matched voice profile template)", color = Color(0xFF10B981))
                    SecurityLevelRow(level = "OWNER-ONLY SECURED COMMANDS", desc = "Security configurations, system audits, administrative settings, backup workflows (Restricted completely to verified Device Owner voice)", color = Color(0xFFEF4444))
                }
            }
        }
    }
}

private fun newWakeWakeCompleted() {}

@Composable
fun SecurityLevelRow(level: String, desc: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(level, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Text(desc, color = Color.White.copy(alpha = 0.6f), fontSize = 10.5.sp, lineHeight = 14.sp)
    }
}

@Composable
fun DialectInsightsPanel(
    isRtl: Boolean,
    logs: List<VoiceLearningLog>
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111420)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "📈 SPEECH LEARNING ANALYSIS ENGINE",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Continuously optimizes speaker phonetics, pronounciation parameters, and Algerian dialect alignments (Darja, French mixed inputs) locally to increase matching rates over time.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Text(
            text = "PARSED LOGS & PHONETIC FEEDBACK",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No dialect variations parsed yet.", color = Color.White.copy(alpha = 0.25f), fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\"${log.phrase}\"",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Acc: ${log.accuracyScore}%",
                                        color = Color(0xFF818CF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Action: ${log.matchedCommand}",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = log.dialectVariance,
                                    color = Color(0xFFF59E0B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isRtl: Boolean) {
    val isUser = message.sender == "user"
    
    // Aligns visually
    val alignment = if (isUser) {
        if (isRtl) Alignment.Start else Alignment.End
    } else {
        if (isRtl) Alignment.End else Alignment.Start
    }

    val containerColor = if (isUser) {
        Color(0xFF6366F1).copy(alpha = 0.2f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, if (isRtl) 16.dp else 4.dp, if (isRtl) 4.dp else 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, if (isRtl) 4.dp else 16.dp, if (isRtl) 16.dp else 4.dp)
    }

    val borderColor = if (isUser) Color(0xFF6366F1).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(containerColor, bubbleShape)
                .border(1.dp, borderColor, bubbleShape)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) (if (isRtl) "أنت" else "You") else (if (isRtl) "المساعد" else "Assistant"),
                    color = if (isUser) Color(0xFFA5B4FC) else Color(0xFFC084FC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

@Composable
fun LanguageSelectionBar(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = listOf(
        "en" to "English",
        "fr" to "Français",
        "ar" to "العربية",
        "dz" to "الدارجة"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (currentLanguage == "ar" || currentLanguage == "dz") "لغة التفاعل الصوتي" else "ACTIVE VOICE LANGUAGE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { (code, name) ->
                    val isSelected = currentLanguage == code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) Color(0xFF6366F1).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = 10.dp)
                            .testTag("language_chip_$code"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color(0xFFA5B4FC) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
