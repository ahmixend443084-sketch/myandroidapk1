
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
import android.widget.TextView
import android.widget.Toast

class QuickKeyboardService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())

    private var numbersMode = false
    private var autoDrop = false
    private var speedBased = true
    private var enterSend = true
    private var stopped = false
    private var keyboardSize = 1
    private var delayMs = 100

    private lateinit var keyboardRoot: LinearLayout

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val autoSendTask = object : Runnable {
        override fun run() {
            loadSettings()

            if (autoDrop && !stopped) {
                sendAutomaticAction()

                val nextDelay = if (speedBased) {
                    delayMs.coerceIn(100, 1000)
                } else {
                    100
                }

                if (autoDrop && !stopped) {
                    handler.postDelayed(this, nextDelay.toLong())
                }
            }
        }
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
        delayMs = prefs.getInt("delay", 100).coerceIn(100, 1000)
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
        handler.removeCallbacks(autoSendTask)

        if (::keyboardRoot.isInitialized) {
            renderKeys()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        handler.removeCallbacks(autoSendTask)
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun buildKeyboard(): View {
        keyboardRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(Color.rgb(20, 20, 29))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "𝙁𝙇𝘼𝙎𝙃🌙"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(190, 160, 255))
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(38), 1f)
        )

        header.addView(
            makeButton("ABC", 12f) {
                numbersMode = false
                renderKeys()
            },
            smallButtonParams()
        )

        header.addView(
            makeButton("123", 12f) {
                numbersMode = true
                renderKeys()
            },
            smallButtonParams()
        )

        keyboardRoot.addView(header)
        renderKeys()
        return keyboardRoot
    }

    private fun renderKeys() {
        if (!::keyboardRoot.isInitialized) return

        while (keyboardRoot.childCount > 1) {
            keyboardRoot.removeViewAt(1)
        }

        if (numbersMode) {
            renderNumberPad()
        } else {
            renderAlphabetKeys()
        }
    }

    private fun renderAlphabetKeys() {
        addKeyRow(listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"))
        addKeyRow(listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"))
        addKeyRow(listOf("Z", "X", "C", "V", "B", "N", "M"))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        bottom.addView(
            makeButton("⌫", 20f) { deleteCharacter() },
            keyParams(1f)
        )

        bottom.addView(
            makeButton("SPACE", 12f) { typeText(" ") },
            keyParams(3f)
        )

        bottom.addView(
            makeButton("SEND", 14f, true) { handleEnter() },
            keyParams(1.5f)
        )

        keyboardRoot.addView(bottom)
    }

    private fun renderNumberPad() {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val numberColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val numberRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )

        numberRows.forEach { numbers ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            numbers.forEach { number ->
                row.addView(
                    makeButton(number, 19f) { typeText(number) },
                    keyParams(1f)
                )
            }

            numberColumn.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
        }

        body.addView(
            numberColumn,
            LinearLayout.LayoutParams(0, dp(184), 3f)
        )

        val actionColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        actionColumn.addView(
            makeButton("⌫", 23f) { deleteCharacter() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        actionColumn.addView(
            makeButton("SEND", 13f, true) { handleEnter() },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                2f
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        body.addView(
            actionColumn,
            LinearLayout.LayoutParams(0, dp(184), 1f)
        )

        keyboardRoot.addView(body)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        bottom.addView(
            makeButton("ABC", 12f) {
                numbersMode = false
                renderKeys()
            },
            keyParams(1f)
        )

        bottom.addView(
            makeButton("SPACE", 12f) { typeText(" ") },
            keyParams(2f)
        )

        keyboardRoot.addView(bottom)
    }

    private fun addKeyRow(keys: List<String>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        keys.forEach { key ->
            row.addView(
                makeButton(key, keyTextSize()) {
                    typeText(key.lowercase())
                },
                keyParams(1f)
            )
        }

        keyboardRoot.addView(row)
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
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(1), dp(1), dp(1), dp(1))

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

            setOnClickListener { action() }
        }
    }

    private fun keyParams(weight: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            dp(keyHeight()),
            weight
        ).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
    }

    private fun smallButtonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            dp(45),
            dp(36)
        ).apply {
            setMargins(dp(2), dp(1), dp(2), dp(1))
        }
    }

    private fun typeText(value: String) {
        val connection = currentInputConnection ?: return
        connection.commitText(value, 1)
    }

    private fun deleteCharacter() {
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
        loadSettings()

        if (enterSend) {
            // Manual Enter: saved position ko foran tap karo.
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

        val sent = if (service.hasSavedSendPosition()) {
            service.tapSavedSendPosition()
        } else {
            service.clickVisibleSend()
        }

        if (!sent) {
            showToast(
                if (service.hasSavedSendPosition()) {
                    "FLASH: Saved position par tap nahi hua"
                } else {
                    "FLASH: Send button nahi mila; position set karo"
                }
            )
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
