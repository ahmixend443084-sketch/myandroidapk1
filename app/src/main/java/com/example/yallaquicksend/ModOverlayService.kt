
package com.example.yallaquicksend

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
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

    private val backgroundColor = Color.rgb(12, 10, 23)
    private val card = Color.rgb(29, 23, 48)
    private val purple = Color.rgb(130, 45, 245)
    private val lightPurple = Color.rgb(205, 170, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(178, 167, 202)
    private val green = Color.rgb(75, 225, 145)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            toast("Pehle Display over other apps permission allow karo")
            stopSelf()
            return START_NOT_STICKY
        }

        windowManager = getSystemService(
            Context.WINDOW_SERVICE
        ) as WindowManager

        prefs.edit().putBoolean("panelEnabled", true).apply()

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

    private fun createLogo(sizeDp: Int): ImageView {
        val outline = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(card)
            setStroke(dp(2), purple)
        }

        return ImageView(this).apply {
            setImageResource(R.drawable.ximena_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = outline
            clipToOutline = true
            contentDescription = "Ximena moon logo"
            elevation = dp(5).toFloat()
        }
    }

    private fun showMainPanel() {
        removePanelOnly()
        removeTabOnly()

        settingsVisible = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            background = rounded(backgroundColor, 20)
            elevation = dp(12).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            createLogo(44),
            LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                rightMargin = dp(9)
            }
        )

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        brand.addView(TextView(this).apply {
            text = "FLASH 🌙"
            textSize = 20f
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.BOLD
            )
            setTextColor(lightPurple)
        })

        brand.addView(TextView(this).apply {
            text = "XIMENA • QUICK SEND"
            textSize = 9f
            letterSpacing = 0.08f
            setTextColor(muted)
        })

        header.addView(
            brand,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        header.addView(panelButton("⚙", 17f) {
            settingsVisible = !settingsVisible
            renderPanel()
        })

        header.addView(panelButton("—", 17f) {
            hidePanel()
        })

        header.addView(panelButton("×", 19f) {
            prefs.edit().putBoolean("panelEnabled", false).apply()
            stopSelf()
        })

        root.addView(header)

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(95, 42, 155))
        }

        root.addView(
            divider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(2)
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
        )

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "panel_content"
        })

        panelView = root

        val params = newWindowParams(
            dp(290),
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
            toast("Panel open nahi hua. Overlay permission check karo.")
            stopSelf()
        }
    }

    private fun renderPanel() {
        val root = panelView ?: return

        val content = root.findViewWithTag<LinearLayout>(
            "panel_content"
        ) ?: return

        content.removeAllViews()

        if (settingsVisible) {
            addSettings(content)
            return
        }

        val positionSaved = prefs.getBoolean(
            "sendPositionSaved",
            false
        )

        content.addView(
            panelButton("◎   SET SEND POSITION", 14f, true) {
                startPositionSelection()
            },
            blockParams(dp(4))
        )

        content.addView(
            panelLabel(
                if (positionSaved) {
                    "● Send position saved"
                } else {
                    "○ Send position not set"
                },
                if (positionSaved) green else muted
            )
        )

        // Alphabet Count
        val alphabetCount = prefs.getInt(
            "alphabetCount",
            1
        ).coerceIn(1, 3)

        content.addView(
            panelLabel("Aa   Alphabet Count: $alphabetCount")
        )

        val countRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        for (count in 1..3) {
            countRow.addView(
                panelButton(
                    count.toString(),
                    14f,
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

        // Keyboard Resize
        val keyboardSize = prefs.getInt(
            "keyboardSize",
            100
        ).coerceIn(50, 200)

        content.addView(
            panelLabel("⌨   Keyboard Size: $keyboardSize%")
        )

        val resizeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        resizeRow.addView(
            panelButton("− 10%", 13f) {
                val current = prefs.getInt(
                    "keyboardSize",
                    100
                ).coerceIn(50, 200)

                val next = (current - 10).coerceAtLeast(50)

                prefs.edit()
                    .putInt("keyboardSize", next)
                    .apply()

                renderPanel()
                toast("Keyboard size: $next%")
            },
            equalParams()
        )

        resizeRow.addView(
            panelButton("RESET 100%", 11f) {
                prefs.edit()
                    .putInt("keyboardSize", 100)
                    .apply()

                renderPanel()
                toast("Keyboard size 100%")
            },
            equalParams()
        )

        resizeRow.addView(
            panelButton("+ 10%", 13f) {
                val current = prefs.getInt(
                    "keyboardSize",
                    100
                ).coerceIn(50, 200)

                val next = (current + 10).coerceAtMost(200)

                prefs.edit()
                    .putInt("keyboardSize", next)
                    .apply()

                renderPanel()
                toast("Keyboard size: $next%")
            },
            equalParams()
        )

        content.addView(resizeRow)

        // Speed Based
        val speed = prefs.getBoolean("speedBased", true)

        content.addView(
            panelButton(
                "⚡   SPEED BASED: ${if (speed) "ON" else "OFF"}",
                13f,
                speed
            ) {
                prefs.edit()
                    .putBoolean("speedBased", !speed)
                    .apply()
                renderPanel()
            },
            blockParams(dp(6))
        )

        // Auto Drop Delay
        val delay = prefs.getInt("delay", 0)
            .coerceIn(0, 60000)

        content.addView(
            panelLabel("◷   Auto Drop Delay: $delay ms")
        )

        val delayRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        delayRow.addView(
            panelButton("− 100 ms", 12f) {
                val next = (
                    prefs.getInt("delay", 0) - 100
                ).coerceIn(0, 60000)

                prefs.edit().putInt("delay", next).apply()
                renderPanel()
            },
            equalParams()
        )

        delayRow.addView(
            panelButton("+ 100 ms", 12f) {
                val next = (
                    prefs.getInt("delay", 0) + 100
                ).coerceIn(0, 60000)

                prefs.edit().putInt("delay", next).apply()
                renderPanel()
            },
            equalParams()
        )

        content.addView(delayRow)

        // Auto Drop
        val auto = prefs.getBoolean("autoDrop", false)

        content.addView(
            panelButton(
                "⚡   AUTO DROP: ${if (auto) "ON" else "OFF"}",
                13f,
                auto
            ) {
                setAutoDrop(!prefs.getBoolean("autoDrop", false))
            },
            blockParams(dp(6))
        )

        content.addView(
            panelButton("■   STOP AUTO DROP", 13f) {
                stopAutoDrop()
            },
            blockParams(dp(5))
        )

        content.addView(
            panelButton("◉   HIDE PANEL", 13f, true) {
                hidePanel()
            },
            blockParams(dp(5))
        )
    }

    private fun addSettings(content: LinearLayout) {
        content.addView(
            createLogo(54),
            LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(8)
            }
        )

        content.addView(
            panelLabel("⚙   FLASH SETTINGS", lightPurple)
        )

        addToggle(
            content,
            "Enter key sends message",
            "enterSend",
            true
        )

        addToggle(
            content,
            "Speed Based",
            "speedBased",
            true
        )

        addToggle(
            content,
            "Auto Drop",
            "autoDrop",
            false
        )

        content.addView(
            panelButton("BACK TO PANEL", 13f, true) {
                settingsVisible = false
                renderPanel()
            },
            blockParams(dp(8))
        )
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
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        val enabled = prefs.getBoolean(key, defaultValue)

        row.addView(
            panelButton(
                if (enabled) "ON" else "OFF",
                12f,
                enabled
            ) {
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

    private fun setAutoDrop(enabled: Boolean) {
        prefs.edit().putBoolean("autoDrop", enabled).apply()
        renderPanel()
        toast(if (enabled) "Auto Drop ON" else "Auto Drop OFF")
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
            setTextColor(white)
            background = rounded(purple, 14)
            elevation = dp(8).toFloat()
        }

        val markerWidth = dp(88)
        val markerHeight = dp(60)

        val params = newWindowParams(
            markerWidth,
            markerHeight,
            Gravity.TOP or Gravity.START
        ).apply {
            x = dp(100)
            y = dp(300)
            flags = flags or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        markerView = marker
        markerParams = params

        marker.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> true

                MotionEvent.ACTION_MOVE -> {
                    params.x = (
                        event.rawX - markerWidth / 2f
                    ).toInt()

                    params.y = (
                        event.rawY - markerHeight / 2f
                    ).toInt()

                    try {
                        windowManager.updateViewLayout(marker, params)
                    } catch (_: Exception) {
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {
                    val x = (
                        params.x + markerWidth / 2
                    ).coerceAtLeast(0)

                    val y = (
                        params.y + markerHeight / 2
                    ).coerceAtLeast(0)

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
            toast("Marker ko Yalla ke Send button par drag karo")
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
            text = "🌙 XIMENA"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(white)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(purple, 20)
            elevation = dp(10).toFloat()

            setOnClickListener {
                showMainPanel()
            }
        }

        val params = newWindowParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            dp(48)
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

    private fun panelLabel(
        value: String,
        textColor: Int = white
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(textColor)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(7), dp(6), dp(7))
        }
    }

    private fun panelButton(
        label: String,
        size: Float,
        highlighted: Boolean = false,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = size
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(5), dp(5), dp(5), dp(5))
            background = rounded(
                if (highlighted) purple else card,
                12
            )
            stateListAnimator = null
            setOnClickListener { action() }
        }
    }

    private fun equalParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            dp(42),
            1f
        ).apply {
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }
    }

    private fun blockParams(
        topMargin: Int = 0
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            this.topMargin = topMargin
        }
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), Color.rgb(76, 43, 120))
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_SHORT
        ).show()
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

        prefs.edit().putBoolean("panelEnabled", false).apply()

        super.onDestroy()
    }
}
