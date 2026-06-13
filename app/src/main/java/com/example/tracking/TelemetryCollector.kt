package com.example.tracking

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager

class TelemetryCollector(private val context: Context) {

    fun collectTelemetryDiagnostics(): Map<String, String> {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val connectionType = getNetworkTypeLabel()

        return mapOf(
            "battery" to "$level%",
            "network" to connectionType,
            "os_version" to android.os.Build.VERSION.RELEASE,
            "hardware" to android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL,
            "security_patch" to android.os.Build.VERSION.SECURITY_PATCH
        )
    }

    private fun getNetworkTypeLabel(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return "Disconnected"
        val actCw = cm.getNetworkCapabilities(nw) ?: return "Disconnected"
        return when {
            actCw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi Hook"
            actCw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular LTE/5G (Algerie Telecom)"
            actCw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet Cable"
            else -> "Active Carrier Data"
        }
    }
}
