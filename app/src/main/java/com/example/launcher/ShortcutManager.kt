package com.example.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

class ShortcutManager(private val context: Context) {

    /**
     * Direct router shortcuts representing quick task templates
     */
    fun triggerDialCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ShortcutManager", "Failed to register quick-call intent trigger", e)
        }
    }

    fun triggerSmsMessage(recipient: String, messageText: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$recipient")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("sms_body", messageText)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ShortcutManager", "SMS activity launcher failed", e)
        }
    }
}
