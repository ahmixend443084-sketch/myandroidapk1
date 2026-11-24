
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

            val updated = oldText.substring(0, a) +
                value + oldText.substring(b)

            val cursor = a + value.length

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

            val safeA = a.coerceIn(0, text.length)
            val safeB = b.coerceIn(safeA, text.length)
            val updated = text.removeRange(safeA, safeB)

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
                val position = safeA.coerceIn(0, updated.length)

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
     * Attempts to activate the actual Send control.
     * Returns true only when an accessibility click succeeds.
     */
    fun clickVisibleSend(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            // 1. Prefer controls explicitly labelled Send.
            if (clickLabeledSend(root)) return true

            // 2. Find clickable controls to the right of the input.
            clickAdjacentSendControl(root)
        } catch (_: Exception) {
            false
        } finally {
            root.recycle()
        }
    }

    private fun isSendLabel(
        node: AccessibilityNodeInfo
    ): Boolean {
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
                label == "send button" ||
                label.contains("send message") ||
                label.contains("send button") ||
                label.endsWith(" send")
        }
    }

    private fun clickSelfOrClickableParent(
        node: AccessibilityNodeInfo
    ): Boolean {
        var current: AccessibilityNodeInfo? =
            AccessibilityNodeInfo.obtain(node)

        repeat(8) {
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
        if (node.isEnabled && isSendLabel(node)) {
            if (clickSelfOrClickableParent(node)) {
                return true
            }
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
        val candidates = mutableListOf<AccessibilityNodeInfo>()

        try {
            val inputBounds = Rect()
            input.getBoundsInScreen(inputBounds)

            collectSendCandidates(
                root,
                inputBounds,
                candidates
            )

            // Try the rightmost clickable control first.
            candidates.sortByDescending { candidate ->
                val bounds = Rect()
                candidate.getBoundsInScreen(bounds)
                bounds.right
            }

            for (candidate in candidates) {
                if (clickSelfOrClickableParent(candidate)) {
                    return true
                }
            }

            return false
        } catch (_: Exception) {
            return false
        } finally {
            candidates.forEach {
                try {
                    it.recycle()
                } catch (_: Exception) {
                }
            }

            input.recycle()
        }
    }

    private fun collectSendCandidates(
        node: AccessibilityNodeInfo,
        inputBounds: Rect,
        candidates: MutableList<AccessibilityNodeInfo>
    ) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val rightOfInput =
            bounds.left >= inputBounds.right - 4

        val overlapsInput =
            bounds.bottom > inputBounds.top &&
                bounds.top < inputBounds.bottom

        if (node.isEnabled &&
            node.isClickable &&
            rightOfInput &&
            overlapsInput &&
            bounds.width() > 0 &&
            bounds.height() > 0
        ) {
            candidates.add(
                AccessibilityNodeInfo.obtain(node)
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            try {
                collectSendCandidates(
                    child,
                    inputBounds,
                    candidates
                )
            } finally {
                child.recycle()
            }
        }
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
