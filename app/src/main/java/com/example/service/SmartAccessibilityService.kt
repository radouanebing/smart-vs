package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class SmartAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Here we track dynamic events like window state changes
        event?.let {
            Log.d("SmartAccessibility", "Accessibility event received: type=${it.eventType}, package=${it.packageName}")
        }
    }

    override fun onInterrupt() {
        Log.e("SmartAccessibility", "Accessibility service interrupted.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("SmartAccessibility", "Accessibility service successfully connected.")
    }

    /**
     * Executes automatic clicks on nodes that match specific text.
     * This allows voice commands to perform actions like "click Search" or "click Done".
     */
    fun performAutoClickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        if (nodes.isNullOrEmpty()) return false
        
        var clicked = false
        for (node in nodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                clicked = true
                Log.d("SmartAccessibility", "Auto clicked: ${node.text ?: text}")
                break
            }
        }
        return clicked
    }

    /**
     * Executes custom gesture clicks on specified screen coordinate offsets.
     */
    fun performClickAtCoordinates(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(x, y)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            dispatchGesture(gesture, null, null)
            Log.d("SmartAccessibility", "Dispatched coordinate tap at ($x, $y)")
        }
    }
}
