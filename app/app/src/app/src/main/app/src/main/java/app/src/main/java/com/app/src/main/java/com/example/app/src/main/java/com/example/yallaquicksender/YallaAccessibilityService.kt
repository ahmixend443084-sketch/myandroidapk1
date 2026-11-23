package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class YallaAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
    override fun onInterrupt() { }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun clickVisibleSend(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findAndClick(root)
    }

    private fun findAndClick(node: AccessibilityNodeInfo): Boolean {
        val label = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
        if (label.trim().equals("send", ignoreCase = true) ||
            label.trim().equals("➤ send", ignoreCase = true)) {
            if (node.isEnabled && node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            var parent = node.parent
            while (parent != null) {
                if (parent.isEnabled && parent.isClickable) return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent = parent.parent
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClick(child)) return true
        }
        return false
    }

    companion object {
        @Volatile var instance: YallaAccessibilityService? = null
    }
}
