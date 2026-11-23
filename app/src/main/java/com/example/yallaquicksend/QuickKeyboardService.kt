
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
    private var twoChars = false
    private var alphabetMode = true
    private var autoDrop = false

    private var keySize = 24
    private var keyHeight = 58
    private var keyboardHeight = 330

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreateInputView(): View {
        loadSettings()
        return createKeyboard()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        keySize = prefs.getInt("keySize", 24).coerceIn(18, 30)
        keyboardHeight = prefs.getInt("keyboardHeight", 330)
            .coerceIn(240, 420)

        keyHeight = (keySize * 2.4f).toInt().coerceIn(48, 76)
        autoDrop = prefs.getBoolean("autoDrop", false)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadSettings()

        window.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(keyboardHeight)
        )
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
            setOnClickListener { action() }
        }
    }

    private fun typeCharacter(value: String) {
        currentInputConnection?.commitText(value, 1)

        if (autoDrop) {
            sendAction(currentInputConnection)
        }
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

        val dropButton = makeButton(
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
        }

        top.addView(
            dropButton,
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

            // ALPHABET KEYBOARD
            fun addRow(keys: String, padding: Int = 0) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(padding), 0, dp(padding), 0)
                }

                keys.forEach { character ->
                    val button = makeButton(
                        character.toString(),
                        keySize.toFloat()
                    ) {
                        typeCharacter(character.toString())
                    }

                    row.addView(
                        button,
                        LinearLayout.LayoutParams(
                            0, dp(keyHeight), 1f
                        ).apply {
                            setMargins(dp(2), dp(2), dp(2), dp(2))
                        }
                    )
                }

                root.addView(row)
            }

            addRow("QWERTYUIOP")
            addRow("ASDFGHJKL", 12)
            addRow("ZXCVBNM", 35)

            val bottom = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            bottom.addView(makeButton("SPACE", 14f) {
                currentInputConnection?.commitText(" ", 1)
            }, LinearLayout.LayoutParams(0, dp(52), 2f))

            bottom.addView(makeButton("⌫", 22f) {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }, LinearLayout.LayoutParams(0, dp(52), 1f))

            bottom.addView(makeButton("SEND", 14f) {
                sendAction(currentInputConnection)
            }, LinearLayout.LayoutParams(0, dp(52), 1.5f))

            root.addView(bottom)

        } else {

            // CALCULATOR STYLE NUMPAD
            val body = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(3), dp(2), dp(3))
            }

            val grid = GridLayout(this).apply {
                columnCount = 3
                rowCount = 4
            }

            val numbers = listOf(
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                ".", "0", ""
            )

            numbers.forEach { number ->

                val button = makeButton(
                    number,
                    keySize.toFloat()
                ) {
                    if (number.isNotEmpty()) {
                        typeCharacter(number)
                    }
                }

                if (number.isEmpty()) {
                    button.visibility = View.INVISIBLE
                }

                grid.addView(
                    button,
                    GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(
                            GridLayout.UNDEFINED, 1f
                        )
                        rowSpec = GridLayout.spec(
                            GridLayout.UNDEFINED, 1f
                        )
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                )
            }

            body.addView(
                grid,
                LinearLayout.LayoutParams(0, -1, 3f)
            )

            // RIGHT SIDE BUTTONS
            val side = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            side.addView(
                makeButton("DELETE ⌫", 13f) {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                },
                LinearLayout.LayoutParams(-1, 0, 1f).apply {
                    setMargins(dp(3), dp(2), dp(2), dp(3))
                }
            )

            side.addView(
                makeButton(
                    "SEND",
                    15f,
                    Color.rgb(40, 160, 80)
                ) {
                    sendAction(currentInputConnection)
                },
                LinearLayout.LayoutParams(-1, 0, 1f).apply {
                    setMargins(dp(3), dp(3), dp(2), dp(2))
                }
            )

            body.addView(
                side,
                LinearLayout.LayoutParams(0, -1, 1.15f)
            )

            root.addView(
                body,
                LinearLayout.LayoutParams(-1, 0, 1f)
            )
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
                prefs.getString("numbers", "12456") ?: "12456"
            }

            if (sequence.isNotEmpty()) {
                val count = if (twoChars) 2 else 1

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
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        twoChars = prefs.getInt("mode", 0) == 1
        delayMs = prefs.getInt("delay", 500).coerceIn(150, 5000)

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
