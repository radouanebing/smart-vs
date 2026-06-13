package com.example.permissions

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.security.SecurityAudit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("Permission_Prefs", Context.MODE_PRIVATE)

    private val _permissionsList = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionsList: StateFlow<List<PermissionItem>> = _permissionsList.asStateFlow()

    private val _previouslyGrantedIds = HashSet<String>()

    init {
        // Load initially known granted state IDs to detect posterior revocations
        val savedGranted = sharedPrefs.getStringSet("granted_ids", emptySet()) ?: emptySet()
        _previouslyGrantedIds.addAll(savedGranted)
        refreshPermissions()
    }

    fun isFirstLaunch(): Boolean {
        return sharedPrefs.getBoolean("first_launch_onboarding", true)
    }

    fun completeOnboarding() {
        sharedPrefs.edit().putBoolean("first_launch_onboarding", false).apply()
        SecurityAudit.logEvent("PERMISSION_SETUP", "Complete guides user onboarding wizard successfully.", "INFO")
    }

    fun resetOnboarding() {
        sharedPrefs.edit().putBoolean("first_launch_onboarding", true).apply()
    }

    fun refreshPermissions(): List<PermissionItem> {
        val list = getDefinitions().map { item ->
            val granted = PermissionChecker.checkPermissionStatus(context, item)
            item.copy(isGranted = granted)
        }
        _permissionsList.value = list

        // Check for permission revocation
        val currentGrantedIds = list.filter { it.isGranted }.map { it.id }.toSet()
        _previouslyGrantedIds.forEach { oldId ->
            if (!currentGrantedIds.contains(oldId)) {
                // Revoked!
                val revokedItem = list.find { it.id == oldId }
                if (revokedItem != null) {
                    val alertMsg = "CRITICAL: Permission for ${revokedItem.name} was REVOKED!"
                    Log.e("PermissionManager", alertMsg)
                    SecurityAudit.logEvent("PERMISSION_REVOKED", "The system permission '${revokedItem.id}' was manually revoked by the user.", "WARNING")
                    triggerRevocationNotification(revokedItem)
                }
            }
        }

        // Save current granted states for next comparisons
        sharedPrefs.edit().putStringSet("granted_ids", currentGrantedIds).apply()
        _previouslyGrantedIds.clear()
        _previouslyGrantedIds.addAll(currentGrantedIds)

        return list
    }

    private fun triggerRevocationNotification(item: PermissionItem) {
        // Notification logic for permission revoked alert
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "permission_revocation_channel",
                "Permission Revocation Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "permissions_dashboard")
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 9991, launchIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, "permission_revocation_channel")
            .setContentTitle("Permission Restored Threat")
            .setContentText("Warning: ${item.name} was revoked! Tap to re-grant immediately.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(7776, notification)
    }

    fun getPermissionHealthStatus(): String {
        val list = _permissionsList.value
        if (list.isEmpty()) return "N/A"
        val grantedCount = list.count { it.isGranted }
        val percentage = (grantedCount.toFloat() / list.size.toFloat() * 100).toInt()
        return when {
            percentage == 100 -> "EXCELLENT"
            percentage >= 70 -> "FUNCTIONAL"
            else -> "DEGRADED"
        }
    }

    fun getPermissionHealthPercentage(): Int {
        val list = _permissionsList.value
        if (list.isEmpty()) return 0
        val grantedCount = list.count { it.isGranted }
        return (grantedCount.toFloat() / list.size.toFloat() * 100).toInt()
    }

    fun getIntentForSpecialPermission(id: String): Intent? {
        return when (id) {
            "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            "device_admin" -> {
                val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
                Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Smart assistant administrators required to manage the lock states.")
                }
            }
            "battery_opt" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else null
            }
            "overlay" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else null
            }
            "exact_alarm" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else null
            }
            else -> null
        }
    }

    private fun getDefinitions(): List<PermissionItem> {
        val pList = ArrayList<PermissionItem>()

        pList.add(PermissionItem(
            id = "microphone",
            name = "Microphone Access",
            systemPermissions = listOf(Manifest.permission.RECORD_AUDIO),
            description = "Allows offline wake word listening and vocal assistant voice-to-text processing.",
            category = "Hardware",
            arabicName = "صلاحية الميكروفون",
            arabicDescription = "يسمح بالاستماع للكلمات المفتاحية في الخلفية ومعالجة وتسجيل الأوامر الصوتية بالكامل.",
            frenchName = "Accès Microphone",
            frenchDescription = "Permet l'écoute continue des mots clés en arrière-plan et la reconnaissance vocale locale.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "location",
            name = "Geolocalisation (Gps)",
            systemPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            description = "Crucial to capture localization context details to forward during active SOS panic alerts.",
            category = "Hardware",
            arabicName = "تحديد الموقع الجغرافي",
            arabicDescription = "ضروري لتحديد موقع الجهاز بدقة وإرساله فوراً في حالة تفعيل ناقوس الطوارئ SOS.",
            frenchName = "Position Géographique",
            frenchDescription = "D'une importance cruciale pour localiser l'appareil et transmettre la position GPS lors des SOS.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "notifications",
            name = "System Notifications",
            systemPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else emptyList(),
            description = "Required to keep our persistent listening foreground services running smoothly in background.",
            category = "System",
            arabicName = "إشعارات النظام",
            arabicDescription = "مطلوب لإظهار وبقاء خدمة المساعد الصوتي والتعرف في الخلفية نشطة دائماً.",
            frenchName = "Notifications Système",
            frenchDescription = "Requis pour maintenir le service vocal persistant actif en arrière-plan sans interruption.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "camera",
            name = "Hardware Camera",
            systemPermissions = listOf(Manifest.permission.CAMERA),
            description = "Required to control the LED light flash during emergency alarm triggers or dark environments.",
            category = "Hardware",
            arabicName = "كاميرا الهاتف والوميض",
            arabicDescription = "مطلوب للتحكم في فلاش وميض الكاميرا وإيقاظ الضوء عند تفعيل وضع الطوارئ والخرائط.",
            frenchName = "Appareil Photo / Flash",
            frenchDescription = "Nécessaire pour faire clignoter le flash LED lors du déclenchement du mode urgence SOS.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "contacts",
            name = "Contacts Directory",
            systemPermissions = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS),
            description = "Find people locally to easily trigger voice-dial connections.",
            category = "Database",
            arabicName = "سجل الأسماء وجهات الاتصال",
            arabicDescription = "يسمح للمساعد بالبحث عن الأسماء محلياً وإجراء مكالمات صوتية مباشرة عبر التخاطب.",
            frenchName = "Répertoire des Contacts",
            frenchDescription = "Requis pour rechercher un contact localement et lancer des appels vocaux rapides.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "sms",
            name = "SMS Messages Handling",
            systemPermissions = listOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
            description = "Send automated alert text notifications to pre-defined rescue contact list.",
            category = "Database",
            arabicName = "إرسال واستقبال رسائل SMS",
            arabicDescription = "تسمح بإرسال رسائل استغاثة محددة تلقائياً ورصد الأوامر القادمة من الأرقام الموثوقة.",
            frenchName = "Gestion des SMS",
            frenchDescription = "Permet d'envoyer des SMS d'alerte automatisés à vos contacts d'urgence enregistrés.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "phone_calls",
            name = "Direct Phone Calls",
            systemPermissions = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE),
            description = "Dial direct system numbers immediately when request triggers vocally.",
            category = "Hardware",
            arabicName = "إجراء المكالمات الهاتفية",
            arabicDescription = "تمكين المساعد من الاتصال المباشر بأرقام النجدة أو العائلة بمجرد نطق أمر الاتصال.",
            frenchName = "Appels Téléphoniques",
            frenchDescription = "Permet de composer et de lancer directement un appel vers vos numéros favoris ou d'urgence.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "bluetooth",
            name = "Bluetooth Connectivity",
            systemPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            } else {
                listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
            },
            description = "Communicates with nearby commercial telemetry BLE smart locks or corporate modules.",
            category = "Hardware",
            arabicName = "اتصال البلوتوث",
            arabicDescription = "يسمح للمساعد بالاقتران مع بوابات الأمان الذكية والموديلات المحيطية لبيئة العمل.",
            frenchName = "Connexion Bluetooth",
            frenchDescription = "Permet de se connecter aux balises de proximité et aux modules IoT professionnels.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "storage",
            name = "External Storage Read",
            systemPermissions = listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            description = "Export workflow analytics summaries, voice logs, or database local backups safely.",
            category = "Database",
            arabicName = "ذاكرة التخزين للملفات",
            arabicDescription = "يتيح قراءة وحفظ تقارير تحليلات المهام وتفضيلات المساعد والنسخ الاحتياطية محلياً.",
            frenchName = "Stockage Externe",
            frenchDescription = "Nécessaire pour lire et écrire les fichiers de configuration, voix et logs de l'application.",
            isSpecial = false
        ))

        pList.add(PermissionItem(
            id = "accessibility",
            name = "Accessibility Service",
            systemPermissions = emptyList(),
            description = "Enables simulating click events on Android lock screen to bypass keyguards on voice wake-up.",
            category = "System",
            arabicName = "خدمة إمكانية الوصول للتعليمات",
            arabicDescription = "تسمح بمحاكاة نقرات فتح شاشة الهاتف تلقائياً عند تفعيل المساعد عن بُعد بصوتك.",
            frenchName = "Service d'Accessibilité",
            frenchDescription = "Permet de simuler des gestes de déverrouillage de l'écran lors du réveil vocal guidé.",
            isSpecial = true
        ))

        pList.add(PermissionItem(
            id = "device_admin",
            name = "Device Administrator",
            systemPermissions = emptyList(),
            description = "Grants the application administrative privileges to instantly secure/lock device on dynamic demand.",
            category = "Security",
            arabicName = "مسؤول إدارة أمان الجهاز",
            arabicDescription = "تمنح المساعد القدرة على إغلاق وحماية شاشة الهاتف فوراً في سيناريوهات الأمان الحرجة.",
            frenchName = "Administrateur de l'appareil",
            frenchDescription = "Donne le pouvoir de verrouiller instantanément l'écran pour des raisons de sécurité.",
            isSpecial = true
        ))

        pList.add(PermissionItem(
            id = "battery_opt",
            name = "Exempt Battery Limits",
            systemPermissions = emptyList(),
            description = "Exempt app from aggressive Android Doze battery optimizations, ensuring steady loop processes.",
            category = "Security",
            arabicName = "تخطي قيود موفر البطارية",
            arabicDescription = "يمنع تجميد المساعد في وضع الخمول لضمان التعرف المستمر للكلمات المفتاحية على مدار الساعة.",
            frenchName = "Optimisation de Batterie",
            frenchDescription = "Exempte l'application des restrictions d'économie d'énergie pour garantir l'écoute permanente.",
            isSpecial = true
        ))

        pList.add(PermissionItem(
            id = "overlay",
            name = "System Overlay Permission",
            systemPermissions = emptyList(),
            description = "Allows the voice visual assist modules or siren pop-ups to display over other active applications.",
            category = "System",
            arabicName = "الظهور فوق التطبيقات الأخرى",
            arabicDescription = "يسمح للمساعد برسم واجهات الاستماع وحلقات الإنذار فوق أي شاشة أو نشاط نشط بالهاتف.",
            frenchName = "Superposition d'écran",
            frenchDescription = "Permet d'afficher la fenêtre flottante d'assistant vocal par-dessus les autres applications.",
            isSpecial = true
        ))

        pList.add(PermissionItem(
            id = "exact_alarm",
            name = "Exact Alarm Scheduling",
            systemPermissions = emptyList(),
            description = "Required to schedule micro millisecond precision system diagnostics audits precisely without sleeps.",
            category = "Security",
            arabicName = "جدولة التنبيهات الدقيقة",
            arabicDescription = "مطلوب لتشغيل مهام روتينية دقيقة للغاية لفحص أداء الموظفين وصحة الهاتف بانتظام.",
            frenchName = "Planification d'Alarmes Précises",
            frenchDescription = "Requis pour planifier des audits système précis à la milliseconde près.",
            isSpecial = true
        ))

        return pList
    }
}
