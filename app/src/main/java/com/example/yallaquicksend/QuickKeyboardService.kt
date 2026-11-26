
package com.example.yallaquicksend

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class QuickKeyboardService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())

    private var numbersMode = false
    private var uppercaseMode = false
    private var autoDrop = false
    private var speedBased = true
    private var enterSend = true
    private var stopped = true

    private var keyboardSize = 100
    private var delayMs = 50
    private var alphabetCount = 26

    private lateinit var keyboardRoot: LinearLayout
    private var keyboardRows: LinearLayout? = null

    private var autoDropCount = 0
    private var pendingAutoSends = 0
    private var autoSendInProgress = false

    private val maxPendingAutoSends = 20

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val green = Color.rgb(0, 195, 105)
    private val keyColor = Color.rgb(48, 49, 62)
    private val textColor = Color.WHITE

    // Settings save hote hi open keyboard ko refresh karo.
    private val settingsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key == "keyboardSize" ||
                key == "autoDrop" ||
                key == "speedBased" ||
                key == "enterSend" ||
                key == "delay" ||
                key == "alphabetCount" ||
                key == "uppercaseMode"
            ) {
                handler.post {
                    if (::keyboardRoot.isInitialized && !stopped) {
                        renderKeys()
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        loadSettings()
        prefs.registerOnSharedPreferenceChangeListener(settingsListener)
    }

    private fun loadSettings() {
        autoDrop = prefs.getBoolean("autoDrop", false)
        speedBased = prefs.getBoolean("speedBased", true)
        enterSend = prefs.getBoolean("enterSend", true)

        keyboardSize = prefs.getInt("keyboardSize", 100)
            .coerceIn(50, 200)

        delayMs = prefs.getInt("delay", 50)
            .coerceIn(0, 5000)

        alphabetCount = prefs.getInt("alphabetCount", 26)
            .coerceIn(1, 100)

        uppercaseMode = prefs.getBoolean("uppercaseMode", false)
    }

    override fun onCreateInputView(): View {
        loadSettings()
        stopped = false
        return buildKeyboard()
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInputView(info, restarting)

        loadSettings()
        stopped = false

        if (::keyboardRoot.isInitialized) {
            renderKeys()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopped = true
        stopPendingActions()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        stopped = true
        stopPendingActions()
        prefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
        super.onDestroy()
    }

    private fun stopPendingActions() {
        handler.removeCallbacksAndMessages(null)
        autoDropCount = 0
        pendingAutoSends = 0
        autoSendInProgress = false
    }

    private fun buildKeyboard(): View {
        keyboardRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(5), dp(5), dp(5))

            background = GradientDrawable().apply {
                cornerRadius = dp(17).toFloat()
                setColor(Color.rgb(20, 21, 27))
                setStroke(dp(2), Color.rgb(49, 210, 91))
            }
        }

        keyboardRows = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        keyboardRoot.addView(
            keyboardRows,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        renderKeys()
        return keyboardRoot
    }

    private fun renderKeys() {
        if (!::keyboardRoot.isInitialized || stopped) return

        loadSettings()

        val rows = keyboardRows ?: return
        rows.removeAllViews()

        addToolbar(rows)

        if (numbersMode) {
            renderNumberPad(rows)
        } else {
            renderAlphabetKeys(rows)
        }

        keyboardRoot.requestLayout()
        keyboardRows?.requestLayout()
    }

    private fun addToolbar(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        addToolbarButton(row, "ABC", !numbersMode, 1f) {
            if (numbersMode) {
                numbersMode = false
                renderKeys()
            }
        }

        addToolbarButton(row, "123", numbersMode, 1f) {
            if (!numbersMode) {
                numbersMode = true
                renderKeys()
            }
        }

        addToolbarButton(row, "😊", false, 0.8f) {
            typeText("😊")
        }

        addToolbarButton(row, "⚙", false, 0.8f) {
            showToast("FLASH keyboard settings app mein kholen")
        }

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(keyHeight() + 4)
            )
        )
    }

    private fun addToolbarButton(
        row: LinearLayout,
        label: String,
        active: Boolean,
        weight: Float,
        action: () -> Unit
    ) {
        val button = makeButton(
            label,
            if (label.length > 2) scaledTextSize(14f)
            else scaledTextSize(19f),
            active,
            action
        )

        row.addView(
            button,
            LinearLayout.LayoutParams(0, dp(keyHeight()), weight).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )
    }

    private fun renderAlphabetKeys(rows: LinearLayout) {
        addKeyRow(
            rows,
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        )

        addKeyRow(
            rows,
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        )

        addKeyRow(
            rows,
            listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "⌫"),
            listOf(1.35f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1.25f)
        ) { key ->
            when (key) {
                "SHIFT" -> {
                    uppercaseMode = !uppercaseMode

                    prefs.edit()
                        .putBoolean("uppercaseMode", uppercaseMode)
                        .apply()

                    renderKeys()
                }

                "⌫" -> deleteCharacter()

                else -> typeText(
                    if (uppercaseMode) key.uppercase()
                    else key
                )
            }
        }

        addKeyRow(
            rows,
            listOf("?123", ",", "SPACE", ".", "SEND"),
            listOf(1.35f, 0.75f, 3.5f, 0.7f, 1.5f)
        ) { key ->
            when (key) {
                "?123" -> {
                    numbersMode = true
                    renderKeys()
                }

                "," -> typeText(",")
                "SPACE" -> typeText(" ")
                "." -> typeText(".")
                "SEND" -> handleEnter()
            }
        }
    }

    private fun renderNumberPad(rows: LinearLayout) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("-", "0", ".")
        ).forEach { keys ->
            addKeyRow(grid, keys) {
                typeText(it)
            }
        }

        content.addView(
            grid,
            LinearLayout.LayoutParams(
                0,
                dp(totalKeyRowsHeight()),
                3f
            )
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        actions.addView(
            makeButton("⌫", scaledTextSize(25f)) {
                deleteCharacter()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(keyHeight())
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        actions.addView(
            makeButton("➤\nSEND", scaledTextSize(17f), true) {
                handleEnter()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                3f
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        content.addView(
            actions,
            LinearLayout.LayoutParams(
                0,
                dp(totalKeyRowsHeight()),
                1.25f
            )
        )

        rows.addView(
            content,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(totalKeyRowsHeight())
            )
        )
    }

    private fun addKeyRow(
        parent: LinearLayout,
        keys: List<String>,
        weights: List<Float> = List(keys.size) { 1f },
        customAction: ((String) -> Unit)? = null
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        keys.forEachIndexed { index, key ->
            val display = when (key) {
                "SHIFT" -> if (uppercaseMode) "⬆" else "⇧"
                "SPACE" -> "English"

                else -> if (key.length == 1 && key[0].isLetter()) {
                    if (uppercaseMode) key.uppercase()
                    else key
                } else {
                    key
                }
            }

            val size = when (key) {
                "SPACE" -> scaledTextSize(11f)
                "SEND", "?123" -> scaledTextSize(12f)
                "SHIFT", "⌫" -> scaledTextSize(19f)
                else -> keyTextSize()
            }

            row.addView(
                makeButton(display, size, key == "SEND") {
                    if (customAction != null) {
                        customAction(key)
                    } else {
                        typeText(display)
                    }
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(keyHeight()),
                    weights.getOrElse(index) { 1f }
                ).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
            )
        }

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(keyHeight() + 4)
            )
        )
    }

    private fun makeButton(
        label: String,
        textSize: Float,
        highlighted: Boolean = false,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            this.textSize = textSize
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER

            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0

            setPadding(dp(1), dp(1), dp(1), dp(1))
            stateListAnimator = null

            background = GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(if (highlighted) green else keyColor)
            }

            setOnClickListener {
                if (!stopped) action()
            }
        }
    }

    private fun typeText(value: String) {
        if (stopped || value.isEmpty()) return

        val connection = currentInputConnection ?: return
        connection.commitText(value, 1)

        if (!autoDrop) {
            autoDropCount = 0
            return
        }

        val countable = value.count { !it.isWhitespace() }
        if (countable == 0) return

        autoDropCount += countable

        while (autoDropCount >= alphabetCount) {
            autoDropCount -= alphabetCount
            queueAutomaticSend()
        }
    }

    private fun queueAutomaticSend() {
        if (stopped || !autoDrop) return
        if (pendingAutoSends >= maxPendingAutoSends) return

        pendingAutoSends++
        processAutoSendQueue()
    }

    private fun processAutoSendQueue() {
        if (stopped || autoSendInProgress || pendingAutoSends <= 0) {
            return
        }

        loadSettings()

        if (!autoDrop) {
            pendingAutoSends = 0
            autoDropCount = 0
            return
        }

        autoSendInProgress = true

        val waitMs = if (speedBased) {
            delayMs.coerceAtLeast(15)
        } else {
            delayMs.coerceAtLeast(50)
        }

        handler.postDelayed({
            try {
                if (!stopped && autoDrop) {
                    sendAutomaticAction()
                }
            } finally {
                pendingAutoSends =
                    (pendingAutoSends - 1).coerceAtLeast(0)

                autoSendInProgress = false
                processAutoSendQueue()
            }
        }, waitMs.toLong())
    }

    private fun deleteCharacter() {
        if (stopped) return

        val connection = currentInputConnection ?: return
        val deleted = connection.deleteSurroundingText(1, 0)

        if (!deleted) {
            connection.sendKeyEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DEL
                )
            )

            connection.sendKeyEvent(
                KeyEvent(
                    KeyEvent.ACTION_UP,
                    KeyEvent.KEYCODE_DEL
                )
            )
        }
    }

    private fun handleEnter() {
        if (stopped) return

        loadSettings()

        if (enterSend) {
            sendManualAction()
        } else {
            sendEnterKey()
        }
    }

    private fun sendEnterKey() {
        val connection = currentInputConnection ?: return

        connection.sendKeyEvent(
            KeyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER
            )
        )

        connection.sendKeyEvent(
            KeyEvent(
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_ENTER
            )
        )
    }

    private fun sendManualAction() {
        val service = YallaAccessibilityService.instance

        if (service == null) {
            showToast("FLASH: Accessibility Service ON karo")
            return
        }

        val sent = if (service.hasSavedSendPosition()) {
            service.tapSavedSendPosition() ||
                service.clickVisibleSend()
        } else {
            service.clickVisibleSend()
        }

        if (!sent) {
            showToast(
                "Send nahi hua: Yalla screen aur saved position check karo"
            )
        }
    }

    private fun sendAutomaticAction() {
        if (stopped || !autoDrop) return

        val service = YallaAccessibilityService.instance ?: return

        if (service.hasSavedSendPosition()) {
            service.tapSavedSendPosition() ||
                service.clickVisibleSend()
        } else {
            service.clickVisibleSend()
        }
    }

    // Alphabet aur number-pad dono mein har key row ki height same hai.
    private fun keyHeight(): Int {
        return (42f * keyboardSize / 100f)
            .toInt()
            .coerceIn(28, 70)
    }

    // Number-pad ke 4 rows ki height.
    private fun totalKeyRowsHeight(): Int {
        return (keyHeight() + 4) * 4
    }

    private fun scaledTextSize(baseSize: Float): Float {
        return (baseSize * keyboardSize / 100f)
            .coerceIn(9f, 30f)
    }

    private fun keyTextSize(): Float {
        return scaledTextSize(16f)
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
