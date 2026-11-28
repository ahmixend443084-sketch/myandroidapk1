
package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YallaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: YallaAccessibilityService? = null
            private set
    }

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events are received while service is enabled.
    }

    override fun onInterrupt() {
        // No continuous operation to interrupt.
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    fun hasSavedSendPosition(): Boolean {
        return prefs.getBoolean("sendPositionSaved", false) &&
            prefs.getInt("sendPositionX", -1) >= 0 &&
            prefs.getInt("sendPositionY", -1) >= 0
    }

    /**
     * Tap the saved Send-button screen position.
     */
    fun tapSavedSendPosition(): Boolean {
        if (!hasSavedSendPosition()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val x = prefs.getInt("sendPositionX", -1)
        val y = prefs.getInt("sendPositionY", -1)

        if (x < 0 || y < 0) return false

        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
            lineTo(x.toFloat(), y.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    100L
                )
            )
            .build()

        return try {
            dispatchGesture(gesture, null, null)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Find and click a visible Send button when available.
     */
    fun clickVisibleSend(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            findAndClickSend(root)
        } finally {
            root.recycle()
        }
    }

    private fun findAndClickSend(
        node: AccessibilityNodeInfo
    ): Boolean {
        if (!node.isVisibleToUser) return false

        val description = buildString {
            append(node.text?.toString().orEmpty())
            append(" ")
            append(node.contentDescription?.toString().orEmpty())
            append(" ")
            append(node.viewIdResourceName.orEmpty())
        }.trim().lowercase()

        val looksLikeSend =
            description == "send" ||
            description.contains("send button") ||
            description.endsWith(":id/send") ||
            description.endsWith("/send")

        if (looksLikeSend && node.isEnabled) {
            if (
                node.isClickable &&
                node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            ) {
                return true
            }

            val parent = node.parent
            if (parent != null) {
                val clicked = try {
                    parent.isVisibleToUser &&
                        parent.isEnabled &&
                        parent.isClickable &&
                        parent.performAction(
                            AccessibilityNodeInfo.ACTION_CLICK
                        )
                } finally {
                    parent.recycle()
                }

                if (clicked) return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val clicked = try {
                findAndClickSend(child)
            } finally {
                child.recycle()
            }

            if (clicked) return true
        }

        return false
    }

    /**
     * Insert text into the focused editable field.
     */
    fun insertText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            val node = findFocusedEditable(root) ?: return false

            try {
                val arguments = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo
                            .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        value
                    )
                }

                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    arguments
                )
            } finally {
                node.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    /**
     * Delete the last character from the focused editable field.
     */
    fun deleteCharacter(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            val node = findFocusedEditable(root) ?: return false

            try {
                val currentText = node.text?.toString() ?: return false
                if (currentText.isEmpty()) return false

                val arguments = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo
                            .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        currentText.dropLast(1)
                    )
                }

                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    arguments
                )
            } finally {
                node.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    private fun findFocusedEditable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val result = try {
                findFocusedEditable(child)
            } finally {
                child.recycle()
            }

            if (result != null) return result
        }

        return null
    }
}
