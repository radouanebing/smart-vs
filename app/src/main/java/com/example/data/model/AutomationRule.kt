package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerPhrase: String,
    val appToLaunch: String = "",       // e.g. "Maps", "WhatsApp", "Camera"
    val enableBluetooth: Boolean = false,
    val changeVolumeLevel: Float = -1f,  // -1 means no change, 0..1 scale
    val smsRecipient: String = "",      // e.g. "+21355512345"
    val smsText: String = "",           // Pre-defined template SMS
    val customSystemAction: String = "", // e.g. "FLASHLIGHT_ON", "FLASHLIGHT_OFF", "FIND_MY_PHONE"
    val silentMode: Boolean = false,    // Mute notification & ringer
    val screenBrightness: Float = -1f,  // -1f means no change, 0..1f scale
    val isActive: Boolean = true
)
