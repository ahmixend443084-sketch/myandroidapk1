
package com.example.yallaquicksend

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ModOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var panelView: View? = null
    private var settingsVisible = false
    private var numbersMode = true
    private var stopped = true

    private val handler = Handler(Looper.getMainLooper())

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val autoTask = object : Runnable {
        override fun run() {
            if (stopped || !prefs.getBoolean("autoDrop", false)) {
                return
            }

            val service = YallaAccessibilityService.instance

            if (service == null || !service.clickVisibleSend()) {
                toast("Send button nahi mila. Yalla message field check karo.")
            }

            // Minimum interval prevents an uncontrolled zero-delay loop.
            val delay = prefs.getInt("delay", 100).coerceIn(100, 60000)
            if (!stopped && prefs.getBoolean("autoDrop", false)) {
                handler.postDelayed(this, delay.toLong())
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (!Settings.canDrawOverlays(this)) {
            toast("FLASH ko Display over other apps permission do")
            stopSelf()
            return START_NOT_STICKY
        }

        showMainPanel()
        return START_NOT_STICKY
    }

    private fun showMainPanel() {
        removePanel()

        windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager

        settingsVisible = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), dp(7), dp(9), dp(9))
            background = roundedBackground(
                Color.rgb(25, 23, 37),
                dp(16).toFloat()
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            TextView(this).apply {
                text = "𝙁𝙇𝘼𝙎𝙃🌙"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.rgb(194, 166, 255))
                gravity = Gravity.CENTER_VERTICAL
            },
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        header.addView(panelButton("⚙", 14f) {
            settingsVisible = !settingsVisible
            renderPanel(root)
        })

        header.addView(panelButton("✕", 14f) {
            stopSelf()
        })

        root.addView(header)

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "panel_content"
        })

        panelView = root
        renderPanel(root)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(10)
            y = dp(110)
        }

        try {
            windowManager.addView(root, params)
        } catch (_: Exception) {
            panelView = null
            stopSelf()
            toast("FLASH panel start nahi hua")
        }
    }

    private fun renderPanel(root: LinearLayout) {
        val content =
            root.findViewWithTag<LinearLayout>("panel_content") ?: return

        content.removeAllViews()

        if (settingsVisible) {
            addSettings(content, root)
            return
        }

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        modeRow.addView(panelButton("123", 13f) {
            numbersMode = true
            renderPanel(root)
        }, equalParams())

        modeRow.addView(panelButton("ABC", 13f) {
            numbersMode = false
            renderPanel(root)
        }, equalParams())

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

        val columns = if (numbersMode) 3 else 7

        sequence.filterNot { it.isWhitespace() }
            .take(100)
            .map { it.toString() }
            .chunked(columns)
            .forEach { keys ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                keys.forEach { key ->
                    row.addView(
                        panelButton(key, 14f) { insertText(key) },
                        equalParams()
                    )
                }

                content.addView(row)
            }

        val editRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        editRow.addView(panelButton("DELETE", 10f) {
            val service = YallaAccessibilityService.instance
            if (service == null || !service.deleteCharacter()) {
                toast("Yalla message field select karo")
            }
        }, equalParams())

        editRow.addView(panelButton("SPACE", 10f) {
            insertText(" ")
        }, equalParams())

        content.addView(editRow)

        content.addView(panelButton("SEND", 13f, true) {
            sendOnce()
        })

        content.addView(panelButton(
            if (prefs.getBoolean("autoDrop", false)) {
                "AUTO DROP: ON"
            } else {
                "AUTO DROP: OFF"
            },
            12f
        ) {
            setAutoDrop(!prefs.getBoolean("autoDrop", false), root)
        })

        content.addView(panelButton("STOP AUTO DROP", 12f) {
            stopAutoDrop(root)
        })

        content.addView(panelButton("OPEN FLASH SETTINGS", 11f) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        })
    }

    private fun sendOnce() {
        val service = YallaAccessibilityService.instance

        if (service == null) {
            toast("Pehle FLASH Accessibility enable karo")
        } else if (!service.clickVisibleSend()) {
            toast("Yalla ka Send button nahi mila")
        }
    }

    private fun addSettings(
        content: LinearLayout,
        root: LinearLayout
    ) {
        content.addView(panelLabel("FLASH SETTINGS"))

        addToggle(
            content, "Enter key sends message",
            "enterSend", true, root
        )

        addToggle(
            content, "Auto Drop",
            "autoDrop", false, root
        )

        addToggle(
            content, "Speed Based",
            "speedBased", true, root
        )

        content.addView(panelButton("BACK TO PANEL", 12f) {
            settingsVisible = false
            renderPanel(root)
        })
    }

    private fun addToggle(
        content: LinearLayout,
        label: String,
        key: String,
        defaultValue: Boolean,
        root: LinearLayout
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        row.addView(
            panelLabel(label),
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        val enabled = prefs.getBoolean(key, defaultValue)

        row.addView(panelButton(if (enabled) "ON" else "OFF", 12f) {
            val next = !prefs.getBoolean(key, defaultValue)
            prefs.edit().putBoolean(key, next).apply()

            if (key == "autoDrop") {
                setAutoDrop(next, root)
            } else {
                renderPanel(root)
            }
        })

        content.addView(row)
    }

    private fun setAutoDrop(
        enabled: Boolean,
        root: LinearLayout
    ) {
        prefs.edit().putBoolean("autoDrop", enabled).apply()
        handler.removeCallbacks(autoTask)
        stopped = !enabled

        if (enabled) {
            // Start the first attempt immediately.
            handler.post(autoTask)
        }

        renderPanel(root)
    }

    private fun stopAutoDrop(root: LinearLayout) {
        stopped = true
        prefs.edit().putBoolean("autoDrop", false).apply()
        handler.removeCallbacks(autoTask)
        renderPanel(root)
        toast("Auto Drop stopped")
    }

    private fun insertText(value: String) {
        val service = YallaAccessibilityService.instance

        if (service == null || !service.insertText(value)) {
            toast("Pehle Yalla message field select karo")
        }
    }

    private fun panelLabel(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(3), dp(4), dp(3))
    }

    private fun panelButton(
        label: String,
        size: Float,
        highlighted: Boolean = false,
        action: () -> Unit
    ) = Button(this).apply {
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
            if (highlighted) Color.rgb(103, 66, 185)
            else Color.rgb(69, 53, 105),
            dp(8).toFloat()
        )

        setOnClickListener { action() }
    }

    private fun equalParams() = LinearLayout.LayoutParams(
        0, dp(42), 1f
    ).apply {
        setMargins(dp(2), dp(2), dp(2), dp(2))
    }

    private fun roundedBackground(
        color: Int,
        radius: Float
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        stopped = true
        handler.removeCallbacksAndMessages(null)
        removePanel()
        super.onDestroy()
    }
}
