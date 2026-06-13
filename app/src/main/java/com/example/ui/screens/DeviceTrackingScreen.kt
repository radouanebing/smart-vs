package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceStatus
import com.example.data.model.SystemSettings
import com.example.viewmodel.AssistantViewModel
import com.example.security.SecurityAudit
import com.example.security.SecurityLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceTrackingScreen(
    viewModel: AssistantViewModel,
    settings: SystemSettings,
    modifier: Modifier = Modifier
) {
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val isRtl = settings.preferredLanguage == "ar" || settings.preferredLanguage == "dz"

    // Local simulated alert states
    var showWipeConfirmationDialog by remember { mutableStateOf(false) }
    var syncHeartbeatState by remember { mutableStateOf("CONNECTED") }

    // Periodically fluctuate sync states to mimic real Firestore syncing
    LaunchedEffect(Unit) {
        // Safe simulator loop
        com.example.security.SecurityAudit.logEvent("FIREBASE_SYNC", "Synchronized telemetry stats with secure cloud server successfully", "INFO")
        while (true) {
            kotlinx.coroutines.delay(8000)
            syncHeartbeatState = "SYNCING"
            kotlinx.coroutines.delay(1000)
            syncHeartbeatState = "SYNCED"
            com.example.security.SecurityAudit.logEvent("TELEMETRY_REFRESH", "Refreshed battery and GPS log arrays", "INFO")
        }
    }

    // Theme color constants (Immersive UI)
    val backgroundBg = Color(0xFF0F1117)
    val cardColor = Color(0xFF161922)
    val primaryGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
    )

    val auditLogs = remember { mutableStateListOf<SecurityLogEntry>() }
    // Fetch initial log list
    LaunchedEffect(syncHeartbeatState) {
        auditLogs.clear()
        auditLogs.addAll(SecurityAudit.retrieveAuditLogs())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // Tracker Section Title
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isRtl) "إدارة الأجهزة عن بُعد" else "Remote Command Hub",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "COMMERCIAL CLOUD DEVICE MANAGER",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF10B981),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Radar GPS Tracking representation
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor, RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Radar Ring glow
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = "Locator target",
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isRtl) "موقع الجهاز الحالي" else "Current Device Location",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val latText = deviceStatus?.latitude?.toString() ?: "36.7538"
                    val lonText = deviceStatus?.longitude?.toString() ?: "3.0588"

                    Text(
                        text = "GPS: $latText N, $lonText E (Algiers, DZ)",
                        color = Color(0xFFA5B4FC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Cloud state banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF34D399), CircleShape)
                            )
                            Text(
                                text = "CLOUD: $syncHeartbeatState",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Hardware Diagnostics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Battery
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(cardColor, RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery level info",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "مستوى البطارية" else "Battery State",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${deviceStatus?.batteryPercentage ?: 85}%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Wi-Fi / Connection
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(cardColor, RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Icon(
                            imageVector = Icons.Default.NetworkWifi,
                            contentDescription = "Connection speed info",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRtl) "حالة الشبكة" else "Signal Status",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = deviceStatus?.connectionType ?: "5G LTE DZ",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Remote Action Buttons Title
        item {
            Text(
                text = if (isRtl) "أوامر التحكم الفوري" else "REMOTE SYSTEM CONTROLS",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // RING TELEPHONY
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .clickable { 
                        viewModel.triggerFindMyPhoneSiren()
                        SecurityAudit.logEvent("ALARM_SIMULATION", "Instructed emergency ringing alarm via remote command console", "WARNING")
                    }
                    .testTag("ring_device_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume alert icon",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRtl) "رنين رادع بعلو مفرط" else "Ring Remotely",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRtl) "سيرن بأقصى شدة للتنبيه" else "Force full volume alarm ring on device",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // LOCK SWITCH
        item {
            val isLocked = deviceStatus?.isLocked == true
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .clickable {
                        if (isLocked) {
                            viewModel.simulateUnlockDevice()
                            SecurityAudit.logEvent("REMOTE_LOCK", "Unlocked system lock safely representing remote parameters override", "INFO")
                        } else {
                            viewModel.simulateLockDevice()
                            SecurityAudit.logEvent("REMOTE_LOCK", "Secured lock screen locally with pin restriction", "INFO")
                        }
                    }
                    .testTag("lock_device_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isLocked) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            tint = if (isLocked) Color(0xFF34D399) else Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLocked) "Release Device Lock" else "Remote System Lock",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isLocked) "Device lock is currently active" else "Immediately lock screen via PIN protection",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isLocked,
                        onCheckedChange = {
                            if (isLocked) {
                                viewModel.simulateUnlockDevice()
                                SecurityAudit.logEvent("REMOTE_LOCK", "Unlocked device remotely", "INFO")
                            } else {
                                viewModel.simulateLockDevice()
                                SecurityAudit.logEvent("REMOTE_LOCK", "Locked device with PIN code remotely", "INFO")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }

        // FACTORY DATA RESET / WIPE TRIGGER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .clickable { showWipeConfirmationDialog = true }
                    .testTag("factory_wipe_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Format Disk forever",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRtl) "محو بيانات الجهاز بالكامل" else "Remote Factory Wipe",
                            color = Color(0xFFFCA5A5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRtl) "إعادة تعيين المصنع ومحو جميع الملفات" else "Erase entire profile files and reset storage",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Security Audit Logs
        item {
            Text(
                text = if (isRtl) "سجل التهديدات والعمليات الأمنية" else "SECURITY & AUDIT AUDIT LOGS",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                Text(
                    text = "No recorded security anomalies detected in local audits.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(auditLogs) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val severityColor = when (entry.severity) {
                        "CRITICAL" -> Color(0xFFEF4444)
                        "WARNING" -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(severityColor, CircleShape)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = entry.eventType,
                                color = severityColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
                            Text(
                                text = formattedTime,
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = entry.description,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }

    if (showWipeConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmationDialog = false },
            containerColor = Color(0xFF161922),
            title = {
                Text("Confirm Commercial Remote Wipe?", color = Color(0xFFEF4444), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "WARNING: This action triggers a secure factory reset. All files, cache profiles, and local databases representing Alger businesses will be formats recursively. This simulation is logged in security log outputs.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SecurityAudit.logEvent("REMOTE_WIPE_MOCK", "Factory Data reset wiped successfully from cloud terminal bypass", "CRITICAL")
                        showWipeConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("YES, EXECUTE RESET")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}
