package com.example.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.security.SecurityAudit

data class ManufacturerSetting(
    val id: String,
    val name: String,
    val arabicName: String,
    val frenchName: String,
    val description: String,
    val arabicDescription: String,
    val frenchDescription: String,
    val intentAction: String? = null,
    val componentPkg: String? = null,
    val componentCls: String? = null
)

class DeviceCompatibilityManager(private val context: Context) {

    val manufacturer: String = Build.MANUFACTURER

    fun getManufacturerDisplayName(): String {
        return manufacturer.lowercase().replaceFirstChar { it.uppercase() }
    }

    /**
     * Checks if the current manufacturer requires custom auto-start or background configs.
     */
    fun requiresManufacturerOptimizations(): Boolean {
        val m = manufacturer.lowercase()
        return m.contains("xiaomi") || m.contains("samsung") || m.contains("oppo") ||
               m.contains("realme") || m.contains("huawei") || m.contains("honor")
    }

    /**
     * Retrieves manufacturer-specific settings with safe launches fellback.
     */
    fun getManufacturerSettings(): List<ManufacturerSetting> {
        val list = mutableListOf<ManufacturerSetting>()
        val m = manufacturer.lowercase()

        when {
            m.contains("xiaomi") -> {
                list.add(ManufacturerSetting(
                    id = "auto_start",
                    name = "Auto Start Mode",
                    arabicName = "البدء التلقائي",
                    frenchName = "Démarrage Automatique",
                    description = "Allow the system engine to boot up immediately on system reboot to listen for wake words.",
                    arabicDescription = "السماح لمحرك النظام بالعمل تلقائياً فور تشغيل الهاتف للاستماع للأوامر الصوتية.",
                    frenchDescription = "Permet au moteur système de démarrer dès le démarrage de l'appareil.",
                    componentPkg = "com.miui.securitycenter",
                    componentCls = "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ))
                list.add(ManufacturerSetting(
                    id = "battery_saver",
                    name = "MIUI App Battery Saver",
                    arabicName = "موفر بطارية MIUI",
                    frenchName = "Économiseur de Batterie MIUI",
                    description = "Set battery restriction to 'No Restrictions' under MIUI settings to avoid background sleep.",
                    arabicDescription = "قم بتعيين موفر طاقة التطبيق على 'بلا قيود' لتجنب إيقاف الخدمة في الخلفية.",
                    frenchDescription = "Basculez sur 'Aucune restriction' pour empêcher MIUI de tuer l'écoute.",
                    componentPkg = "com.miui.securitycenter",
                    componentCls = "com.miui.powercenter.insstyle.PowerConsumptionActivity"
                ))
            }
            m.contains("samsung") -> {
                list.add(ManufacturerSetting(
                    id = "sleeping_apps",
                    name = "Never Sleeping Apps Exception",
                    arabicName = "التطبيقات قيد التشغيل الدائم",
                    frenchName = "Exception d'applications en veille",
                    description = "Add the NHN assistant to 'Never sleeping apps' checklist under device maintenance.",
                    arabicDescription = "أضف البرنامج لقائمة التطبيقات المستثناة من السكون الدائم ضمن صيانة البطارية.",
                    frenchDescription = "Ajoutez NHN à la liste des applications qui ne sont jamais mises en veille.",
                    componentPkg = "com.samsung.android.lool",
                    componentCls = "com.samsung.android.sm.ui.battery.BatteryActivity"
                ))
                list.add(ManufacturerSetting(
                    id = "auto_run",
                    name = "Auto-run Background Executions",
                    arabicName = "التشغيل الذاتي بالخلفية",
                    frenchName = "Lancement Automatique en Arrière-plan",
                    description = "Enable auto-run access for continuous device tracking and ambient wakeups.",
                    arabicDescription = "تفعيل التشغيل التلقائي لاستمرارية تتبع الهاتف واستيقاظ المساعد الصوتي.",
                    frenchDescription = "Activez l'exécution automatique pour garantir un fonctionnement stable.",
                    componentPkg = "com.samsung.android.lool",
                    componentCls = "com.samsung.android.sm.ui.battery.BatteryActivity" // Fallback matching
                ))
            }
            m.contains("huawei") || m.contains("honor") -> {
                list.add(ManufacturerSetting(
                    id = "app_launch",
                    name = "Manual App Launch management",
                    arabicName = "إدارة إطلاق التطبيق يدوياً",
                    frenchName = "Gestion de lancement d'application",
                    description = "Change app launch manager to 'Manage Manually' and activate Autostart and Run in Background.",
                    arabicDescription = "غيّر الإدارة إلى 'إدارة يدوية' وقم بتمكين التشغيل التلقائي والتشغيل بالخلفية.",
                    frenchDescription = "Désactivez la gestion automatique et cochez les lancements manuel et arrière-plan.",
                    componentPkg = "com.huawei.systemmanager",
                    componentCls = "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                ))
                list.add(ManufacturerSetting(
                    id = "protected_apps",
                    name = "Protected Active Applications",
                    arabicName = "التطبيقات المحمية",
                    frenchName = "Applications protégées",
                    description = "Mark this package as Protected within the Huawei System Manager dashboard.",
                    arabicDescription = "تحديد هذا البرنامج كتطبيق محمي وموثوق في واجهة مدير النظام من هواوي.",
                    frenchDescription = "Déclarez l'application en tant qu'application protégée dans Huawei System Manager.",
                    componentPkg = "com.huawei.systemmanager",
                    componentCls = "com.huawei.systemmanager.optimize.process.ProtectActivity"
                ))
            }
            m.contains("oppo") || m.contains("realme") -> {
                list.add(ManufacturerSetting(
                    id = "startup_manager",
                    name = "Startup App Permission",
                    arabicName = "إدارة تطبيقات بدء التشغيل",
                    frenchName = "Permission de démarrage automatique",
                    description = "Permit application to auto-boot instantly upon device start sequence.",
                    arabicDescription = "اسمح للبرنامج ببدء التشغيل التلقائي فور إقلاع نظام الأندرويد.",
                    frenchDescription = "Permettre le démarrage de l'application dès la mise en route de l'appareil.",
                    componentPkg = "com.coloros.safecenter",
                    componentCls = "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                ))
                list.add(ManufacturerSetting(
                    id = "background_freeze",
                    name = "Ignore Background Freezing",
                    arabicName = "تخطي تجميد المهام الخلفية",
                    frenchName = "Ignorer la mise en veille arrière-plan",
                    description = "Disable freezing or snooze controls for NHN background daemon threads.",
                    arabicDescription = "تعطيل تجميد عمليات التطبيق أو إخمادها لحماية استماع المساعد الصوتي.",
                    frenchDescription = "Désactiver le gel automatique d'arrière-plan pour le démon de veille.",
                    componentPkg = "com.coloros.safecenter",
                    componentCls = "com.coloros.safecenter.startupapp.StartupAppListActivity"
                ))
            }
            else -> {
                // Generic configurations
                list.add(ManufacturerSetting(
                    id = "generic_battery",
                    name = "App Battery Management",
                    arabicName = "إدارة أداء البطارية",
                    frenchName = "Optimisation de batterie standard",
                    description = "Bypass Doze optimization using general Android Power controls.",
                    arabicDescription = "تجاوز قيود توفير طاقة المعالج لضمان استمرارية استماع المساعد محلياً.",
                    frenchDescription = "Configurez les paramètres de batterie système pour NHN sur 'Non Restreint'.",
                    intentAction = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                ))
                list.add(ManufacturerSetting(
                    id = "generic_autostart",
                    name = "Background App Locks",
                    arabicName = "قفل الخلفية المستمر",
                    frenchName = "Lancement arrière-plan",
                    description = "Review background activities locks under primary developer settings.",
                    arabicDescription = "فحص والتحكم في إستثنائيات المهام بالخلفية لفتح قفل الشاشة عن بعد.",
                    frenchDescription = "Valider que l'exécution en arrière-plan est toujours autorisée par le système.",
                    intentAction = android.provider.Settings.ACTION_SETTINGS
                ))
            }
        }
        return list
    }

    /**
     * Attempts to safely launch custom manufacturer specific settings.
     * Falls back to general app settings if it fails, ensuring zero crashes.
     */
    fun launchManufacturerSetting(setting: ManufacturerSetting): Boolean {
        try {
            if (setting.componentPkg != null && setting.componentCls != null) {
                val intent = Intent().apply {
                    component = ComponentName(setting.componentPkg, setting.componentCls)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                SecurityAudit.logEvent("DEVICE_COMPAT_LAUNCH", "Launched manufacturer setting: ${setting.id} of $manufacturer", "INFO")
                return true
            } else if (setting.intentAction != null) {
                val intent = Intent(setting.intentAction).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                SecurityAudit.logEvent("DEVICE_COMPAT_LAUNCH", "Launched generic intent: ${setting.id}", "INFO")
                return true
            }
        } catch (e: Exception) {
            Log.e("DeviceCompat", "Failed to launch custom manufacturer activity: ${e.message}. Falling back...", e)
        }

        // Graceful Fallback
        try {
            val fallbackIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
            SecurityAudit.logEvent("DEVICE_COMPAT_FALLBACK", "Launched generic application details for ${setting.id} fallback.", "WARNING")
            return true
        } catch (ex: Exception) {
            Log.e("DeviceCompat", "Fatal setting backup failure: ${ex.message}")
        }
        return false
    }
}
