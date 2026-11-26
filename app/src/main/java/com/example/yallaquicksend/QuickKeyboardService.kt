
package com.example.yallaquicksend

import android.inputmethodservice.InputMethodService
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.util.ArrayDeque

class QuickKeyboardService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())

    private var numbersMode = false
    private var uppercaseMode = false
    private var autoDrop = false
    private var speedBased = true
    private var enterSend = true
    private var stopped = false
    private var keyboardSize = 1
    private var delayMs = 0
    private var alphabetCount = 1

    private lateinit var keyboardRoot: LinearLayout
    private var keyboardRows: LinearLayout? = null

    private val pendingCharacters = ArrayDeque<String>()
    private var autoSendInProgress = false

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        loadSettings()
    }

    private fun loadSettings() {
        autoDrop = prefs.getBoolean("autoDrop", false)
        speedBased = prefs.getBoolean("speedBased", true)
        enterSend = prefs.getBoolean("enterSend", true)
        keyboardSize = prefs.getInt("keyboardSize", 1).coerceIn(0, 2)
        delayMs = prefs.getInt("delay", 0).coerceIn(0, 1000)
        alphabetCount = prefs.getInt("alphabetCount", 1).coerceIn(1, 3)
        uppercaseMode = prefs.getBoolean("uppercaseMode", false)
    }

    override fun onCreateInputView(): View {
        loadSettings()
        return buildKeyboard()
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInputView(info, restarting)
        loadSettings()
        stopped = false
        renderKeys()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        handler.removeCallbacksAndMessages(null)
        pendingCharacters.clear()
        autoSendInProgress = false
        stopped = true
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        pendingCharacters.clear()
        autoSendInProgress = false
        super.onDestroy()
    }

    private fun buildKeyboard(): View {
        keyboardRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(Color.rgb(20, 20, 29))
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
        if (!::keyboardRoot.isInitialized) return

        loadSettings()

        val rows = keyboardRows ?: return
        rows.removeAllViews()

        if (numbersMode) {
            renderNumberPad(rows)
        } else {
            renderAlphabetKeys(rows)
        }
    }

    /*
     * Dono modes mein exactly 4 rows hoti hain.
     * Har row ki height keyboardSize se aati hai.
     * Isliye ABC/NUM switch karne par keyboard ki
     * row height aur total row count same rehte hain.
     */

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
            listOf("z", "x", "c", "v", "b", "n", "m")
        )

        addKeyRow(
            rows,
            listOf("123", "⌫", "SPACE", "SEND"),
            weights = listOf(1f, 1f, 2.5f, 1.3f)
        ) { key ->
            when (key) {
                "123" -> {
                    numbersMode = true
                    renderKeys()
                }
                "⌫" -> deleteCharacter()
                "SPACE" -> typeText(" ")
                "SEND" -> handleEnter()
            }
        }
    }

    private fun renderNumberPad(rows: LinearLayout) {
        addKeyRow(rows, listOf("1", "2", "3")) { typeText(it) }
        addKeyRow(rows, listOf("4", "5", "6")) { typeText(it) }
        addKeyRow(rows, listOf("7", "8", "9")) { typeText(it) }

        addKeyRow(
            rows,
            listOf("ABC", "⌫", "*", "0", "#", "SPACE", "SEND"),
            weights = listOf(1.2f, 1f, 0.8f, 1f, 0.8f, 1.4f, 1.2f)
        ) { key ->
            when (key) {
                "ABC" -> {
                    numbersMode = false
                    renderKeys()
                }
                "⌫" -> deleteCharacter()
                "SPACE" -> typeText(" ")
                "SEND" -> handleEnter()
                else -> typeText(key)
            }
        }
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
            val displayKey = when {
                key.length == 1 && key[0].isLetter() ->
                    if (uppercaseMode) key.uppercase() else key.lowercase()
                else -> key
            }

            val button = makeButton(
                displayKey,
                when (key) {
                    "SPACE" -> 11f
                    "SEND" -> 12f
                    "123", "ABC" -> 12f
                    "⌫" -> 18f
                    else -> keyTextSize()
                },
                highlighted = key == "SEND"
            ) {
                if (customAction != null) {
                    customAction(key)
                } else {
                    typeText(displayKey)
                }
            }

            row.addView(
                button,
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
            typeface = Typeface.DEFAULT
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0

            setPadding(dp(1), dp(1), dp(1), dp(1))
            stateListAnimator = null

            background = GradientDrawable().apply {
                cornerRadius = dp(7).toFloat()
                setColor(
                    if (highlighted) {
                        Color.rgb(103, 66, 185)
                    } else {
                        Color.rgb(48, 47, 63)
                    }
                )
            }

            setOnClickListener {
                if (!stopped) action()
            }
        }
    }

    private fun typeText(value: String) {
        loadSettings()
        if (stopped) return

        val connection = currentInputConnection ?: return

        if (!autoDrop) {
            connection.commitText(value, 1)
            return
        }

        if (value.isBlank()) return

        pendingCharacters.addLast(value)
        processNextAutoSend()
    }

    private fun processNextAutoSend() {
        if (stopped || autoSendInProgress || pendingCharacters.isEmpty()) {
            return
        }

        loadSettings()

        if (!autoDrop) {
            pendingCharacters.clear()
            return
        }

        if (pendingCharacters.size < alphabetCount) return

        val connection = currentInputConnection ?: return
        val group = StringBuilder()

        repeat(alphabetCount) {
            if (pendingCharacters.isNotEmpty()) {
                group.append(pendingCharacters.removeFirst())
            }
        }

        if (group.isEmpty()) return

        autoSendInProgress = true
        connection.commitText(group.toString(), 1)

        handler.postDelayed({
            try {
                if (!stopped && autoDrop) {
                    sendAutomaticAction()
                }
            } finally {
                autoSendInProgress = false
                processNextAutoSend()
            }
        }, delayMs.coerceIn(0, 1000).toLong())
    }

    private fun deleteCharacter() {
        if (stopped) return

        val connection = currentInputConnection ?: return
        val deleted = connection.deleteSurroundingText(1, 0)

        if (!deleted) {
            connection.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
            )
            connection.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)
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
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
        )
        connection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
        )
    }

    private fun sendManualAction() {
        val service = YallaAccessibilityService.instance

        if (service == null) {
            showToast("FLASH: Accessibility Service ON karo")
            return
        }

        if (service.hasSavedSendPosition()) {
            service.tapSavedSendPosition()
        } else {
            service.clickVisibleSend()
        }
    }

    private fun sendAutomaticAction() {
        if (stopped || !autoDrop) return

        val service = YallaAccessibilityService.instance ?: return

        if (service.hasSavedSendPosition()) {
            service.tapSavedSendPosition()
        } else {
            service.clickVisibleSend()
        }
    }

    private fun keyTextSize(): Float {
        return when (keyboardSize) {
            0 -> 12f
            2 -> 19f
            else -> 16f
        }
    }

    private fun keyHeight(): Int {
        return when (keyboardSize) {
            0 -> 36
            2 -> 48
            else -> 42
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
