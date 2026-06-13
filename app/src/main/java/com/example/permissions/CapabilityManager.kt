package com.example.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.AudioManager
import android.os.Build
import com.example.security.SecurityAudit

enum class CapabilityStatus {
    READY,
    DEGRADED,
    NOT_READY
}

data class DeviceCapability(
    val id: String,
    val name: String,
    val status: CapabilityStatus,
    val statusText: String,
    val arabicName: String,
    val arabicStatusText: String,
    val frenchName: String,
    val frenchStatusText: String,
    val recommendations: String,
    val arabicRecommendations: String,
    val frenchRecommendations: String
)

class CapabilityManager(private val context: Context) {

    /**
     * Evaluates actual device capabilities by correlating permissions check with hardware/system services.
     */
    fun evaluateCapabilities(): List<DeviceCapability> {
        val list = mutableListOf<DeviceCapability>()

        // 1. Microphone Capability
        val hasMicHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        val micPermission = PermissionChecker.isPermissionGranted(context, android.Manifest.permission.RECORD_AUDIO)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val micMuted = audioManager?.isMicrophoneMute == true

        val micStatus = when {
            !hasMicHardware -> CapabilityStatus.NOT_READY
            !micPermission -> CapabilityStatus.NOT_READY
            micMuted -> CapabilityStatus.DEGRADED
            else -> CapabilityStatus.READY
        }
        val micStatusText = when (micStatus) {
            CapabilityStatus.READY -> "Fully Operational"
            CapabilityStatus.DEGRADED -> "Microphone Muted/Blocked"
            CapabilityStatus.NOT_READY -> "Microphone Permission Missing or Hardware Absent"
        }
        val micArText = when (micStatus) {
            CapabilityStatus.READY -> "جاهز للعمل تماماً"
            CapabilityStatus.DEGRADED -> "الميكروفون مكتوم أو محجوب"
            CapabilityStatus.NOT_READY -> "صلاحية الميكروفون مفقودة أو الجهاز غير متوفر"
        }
        val micFrText = when (micStatus) {
            CapabilityStatus.READY -> "Entièrement Opérationnel"
            CapabilityStatus.DEGRADED -> "Microphone Muet ou Bloqué"
            CapabilityStatus.NOT_READY -> "Permission Microphone Manquante ou Matériel Absent"
        }
        list.add(DeviceCapability(
            id = "microphone",
            name = "Vocal Listening Engine",
            arabicName = "محرك الاستماع والاستجابة",
            frenchName = "Moteur d'Écoute Vocale",
            status = micStatus,
            statusText = micStatusText,
            arabicStatusText = micArText,
            frenchStatusText = micFrText,
            recommendations = "Unmute microphone and ensure system mic works correctly.",
            arabicRecommendations = "قم بإلغاء كتم الصوت وتأكد من عمل ميكروفون الهاتف.",
            frenchRecommendations = "Désactivez le mode muet et vérifiez le micro."
        ))

        // 2. Location Capability (GPS correlation)
        val locCoarse = PermissionChecker.isPermissionGranted(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        val locFine = PermissionChecker.isPermissionGranted(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

        val locStatus = when {
            !locFine -> CapabilityStatus.NOT_READY
            !gpsEnabled -> CapabilityStatus.DEGRADED
            else -> CapabilityStatus.READY
        }
        val locStatusText = when (locStatus) {
            CapabilityStatus.READY -> "High Precision Active"
            CapabilityStatus.DEGRADED -> "GPS Disabled (Low accuracy cell tower only)"
            CapabilityStatus.NOT_READY -> "Location Accuracy Permission Compromised"
        }
        val locArText = when (locStatus) {
            CapabilityStatus.READY -> "تتبع عالي الدقة نشط"
            CapabilityStatus.DEGRADED -> "نظام تحديد المواقع GPS معطل (دقة منخفضة جداً)"
            CapabilityStatus.NOT_READY -> "صلاحية تحديد الموقع بمستوى الخصوصية معطلة"
        }
        val locFrText = when (locStatus) {
            CapabilityStatus.READY -> "Tracker Haute Précision Actif"
            CapabilityStatus.DEGRADED -> "GPS Désactivé (Localisation approximative uniquement)"
            CapabilityStatus.NOT_READY -> "Permission de Localisation non accordée"
        }
        list.add(DeviceCapability(
            id = "location",
            name = "Emergency SOS Tracking Engine",
            arabicName = "محرك تتبع الاستغاثة الطارئ",
            frenchName = "Localisation SOS d'Urgence",
            status = locStatus,
            statusText = locStatusText,
            arabicStatusText = locArText,
            frenchStatusText = locFrText,
            recommendations = "Turn on GPS localization in standard quick system panels.",
            arabicRecommendations = "تفعيل الموقع الجغرافي GPS من قائمة الفحص السريع بالهاتف.",
            frenchRecommendations = "Activez le récepteur GPS dans vos raccourcis système."
        ))

        // 3. Accessibility Service Capability
        val accEnabled = PermissionChecker.isAccessibilityServiceEnabled(context)
        val accStatus = if (accEnabled) CapabilityStatus.READY else CapabilityStatus.NOT_READY
        val accStatusText = if (accEnabled) "Fully Enabled" else "Service Stopped/Disabled"
        val accArText = if (accEnabled) "مفعل بالكامل" else "الخدمة متوقفة أو معطلة"
        val accFrText = if (accEnabled) "Activé Correctement" else "Service Arrêté ou Désactivé"

        list.add(DeviceCapability(
            id = "accessibility",
            name = "Smart Remote Lock Bypass",
            arabicName = "تخطي شاشة القفل وتأمين الهاتف",
            frenchName = "Contournement Sécurisé de Verrouillage",
            status = accStatus,
            statusText = accStatusText,
            arabicStatusText = accArText,
            frenchStatusText = accFrText,
            recommendations = "Activate the SmartAccessibilityService inside accessibility configurations.",
            arabicRecommendations = "تفعيل خدمة SmartAccessibilityService ضمن حزمة إمكانية الوصول.",
            frenchRecommendations = "Activez SmartAccessibilityService dans vos paramètres système."
        ))

        // 4. Device Admin Active Admin
        val adminActive = PermissionChecker.isDeviceAdminActive(context)
        val adminStatus = if (adminActive) CapabilityStatus.READY else CapabilityStatus.NOT_READY
        val adminStatusText = if (adminActive) "Device Protection Active" else "Privileges Blocked"
        val adminArText = if (adminActive) "حماية الهاتف مفعلة" else "صلاحيات المدير معطلة"
        val adminFrText = if (adminActive) "Protection Active" else "Privilèges bloqués"

        list.add(DeviceCapability(
            id = "device_admin",
            name = "Anti-Theft Lock Controller",
            arabicName = "وحدة قفل الهجمات والسرقات",
            frenchName = "Démon Anti-Vol & Verrouillage",
            status = adminStatus,
            statusText = adminStatusText,
            arabicStatusText = adminArText,
            frenchStatusText = adminFrText,
            recommendations = "Grant active administrator status within system application guard panels.",
            arabicRecommendations = "منح ترخيص مسؤول الجهاز في إدارة حماية أنظمة الهاتف.",
            frenchRecommendations = "Accorder les privilèges d'administration de l'appareil."
        ))

        // 5. Overlay Draws Overlays
        val canOverlay = PermissionChecker.canDrawOverlays(context)
        val overlayStatus = if (canOverlay) CapabilityStatus.READY else CapabilityStatus.NOT_READY
        val overlayStatusText = if (canOverlay) "Allowed (Draw visuals OK)" else "Draw visuals blocked"
        val overlayArText = if (canOverlay) "مسموح (رسم الواجهات فوق الشاشة)" else "رسم الواجهات محجوب حالياً"
        val overlayFrText = if (canOverlay) "Autorisé avec succès" else "Superposition bloquée"

        list.add(DeviceCapability(
            id = "overlay",
            name = "Ambient UI Float Presenter",
            arabicName = "لوحة عرض الواجهات العائمة",
            frenchName = "Fenêtre Flottante Ambiante",
            status = overlayStatus,
            statusText = overlayStatusText,
            arabicStatusText = overlayArText,
            frenchStatusText = overlayFrText,
            recommendations = "Enable Overlay/Draw over other apps inside Android configuration settings.",
            arabicRecommendations = "تمكين ميزة الظهور فوق التطبيقات داخل إعدادات الأندرويد.",
            frenchRecommendations = "Autorisez le dessin par-dessus les autres applications."
        ))

        return list
    }
}
