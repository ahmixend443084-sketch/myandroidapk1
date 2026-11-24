
package com.example.yallaquicksend

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

class ModOverlayService : Service() {

    private lateinit var wm: WindowManager
    private var panel: View? = null
    private var settingsOpen = false

    private var alphabetMode = false

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        showPanel()
        return START_NOT_STICKY
    }

    private fun windowParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            dp(260),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8)
            y = dp(120)
        }
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(65, 61, 95))
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(3), 0, dp(3), 0)
            minHeight = dp(42)
            setOnClickListener { action() }
        }
    }

    private fun showPanel() {
        if (panel != null) return

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showMainPanel()
    }

    private fun showMainPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), dp(9), dp(9), dp(9))
            background = GradientDrawable().apply {
                setColor(Color.rgb(30, 27, 52))
                cornerRadius = dp(12).toFloat()
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "YALLA QUICK SEND"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        header.addView(
            makeButton("SETTINGS") {
                showSettingsPanel()
            }
        )

        header.addView(
            makeButton("✕") {
                stopSelf()
            }
        )

        root.addView(header)

        val modes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        modes.addView(
            makeButton("123") {
                alphabetMode = false
                showMainPanel()
            },
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        modes.addView(
            makeButton("ABC") {
                alphabetMode = true
                showMainPanel()
            },
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        root.addView(modes)

        val keys = if (alphabetMode) {
            listOf(
                "A", "B", "C", "D", "E",
                "F", "G", "H", "I", "J",
                "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T",
                "U", "V", "W", "X", "Y",
                "Z"
            )
        } else {
            listOf(
                "1", "2", "3", "4", "5",
                "6", "7", "8", "9", "0"
            )
        }

        val grid = GridLayout(this).apply {
            columnCount = 5
            setPadding(0, dp(5), 0, dp(5))
        }

        keys.forEach { key ->
            grid.addView(
                makeButton(key) {
                    KeyboardBridge.insert(this@ModOverlayService, key)
                },
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(44)
                    columnSpec = GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                    )
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
            )
        }

        root.addView(grid)

        val delete = makeButton("DELETE") {
            Toast.makeText(
                this,
                "Use DELETE on the keyboard",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(delete)

        panel = root
        wm.addView(root, windowParams())
    }

    private fun showSettingsPanel() {
        val oldPanel = panel ?: return
        wm.removeView(oldPanel)
        panel = null
        settingsOpen = true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(30, 27, 52))
                cornerRadius = dp(12).toFloat()
            }
        }

        root.addView(TextView(this).apply {
            text = "QUICK SEND SETTINGS"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(12))
        })

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val enterSend = CheckBox(this).apply {
            text = "ENTER = SEND"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("enterSend", true)
        }

        root.addView(enterSend)

        val autoDrop = CheckBox(this).apply {
            text = "AUTO DROP"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("autoDrop", false)
        }

        root.addView(autoDrop)

        val speedBased = CheckBox(this).apply {
            text = "SPEED BASED"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("speedBased", false)
        }

        root.addView(speedBased)

        root.addView(makeButton("SAVE") {
            prefs.edit()
                .putBoolean("enterSend", enterSend.isChecked)
                .putBoolean("autoDrop", autoDrop.isChecked)
                .putBoolean("speedBased", speedBased.isChecked)
                .apply()

            Toast.makeText(
                this,
                "Settings saved. Reopen keyboard to apply.",
                Toast.LENGTH_SHORT
            ).show()
        })

        root.addView(makeButton("BACK") {
            val current = panel
            if (current != null) {
                wm.removeView(current)
                panel = null
            }
            settingsOpen = false
            showMainPanel()
        })

        panel = root
        wm.addView(root, windowParams())
    }

    override fun onDestroy() {
        panel?.let {
            if (::wm.isInitialized) {
                wm.removeView(it)
            }
        }
        panel = null
        super.onDestroy()
    }
}
