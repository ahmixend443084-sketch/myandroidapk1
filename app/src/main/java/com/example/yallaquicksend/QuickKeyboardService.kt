
package com.example.yallaquicksend

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.*
import android.graphics.Color
import android.os.Handler
import android.os.Looper

class QuickKeyboardService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())

    private var running = false
    private var index = 0
    private var delayMs = 500
    private var alphabetMode = true
    private var autoDrop = false

    private var keySize = 24
    private var keyHeight = 58
    private var keyboardHeight = 330

    private var digitCount = 1
    private var alphaCount = 1

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreateInputView(): View {
        loadSettings()
        return createKeyboard()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val sizeMode = prefs.getInt("keyboardSize", 1).coerceIn(0, 2)

        when (sizeMode) {
            0 -> {
                keySize = 20
                keyHeight = 48
                keyboardHeight = 280
            }
            1 -> {
                keySize = 24
                keyHeight = 58
                keyboardHeight = 330
            }
            2 -> {
                keySize = 28
                keyHeight = 66
                keyboardHeight = 380
            }
        }

        digitCount = prefs.getInt("digitCount", 1).coerceIn(1, 5)
        alphaCount = prefs.getInt("alphaCount", 1).coerceIn(1, 5)

        autoDrop = prefs.getBoolean("autoDrop", false)
        delayMs = prefs.getInt("delay", 500).coerceIn(150, 5000)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        loadSettings()

        window.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(keyboardHeight)
        )

        setInputView(createKeyboard())
    }

    private fun makeButton(
        label: String,
        size: Float,
        color: Int = Color.WHITE,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = size
            isAllCaps = false
            setTextColor(Color.BLACK)
            setBackgroundColor(color)
            setPadding(0, 0, 0, 0)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setOnClickListener { action() }
        }
    }

    private fun typeCharacter(value: String) {
        currentInputConnection?.commitText(value, 1)

        if (autoDrop) {
            sendAction(currentInputConnection)
        }
    }

    private fun addKeyRow(
        root: LinearLayout,
        keys: List<String>,
        height: Int = keyHeight
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        keys.forEach { key ->
            val button = makeButton(key, keySize.toFloat()) {
                when (key) {
                    "⌫" -> currentInputConnection
                        ?.deleteSurroundingText(1, 0)

                    "SEND" -> sendAction(currentInputConnection)

                    else -> typeCharacter(key)
                }
            }

            row.addView(
                button,
                LinearLayout.LayoutParams(0, dp(height), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
            )
        }

        root.addView(
            row,
            LinearLayout.LayoutParams(-1, dp(height))
        )
    }

    private fun createKeyboard(): View {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(4), dp(3), dp(4))
            setBackgroundColor(Color.rgb(215, 220, 230))
        }

        // TOP BAR
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        top.addView(makeButton("ABC", 12f) {
            alphabetMode = true
            index = 0
            setInputView(createKeyboard())
        }, LinearLayout.LayoutParams(0, dp(44), 1f))

        top.addView(makeButton("123", 12f) {
            alphabetMode = false
            index = 0
            setInputView(createKeyboard())
        }, LinearLayout.LayoutParams(0, dp(44), 1f))

        top.addView(
            makeButton(
                if (autoDrop) "DROP ON" else "DROP OFF",
                11f,
                if (autoDrop) Color.rgb(50, 170, 90) else Color.LTGRAY
            ) {
                autoDrop = !autoDrop

                getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("autoDrop", autoDrop)
                    .apply()

                setInputView(createKeyboard())
            },
            LinearLayout.LayoutParams(0, dp(44), 1.5f)
        )

        top.addView(makeButton("AUTO", 11f) {
            startAuto()
        }, LinearLayout.LayoutParams(0, dp(44), 1f))

        top.addView(makeButton("STOP", 11f) {
            running = false
            handler.removeCallbacks(autoTask)
        }, LinearLayout.LayoutParams(0, dp(44), 1f))

        root.addView(top)

        if (alphabetMode) {

            // NORMAL ALPHABET LAYOUT
            addKeyRow(root, "QWERTYUIOP".map { it.toString() })

            addKeyRow(
                root,
                "ASDFGHJKL".map { it.toString() }
            )

            addKeyRow(
                root,
                "ZXCVBNM".map { it.toString() }
            )

            val bottom = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            bottom.addView(
                makeButton("SPACE", 14f) {
                    currentInputConnection?.commitText(" ", 1)
                },
                LinearLayout.LayoutParams(0, dp(52), 2f)
            )

            bottom.addView(
                makeButton("⌫", 22f) {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                },
                LinearLayout.LayoutParams(0, dp(52), 1f)
            )

            bottom.addView(
                makeButton("SEND", 14f) {
                    sendAction(currentInputConnection)
                },
                LinearLayout.LayoutParams(0, dp(52), 1.5f)
            )

            root.addView(bottom)

        } else {

            // NORMAL NUMBER KEYBOARD
            // Same row-based layout as alphabet keyboard.

            addKeyRow(root, listOf("1", "2", "3"))
            addKeyRow(root, listOf("4", "5", "6"))
            addKeyRow(root, listOf("7", "8", "9"))

            addKeyRow(root, listOf(".", "0", "⌫"))

            val sendRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            sendRow.addView(
                makeButton(
                    "SEND",
                    15f,
                    Color.rgb(40, 160, 80)
                ) {
                    sendAction(currentInputConnection)
                },
                LinearLayout.LayoutParams(-1, dp(46))
            )

            root.addView(sendRow)
        }

        return root
    }

    private fun sendAction(ic: InputConnection?) {

        if (YallaAccessibilityService.instance
                ?.clickVisibleSend() == true
        ) return

        if (ic == null) return

        val action = currentInputEditorInfo?.imeOptions
            ?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_UNSPECIFIED

        if (
            action == EditorInfo.IME_ACTION_SEND ||
            action == EditorInfo.IME_ACTION_GO ||
            action == EditorInfo.IME_ACTION_DONE
        ) {
            ic.performEditorAction(action)
        } else {
            ic.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_ENTER
                )
            )
            ic.sendKeyEvent(
                android.view.KeyEvent(
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_ENTER
                )
            )
        }
    }

    private val autoTask = object : Runnable {
        override fun run() {

            if (!running) return

            val prefs = getSharedPreferences("settings", MODE_PRIVATE)

            val sequence = if (alphabetMode) {
                prefs.getString(
                    "letters",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                ) ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            } else {
                prefs.getString(
                    "numbers",
                    "1234567890"
                ) ?: "1234567890"
            }

            if (sequence.isNotEmpty()) {

                val count = if (alphabetMode) {
                    alphaCount
                } else {
                    digitCount
                }

                val value = buildString {
                    repeat(count) {
                        append(sequence[index++ % sequence.length])
                    }
                }

                currentInputConnection?.commitText(value, 1)
                sendAction(currentInputConnection)
            }

            handler.postDelayed(this, delayMs.toLong())
        }
    }

    private fun startAuto() {
        loadSettings()

        index = 0
        running = true

        handler.removeCallbacks(autoTask)
        handler.post(autoTask)
    }

    override fun onFinishInput() {
        running = false
        handler.removeCallbacks(autoTask)
        super.onFinishInput()
    }
}
