
package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YallaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: YallaAccessibilityService? = null
            private set
    }

    private val prefs by lazy {
        getSharedPreferences("settings", MODE_PRIVATE)
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

    fun saveSendPosition(x: Int, y: Int) {
        prefs.edit()
            .putInt("sendPositionX", x)
            .putInt("sendPositionY", y)
            .putBoolean("sendPositionSaved", true)
            .apply()
    }

    fun hasSavedSendPosition(): Boolean =
        prefs.getBoolean("sendPositionSaved", false)

    fun tapSavedSendPosition(): Boolean {
        if (!hasSavedSendPosition()) return false

        val x = prefs.getInt("sendPositionX", -1)
        val y = prefs.getInt("sendPositionY", -1)

        if (x < 0 || y < 0) return false

        return tapAt(x, y)
    }

    fun tapAt(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0) return false

        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(path, 0, 100)
            )
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun insertText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            val focused =
                root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    ?: findEditable(root)
                    ?: return false

            try {
                if (!focused.isEditable || !focused.isEnabled) {
                    return false
                }

                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo
                            .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        value
                    )
                }

                focused.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    args
                )
            } finally {
                focused.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    fun deleteCharacter(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            val focused =
                root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    ?: findEditable(root)
                    ?: return false

            try {
                if (!focused.isEditable || !focused.isEnabled) {
                    return false
                }

                val text = focused.text?.toString() ?: return false
                if (text.isEmpty()) return false

                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo
                            .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text.dropLast(1)
                    )
                }

                focused.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    args
                )
            } finally {
                focused.recycle()
            }
        } finally {
            root.recycle()
        }
    }

    fun clickVisibleSend(): Boolean {
        val root = rootInActiveWindow ?: return false

        return try {
            val labelled = findSendControl(root)

            if (labelled != null) {
                try {
                    if (clickSelfOrClickableParent(labelled)) return true
                } finally {
                    labelled.recycle()
                }
            }

            clickAdjacentSendControl(root)
        } finally {
            root.recycle()
        }
    }

    private fun findEditable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused && node.isEnabled) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = try {
                findEditable(child)
            } finally {
                child.recycle()
            }

            if (found != null) return found
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
            val found = try {
                findSendControl(child)
            } finally {
                child.recycle()
            }

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
        val focused =
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: return false

        return try {
            if (!focused.isEditable) return false

            val inputBounds = Rect()
            focused.getBoundsInScreen(inputBounds)

            val candidate = findRightSideClickable(root, inputBounds)
                ?: return false

            try {
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
        inputBounds: Rect
    ): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestDistance = Int.MAX_VALUE

        fun visit(current: AccessibilityNodeInfo) {
            if (current.isVisibleToUser &&
                current.isEnabled &&
                current.isClickable
            ) {
                val bounds = Rect()
                current.getBoundsInScreen(bounds)

                val verticallyAligned =
                    bounds.bottom >= inputBounds.top &&
                    bounds.top <= inputBounds.bottom

                val distance = bounds.left - inputBounds.right

                if (verticallyAligned &&
                    bounds.left >= inputBounds.right - 12 &&
                    distance in -12..180 &&
                    distance < bestDistance
                ) {
                    best?.recycle()
                    best = AccessibilityNodeInfo.obtain(current)
                    bestDistance = distance
                }
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                try {
                    visit(child)
                } finally {
                    child.recycle()
                }
            }
        }

        visit(node)
        return best
    }
}
