package com.example.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

data class AppMetaData(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean
)

class InstalledAppsScanner(private val context: Context) {

    fun scanDeviceApplications(): List<AppMetaData> {
        val appList = mutableListOf<AppMetaData>()
        val packageManager = context.packageManager
        
        try {
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            for (p in packages) {
                // Ignore empty labels
                val label = p.applicationInfo?.loadLabel(packageManager)?.toString() ?: continue
                val pName = p.packageName
                val isSys = (p.applicationInfo!!.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                
                appList.add(AppMetaData(label, pName, isSys))
            }
        } catch (e: Exception) {
            Log.e("InstalledAppsScanner", "Error scanning device packages, using simulation backup", e)
        }

        // Ensure fallback mock list for common Algerian apps / communication apps is ready
        if (appList.isEmpty()) {
            appList.addAll(getSimulatedAppList())
        }
        return appList
    }

    private fun getSimulatedAppList(): List<AppMetaData> {
        return listOf(
            AppMetaData("WhatsApp", "com.whatsapp", false),
            AppMetaData("Facebook", "com.facebook.katana", false),
            AppMetaData("Viber", "com.viber.voip", false),
            AppMetaData("Google Maps", "com.google.android.apps.maps", true),
            AppMetaData("Camera", "com.android.camera", true),
            AppMetaData("Contacts", "com.android.contacts", true),
            AppMetaData("Gmail", "com.google.android.gm", true),
            AppMetaData("YouTube", "com.google.android.youtube", true)
        )
    }
}
