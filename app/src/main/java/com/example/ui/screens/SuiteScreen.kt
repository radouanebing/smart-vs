package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomationRule
import com.example.data.model.SystemSettings
import com.example.security.SecurityAudit
import com.example.viewmodel.AssistantViewModel
import com.example.launcher.NativeExecutionAction
import com.example.launcher.AppMetaDataDetail
import com.example.launcher.ExecutionOutcome
import androidx.compose.foundation.BorderStroke

@Composable
fun SuiteScreen(
    viewModel: AssistantViewModel,
    settings: SystemSettings,
    modifier: Modifier = Modifier
) {
    val isRtl = settings.preferredLanguage == "ar" || settings.preferredLanguage == "dz"
    val currentContextState by viewModel.currentContext.collectAsState()
    val emergencyModeActive by viewModel.emergencyModeActive.collectAsState()
    val automationRules by viewModel.automationRules.collectAsState()

    var activeSubTab by remember { mutableStateOf("analytics") } // analytics, context, health, plugins
    var isOfflineModeEnabled by remember { mutableStateOf(false) }
    var isMultiUserModeEnabled by remember { mutableStateOf(false) }
    var cloudSyncEnabled by remember { mutableStateOf(true) }

    // Optimization triggers
    var isOptimizing by remember { mutableStateOf(false) }
    var optimizationResult by remember { mutableStateOf("") }

    // Theme Colors
    val cardColor = Color(0xFF161922)
    val backgroundBg = Color(0xFF0F1117)
    val indigoAccent = Color(0xFF6366F1)
    val emeraldAccent = Color(0xFF10B981)

    LaunchedEffect(isOptimizing) {
        if (isOptimizing) {
            kotlinx.coroutines.delay(2000)
            isOptimizing = false
            optimizationResult = if (isRtl) "تم تحسين الذاكرة ومسح المؤقت بنجاح (وفرت 240 ميجا)" else "Cleared cached logs & optimized 240MB RAM successfully."
            SecurityAudit.logEvent("SYSTEM_OPTIMIZE", "Recycled local runtime memory indexes", "INFO")
        }
    }

    Scaffold(
        containerColor = backgroundBg,
        modifier = modifier.fillMaxSize()
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
        ) {
            // Enterprise Title Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
                            )
                        )
                        .border(1.dp, Color(0xFF4338CA).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BusinessCenter,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isRtl) "منصة المؤسسة الذكية" else "NHN Workflow Suite",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ALGERIA ENTERPRISE APPLICATION ENGINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick SOS Panel (If emergency mode is active, display high visibility alert card)
            item {
                AnimatedVisibility(
                    visible = emergencyModeActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF7F1D1D))
                            .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (isRtl) "وضع الطوارئ نشط حالياً!" else "EMERGENCY SOS IS ACTIVE",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isRtl) "يتم الآن رنين الهاتف بأعلى قوة وتفعيل الفلاش الوميضي الدائم للمساعدة في العثور على الشخص." 
                                       else "The siren is activated at maximum volume, and LED camera torch is blinking continuously to signal rescue.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = { viewModel.dismissEmergencyMode() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dismiss_emergency_btn")
                            ) {
                                Text(if (isRtl) "تعطيل وضع الطوارئ والإنذار" else "TERMINATE EMERGENCY ALARM", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Segmented Sub-Tab Selection Slider
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "analytics" to (if (isRtl) "الأداء والتحليلات" else "Analytics"),
                        "context" to (if (isRtl) "السياق والتعلم" else "Contexts"),
                        "health" to (if (isRtl) "صحة النظام" else "Diagnostics"),
                        "permissions" to (if (isRtl) "الصلاحيات والأمان" else "Permissions"),
                        "plugins" to (if (isRtl) "الإضافات والمؤسسة" else "Enterprise"),
                        "execution" to (if (isRtl) "التنفيذ الأصلي والمسح" else "Native Radar")
                    ).forEach { (tabId, label) ->
                        val isSelected = activeSubTab == tabId
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) indigoAccent else cardColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable { activeSubTab = tabId }
                                .testTag("suite_subtab_$tabId")
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

             // Active Tab Content Display Block
            item {
                when (activeSubTab) {
                    "analytics" -> AnalyticsTab(isRtl, cardColor, indigoAccent, emeraldAccent)
                    "context" -> ContextsTab(
                        isRtl = isRtl,
                        cardColor = cardColor,
                        indigoAccent = indigoAccent,
                        emeraldAccent = emeraldAccent,
                        currentContext = currentContextState,
                        automationRules = automationRules,
                        onContextChange = { viewModel.updateCurrentContext(it) },
                        onExecuteRule = { viewModel.executeVoiceCommand(it) },
                        onTriggerEmergency = { viewModel.triggerEmergencyMode() }
                    )
                    "health" -> SystemHealthTab(
                        isRtl = isRtl,
                        cardColor = cardColor,
                        emeraldAccent = emeraldAccent,
                        isOptimizing = isOptimizing,
                        optimizationResult = optimizationResult,
                        onTriggerOptimize = {
                            isOptimizing = true
                            optimizationResult = ""
                        }
                    )
                    "permissions" -> {
                        val context = LocalContext.current
                        val pManager = remember { com.example.permissions.PermissionManager(context) }
                        com.example.permissions.PermissionDashboardScreen(
                            manager = pManager,
                            preferredLanguage = settings.preferredLanguage
                        )
                    }
                    "plugins" -> PluginsTab(
                        isRtl = isRtl,
                        cardColor = cardColor,
                        indigoAccent = indigoAccent,
                        emeraldAccent = emeraldAccent,
                        isOfflineMode = isOfflineModeEnabled,
                        isMultiUser = isMultiUserModeEnabled,
                        cloudSync = cloudSyncEnabled,
                        onOfflineToggle = { isOfflineModeEnabled = it },
                        onMultiUserToggle = { isMultiUserModeEnabled = it },
                        onCloudSyncToggle = { cloudSyncEnabled = it }
                    )
                    "execution" -> NativeExecutionTab(
                        viewModel = viewModel,
                        isRtl = isRtl,
                        cardColor = cardColor,
                        indigoAccent = indigoAccent,
                        emeraldAccent = emeraldAccent
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsTab(isRtl: Boolean, cardColor: Color, indigoAccent: Color, emeraldAccent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Voice command stats display
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRtl) "تحليلات الأوامر والتعرف" else "Command Recognition Dashboard",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Voice recognition correctness rate gauge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isRtl) "دقة التعرف على الصوت" else "Voice Accuracy Rate", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("98.4%", color = emeraldAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isRtl) "الأوامر المحلية (بيدون إنترنت)" else "Local Offline Processed", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("72%", color = Color(0xFF60A5FA), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Chart: Most used command categories
                Text(
                    text = if (isRtl) "منحنى الأوامر الأكثر استخداماً" else "MOST FREQUENT COMMAND ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                val categories = listOf(
                    "System Control (Flash/Siren)" to 0.85f,
                    "Enterprise Workflow Queries" to 0.65f,
                    "Application Launchers" to 0.50f,
                    "Cloud DB Synchronization" to 0.35f
                )

                categories.forEach { (catName, ratio) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(catName, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("${(ratio * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(indigoAccent, Color(0xFFA855F7))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Most used Application launch habits
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isRtl) "تطبيقات التشغيل المفضلة" else "COMMERCIAL APP LAUNCH FREQUENCY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppUsageItem("Maps", 24, Color(0xFFEF4444))
                    AppUsageItem("WhatsApp", 19, Color(0xFF10B981))
                    AppUsageItem("Camera", 12, Color(0xFF60A5FA))
                    AppUsageItem("Custom Apps", 8, Color(0xFFF59E0B))
                }
            }
        }
    }
}

@Composable
fun AppUsageItem(appName: String, count: Int, accentColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.1f))
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(appName.take(1), color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Text(appName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text("$count launches", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
    }
}

@Composable
fun ContextsTab(
    isRtl: Boolean,
    cardColor: Color,
    indigoAccent: Color,
    emeraldAccent: Color,
    currentContext: String,
    automationRules: List<AutomationRule>,
    onContextChange: (String) -> Unit,
    onExecuteRule: (String) -> Unit,
    onTriggerEmergency: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Environment Context Simulator Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRtl) "محاكي سياق البيئة التلقائي" else "Automated Environment Context",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isRtl) "يتعلم المساعد من سياقك الحالي ويقدم اختصارات ذكية وتعديلات للنظام فوراً:" 
                           else "The assistant auto adapts settings and suggests workflows optimized for Algerian business cases based on active states:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DRIVING" to (if (isRtl) "القيادة 🚗" else "Driving 🚗"),
                        "WORK" to (if (isRtl) "العمل 💼" else "At Work 💼"),
                        "HOME" to (if (isRtl) "المنزل 🏠" else "At Home 🏠")
                    ).forEach { (code, name) ->
                        val isSelected = currentContext == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF1E3A8A) else Color.White.copy(alpha = 0.03f))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onContextChange(code) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) Color(0xFF93C5FD) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Dynamic Learning Suggestion Box
                val contextSuggestion = when (currentContext) {
                    "DRIVING" -> Triple(
                        if (isRtl) "وضع القيادة مفعل" else "Driving Mode Suggestion",
                        if (isRtl) "تم تفعيل القراءة الصوتية للإشعارات وتثبيت اختصارات خرائط GPS لسهولة تتبع العمال." 
                        else "Activated read notifications aloud and set GPS maps navigation widgets for easy travel.",
                        "I am going to work"
                    )
                    "WORK" -> Triple(
                        if (isRtl) "سياق العمل الذكي" else "At Work Suggestion",
                        if (isRtl) "نقترح فتح تطبيق إدارة Hospital Management أو تفعيل سيناريو Hospital Mode الصامت." 
                        else "Prompting open CRM apps, Hospital management modules or triggering silent Hospital Mode shortcuts.",
                        "Hospital Mode"
                    )
                    "HOME" -> Triple(
                        if (isRtl) "تفضيل سياق المنزل" else "At Home Suggestion",
                        if (isRtl) "اقتراح فتح تطبيقات العائلة، زيادة مستوى رنين المنبه، وضبط فلاش LED على وضع مفعّل." 
                        else "Suggested opening personal modules, restoring sound system ringer, and disabling workplace restrict parameters.",
                        "Good night"
                    )
                    else -> Triple(
                        if (isRtl) "سلوك التعلم التلقائي" else "Behavior Suggestion Engine",
                        if (isRtl) "اكتب أو انطق سلوكياً ليتعلم منها المساعد تلقائياً." 
                        else "Say keywords often to let AI automatically generate persistent smart behavior suggestions.",
                        ""
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(indigoAccent.copy(alpha = 0.06f))
                        .border(1.dp, indigoAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF60A5FA), CircleShape))
                            Text(contextSuggestion.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(contextSuggestion.second, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 15.sp)
                        if (contextSuggestion.third.isNotEmpty()) {
                            TextButton(
                                onClick = { onExecuteRule(contextSuggestion.third) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("⚡ TRIGGER VOICE SHORTCUT: \"${contextSuggestion.third}\"", color = Color(0xFF60A5FA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Emergency Activation Trigger Box
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF7F1D1D).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isRtl) "مفتاح الطوارئ الحرج SOS" else "CRITICAL SOS CODES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = if (isRtl) "يمكنك نطق كلمات الطوارئ ('SOS', 'النجدة') لتفعيل منبه عالي الشدة مع وميض LED دائم وإرسال خطوط الموقع مباشرة" 
                           else "You can say SOS / Emergency vocal phrases locally offline to immediately flash the camera torch & ring alarm.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Button(
                    onClick = { onTriggerEmergency() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth().testTag("trigger_emergency_sos_btn")
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRtl) "تشغيل طوارئ فوري (محاكاة)" else "TRIGGER EMERGENCY SOS NOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SystemHealthTab(
    isRtl: Boolean,
    cardColor: Color,
    emeraldAccent: Color,
    isOptimizing: Boolean,
    optimizationResult: String,
    onTriggerOptimize: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Health Indicators Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRtl) "صحة نظام الجهاز والمصادر" else "Smart Device Health & Storage",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Battery, Storage, Memory progress rows
                HealthMetricBar(
                    label = if (isRtl) "سلامة البطارية" else "Battery Health & Temp",
                    value = "Good (37°C)",
                    percentage = 0.94f,
                    tint = emeraldAccent
                )

                HealthMetricBar(
                    label = if (isRtl) "مساحة التخزين المستخدمة" else "Storage Disk Usage",
                    value = "45.2 GB of 128 GB",
                    percentage = 0.35f,
                    tint = Color(0xFF3B82F6)
                )

                HealthMetricBar(
                    label = if (isRtl) "الذاكرة العشوائية المتاحة" else "RAM Allocations (Memory)",
                    value = "3.1 GB of 6 GB",
                    percentage = 0.51f,
                    tint = Color(0xFFA855F7)
                )

                Divider(color = Color.White.copy(alpha = 0.05f))

                Button(
                    onClick = onTriggerOptimize,
                    enabled = !isOptimizing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                    modifier = Modifier.fillMaxWidth().testTag("optimize_system_btn")
                ) {
                    if (isOptimizing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRtl) "جاري فحص الذاكرة والمؤقت..." else "OPTIMIZING LOCAL CACHE...")
                    } else {
                        Icon(imageVector = Icons.Default.OfflineBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRtl) "تنظيف وتحسين أداء الهاتف" else "OPTIMIZE DEVICE RESOURCES")
                    }
                }

                if (optimizationResult.isNotEmpty()) {
                    Text(
                        text = optimizationResult,
                        color = emeraldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HealthMetricBar(label: String, value: String, percentage: Float, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.04f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(tint)
            )
        }
    }
}

