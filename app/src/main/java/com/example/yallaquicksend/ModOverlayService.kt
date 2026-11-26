
package com.example.yallaquicksend

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ModOverlayService : Service() {

    private lateinit var windowManager: WindowManager

    private var panelView: LinearLayout? = null
    private var tabView: TextView? = null
    private var markerView: TextView? = null

    private var panelParams: WindowManager.LayoutParams? = null
    private var tabParams: WindowManager.LayoutParams? = null
    private var markerParams: WindowManager.LayoutParams? = null

    private var settingsVisible = false

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
            toast("FLASH ko Display over other apps permission do")
            stopSelf()
            return START_NOT_STICKY
        }

        windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (panelView == null && tabView == null) {
            showMainPanel()
        }

        return START_NOT_STICKY
    }

    private fun overlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun newWindowParams(
        width: Int,
        height: Int,
        gravityValue: Int = Gravity.TOP or Gravity.END
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = gravityValue
        }
    }

    private fun showMainPanel() {
        removePanelOnly()
        removeTabOnly()

        settingsVisible = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(8))
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
            LinearLayout.LayoutParams(0, dp(40), 1f)
        )

        header.addView(panelButton("⚙", 14f) {
            settingsVisible = !settingsVisible
            renderPanel()
        })

        header.addView(panelButton("—", 16f) {
            hidePanel()
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

        val params = newWindowParams(
            dp(235),
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            x = dp(8)
            y = dp(110)
        }

        panelParams = params

        try {
            windowManager.addView(root, params)
            renderPanel()
        } catch (_: Exception) {
            panelView = null
            panelParams = null
            toast("FLASH panel start nahi hua")
            stopSelf()
        }
    }

    private fun renderPanel() {
        val root = panelView ?: return

        val content =
            root.findViewWithTag<LinearLayout>("panel_content") ?: return

        content.removeAllViews()

        if (settingsVisible) {
            addSettings(content)
            return
        }

        content.addView(
            panelButton("SET SEND POSITION", 12f, true) {
                startPositionSelection()
            }
        )

        content.addView(
            panelLabel(
                if (prefs.getBoolean("sendPositionSaved", false)) {
                    "Send position: SAVED"
                } else {
                    "Send position: NOT SET"
                }
            )
        )

        // Alphabet count selector: 1, 2, or 3
        val alphabetCount = prefs
            .getInt("alphabetCount", 1)
            .coerceIn(1, 3)

        content.addView(panelLabel("Alphabet Count: $alphabetCount"))

        val countRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        for (count in 1..3) {
            countRow.addView(
                panelButton(
                    count.toString(),
                    13f,
                    alphabetCount == count
                ) {
                    prefs.edit()
                        .putInt("alphabetCount", count)
                        .apply()
                    renderPanel()
                },
                equalParams()
            )
        }

        content.addView(countRow)

        content.addView(
            panelButton(
                if (prefs.getBoolean("speedBased", true)) {
                    "SPEED BASED: ON"
                } else {
                    "SPEED BASED: OFF"
                },
                12f
            ) {
                val next = !prefs.getBoolean("speedBased", true)
                prefs.edit().putBoolean("speedBased", next).apply()
                renderPanel()
            }
        )

        // Delay supports 0 ms through 1000 ms.
        val delay = prefs.getInt("delay", 0).coerceIn(0, 1000)

        content.addView(panelLabel("Auto Drop Delay: $delay ms"))

        val delayRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        delayRow.addView(
            panelButton("− 100 ms", 11f) {
                val next = (
                    prefs.getInt("delay", 0) - 100
                ).coerceIn(0, 1000)

                prefs.edit().putInt("delay", next).apply()
                renderPanel()
            },
            equalParams()
        )

        delayRow.addView(
            panelButton("+ 100 ms", 11f) {
                val next = (
                    prefs.getInt("delay", 0) + 100
                ).coerceIn(0, 1000)

                prefs.edit().putInt("delay", next).apply()
                renderPanel()
            },
            equalParams()
        )

        content.addView(delayRow)

        content.addView(
            panelButton(
                if (prefs.getBoolean("autoDrop", false)) {
                    "AUTO DROP: ON"
                } else {
                    "AUTO DROP: OFF"
                },
                12f
            ) {
                setAutoDrop(!prefs.getBoolean("autoDrop", false))
            }
        )

        content.addView(panelButton("STOP AUTO DROP", 12f) {
            stopAutoDrop()
        })

        content.addView(panelButton("HIDE PANEL", 12f) {
            hidePanel()
        })
    }

    private fun addSettings(content: LinearLayout) {
        content.addView(panelLabel("FLASH SETTINGS"))

        addToggle(content, "Enter key sends message", "enterSend", true)
        addToggle(content, "Speed Based", "speedBased", true)
        addToggle(content, "Auto Drop", "autoDrop", false)

        content.addView(panelButton("BACK TO PANEL", 12f) {
            settingsVisible = false
            renderPanel()
        })
    }

    private fun addToggle(
        content: LinearLayout,
        label: String,
        key: String,
        defaultValue: Boolean
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

        row.addView(
            panelButton(if (enabled) "ON" else "OFF", 12f) {
                val next = !prefs.getBoolean(key, defaultValue)

                if (key == "autoDrop") {
                    setAutoDrop(next)
                } else {
                    prefs.edit().putBoolean(key, next).apply()
                    renderPanel()
                }
            }
        )

        content.addView(row)
    }

    // This only changes the Auto Drop setting.
    // It deliberately does NOT run a repeating Send-click loop.
    private fun setAutoDrop(enabled: Boolean) {
        prefs.edit().putBoolean("autoDrop", enabled).apply()
        renderPanel()

        toast(
            if (enabled) {
                "Auto Drop ON"
            } else {
                "Auto Drop OFF"
            }
        )
    }

    private fun stopAutoDrop() {
        prefs.edit().putBoolean("autoDrop", false).apply()
        renderPanel()
        toast("Auto Drop stopped")
    }

    private fun startPositionSelection() {
        if (YallaAccessibilityService.instance == null) {
            toast("Pehle FLASH Accessibility enable karo")
            return
        }

        removeMarkerOnly()

        val marker = TextView(this).apply {
            text = "DRAG\nTO SEND"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBackground(
                Color.rgb(103, 66, 185),
                dp(12).toFloat()
            )
            elevation = dp(8).toFloat()
        }

        val params = newWindowParams(
            dp(82),
            dp(58),
            Gravity.TOP or Gravity.START
        ).apply {
            x = dp(100)
            y = dp(300)
            flags =
                flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        markerView = marker
        markerParams = params

        marker.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> true

                MotionEvent.ACTION_MOVE -> {
                    params.x = (event.rawX - dp(41)).toInt()
                    params.y = (event.rawY - dp(29)).toInt()

                    try {
                        windowManager.updateViewLayout(marker, params)
                    } catch (_: Exception) {
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {
                    val x = (params.x + dp(41)).coerceAtLeast(0)
                    val y = (params.y + dp(29)).coerceAtLeast(0)

                    prefs.edit()
                        .putInt("sendPositionX", x)
                        .putInt("sendPositionY", y)
                        .putBoolean("sendPositionSaved", true)
                        .apply()

                    removeMarkerOnly()
                    renderPanel()

                    toast("Send position save ho gayi")
                    true
                }

                else -> true
            }
        }

        try {
            windowManager.addView(marker, params)
            toast("Marker ko Yalla ke Send button par drag karke chhor do")
        } catch (_: Exception) {
            markerView = null
            markerParams = null
            toast("Marker nahi khula. Overlay permission check karo.")
        }
    }

    private fun hidePanel() {
        removePanelOnly()
        showTab()
    }

    private fun showTab() {
        removeTabOnly()

        val tab = TextView(this).apply {
            text = "FLASH🌙"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = roundedBackground(
                Color.rgb(103, 66, 185),
                dp(18).toFloat()
            )

            setOnClickListener {
                showMainPanel()
            }
        }

        val params = newWindowParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            dp(46)
        ).apply {
            x = dp(4)
            y = dp(180)
        }

        tabView = tab
        tabParams = params

        try {
            windowManager.addView(tab, params)
        } catch (_: Exception) {
            tabView = null
            tabParams = null
            toast("Floating tab nahi khula")
        }
    }

    private fun panelLabel(value: String) = TextView(this).apply {
        text = value
        textSize = 12f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(5), dp(5), dp(5), dp(5))
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
        setPadding(dp(4), dp(2), dp(4), dp(2))

        background = roundedBackground(
            if (highlighted) {
                Color.rgb(103, 66, 185)
            } else {
                Color.rgb(69, 53, 105)
            },
            dp(8).toFloat()
        )

        setOnClickListener { action() }
    }

    private fun equalParams() = LinearLayout.LayoutParams(
        0,
        dp(40),
        1f
    ).apply {
        setMargins(dp(2), dp(2), dp(2), dp(2))
    }

    private fun roundedBackground(
        color: Int,
        radius: Float
    ) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun removePanelOnly() {
        val view = panelView ?: return

        try {
            if (::windowManager.isInitialized) {
                windowManager.removeView(view)
            }
        } catch (_: Exception) {
        }

        panelView = null
        panelParams = null
    }

    private fun removeTabOnly() {
        val view = tabView ?: return

        try {
            if (::windowManager.isInitialized) {
                windowManager.removeView(view)
            }
        } catch (_: Exception) {
        }

        tabView = null
        tabParams = null
    }

    private fun removeMarkerOnly() {
        val view = markerView ?: return

        try {
            if (::windowManager.isInitialized) {
                windowManager.removeView(view)
            }
        } catch (_: Exception) {
        }

        markerView = null
        markerParams = null
    }

    override fun onDestroy() {
        removePanelOnly()
        removeTabOnly()
        removeMarkerOnly()
        super.onDestroy()
    }
}
