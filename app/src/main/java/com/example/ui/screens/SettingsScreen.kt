package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SystemSettings
import com.example.viewmodel.AssistantViewModel

@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    settings: SystemSettings,
    modifier: Modifier = Modifier
) {
    val isRtl = settings.preferredLanguage == "ar" || settings.preferredLanguage == "dz"
    val scrollState = rememberScrollState()

    // Key configuration inputs
    var pinTextState by remember { mutableStateOf(settings.pinCode) }
    var wakeWordTextState by remember { mutableStateOf(settings.remoteWakeKeyword) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    // Theme values
    val backgroundBg = Color(0xFF0F1117)
    val cardColor = Color(0xFF161922)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isRtl) "الإعدادات الأمنية والتقنية" else "System Settings",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "SYSTEM DESIGN & PREFERENCES",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF818CF8),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Language Select Section
        Text(
            text = if (isRtl) "اختيار لغة المساعد" else "ASSISTANT LANGUAGE",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Fluent options: EN, AR, DZ, FR side-by-side or stacked nicely
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val languages = listOf(
                "en" to "English (Aesthetic)",
                "fr" to "Français (Algerien style)",
                "ar" to "العربية (Classical Arabic)",
                "dz" to "الدارجة الجزائرية (Algerian Darja)"
            )

            languages.forEach { (code, name) ->
                val isSelected = settings.preferredLanguage == code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF6366F1).copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { viewModel.setPreferredLanguage(code) }
                        .padding(14.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) Color(0xFFA5B4FC) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security / Verification Section
        Text(
            text = if (isRtl) "الأمان والتحقق" else "SECURITY & AUTHENTICATION",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Secret Find Keyword text field
            Column {
                Text(
                    text = if (isRtl) "كلمة المرور الصوتية السرية" else "Secret Voice Wake Command",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = wakeWordTextState,
                        onValueChange = {
                            wakeWordTextState = it
                            viewModel.updateRemoteWakeWord(it)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFF6366F1),
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("wake_word_input")
                    )
                }
            }

            // PIN Code Setting field
            Column {
                Text(
                    text = if (isRtl) "تعديل رمز PIN للأمان" else "Set Security PIN Protection Code",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = pinTextState,
                    onValueChange = {
                        pinTextState = it
                        if (it.length <= 6) {
                            viewModel.updatePinSettings(it, settings.pinEnabled)
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFF6366F1),
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_code_setting")
                )
            }

            // Pin Protection toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isRtl) "تفعيل قفل PIN" else "PIN Lock Protection",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Require PIN overlay during remote ring",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = settings.pinEnabled,
                    onCheckedChange = { viewModel.updatePinSettings(pinTextState, it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6366F1),
                        checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("pin_toggle")
                )
            }

            // Biometric Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isRtl) "تفعيل البصمة لتجاوز القفل" else "Biometrics Integration",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Enable fingerprint biometric checks",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = settings.biometricEnabled,
                    onCheckedChange = { viewModel.updateBiometricSettings(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6366F1),
                        checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("biometric_toggle")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Controls (Sound/Flashlight selectors)
        Text(
            text = if (isRtl) "خيارات تنبيه العثور على الهاتف" else "FIND MY PHONE TRIGGER OPTIONS",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Flashlight option trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isRtl) "وميض الفلاش الضوئي" else "Flashlight Blinking",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Pulse LED torch when triggered",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = settings.flashlightBlinkOnRemote,
                    onCheckedChange = { viewModel.updateFlashlightBlinkOnRemote(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6366F1),
                        checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }

            // Siren option trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isRtl) "تشغيل الصوت بأقصى شدة" else "Loud Siren Player",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Rings even if phone is on silent",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = settings.sirenOnRemote,
                    onCheckedChange = { viewModel.updateSirenOnRemote(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6366F1),
                        checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions Guard Section
        Text(
            text = if (isRtl) "حارس صلاحيات الأمان" else "SECURITY & PERMISSIONS GUARD",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRtl) "لوحة الصلاحيات الأمنية" else "Central Security Coordinator",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isRtl) 
                            "تهيئة ومراقبة صحة الصلاحيات والحماية لـ 14 موديول خاص بالنظام." 
                            else "Initialize, audit, and re-configure active locks for all 14 required engine permissions.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { showPermissionsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isRtl) "إدارة" else "AUDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showPermissionsDialog) {
            Dialog(
                onDismissRequest = { showPermissionsDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundBg)
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val pManager = remember { com.example.permissions.PermissionManager(context) }
                    com.example.permissions.PermissionDashboardScreen(
                        manager = pManager,
                        preferredLanguage = settings.preferredLanguage,
                        onNavigateBack = { showPermissionsDialog = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Background Voice Activation Section
        Text(
            text = if (isRtl) "أوضاع الاستماع وحفظ الطاقة" else "VOICE LISTENING & BATTERY RADAR",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val deviceStatusState by viewModel.deviceStatus.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRtl) "خدمة التنشيط الصوتي" else "Background Service Activation",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isRtl) 
                            "التحكم في تشغيل ميكروفون الخلفية المدمج." 
                            else "Main master switch to enable background voice assistant ears.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = settings.backgroundVoiceEnabled,
                    onCheckedChange = { viewModel.updateBackgroundVoiceEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF10B981),
                        checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("background_voice_toggle")
                )
            }

            if (settings.backgroundVoiceEnabled) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                // Segmented Modes Selection
                Text(
                    text = if (isRtl) "اختر وضع الاستماع النشط" else "SELECT ACTIVE LISTENING MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF818CF8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        Triple("always", if (isRtl) "دائم" else "Always", Icons.Default.Mic),
                        Triple("smart", if (isRtl) "ذكي" else "Smart", Icons.Default.Bolt),
                        Triple("ptt", if (isRtl) "يدوي PTT" else "PTT Mode", Icons.Default.TouchApp)
                    )

                    modes.forEach { (modeCode, label, icon) ->
                        val isSelected = settings.listeningMode == modeCode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF4F46E5).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.02f))
                                .border(
                                    1.dp, 
                                    if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.08f), 
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.updateListeningMode(modeCode) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color(0xFFA5B4FC) else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                if (settings.listeningMode != "ptt") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRtl) "تردد الاستماع التكيفي ذو الخرج المنخفض" else "Adaptive Idle Throttling",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isRtl) 
                                    "تقليل سرعة الميكروفون بعد 3 دقائق من عدم وجود نشاط صوتي لتخفيف الحمل الحراري والبطارية." 
                                    else "Duty-cycles ears to low-frequency check after 3 mins of idle to drop CPU/thermal load.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.adaptiveListeningEnabled,
                            onCheckedChange = { viewModel.updateAdaptiveListeningEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF6366F1),
                                checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                // Privacy & Battery Radar Panel
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1115))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (deviceStatusState?.micActivityActive == true) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRtl) "البث الصوتي الذكي في الوقت الحقيقي" else "Voice Radar Diagnostics",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = deviceStatusState?.listeningStatus ?: (if (isRtl) "غير نشط" else "Inactive"),
                            color = if (deviceStatusState?.micActivityActive == true) Color(0xFF34D399) else Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    (if (deviceStatusState?.micActivityActive == true) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.1f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(vertical = 2.dp, horizontal = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Column of telemetry numbers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Estimated drain per/hr card
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (isRtl) "الاستهلاك التقريبي" else "Est. Drain",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${deviceStatusState?.estimatedHourlyDrain ?: 0.0f}% / hr",
                                color = if ((deviceStatusState?.estimatedHourlyDrain ?: 0.0f) < 0.2f) Color(0xFF34D399) else Color(0xFFFBBF24),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // CPU savings card
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (isRtl) "تخفيف معالج CPU" else "CPU Savings",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "+${deviceStatusState?.cpuSavingsPercent ?: 0}%",
                                color = Color(0xFF818CF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Temp card
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (isRtl) "حرارة البطارية" else "Core Temp",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${deviceStatusState?.deviceTemperature ?: 28.0f}°C",
                                color = when (deviceStatusState?.thermalStatus) {
                                    "Critical" -> Color(0xFFF87171)
                                    "Warm" -> Color(0xFFFBBF24)
                                    else -> Color(0xFF34D399)
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (deviceStatusState?.isBatterySaverActive == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFBBF24).copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRtl) "توفير نطاق البطارية مفعّل (تم تعليق خدمة الاستماع مؤقتاً)" else "Battery Saver Active (Monitoring temporarily suspended)",
                                color = Color(0xFFFBBF24),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset / Data Clear Tool Widget
        Button(
            onClick = { viewModel.clearChat() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(bottom = 24.dp)
                .testTag("clear_history_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFFF87171),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRtl) "مسح سجل الأوامر بالكامل" else "Reset Interaction Logs History",
                color = Color(0xFFF87171),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
