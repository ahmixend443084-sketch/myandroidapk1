
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
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class QuickKeyboardService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())

    private var numbersMode = false
    private var autoDrop = false
    private var speedBased = false
    private var enterSend = true
    private var stopped = false
    private var keyboardSize = 1
    private var delayMs = 300

    private lateinit var keyboardRoot: LinearLayout

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val autoSendTask = Runnable {
        loadSettings()
        if (autoDrop && !stopped) {
            sendAction()
        }
    }

    override fun onCreate() {
        super.onCreate()
        loadSettings()
    }

    private fun loadSettings() {
        autoDrop = prefs.getBoolean("autoDrop", false)
        speedBased = prefs.getBoolean("speedBased", false)
        enterSend = prefs.getBoolean("enterSend", true)
        keyboardSize = prefs.getInt("keyboardSize", 1).coerceIn(0, 2)
        delayMs = prefs.getInt("delay", 300).coerceIn(0, 60000)
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
        renderKeys()
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
            makeButton("⌫", 20f) {
                deleteCharacter()
            },
            keyParams(1f)
        )

        bottom.addView(
            makeButton("SPACE", 12f) {
                typeText(" ")
            },
            keyParams(3f)
        )

        bottom.addView(
            makeButton("↵", 20f, true) {
                handleEnter()
            },
            keyParams(1f)
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
                    makeButton(number, 19f) {
                        typeText(number)
                    },
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
            makeButton("⌫", 23f) {
                deleteCharacter()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        actionColumn.addView(
            makeButton("↵", 23f, true) {
                handleEnter()
            },
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
            makeButton("SPACE", 12f) {
                typeText(" ")
            },
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
        loadSettings()

        val connection = currentInputConnection ?: return
        connection.commitText(value, 1)

        if (autoDrop && !stopped) {
            handler.removeCallbacks(autoSendTask)

            val wait = if (speedBased) {
                (delayMs / 2).coerceAtLeast(50)
            } else {
                delayMs
            }

            handler.postDelayed(autoSendTask, wait.toLong())
        }
    }

    private fun deleteCharacter() {
        val connection = currentInputConnection ?: return

        if (!connection.deleteSurroundingText(1, 0)) {
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
            sendAction()
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

    private fun sendAction() {
        handler.removeCallbacks(autoSendTask)

        val connection: InputConnection =
            currentInputConnection ?: return

        val info = currentInputEditorInfo
        val actionId = info?.imeOptions?.and(
            EditorInfo.IME_MASK_ACTION
        ) ?: EditorInfo.IME_ACTION_UNSPECIFIED

        // Try the action requested by the target app first.
        if (actionId != EditorInfo.IME_ACTION_UNSPECIFIED &&
            actionId != EditorInfo.IME_ACTION_NONE &&
            connection.performEditorAction(actionId)
        ) {
            return
        }

        // Some chat fields advertise no action or a different action.
        if (connection.performEditorAction(EditorInfo.IME_ACTION_SEND)) {
            return
        }

        // Last fallback. Some apps treat Enter as Send; others insert a newline.
        sendEnterKey()
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
