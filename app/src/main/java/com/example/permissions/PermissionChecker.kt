package com.example.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat

object PermissionChecker {

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedService = ComponentName(context, com.example.service.SmartAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedService) {
                return true
            }
        }
        return false
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    fun isBatteryOptimizationExempt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    fun canDrawOverlays(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context)
        }
        return true
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    fun checkPermissionStatus(context: Context, item: PermissionItem): Boolean {
        return if (item.isSpecial) {
            when (item.id) {
                "accessibility" -> isAccessibilityServiceEnabled(context)
                "device_admin" -> isDeviceAdminActive(context)
                "battery_opt" -> isBatteryOptimizationExempt(context)
                "overlay" -> canDrawOverlays(context)
                "exact_alarm" -> canScheduleExactAlarms(context)
                else -> false
            }
        } else {
            if (item.systemPermissions.isEmpty()) true
            else item.systemPermissions.all { isPermissionGranted(context, it) }
        }
    }
}

data class PermissionItem(
    val id: String,
    val name: String,
    val systemPermissions: List<String> = emptyList(),
    val description: String,
    val isSpecial: Boolean = false,
    val category: String, // "Hardware", "Database", "Security"
    val arabicName: String,
    val arabicDescription: String,
    val frenchName: String,
    val frenchDescription: String,
    val isGranted: Boolean = false
)
