
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
import kotlin.math.abs

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
            background = outline
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            contentDescription = "FLASH moon logo"
            elevation = dp(4).toFloat()

            val logoId = resources.getIdentifier(
                "ximena_icon",
                "drawable",
                packageName
            )

            if (logoId != 0) {
                setImageResource(logoId)
            } else {
                setImageDrawable(null)
            }
        }
    }

    private fun showMainPanel() {
        removePanelOnly()
        removeTabOnly()

        settingsVisible = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(8))
            background = rounded(backgroundColor, 16)
            elevation = dp(10).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            createLogo(34),
            LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                rightMargin = dp(6)
            }
        )

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        brand.addView(TextView(this).apply {
            text = "FLASH 🌙"
            textSize = 17f
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.BOLD
            )
            setTextColor(lightPurple)
        })

        brand.addView(TextView(this).apply {
            text = "XIMENA • QUICK SEND"
            textSize = 8f
            setTextColor(muted)
        })

        header.addView(
            brand,
            LinearLayout.LayoutParams(0, dp(40), 1f)
        )

        header.addView(panelButton("⚙", 15f) {
            settingsVisible = !settingsVisible
            renderPanel()
        })

        header.addView(panelButton("—", 15f) {
            hidePanel()
        })

        header.addView(panelButton("×", 17f) {
            prefs.edit().putBoolean("panelEnabled", false).apply()
            stopSelf()
        })

        root.addView(header)

        root.addView(
            View(this).apply {
                setBackgroundColor(Color.rgb(95, 42, 155))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(5)
                bottomMargin = dp(5)
            }
        )

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "panel_content"
        })

        panelView = root

        val params = newWindowParams(
            dp(260),
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            x = dp(6)
            y = dp(100)
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

    // One shared count for the panel and keyboard.
    // alphabetCount is the keyboard's existing preference key.
    private fun getAutoDropCount(): Int {
        val count = prefs.getInt(
            "alphabetCount",
            prefs.getInt("autoDropQuantity", 3)
        ).coerceIn(1, 10)

        return count
    }

    private fun saveAutoDropCount(count: Int) {
        val safeCount = count.coerceIn(1, 10)

        prefs.edit()
            .putInt("alphabetCount", safeCount)
            .putInt("autoDropQuantity", safeCount)
            .apply()
    }

    private fun renderPanel() {
        val root = panelView ?: return
        val content = root.findViewWithTag<LinearLayout>("panel_content")
            ?: return

        content.removeAllViews()

        if (settingsVisible) {
            addSettings(content)
            return
        }

        val positionSaved = prefs.getBoolean("sendPositionSaved", false)

        content.addView(
            panelButton("◎  SET SEND POSITION", 12f, true) {
                startPositionSelection()
            },
            blockParams(dp(3))
        )

        content.addView(
            panelLabel(
                if (positionSaved) "● Send position saved"
                else "○ Send position not set",
                if (positionSaved) green else muted
            )
        )

        // Auto Drop count: 1–10.
        val autoDropQuantity = getAutoDropCount()

        content.addView(
            panelLabel("⬇  Auto Drop After: $autoDropQuantity characters")
        )

        val countRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        countRow.addView(
            panelButton("−", 16f) {
                val next = (getAutoDropCount() - 1).coerceAtLeast(1)
                saveAutoDropCount(next)
                renderPanel()
            },
            equalParams()
        )

        countRow.addView(
            panelButton("$autoDropQuantity / 10", 12f, true) {
                toast("Auto Drop count: $autoDropQuantity")
            },
            equalParams()
        )

        countRow.addView(
            panelButton("+", 16f) {
                val next = (getAutoDropCount() + 1).coerceAtMost(10)
                saveAutoDropCount(next)
                renderPanel()
            },
            equalParams()
        )

        content.addView(countRow)

        val keyboardSize = prefs.getInt("keyboardSize", 100)
            .coerceIn(50, 200)

        content.addView(
            panelLabel("⌨  Keyboard Size: $keyboardSize%")
        )

        val resizeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        resizeRow.addView(
            panelButton("−10%", 11f) {
                val next = (
                    prefs.getInt("keyboardSize", 100) - 10
                ).coerceAtLeast(50)

                prefs.edit().putInt("keyboardSize", next).apply()
                renderPanel()
                toast("Keyboard size: $next%")
            },
            equalParams()
        )

        resizeRow.addView(
            panelButton("RESET", 11f) {
                prefs.edit().putInt("keyboardSize", 100).apply()
                renderPanel()
                toast("Keyboard size 100%")
            },
            equalParams()
        )

        resizeRow.addView(
            panelButton("+10%", 11f) {
                val next = (
                    prefs.getInt("keyboardSize", 100) + 10
                ).coerceAtMost(200)

                prefs.edit().putInt("keyboardSize", next).apply()
                renderPanel()
                toast("Keyboard size: $next%")
            },
            equalParams()
        )

        content.addView(resizeRow)

        addToggle(content, "Speed Based", "speedBased", true)

        val delay = prefs.getInt("delay", 50).coerceIn(0, 5000)

        content.addView(
            panelLabel("◷  Auto Drop Delay: $delay ms")
        )

        val delayRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        delayRow.addView(
            panelButton("−100 ms", 11f) {
                val next = (
                    prefs.getInt("delay", 50) - 100
                ).coerceIn(0, 5000)

                prefs.edit().putInt("delay", next).apply()
                renderPanel()
            },
            equalParams()
        )

        delayRow.addView(
            panelButton("+100 ms", 11f) {
                val next = (
                    prefs.getInt("delay", 50) + 100
                ).coerceIn(0, 5000)

                prefs.edit().putInt("delay", next).apply()
                renderPanel()
            },
            equalParams()
        )

        content.addView(delayRow)

        addToggle(content, "Auto Drop", "autoDrop", false)

        content.addView(
            panelButton("■  STOP AUTO DROP", 11f) {
                stopAutoDrop()
            },
            blockParams(dp(3))
        )

        content.addView(
            panelButton("◉  HIDE PANEL", 11f, true) {
                hidePanel()
            },
            blockParams(dp(3))
        )
    }

    private fun addSettings(content: LinearLayout) {
        content.addView(
            createLogo(44),
            LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(5)
            }
        )

        content.addView(
            panelLabel("⚙  FLASH SETTINGS", lightPurple)
        )

        addToggle(content, "Enter key sends message", "enterSend", true)
        addToggle(content, "Speed Based", "speedBased", true)
        addToggle(content, "Auto Drop", "autoDrop", false)

        content.addView(
            panelButton("BACK TO PANEL", 12f, true) {
                settingsVisible = false
                renderPanel()
            },
            blockParams(dp(5))
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
            LinearLayout.LayoutParams(0, dp(36), 1f)
        )

        val enabled = prefs.getBoolean(key, defaultValue)

        row.addView(
            panelButton(
                if (enabled) "ON" else "OFF",
                11f,
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

        if (!enabled) {
            // Do not leave queued actions running after turning it off.
            toast("Auto Drop OFF")
        } else {
            toast("Auto Drop ON")
        }

        renderPanel()
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
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(white)
            background = rounded(purple, 12)
            elevation = dp(7).toFloat()
        }

        val markerWidth = dp(76)
        val markerHeight = dp(52)

        val params = newWindowParams(
            markerWidth,
            markerHeight,
            Gravity.TOP or Gravity.START
        ).apply {
            x = dp(100)
            y = dp(300)
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        markerView = marker
        markerParams = params

        marker.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> true

                MotionEvent.ACTION_MOVE -> {
                    params.x = (
                        event.rawX - markerWidth / 2f
                    ).toInt().coerceAtLeast(0)

                    params.y = (
                        event.rawY - markerHeight / 2f
                    ).toInt().coerceAtLeast(0)

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
            text = "🌙 FLASH"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(white)
            setPadding(dp(9), dp(8), dp(9), dp(8))
            background = rounded(purple, 18)
            elevation = dp(8).toFloat()
        }

        val params = newWindowParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            dp(42),
            Gravity.TOP or Gravity.START
        ).apply {
            x = prefs.getInt("tabX", dp(3)).coerceAtLeast(0)
            y = prefs.getInt("tabY", dp(180)).coerceAtLeast(0)
        }

        tabView = tab
        tabParams = params

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragged = false

        tab.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()

                    if (abs(dx) > dp(5) || abs(dy) > dp(5)) {
                        dragged = true
                    }

                    if (dragged) {
                        val screenWidth =
                            resources.displayMetrics.widthPixels

                        val screenHeight =
                            resources.displayMetrics.heightPixels

                        params.x = (startX + dx).coerceIn(
                            0,
                            (screenWidth - view.width).coerceAtLeast(0)
                        )

                        params.y = (startY + dy).coerceIn(
                            0,
                            (screenHeight - view.height).coerceAtLeast(0)
                        )

                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) {
                        }
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (dragged) {
                        val screenWidth =
                            resources.displayMetrics.widthPixels

                        val viewWidth = view.width

                        // Snap the chip to the nearest side.
                        params.x = if (
                            params.x + viewWidth / 2 < screenWidth / 2
                        ) {
                            0
                        } else {
                            (screenWidth - viewWidth).coerceAtLeast(0)
                        }

                        prefs.edit()
                            .putInt("tabX", params.x)
                            .putInt("tabY", params.y)
                            .apply()

                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) {
                        }
                    } else {
                        showMainPanel()
                    }

                    true
                }

                else -> true
            }
        }

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
            textSize = 11f
            setTextColor(textColor)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(5), dp(4), dp(5), dp(4))
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
            setPadding(dp(4), dp(3), dp(4), dp(3))

            background = rounded(
                if (highlighted) purple else card,
                10
            )

            stateListAnimator = null
            setOnClickListener { action() }
        }
    }

    private fun equalParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, dp(34), 1f).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
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
