package com.example.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.security.SecurityAudit

@Composable
fun getPermissionIcon(id: String) = when (id) {
    "microphone" -> Icons.Default.Mic
    "location" -> Icons.Default.Place
    "notifications" -> Icons.Default.Notifications
    "camera" -> Icons.Default.CameraAlt
    "contacts" -> Icons.Default.Contacts
    "sms" -> Icons.Default.Sms
    "phone_calls" -> Icons.Default.Phone
    "bluetooth" -> Icons.Default.Bluetooth
    "storage" -> Icons.Default.Folder
    "accessibility" -> Icons.Default.AccessibilityNew
    "device_admin" -> Icons.Default.AdminPanelSettings
    "battery_opt" -> Icons.Default.BatteryAlert
    "overlay" -> Icons.Default.SettingsSystemDaydream
    "exact_alarm" -> Icons.Default.Alarm
    else -> Icons.Default.Shield
}

@Composable
fun PermissionWizardScreen(
    manager: PermissionManager,
    onWizardComplete: () -> Unit,
    initialLanguage: String = "en"
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(initialLanguage) }
    var permissionsList by remember { mutableStateOf(manager.refreshPermissions()) }
    
    val pendingPermissions = permissionsList.filter { !it.isGranted }
    val isRtl = currentLanguage == "ar" || currentLanguage == "dz"

    // Single multi-permission launcher for runtime items
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val grantedCount = results.values.count { it }
        Toast.makeText(context, "Granted $grantedCount permissions", Toast.LENGTH_SHORT).show()
        permissionsList = manager.refreshPermissions()
    }

    // Refresh permissions when screen starts or regains focus
    LaunchedEffect(Unit) {
        permissionsList = manager.refreshPermissions()
    }

    if (pendingPermissions.isEmpty()) {
        val titleText = if (isRtl) "تهانينا! تم منح جميع الصلاحيات" else "All Permissions Configured!"
        val actionText = if (isRtl) "الدخول إلى التطبيق" else "PROCEED TO APPLICATION"
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1117))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Text(
                    text = titleText,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isRtl) "لقد تم تهيئة نظام الأمان وإمكانية الاستماع في الخلفية بشكل متكامل وآمن." else "You have fully authorized the platform to listen for voice wake phrases and process local automations.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Button(
                    onClick = {
                        manager.completeOnboarding()
                        onWizardComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("onboarding_complete_btn")
                ) {
                    Text(actionText, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    } else {
        val currentStepItem = pendingPermissions.first()
        val totalCount = permissionsList.size
        val grantedCount = permissionsList.count { it.isGranted }
        val currentStepNum = grantedCount + 1
        val progressVal = grantedCount.toFloat() / totalCount.toFloat()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1117))
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isRtl) "معالج إعداد الصلاحيات" else "Guided Security Setup",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRtl) "الخطوة $currentStepNum من $totalCount" else "Permission Step $currentStepNum of $totalCount",
                            color = Color(0xFFF59E0B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Simple Language Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("en" to "EN", "ar" to "AR", "fr" to "FR").forEach { (code, label) ->
                            val isSel = currentLanguage == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFF6366F1) else Color.Transparent)
                                    .clickable { currentLanguage = code }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Global Progress Gauge
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isRtl) "مستوى تهيئة النظام" else "System Optimization Level",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${(progressVal * 100).toInt()}%",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressVal)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF6366F1), Color(0xFF10B981))
                                    )
                                )
                        )
                    }
                }
            }

            // Gilded Permission Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Glowing Icon
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getPermissionIcon(currentStepItem.id),
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Category Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentStepItem.category.uppercase(),
                                color = Color(0xFFF59E0B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Localized Text details
                        val title = when (currentLanguage) {
                            "ar" -> currentStepItem.arabicName
                            "fr" -> currentStepItem.frenchName
                            else -> currentStepItem.name
                        }
                        val desc = when (currentLanguage) {
                            "ar" -> currentStepItem.arabicDescription
                            "fr" -> currentStepItem.frenchDescription
                            else -> currentStepItem.description
                        }

                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = desc,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Unified Guided Action Button
                        Button(
                            onClick = {
                                if (currentStepItem.isSpecial) {
                                    val intent = manager.getIntentForSpecialPermission(currentStepItem.id)
                                    if (intent != null) {
                                        context.startActivity(intent)
                                        SecurityAudit.logEvent("PERMISSION_UPGRADE", "Launch special system configuration page: ${currentStepItem.id}", "INFO")
                                    } else {
                                        Toast.makeText(context, "System intent configuration mismatch", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    launcher.launch(currentStepItem.systemPermissions.toTypedArray())
                                    SecurityAudit.logEvent("PERMISSION_UPGRADE", "Launch native runtime permissions dialog for: ${currentStepItem.id}", "INFO")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("guided_action_btn_${currentStepItem.id}")
                        ) {
                            Icon(imageVector = Icons.Default.OfflineBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRtl) "تفعيل وإعطاء الصلاحية ⚡" else "AUTHORIZE PERMISSION ⚡",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Helper / Skip option
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            permissionsList = manager.refreshPermissions()
                        }
                    ) {
                        Text(if (isRtl) "تحديث الحالة 🔄" else "RE-CHECK STATUS 🔄", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            manager.completeOnboarding()
                            onWizardComplete()
                        }
                    ) {
                        Text(if (isRtl) "تخطي الكل (دخول مع خيارات جزئية) ➔" else "Skip Onboarding & Access ➔", color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDashboardScreen(
    manager: PermissionManager,
    preferredLanguage: String = "en",
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coordinator = remember { StartupPermissionCoordinator(context) }
    val capabilityManager = remember { CapabilityManager(context) }
    val compatManager = remember { DeviceCompatibilityManager(context) }
    val healthMonitor = remember { SecurityHealthMonitor.getInstance(context) }

    var permissionsList by remember { mutableStateOf(manager.refreshPermissions()) }
    var selectedFilter by remember { mutableStateOf("all") } // all, granted, missing
    var isAuditing by remember { mutableStateOf(false) }
    var auditReportMsg by remember { mutableStateOf("") }
    
    // Read and listen to security logs & states dynamically
    val healthState by healthMonitor.healthState.collectAsState()
    val safetyLogs by healthMonitor.monitoringLogs.collectAsState()

    var activeTab by remember { mutableStateOf("readiness") } // readiness, capabilities, manufacturer, raw_permissions, logs
    var currentLanguage by remember { mutableStateOf(preferredLanguage) }
    val isRtl = currentLanguage == "ar" || currentLanguage == "dz"

    val readiness = coordinator.calculateReadinessScores()
    val ecoScan = coordinator.performEcosystemScan()
    val capabilities = capabilityManager.evaluateCapabilities()

    val totalCount = permissionsList.size
    val grantedCount = permissionsList.count { it.isGranted }
    val missingCount = totalCount - grantedCount
    val percentage = manager.getPermissionHealthPercentage()
    val healthMetricText = manager.getPermissionHealthStatus()

    val filteredList = when (selectedFilter) {
        "granted" -> permissionsList.filter { it.isGranted }
        "missing" -> permissionsList.filter { !it.isGranted }
        else -> permissionsList
    }

    val multipleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionsList = manager.refreshPermissions()
        healthMonitor.performHealthAudit()
    }

    LaunchedEffect(Unit) {
        permissionsList = manager.refreshPermissions()
        healthMonitor.performHealthAudit()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // Toolbar / Back Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                    Column {
                        Text(
                            text = if (isRtl) "لوحة الصلاحيات والأمان" else "Enterprise Shield Gate",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "NHN PLATFORM ORCHESTRATION SHIELD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6366F1),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Inline Instant Language Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("en" to "EN", "ar" to "AR", "fr" to "FR").forEach { (code, label) ->
                        val isSel = currentLanguage == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Color(0xFF6366F1) else Color.Transparent)
                                .clickable { currentLanguage = code }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Global System Overview & Health Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Dynamic Arc Gauge Display
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF1E1E2F), Color(0xFF111122))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${readiness.overallScore}%",
                                    color = if (readiness.overallScore >= 90) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isRtl) "الصحة" else "SCORE",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (readiness.healthClassification) {
                                                "EXCELLENT" -> Color(0xFF10B981)
                                                "FUNCTIONAL" -> Color(0xFFF59E0B)
                                                else -> Color(0xFFEF4444)
                                            }
                                        )
                                )
                                val classText = when (currentLanguage) {
                                    "ar" -> readiness.arabicClassification
                                    "fr" -> readiness.frenchClassification
                                    else -> readiness.healthClassification
                                }
                                Text(
                                    text = "${if (isRtl) "الحالة الإجمالية:" else "SYSTEM:"} $classText",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isRtl) 
                                    "تم رصد $grantedCount من $totalCount صلاحيات موافق عليها. البيئة الإجمالية آمنة ومدرعة."
                                    else "Configured $grantedCount of $totalCount approvals. System daemon threads are running.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Divider Line
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Environment Details Subrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (isRtl) "إصدار الأندرويد" else "Android Level", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(ecoScan.androidVersion, color = Color.White, fontSize = 11.sp)
                        }
                        Column {
                            Text(if (isRtl) "الشركة المصنعة" else "Manufacturer", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            val restrictionsNote = if (ecoScan.hasManufacturerLock) {
                                if (isRtl) " (قيود وحماية)" else " (Has Limits)"
                            } else ""
                            Text(ecoScan.manufacturer + restrictionsNote, color = Color(0xFF6366F1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sub-tabs Segmented Row Picker
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "readiness" to (if (isRtl) "مؤشرات الجاهزية" else "Module Readiness"),
                    "capabilities" to (if (isRtl) "القدرات النشطة" else "Capabilities"),
                    "manufacturer" to (if (isRtl) "ضبط المصنع" else "Manufacturer Fix"),
                    "raw_permissions" to (if (isRtl) "الصلاحيات الخام" else "Raw Permits"),
                    "logs" to (if (isRtl) "سجل الرصد والأمان" else "Security Monitor")
                ).forEach { (tabId, label) ->
                    val isSelected = activeTab == tabId
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clickable { activeTab = tabId }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF818CF8) else Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Active dynamic auditing indicator
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        isAuditing = true
                        val ok = PermissionRecovery.performEmergencyScan(context, manager)
                        healthMonitor.performHealthAudit()
                        isAuditing = false
                        auditReportMsg = if (ok) {
                            if (isRtl) "أحسنت! لم يتم العثور على أي هجمات أو إلغاء صلاحيات مريب." else "Audit Shield Clear: NHN Service channels and files verified."
                        } else {
                            if (isRtl) "إنذار: تم كشف انتهاك أو إلغاء غير مصرح به لصلاحية حساسة!" else "Auditor Alert: One or more crucial features have been compromised!"
                        }
                        permissionsList = manager.refreshPermissions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("custom_audit_btn")
                ) {
                    if (isAuditing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRtl) "تحديث وفحص الأمان" else "Audit Shield Security", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (missingCount > 0) {
                    val nextItem = permissionsList.find { !it.isGranted }
                    if (nextItem != null) {
                        Button(
                            onClick = {
                                if (nextItem.isSpecial) {
                                    val intent = manager.getIntentForSpecialPermission(nextItem.id)
                                    if (intent != null) context.startActivity(intent)
                                } else {
                                    multipleRequestLauncher.launch(nextItem.systemPermissions.toTypedArray())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("one_click_setup_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val nLabel = if (isRtl) nextItem.arabicName else nextItem.name
                            Text(
                                text = "${if (isRtl) "منح" else "Grant"} ${nLabel.take(10)}...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Auditing report logger display
        if (auditReportMsg.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (auditReportMsg.contains("Success") || auditReportMsg.contains("أحسنت") || auditReportMsg.contains("Clear")) Color(0xFF10B981).copy(alpha = 0.08f)
                            else Color(0xFFEF4444).copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (auditReportMsg.contains("Success") || auditReportMsg.contains("أحسنت") || auditReportMsg.contains("Clear")) Color(0xFF10B981).copy(alpha = 0.3f)
                            else Color(0xFFEF4444).copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (auditReportMsg.contains("Success") || auditReportMsg.contains("أحسنت") || auditReportMsg.contains("Clear")) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Report",
                            tint = if (auditReportMsg.contains("Success") || auditReportMsg.contains("أحسنت") || auditReportMsg.contains("Clear")) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(auditReportMsg, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { auditReportMsg = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Dynamic Tab Content Switcher
        when (activeTab) {
            "readiness" -> {
                // Readiness Score Gauges
                item {
                    Text(
                        text = if (isRtl) "مؤشرات الجاهزية والفعالية للموديولات" else "SYSTEM ACTIVE UTILITY STATUS STATUS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Voice Assistant Readiness Row Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (isRtl) "جاهزية المساعد الصوتي" else "Voice Assistant Capability",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                readiness.voiceAssistantScore >= 90 -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                readiness.voiceAssistantScore >= 70 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${readiness.voiceAssistantScore}%",
                                        color = when {
                                            readiness.voiceAssistantScore >= 90 -> Color(0xFF10B981)
                                            readiness.voiceAssistantScore >= 70 -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (isRtl) 
                                    "تتطلب: ميكروفون، موفر بطاقة معفي، إشعارات نظام، تشغيل تلقائي."
                                    else "Requires: Microphone, Battery optimization exempt, Foreground service type, Notifications.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            // Linear indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(readiness.voiceAssistantScore / 100f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1))
                                )
                            }
                        }
                    }
                }

                // Find My Phone Readiness Row Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (isRtl) "جاهزية العثور على الهاتف" else "Find My Phone GPS Locator",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                readiness.findMyPhoneScore >= 90 -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                readiness.findMyPhoneScore >= 70 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${readiness.findMyPhoneScore}%",
                                        color = when {
                                            readiness.findMyPhoneScore >= 90 -> Color(0xFF10B981)
                                            readiness.findMyPhoneScore >= 70 -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (isRtl) 
                                    "تتطلب: صلاحية الموقع، مستقبل GPS، ترخيص مسؤول الهاتف، إشعارات."
                                    else "Requires: Detailed GPS Location, GPS status enabled, Device Administrator locks.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            // Linear indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(readiness.findMyPhoneScore / 100f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }
                    }
                }

                // Automation Readiness Row Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (isRtl) "جاهزية محرّك الأتمتة" else "Enterprise Automation Engine",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                readiness.automationScore >= 90 -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                readiness.automationScore >= 70 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${readiness.automationScore}%",
                                        color = when {
                                            readiness.automationScore >= 90 -> Color(0xFF10B981)
                                            readiness.automationScore >= 70 -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (isRtl) 
                                    "تتطلب: خدمة التخاطب بالوصول، تخطي حماية الأوان، الظهور الفوقي، تنبيهات دقيقة."
                                    else "Requires: Accessibility system hook, System draws overlays, Exact Alarm logs.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            // Linear indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(readiness.automationScore / 100f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B))
                                )
                            }
                        }
                    }
                }
            }

            "capabilities" -> {
                // Capabilities List (Evaluates real-time device configurations)
                item {
                    Text(
                        text = if (isRtl) "قدرات الجهاز والنشاط العملي" else "ACTIVE HARDWARE & SYSTEM CAPABILITIES",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                items(capabilities) { cap ->
                    val capTitle = when (currentLanguage) {
                        "ar" -> cap.arabicName
                        "fr" -> cap.frenchName
                        else -> cap.name
                    }
                    val capStatusStr = when (currentLanguage) {
                        "ar" -> cap.arabicStatusText
                        "fr" -> cap.frenchStatusText
                        else -> cap.statusText
                    }
                    val recStr = when (currentLanguage) {
                        "ar" -> cap.arabicRecommendations
                        "fr" -> cap.frenchRecommendations
                        else -> cap.recommendations
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        border = BorderStroke(
                            1.dp,
                            when (cap.status) {
                                CapabilityStatus.READY -> Color(0xFF10B981).copy(alpha = 0.15f)
                                CapabilityStatus.DEGRADED -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                CapabilityStatus.NOT_READY -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (cap.status) {
                                                    CapabilityStatus.READY -> Color(0xFF10B981).copy(alpha = 0.1f)
                                                    CapabilityStatus.DEGRADED -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                                                    CapabilityStatus.NOT_READY -> Color(0xFFEF4444).copy(alpha = 0.1f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getPermissionIcon(cap.id),
                                            contentDescription = null,
                                            tint = when (cap.status) {
                                                CapabilityStatus.READY -> Color(0xFF10B981)
                                                CapabilityStatus.DEGRADED -> Color(0xFFF59E0B)
                                                CapabilityStatus.NOT_READY -> Color(0xFFEF4444)
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Text(
                                        text = capTitle,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (cap.status) {
                                                CapabilityStatus.READY -> Color(0xFF10B981).copy(alpha = 0.12f)
                                                CapabilityStatus.DEGRADED -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                                CapabilityStatus.NOT_READY -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = capStatusStr,
                                        color = when (cap.status) {
                                            CapabilityStatus.READY -> Color(0xFF10B981)
                                            CapabilityStatus.DEGRADED -> Color(0xFFF59E0B)
                                            CapabilityStatus.NOT_READY -> Color(0xFFEF4444)
                                        },
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (cap.status != CapabilityStatus.READY) {
                                Text(
                                    text = "${if (isRtl) "توصية:" else "Recommendation:"} $recStr",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                Button(
                                    onClick = {
                                        val intent = manager.getIntentForSpecialPermission(cap.id)
                                        if (intent != null) {
                                            context.startActivity(intent)
                                        } else {
                                            val pItem = permissionsList.find { it.id == cap.id }
                                            if (pItem != null) {
                                                multipleRequestLauncher.launch(pItem.systemPermissions.toTypedArray())
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(if (isRtl) "إصلاح وتخصيص" else "Resolve Issue", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            "manufacturer" -> {
                // Manufacturer Specific Diagnostics & Custom Settings Launchers
                item {
                    Text(
                        text = if (isRtl) "تحسينات الشركة المصنعة للهاتف" else "MANUFACTURER OPTIMIZATION DIAGNOSTICS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                val mSettings = compatManager.getManufacturerSettings()
                if (mSettings.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isRtl) 
                                    "لا توجد إعدادات مخصّصة مطلوبة للهواتف العامة القياسية." 
                                    else "Clean Environment: No manufacturer specific restrictions detected for this brand.",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(mSettings) { setting ->
                        val setLabel = when (currentLanguage) {
                            "ar" -> setting.arabicName
                            "fr" -> setting.frenchName
                            else -> setting.name
                        }
                        val setDesc = when (currentLanguage) {
                            "ar" -> setting.arabicDescription
                            "fr" -> setting.frenchDescription
                            else -> setting.description
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = setLabel,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = setDesc,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                Button(
                                    onClick = {
                                        val success = compatManager.launchManufacturerSetting(setting)
                                        // Save that we guided the user
                                        context.getSharedPreferences("Permission_Prefs", Context.MODE_PRIVATE)
                                            .edit()
                                            .putBoolean("guided_manufacturer_autostart", true)
                                            .apply()
                                        
                                        if (!success) {
                                            Toast.makeText(context, "Guided fallback launched instead.", Toast.LENGTH_SHORT).show()
                                        }
                                        permissionsList = manager.refreshPermissions()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isRtl) "انتقال وضبط" else "PROCEED TO SETTING", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            "raw_permissions" -> {
                // Fallback traditional Raw Permissions filter & list control
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "all" to (if (isRtl) "الكل" else "Show All"),
                            "granted" to (if (isRtl) "الممنوحة" else "Granted"),
                            "missing" to (if (isRtl) "المفقودة" else "Missing")
                        ).forEach { (filterId, label) ->
                            val isSel = selectedFilter == filterId
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) Color(0xFF6366F1).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF6366F1) else Color.White.copy(alpha = 0.04f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFilter = filterId }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color(0xFF818CF8) else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                items(filteredList) { item ->
                    var isExpanded by remember { mutableStateOf(false) }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (item.isGranted) Color.White.copy(alpha = 0.04f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (item.isGranted) Color(0xFF10B981).copy(alpha = 0.08f)
                                            else Color(0xFFEF4444).copy(alpha = 0.08f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getPermissionIcon(item.id),
                                        contentDescription = null,
                                        tint = if (item.isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isRtl) item.arabicName else item.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = item.category.uppercase(),
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (item.isGranted) Color(0xFF10B981).copy(alpha = 0.12f)
                                            else Color(0xFFEF4444).copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (item.isGranted) {
                                            if (isRtl) "ممنوحة" else "GRANTED"
                                        } else {
                                            if (isRtl) "مطلوبة" else "REQUIRED"
                                        },
                                        color = if (item.isGranted) Color(0xFF10B981) else Color(0xFFEF4444),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Expandable Description & Action Button
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, start = 48.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isRtl) item.arabicDescription else item.description,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )

                                    if (!item.isGranted) {
                                        Button(
                                            onClick = {
                                                if (item.isSpecial) {
                                                    val intent = manager.getIntentForSpecialPermission(item.id)
                                                    if (intent != null) context.startActivity(intent)
                                                } else {
                                                    multipleRequestLauncher.launch(item.systemPermissions.toTypedArray())
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .testTag("action_grant_${item.id}")
                                        ) {
                                            Text(if (isRtl) "منح الصلاحية الآن" else "AUTHORIZE NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // Already granted support - reset link to app settings if they want to toggle
                                        TextButton(
                                            onClick = {
                                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = android.net.Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text(if (isRtl) "إدارة في إعدادات النظام ➔" else "Manage in App Info ➔", color = Color(0xFF60A5FA), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "logs" -> {
                // Real-time security logs display
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRtl) "أحداث وسجلات المراقبة الحية" else "LIVE DAEMON BREACH LOGS",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        TextButton(
                            onClick = {
                                healthState // read state to force recompose
                                healthMonitor.performHealthAudit()
                            }
                        ) {
                            Text(if (isRtl) "مسح ورصد 🔄" else "Flush Sync 🔄", color = Color(0xFF6366F1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (safetyLogs.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isRtl) 
                                    "لم يتم تسجيل أي خروقات أمنية أو أحداث غير مصرح بها." 
                                    else "Clean state. No privilege revocations or breaches logged by security daemon.",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(safetyLogs) { logLine ->
                        val isCritical = logLine.contains("CRITICAL") || logLine.contains("BREACH")
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                            border = BorderStroke(
                                1.dp,
                                if (isCritical) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.10f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isCritical) Color(0xFFEF4444) else Color(0xFFF59E0B))
                                )
                                Text(
                                    text = logLine,
                                    color = if (isCritical) Color(0xFFFCA5A5) else Color(0xFFFDE047),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
