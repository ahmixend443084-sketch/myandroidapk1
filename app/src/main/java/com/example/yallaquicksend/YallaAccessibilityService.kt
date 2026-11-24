
package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

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
        // Accessibility events are enabled through accessibility_service.xml.
    }

    override fun onInterrupt() {
        // No ongoing accessibility operation to interrupt.
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    /**
     * Inserts text into the currently focused editable field.
     * Existing selected text is replaced by the inserted text.
     */
    fun insertText(value: String): Boolean {
        val node = findFocusedEditable() ?: return false

        return try {
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    value
                )
            }

            val currentText = node.text?.toString() ?: ""
            val selectionStart = node.textSelectionStart
            val selectionEnd = node.textSelectionEnd

            if (selectionStart >= 0 && selectionEnd >= 0) {
                val start = minOf(selectionStart, selectionEnd)
                val end = maxOf(selectionStart, selectionEnd)

                val updatedText = buildString {
                    append(currentText.substring(0, start.coerceAtMost(currentText.length)))
                    append(value)
                    append(currentText.substring(end.coerceAtMost(currentText.length)))
                }

                val success = node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            updatedText
                        )
                    }
                )

                if (success) {
                    val cursor = (start + value.length).coerceIn(
                        0,
                        updatedText.length
                    )

                    node.performAction(
                        AccessibilityNodeInfo.ACTION_SET_SELECTION,
                        Bundle().apply {
                            putInt(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                                cursor
                            )
                            putInt(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                                cursor
                            )
                        }
                    )
                }

                success
            } else {
                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    args
                )
            }
        } catch (_: Exception) {
            false
        } finally {
            node.recycle()
        }
    }

    /**
     * Deletes the selected text, or one character before the cursor.
     */
    fun deleteCharacter(): Boolean {
        val node = findFocusedEditable() ?: return false

        return try {
            val currentText = node.text?.toString() ?: ""
            val start = node.textSelectionStart
            val end = node.textSelectionEnd

            if (start < 0 || end < 0) {
                return false
            }

            val deleteStart: Int
            val deleteEnd: Int

            if (start != end) {
                deleteStart = minOf(start, end)
                deleteEnd = maxOf(start, end)
            } else if (start > 0) {
                deleteStart = start - 1
                deleteEnd = start
            } else {
                return false
            }

            val updatedText = currentText.removeRange(
                deleteStart.coerceIn(0, currentText.length),
                deleteEnd.coerceIn(0, currentText.length)
            )

            val success = node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        updatedText
                    )
                }
            )

            if (success) {
                val cursor = deleteStart.coerceIn(0, updatedText.length)

                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_SELECTION,
                    Bundle().apply {
                        putInt(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                            cursor
                        )
                        putInt(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                            cursor
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
     * Attempts to find and activate a visible Send button.
     * Messaging apps use different labels and implementations, so this
     * may not work in every app.
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

    private fun clickSendInTree(
        node: AccessibilityNodeInfo
    ): Boolean {
        val description = node.contentDescription
            ?.toString()
            ?.trim()
            ?.lowercase()
            .orEmpty()

        val text = node.text
            ?.toString()
            ?.trim()
            ?.lowercase()
            .orEmpty()

        val viewId = node.viewIdResourceName
            ?.lowercase()
            .orEmpty()

        val isSendButton =
            (text == "send" ||
             description == "send" ||
             viewId.endsWith("/send")) &&
            node.isEnabled &&
            node.isClickable

        if (isSendButton && node.performAction(
                AccessibilityNodeInfo.ACTION_CLICK
            )
        ) {
            return true
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue

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
                if (focused.isEditable) {
                    return AccessibilityNodeInfo.obtain(focused)
                }
                focused.recycle()
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

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue

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
