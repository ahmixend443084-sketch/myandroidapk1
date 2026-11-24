
package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

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

            val a = if (start >= 0 && end >= 0) {
                minOf(start, end).coerceIn(0, oldText.length)
            } else {
                oldText.length
            }

            val b = if (start >= 0 && end >= 0) {
                maxOf(start, end).coerceIn(0, oldText.length)
            } else {
                oldText.length
            }

            val newText = oldText.substring(0, a) +
                value + oldText.substring(b)
            val cursor = a + value.length

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
            } else {
                if (start == 0) return false
                a = start - 1
                b = start
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

    fun clickVisibleSend(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            // First try a clearly labeled Send control.
            if (clickLabeledSend(root)) return true

            // Fallback: inspect clickable controls next to the
            // currently focused message input field.
            clickAdjacentSendControl(root)
        } catch (_: Exception) {
            false
        } finally {
            root.recycle()
        }
    }

    private fun isSendLabel(node: AccessibilityNodeInfo): Boolean {
        val labels = listOfNotNull(
            node.text?.toString(),
            node.contentDescription?.toString(),
            node.viewIdResourceName
        )

        return labels.any { raw ->
            val label = raw.lowercase()
                .substringAfterLast('/')
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()

            label == "send" ||
                label == "send message" ||
                label.endsWith(" send") ||
                label.contains("send message") ||
                label.contains("send button")
        }
    }

    private fun clickSelfOrClickableParent(
        node: AccessibilityNodeInfo
    ): Boolean {
        var current: AccessibilityNodeInfo? =
            AccessibilityNodeInfo.obtain(node)

        repeat(6) {
            val item = current ?: return false
            val parent = item.parent

            val clicked = try {
                item.isEnabled &&
                    item.isClickable &&
                    item.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
            } catch (_: Exception) {
                false
            }

            item.recycle()
            current = parent

            if (clicked) {
                current?.recycle()
                current = null
                return true
            }

            if (parent == null) return false
        }

        current?.recycle()
        return false
    }

    private fun clickLabeledSend(
        node: AccessibilityNodeInfo
    ): Boolean {
        if (isSendLabel(node) && node.isEnabled) {
            if (clickSelfOrClickableParent(node)) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val clicked = try {
                clickLabeledSend(child)
            } finally {
                child.recycle()
            }

            if (clicked) return true
        }

        return false
    }

    private fun clickAdjacentSendControl(
        root: AccessibilityNodeInfo
    ): Boolean {
        val input = findFocusedEditable() ?: return false

        try {
            val inputBounds = Rect()
            input.getBoundsInScreen(inputBounds)

            val parent = input.parent ?: return false

            try {
                val candidates = mutableListOf<AccessibilityNodeInfo>()

                for (i in 0 until parent.childCount) {
                    val child = parent.getChild(i) ?: continue

                    val bounds = Rect()
                    child.getBoundsInScreen(bounds)

                    val isRightSide = bounds.left >= inputBounds.right - 8
                    val overlapsVertically =
                        bounds.bottom > inputBounds.top &&
                        bounds.top < inputBounds.bottom

                    if (child.isEnabled &&
                        (child.isClickable || child.childCount > 0) &&
                        isRightSide &&
                        overlapsVertically &&
                        bounds.width() > 0 &&
                        bounds.height() > 0
                    ) {
                        candidates.add(
                            AccessibilityNodeInfo.obtain(child)
                        )
                    }

                    child.recycle()
                }

                // Prefer the nearest control to the right of the
                // message input, rather than an arbitrary screen button.
                candidates.sortBy { candidate ->
                    val bounds = Rect()
                    candidate.getBoundsInScreen(bounds)
                    bounds.left
                }

                for (candidate in candidates) {
                    val clicked = try {
                        findAndClickClickable(candidate)
                    } finally {
                        candidate.recycle()
                    }

                    if (clicked) {
                        candidates.dropWhile { it !== candidate }
                            .drop(1)
                            .forEach {
                                try {
                                    it.recycle()
                                } catch (_: Exception) {
                                }
                            }
                        return true
                    }
                }

                return false
            } finally {
                parent.recycle()
            }
        } finally {
            input.recycle()
        }
    }

    private fun findAndClickClickable(
        node: AccessibilityNodeInfo
    ): Boolean {
        if (node.isEnabled && node.isClickable) {
            if (node.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            ) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val clicked = try {
                findAndClickClickable(child)
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
