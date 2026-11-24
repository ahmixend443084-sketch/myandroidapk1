
package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Bundle

class YallaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    fun insertText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = findFocusedEditable(root) ?: return false

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                value
            )
        }

        return field.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            args
        )
    }

    private fun findFocusedEditable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {

        if (node.isEditable && node.isFocused && node.isEnabled) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFocusedEditable(child)
            if (result != null) return result
        }

        return null
    }

    fun clickVisibleSend(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findAndClickSend(root)
    }

    private fun findAndClickSend(
        node: AccessibilityNodeInfo
    ): Boolean {

        val label = (
            (node.text?.toString() ?: "") + " " +
            (node.contentDescription?.toString() ?: "") + " " +
            (node.viewIdResourceName ?: "")
        ).lowercase()

        val matches = label.contains("send") ||
            label.contains("paper plane")

        if (matches && node.isEnabled) {
            if (node.isClickable &&
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) {
                return true
            }

            var parent = node.parent
            while (parent != null) {
                if (parent.isEnabled && parent.isClickable &&
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ) {
                    return true
                }
                parent = parent.parent
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickSend(child)) return true
        }

        return false
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: YallaAccessibilityService? = null
    }
}
