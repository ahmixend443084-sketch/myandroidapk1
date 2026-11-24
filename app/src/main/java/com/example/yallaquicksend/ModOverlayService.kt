
package com.example.yallaquicksend

import android.accessibilityservice.AccessibilityService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ModOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var panelView: View? = null
    private var settingsVisible = false
    private var numbersMode = true

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        showMainPanel()
        return START_NOT_STICKY
    }

    private fun showMainPanel() {
        removePanel()
        windowManager = getSystemService(
            Context.WINDOW_SERVICE
        ) as WindowManager

        settingsVisible = false
        numbersMode = true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            background = roundedBackground(
                Color.rgb(25, 23, 37),
                dp(16).toFloat()
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "𝙁𝙇𝘼𝙎𝙃🌙"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(194, 166, 255))
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        header.addView(
            panelButton("⚙", 14f) {
                settingsVisible = !settingsVisible
                renderPanel(root)
            }
        )

        header.addView(
            panelButton("✕", 14f) {
                stopSelf()
            }
        )

        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "panel_content"
        }

        root.addView(content)
        panelView = root

        renderPanel(root)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(120)
        }

        try {
            windowManager.addView(root, params)
        } catch (e: Exception) {
            panelView = null
            stopSelf()
            Toast.makeText(
                this,
                "FLASH panel start nahi hua",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun renderPanel(root: LinearLayout) {
        val content = root.findViewWithTag<LinearLayout>("panel_content")
            ?: return

        content.removeAllViews()

        if (settingsVisible) {
            addSettings(content)
            return
        }

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        modeRow.addView(
            panelButton("123", 13f) {
                numbersMode = true
                renderPanel(root)
            },
            equalButtonParams()
        )

        modeRow.addView(
            panelButton("ABC", 13f) {
                numbersMode = false
                renderPanel(root)
            },
            equalButtonParams()
        )

        content.addView(modeRow)

        val sequence = if (numbersMode) {
            prefs.getString("numberSequence", "1234567890")
                ?: "1234567890"
        } else {
            prefs.getString(
                "alphabetSequence",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            ) ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        }

        val keyList = sequence
            .filterNot { it.isWhitespace() }
            .take(100)
            .map { it.toString() }

        val columns = if (numbersMode) 3 else 7

        keyList.chunked(columns).forEach { keys ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            keys.forEach { key ->
                row.addView(
                    panelButton(key, 14f) {
                        insertText(key)
                    },
                    equalButtonParams()
                )
            }

            content.addView(row)
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actions.addView(
            panelButton("DELETE", 11f) {
                deleteCharacter()
            },
            LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        actions.addView(
            panelButton("SPACE", 11f) {
                insertText(" ")
            },
            LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        content.addView(actions)

        content.addView(
            panelButton("OPEN KEYBOARD", 12f) {
                try {
                    startActivity(
                        Intent(
                            this,
                            MainActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        "Main screen open nahi hui",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun addSettings(content: LinearLayout) {
        content.addView(
            panelLabel("FLASH SETTINGS")
        )

        addSettingToggle(
            content,
            "Auto Drop",
            "autoDrop"
        )

        addSettingToggle(
            content,
            "Speed Based",
            "speedBased"
        )

        addSettingToggle(
            content,
            "Enter = Send",
            "enterSend",
            true
        )

        content.addView(
            panelButton("BACK TO PANEL", 12f) {
                settingsVisible = false
                panelView?.let {
                    renderPanel(it as LinearLayout)
                }
            }
        )
    }

    private fun addSettingToggle(
        content: LinearLayout,
        label: String,
        key: String,
        defaultValue: Boolean = false
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val text = panelLabel(label)
        row.addView(
            text,
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        val enabled = prefs.getBoolean(key, defaultValue)

        row.addView(
            panelButton(if (enabled) "ON" else "OFF", 12f) {
                val newValue = !prefs.getBoolean(key, defaultValue)
                prefs.edit().putBoolean(key, newValue).apply()
                renderSettingsAgain(content)
            }
        )

        content.addView(row)
    }

    private fun renderSettingsAgain(content: LinearLayout) {
        content.removeAllViews()
        addSettings(content)
    }

    private fun insertText(value: String) {
        val service = YallaAccessibilityService.instance

        if (service != null) {
            service.insertText(value)
        } else {
            Toast.makeText(
                this,
                "Pehle FLASH Accessibility Service enable karo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteCharacter() {
        val service = YallaAccessibilityService.instance

        if (service == null) {
            Toast.makeText(
                this,
                "Pehle Accessibility Service enable karo",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val node = service.rootInActiveWindow
                ?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

            if (node == null || !node.isEditable) {
                Toast.makeText(
                    this,
                    "Pehle message field select karo",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val text = node.text?.toString() ?: ""
            val start = node.textSelectionStart
            val end = node.textSelectionEnd

            val newText: String
            val newCursor: Int

            if (start >= 0 && end >= 0 && start != end) {
                newText = text.removeRange(
                    minOf(start, end),
                    maxOf(start, end)
                )
                newCursor = minOf(start, end)
            } else if (start > 0) {
                newText = text.removeRange(start - 1, start)
                newCursor = start - 1
            } else {
                return
            }

            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }

            if (node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    args
                )
            ) {
                val selectionArgs = Bundle().apply {
                    putInt(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                        newCursor.coerceIn(0, newText.length)
                    )
                    putInt(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                        newCursor.coerceIn(0, newText.length)
                    )
                }

                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_SELECTION,
                    selectionArgs
                )
            }
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Is field mein delete support nahi ho raha",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun panelLabel(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(3), dp(4), dp(3))
        }
    }

    private fun panelButton(
        label: String,
        size: Float,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = size
            isAllCaps = false
            setTextColor(Color.WHITE)
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(5), dp(2), dp(5), dp(2))
            background = roundedBackground(
                Color.rgb(69, 53, 105),
                dp(8).toFloat()
            )
            setOnClickListener { action() }
        }
    }

    private fun equalButtonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            dp(42),
            1f
        ).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun removePanel() {
        val view = panelView ?: return
        try {
            if (::windowManager.isInitialized) {
                windowManager.removeView(view)
            }
        } catch (_: Exception) {
        }
        panelView = null
    }

    override fun onDestroy() {
        removePanel()
        super.onDestroy()
    }
}
