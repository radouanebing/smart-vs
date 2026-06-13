package com.example.permissions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.security.SecurityAudit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SecurityHealthState(
    val percentage: Int,
    val description: String,
    val accessibilityGone: Boolean,
    val batteryRestricted: Boolean,
    val notificationBlocked: Boolean,
    val deviceAdminRevoked: Boolean,
    val logs: List<String> = emptyList()
)

class SecurityHealthMonitor private constructor(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("Security_Health_Prefs", Context.MODE_PRIVATE)

    private val _healthState = MutableStateFlow(SecurityHealthState(100, "Secure", false, false, false, false))
    val healthState: StateFlow<SecurityHealthState> = _healthState.asStateFlow()

    private val _monitoringLogs = MutableStateFlow<List<String>>(emptyList())
    val monitoringLogs: StateFlow<List<String>> = _monitoringLogs.asStateFlow()

    init {
        performHealthAudit()
    }

    /**
     * Runs an administrative security inspection checking active system hooks and permissions.
     * Logs discrepancies in SecurityAudit and fires explicit notifications if critical privileges are lost.
     */
    fun performHealthAudit(): SecurityHealthState {
        Log.d("SecurityHealthMonitor", "Executing continuous security monitoring cycle...")

        val permissionManager = PermissionManager(context)
        val checkerList = permissionManager.refreshPermissions()
        
        // 1. Check previous active states to detect active revocations
        val prevAccessibility = sharedPrefs.getBoolean("prev_accessibility", false)
        val prevDeviceAdmin = sharedPrefs.getBoolean("prev_device_admin", false)
        val prevBatteryOpt = sharedPrefs.getBoolean("prev_battery_opt", false)
        val prevNotifications = sharedPrefs.getBoolean("prev_notifications", false)

        val currentAccessibility = PermissionChecker.isAccessibilityServiceEnabled(context)
        val currentDeviceAdmin = PermissionChecker.isDeviceAdminActive(context)
        val currentBatteryOpt = PermissionChecker.isBatteryOptimizationExempt(context)

        // Check notify permissions
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val hasNotifyPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionChecker.isPermissionGranted(context, android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        val currentNotifications = (notificationManager?.areNotificationsEnabled() == true) && hasNotifyPerm

        val revList = mutableListOf<String>()

        // Analyze Accessibility Service revocation
        if (prevAccessibility && !currentAccessibility) {
            val msg = "CRITICAL BREACH: Accessibility Service was disabled by user or OS!"
            logLocalAndAudit("accessibility_loss", msg, "CRITICAL")
            triggerSystemAlert("Accessibility Disabled", "Bypassing screens capability is now inactive. Tap to re-enable.")
            revList.add("Accessibility Service")
        }

        // Analyze Device Admin revocation
        if (prevDeviceAdmin && !currentDeviceAdmin) {
            val msg = "CRITICAL BREACH: Device Administrator access was revoked!"
            logLocalAndAudit("device_admin_loss", msg, "CRITICAL")
            triggerSystemAlert("Device Admin Revoked", "Anti-theft locks and security protections are disabled. Tap to re-grant.")
            revList.add("Device Administrator")
        }

        // Analyze Battery optimization exemption loss
        if (prevBatteryOpt && !currentBatteryOpt) {
            val msg = "WARNING: Battery optimization exclusion revoked. App may be paused in background."
            logLocalAndAudit("battery_opt_loss", msg, "WARNING")
            revList.add("Battery Optimization Exception")
        }

        // Analyze Notification permission loss
        if (prevNotifications && !currentNotifications) {
            val msg = "WARNING: Notification permission revoked. Background assistant is invisible."
            logLocalAndAudit("notifications_loss", msg, "WARNING")
            revList.add("System Notifications")
        }

        // Save current states as past references for subsequent checks
        sharedPrefs.edit().apply {
            putBoolean("prev_accessibility", currentAccessibility)
            putBoolean("prev_device_admin", currentDeviceAdmin)
            putBoolean("prev_battery_opt", currentBatteryOpt)
            putBoolean("prev_notifications", currentNotifications)
            apply()
        }

        // Calculate dynamic health percentage
        var score = 100
        if (!currentAccessibility) score -= 30
        if (!currentDeviceAdmin) score -= 30
        if (!currentBatteryOpt) score -= 20
        if (!currentNotifications) score -= 20
        val finalScore = score.coerceIn(0, 100)

        val desc = when {
            finalScore == 100 -> "Ecosystem completely protected and secured."
            finalScore >= 70 -> "System operational but sub-optimal."
            finalScore >= 50 -> "Severe security restrictions active."
            else -> "Critical system compromise!"
        }

        val updatedState = SecurityHealthState(
            percentage = finalScore,
            description = desc,
            accessibilityGone = !currentAccessibility,
            batteryRestricted = !currentBatteryOpt,
            notificationBlocked = !currentNotifications,
            deviceAdminRevoked = !currentDeviceAdmin,
            logs = _monitoringLogs.value
        )

        _healthState.value = updatedState
        return updatedState
    }

    private fun logLocalAndAudit(id: String, msg: String, severity: String) {
        val currentList = _monitoringLogs.value.toMutableList()
        currentList.add(0, "[${severity}] $msg")
        if (currentList.size > 20) currentList.removeAt(currentList.size - 1)
        _monitoringLogs.value = currentList

        SecurityAudit.logEvent("HEALTH_AUDIT_${id.uppercase()}", msg, severity)
    }

    private fun triggerSystemAlert(title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "security_health_channel",
                "Security Health Monitor Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "permissions_dashboard")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 8812, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "security_health_channel")
            .setContentTitle("🛡️ NHN Shield: $title")
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        manager.notify(8812, builder.build())
    }

    companion object {
        @Volatile
        private var INSTANCE: SecurityHealthMonitor? = null

        fun getInstance(context: Context): SecurityHealthMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityHealthMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