@Composable
fun PluginsTab(
    isRtl: Boolean,
    cardColor: Color,
    indigoAccent: Color,
    emeraldAccent: Color,
    isOfflineMode: Boolean,
    isMultiUser: Boolean,
    cloudSync: Boolean,
    onOfflineToggle: (Boolean) -> Unit,
    onMultiUserToggle: (Boolean) -> Unit,
    onCloudSyncToggle: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Enterprise core configurations
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRtl) "إعدادات الأمان والمؤسسات" else "Enterprise Ready Architecture",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Single/Multi User Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isRtl) "وضع تعدد المستخدمين" else "Multi-User Mode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (isRtl) "دعم ملفات منفصلة ومجالات مخصصة للشركات بالتوازي" else "Run independent isolated user spaces on same device", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Switch(
                        checked = isMultiUser,
                        onCheckedChange = onMultiUserToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = indigoAccent)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Cloud Synchronization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isRtl) "المزامنة السحابية الذكية" else "Cloud Synchronization", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (isRtl) "ربط البيانات المؤمن مع قاعدة البيانات السحابية المركزية" else "Sync telemetry & configurations securely with cloud", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Switch(
                        checked = cloudSync,
                        onCheckedChange = onCloudSyncToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = indigoAccent)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Offline Secure Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isRtl) "الوضع المحلي المعزول" else "Isolated Offline Mode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(if (isRtl) "معالجة محلية كاملة للأوامر دون أي اتصال بالإنترنت" else "Secure local command routing without external connections", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                    Switch(
                        checked = isOfflineMode,
                        onCheckedChange = onOfflineToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = indigoAccent)
                    )
                }
            }
        }

        // Plugin Management Module Registry Showcase
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRtl) "سجل المكونات البرمجية المخصصة" else "Modules & Plugin Registry",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isRtl) "يمكن للمطورين والشركات تطوير موديولات إضافية تتكامل مباشرة مع المساعد الذكي دون تغيير النواة الأساسية:" 
                           else "Extension endpoints allow adding business features dynamically without redeploying core engines:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                val plugins = listOf(
                    PluginConfig("Hospital Management API", "Clinical monitoring endpoints integration", true),
                    PluginConfig("Smart Home (Algeria IoT)", "Control localized security gateways", false),
                    PluginConfig("Inventory Tracking System Log", "Automate workflow scan lines & DB updates", false),
                    PluginConfig("Advanced GPS Fleet command", "Tracks multiple mobile assets", true)
                )

                plugins.forEach { plugin ->
                    var isLoaded by remember { mutableStateOf(plugin.initiallyLoaded) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plugin.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(plugin.description, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLoaded) emeraldAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                .clickable { isLoaded = !isLoaded }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isLoaded) "LOADED" else "UNLOADED",
                                color = if (isLoaded) emeraldAccent else Color.White.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PluginConfig(
    val name: String,
    val description: String,
    val initiallyLoaded: Boolean
)

@Composable
fun NativeExecutionTab(
    viewModel: AssistantViewModel,
    isRtl: Boolean,
    cardColor: Color,
    indigoAccent: Color,
    emeraldAccent: Color
) {
    val context = LocalContext.current
    var jsonPayloadInput by remember { mutableStateOf("{\n  \"actionType\": \"SYSTEM_SETTING\",\n  \"target\": \"wifi\"\n}") }
    var executionLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var scannedAppsList by remember { mutableStateOf<List<AppMetaDataDetail>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }

    fun addLog(msg: String) {
        executionLogs = (listOf("[$] $msg") + executionLogs).take(20)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Concept and Architecture Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isRtl) "البنية الهندسية للتنفيذ الأصلي" else "Android Native Execution Architecture",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRtl) 
                        "تمكين مستويات الفصل الأمنية المعيارية. تقوم محركات الذكاء الاصطناعي (Gemini) بفهم السياق وتحويله إلى كائنات أوامر JSON مبسطة فقط. تنفيذ الأوامر، والوصول إلى ملفات النظام وإطلاق الأنشطة ملقى بالكامل على عاتق طبقة التنفيذ الأصلي (Native Android Execution Layer) بطريقة تمنع الوصول المباشر والعبث بالأجهزة." 
                        else "Defines a critical security boundary. Generative components (AI) only generate actions as standardized JSON payloads. Device APIs, PackageManager searches, activity launches, and intent routing are executed strictly by native, secure compiled Android code.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "AI (Action Generator JSON) ➔ Native Layer (Resolution & Execution)",
                        color = Color(0xFF818CF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Test Interactive Execution Box
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isRtl) "محاكي طبقة الأوامر التنفيذية لـ AI" else "AI Action Execution Simulator",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (isRtl) "يمكنك كتابة حمولة JSON التجريبية لتنفيذها ومراقبة استدعاء النظام الأصلي:" else "Input execution JSON payload below to simulate native layer intercept:",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = jsonPayloadInput,
                    onValueChange = { jsonPayloadInput = it },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = indigoAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("action_payload_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val outcome = viewModel.nativeExecutionLayer.executeFromJson(jsonPayloadInput)
                            addLog("Executed JSON: Type=${outcome.actionType}, Success=${outcome.success}, Message=${outcome.message}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = indigoAccent),
                        modifier = Modifier.weight(1f).testTag("trigger_native_json_btn")
                    ) {
                        Text(if (isRtl) "تشغيل الحمولة" else "Launch Payload", fontSize = 11.sp)
                    }

                    // Reset button
                    OutlinedButton(
                        onClick = {
                            jsonPayloadInput = "{\n  \"actionType\": \"SYSTEM_SETTING\",\n  \"target\": \"display\"\n}"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text(if (isRtl) "إعادة تعيين" else "Reset", fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                // Predefined test actions
                Text(
                    text = if (isRtl) "اختبارات سريعة فورية" else "PRESET TEST INTENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA5B4FC),
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        "Camera" to NativeExecutionAction("LAUNCH_APP", "com.android.camera"),
                        "Settings" to NativeExecutionAction("SYSTEM_SETTING", "display"),
                        "WiFi" to NativeExecutionAction("SYSTEM_SETTING", "wifi"),
                        "Dialer" to NativeExecutionAction("DIAL", "0550123456"),
                        "Maps" to NativeExecutionAction("NAVIGATE", "Alger, Algérie"),
                        "Alarm" to NativeExecutionAction("SET_ALARM", "NHN Wakeup", extraParams = mapOf("hour" to "8", "minutes" to "15")),
                        "Share" to NativeExecutionAction("SHARE", "Greetings from NHN Workflow Suite", "NHN Hello")
                    )

                    items(presets) { (label, act) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val outcome = viewModel.nativeExecutionLayer.executeAction(act)
                                    addLog("Preset Execute: ${act.actionType} on target '${act.target}' -> Success=${outcome.success}")
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Live Logs Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isRtl) "سجل التنفيذ المباشر (أندرويد)" else "Execution Log Radar",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (executionLogs.isEmpty()) {
                        Text(
                            text = if (isRtl) "لا يوجد أي سجلات تشغيل حالية..." else "No current operations registered...",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(executionLogs) { log ->
                                Text(
                                    text = log,
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // PackageManager System Applications Scanner
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isRtl) "فاحص حزم PackageManager" else "PackageManager Diagnostic Scanner",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRtl) "مسح واستخراج حزم تصفح التطبيقات المثبتة في الهاتف." else "Query and catalog client app package structures.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(indigoAccent)
                            .clickable {
                                isScanning = true
                                scannedAppsList = viewModel.nativeExecutionLayer.getInstalledAppsList(onlyUserApps = true)
                                isScanning = false
                                addLog("Scanned phone packages: Extracted ${scannedAppsList.size} applications.")
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isScanning) (if (isRtl) "يجري البحث..." else "SCANNING...") else (if (isRtl) "مسح حزم التطبيقات" else "SCAN PACKAGES"),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (scannedAppsList.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(scannedAppsList) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(app.packageName, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                    }
                                    Text(
                                        text = "V: ${app.versionCode}",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color(0xFF60A5FA).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
