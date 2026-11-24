
package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
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

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // Visible screen updates are handled when a send is requested.
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

    fun insertText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: findEditable(root)
                ?: return false

            if (!focused.isEditable || !focused.isEnabled) {
                return false
            }

            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    value
                )
            }

            focused.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                args
            )
        } finally {
            root.recycle()
        }
    }

    fun deleteCharacter(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: findEditable(root)
                ?: return false

            if (!focused.isEditable || !focused.isEnabled) {
                return false
            }

            val text = focused.text?.toString() ?: return false
            if (text.isEmpty()) return false

            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text.dropLast(1)
                )
            }

            focused.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                args
            )
        } finally {
            root.recycle()
        }
    }

    fun clickVisibleSend(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            // Prefer an explicitly labelled Send control.
            val labelled = findSendControl(root)
            if (labelled != null && clickSelfOrClickableParent(labelled)) {
                return true
            }

            // Fallback: a clickable control immediately to the right
            // of the focused editable field, aligned vertically.
            clickAdjacentSendControl(root)
        } finally {
            root.recycle()
        }
    }

    private fun findEditable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused && node.isEnabled) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditable(child)

            if (found != null) {
                child.recycle()
                return found
            }

            child.recycle()
        }

        return null
    }

    private fun findSendControl(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && node.isEnabled && isSendLabel(node)) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findSendControl(child)
            child.recycle()

            if (found != null) return found
        }

        return null
    }

    private fun isSendLabel(node: AccessibilityNodeInfo): Boolean {
        val label = buildString {
            append(node.text?.toString().orEmpty())
            append(' ')
            append(node.contentDescription?.toString().orEmpty())
            append(' ')
            append(node.viewIdResourceName.orEmpty())
        }.lowercase()

        return label.contains("send") ||
            label.contains("paper_plane") ||
            label.contains("paper-plane") ||
            label.contains("send_button")
    }

    private fun clickSelfOrClickableParent(
        node: AccessibilityNodeInfo
    ): Boolean {
        var current: AccessibilityNodeInfo? =
            AccessibilityNodeInfo.obtain(node)

        repeat(8) {
            val item = current ?: return false

            if (item.isEnabled && item.isClickable) {
                val clicked = item.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
                item.recycle()
                current = null
                return clicked
            }

            val parent = item.parent
            item.recycle()
            current = parent
        }

        current?.recycle()
        return false
    }

    private fun clickAdjacentSendControl(
        root: AccessibilityNodeInfo
    ): Boolean {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return false

        try {
            if (!focused.isEditable) return false

            val inputBounds = android.graphics.Rect()
            focused.getBoundsInScreen(inputBounds)

            val candidate = findRightSideClickable(root, inputBounds)
                ?: return false

            return try {
                clickSelfOrClickableParent(candidate)
            } finally {
                candidate.recycle()
            }
        } finally {
            focused.recycle()
        }
    }

    private fun findRightSideClickable(
        node: AccessibilityNodeInfo,
        inputBounds: android.graphics.Rect
    ): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestDistance = Int.MAX_VALUE

        fun visit(current: AccessibilityNodeInfo) {
            if (current.isVisibleToUser &&
                current.isEnabled &&
                current.isClickable
            ) {
                val bounds = android.graphics.Rect()
                current.getBoundsInScreen(bounds)

                val verticallyAligned =
                    bounds.bottom >= inputBounds.top &&
                    bounds.top <= inputBounds.bottom

                val toRight = bounds.left >= inputBounds.right - 12
                val distance = bounds.left - inputBounds.right

                // Limit fallback to nearby controls, avoiding distant
                // buttons elsewhere on the screen.
                if (verticallyAligned && toRight &&
                    distance in -12..180
                ) {
                    if (distance < bestDistance) {
                        best?.recycle()
                        best = AccessibilityNodeInfo.obtain(current)
                        bestDistance = distance
                    }
                }
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                visit(child)
                child.recycle()
            }
        }

        visit(node)
        return best
    }
}
