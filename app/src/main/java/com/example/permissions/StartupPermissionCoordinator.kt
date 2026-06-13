package com.example.permissions

import android.content.Context
import android.os.Build
import com.example.security.SecurityAudit

data class VerificationChecksReport(
    val androidVersion: String,
    val manufacturer: String,
    val hasManufacturerLock: Boolean,
    val isBatteryExempt: Boolean,
    val isAccessibilityActive: Boolean,
    val isOverlayGranted: Boolean,
    val isNotificationsGranted: Boolean,
    val isDeviceAdminActive: Boolean
)

data class ReadinessScores(
    val voiceAssistantScore: Int,
    val findMyPhoneScore: Int,
    val automationScore: Int,
    val overallScore: Int,
    val healthClassification: String,
    val arabicClassification: String,
    val frenchClassification: String
)

class StartupPermissionCoordinator(private val context: Context) {

    private val capabilityManager = CapabilityManager(context)
    private val compatManager = DeviceCompatibilityManager(context)

    /**
     * Conducts a detailed scan of startup ecosystem criteria.
     */
    fun performEcosystemScan(): VerificationChecksReport {
        val buildVer = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val manufacturer = compatManager.getManufacturerDisplayName()
        val hasLock = compatManager.requiresManufacturerOptimizations()
        val batteryExempt = PermissionChecker.isBatteryOptimizationExempt(context)
        val accessibilityActive = PermissionChecker.isAccessibilityServiceEnabled(context)
        val overlayGranted = PermissionChecker.canDrawOverlays(context)
        val deviceAdmin = PermissionChecker.isDeviceAdminActive(context)

        // Check notify permissions
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        val hasNotifyPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionChecker.isPermissionGranted(context, android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        val notifsEnabled = (notificationManager?.areNotificationsEnabled() == true) && hasNotifyPerm

        return VerificationChecksReport(
            androidVersion = buildVer,
            manufacturer = manufacturer,
            hasManufacturerLock = hasLock,
            isBatteryExempt = batteryExempt,
            isAccessibilityActive = accessibilityActive,
            isOverlayGranted = overlayGranted,
            isNotificationsGranted = notifsEnabled,
            isDeviceAdminActive = deviceAdmin
        )
    }

    /**
     * Computes the dynamic readiness and utility scores for each individual platform system module.
     */
    fun calculateReadinessScores(): ReadinessScores {
        val scan = performEcosystemScan()
        val hasMic = PermissionChecker.isPermissionGranted(context, android.Manifest.permission.RECORD_AUDIO)
        val hasLoc = PermissionChecker.isPermissionGranted(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        
        // Location GPS provider enabled checks
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        val gpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true

        // 1. Voice Assistant Readiness calculation
        var vaScore = 0
        if (hasMic) vaScore += 40
        if (scan.isBatteryExempt) vaScore += 20
        if (scan.isNotificationsGranted) vaScore += 20
        if (scan.isOverlayGranted) vaScore += 10
        // Background launch / Auto start setting simulator (assume 10% defaults, reducing if is custom manufacturer but setting remains)
        if (!scan.hasManufacturerLock) {
            vaScore += 10
        } else {
            // Check if user completed auto start
            val prefs = context.getSharedPreferences("Permission_Prefs", Context.MODE_PRIVATE)
            val alreadyGuided = prefs.getBoolean("guided_manufacturer_autostart", false)
            if (alreadyGuided) vaScore += 10 else vaScore += 4 // partial default
        }

        // 2. Find My Phone Readiness calculation
        var fmpScore = 0
        if (hasLoc) fmpScore += 40
        if (gpsEnabled) fmpScore += 20
        if (scan.isDeviceAdminActive) fmpScore += 30
        if (scan.isNotificationsGranted) fmpScore += 10

        // 3. Automation Readiness calculation
        var autoScore = 0
        if (scan.isAccessibilityActive) autoScore += 40
        if (scan.isBatteryExempt) autoScore += 20
        if (scan.isOverlayGranted) autoScore += 20
        // Exact alarms
        if (PermissionChecker.canScheduleExactAlarms(context)) autoScore += 20

        // Cap scores to 100 max, 0 min
        val finalVa = vaScore.coerceIn(0, 100)
        val finalFmp = fmpScore.coerceIn(0, 100)
        val finalAuto = autoScore.coerceIn(0, 100)

        // Overall readiness
        val overall = ((finalVa + finalFmp + finalAuto) / 3)

        val (lvl, arLvl, frLvl) = when {
            overall >= 90 -> Triple("EXCELLENT", "ممتاز جداً", "EXCELLENT")
            overall >= 70 -> Triple("FUNCTIONAL", "مستوى تشغيلي كافٍ", "FONCTIONNEL")
            overall >= 50 -> Triple("LIMITED", "قدرات محدودة", "LIMITÉ")
            else -> Triple("CRITICAL", "مستوى حرج جداً", "CRITIQUE")
        }

        return ReadinessScores(
            voiceAssistantScore = finalVa,
            findMyPhoneScore = finalFmp,
            automationScore = finalAuto,
            overallScore = overall,
            healthClassification = lvl,
            arabicClassification = arLvl,
            frenchClassification = frLvl
        )
    }
}
