
package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YallaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: YallaAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Target app UI is inspected only when an action is requested.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun insertText(value: String): Boolean {
        val node = findFocusedEditable() ?: return false

        return try {
            val oldText = node.text?.toString() ?: ""
            val start = node.textSelectionStart
            val end = node.textSelectionEnd

            val newText: String
            val cursor: Int

            if (start >= 0 && end >= 0) {
                val a = minOf(start, end).coerceIn(0, oldText.length)
                val b = maxOf(start, end).coerceIn(0, oldText.length)

                newText = oldText.substring(0, a) +
                    value + oldText.substring(b)
                cursor = a + value.length
            } else {
                newText = oldText + value
                cursor = newText.length
            }

            val success = node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        newText
                    )
                }
            )

            if (success) {
                val position = cursor.coerceIn(0, newText.length)
                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_SELECTION,
                    Bundle().apply {
                        putInt(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                            position
                        )
                        putInt(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                            position
                        )
                    }
                )
            }

            success
        } catch (_: Exception) {
            false
        } finally {
            node.recycle()
        }
    }

    fun deleteCharacter(): Boolean {
        val node = findFocusedEditable() ?: return false

        return try {
            val text = node.text?.toString() ?: ""
            val start = node.textSelectionStart
            val end = node.textSelectionEnd

            if (start < 0 || end < 0) return false

            val a: Int
            val b: Int

            if (start != end) {
                a = minOf(start, end)
                b = maxOf(start, end)
            } else if (start > 0) {
                a = start - 1
                b = start
            } else {
                return false
            }

            val updated = text.removeRange(
                a.coerceIn(0, text.length),
                b.coerceIn(0, text.length)
            )

            val success = node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        updated
                    )
                }
            )

            if (success) {
                val position = a.coerceIn(0, updated.length)
                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_SELECTION,
                    Bundle().apply {
                        putInt(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                            position
                        )
                        putInt(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                            position
                        )
                    }
                )
            }

            success
        } catch (_: Exception) {
            false
        } finally {
            node.recycle()
        }
    }

    /**
     * Searches the active screen for a likely Send control.
     * Returns false if the app does not expose a suitable accessible node.
     */
    fun clickVisibleSend(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            clickSendInTree(root)
        } catch (_: Exception) {
            false
        } finally {
            root.recycle()
        }
    }

    private fun clickSendInTree(node: AccessibilityNodeInfo): Boolean {
        val labels = listOf(
            node.text?.toString(),
            node.contentDescription?.toString(),
            node.viewIdResourceName
        ).filterNotNull().map { it.lowercase() }

        val looksLikeSend = labels.any { label ->
            val normalized = label.substringAfterLast('/')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()

            normalized == "send" ||
            normalized == "send message" ||
            normalized.endsWith(" send") ||
            normalized.contains("send_button") ||
            normalized.contains("send message")
        }

        if (looksLikeSend && node.isEnabled) {
            // Click the node itself, or a clickable ancestor.
            if (node.isClickable &&
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) {
                return true
            }

            val parent = node.parent
            if (parent != null) {
                try {
                    if (parent.isEnabled && parent.isClickable &&
                        parent.performAction(
                            AccessibilityNodeInfo.ACTION_CLICK
                        )
                    ) {
                        return true
                    }
                } finally {
                    parent.recycle()
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val clicked = try {
                clickSendInTree(child)
            } finally {
                child.recycle()
            }

            if (clicked) return true
        }

        return false
    }

    private fun findFocusedEditable(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null

        try {
            val focused = root.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            )

            if (focused != null) {
                try {
                    if (focused.isEditable) {
                        return AccessibilityNodeInfo.obtain(focused)
                    }
                } finally {
                    focused.recycle()
                }
            }

            return findEditableInTree(root)
        } finally {
            root.recycle()
        }
    }

    private fun findEditableInTree(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val result = try {
                findEditableInTree(child)
            } finally {
                child.recycle()
            }

            if (result != null) return result
        }

        return null
    }
}
