
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
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInputView(info, restarting)

        loadSettings()

        window.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(keyboardHeight)
        )
    }

    private fun createKeyboard(): View {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(4), dp(3), dp(4))
            setBackgroundColor(Color.rgb(215, 220, 230))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        fun makeButton(
            label: String,
            size: Float,
            action: () -> Unit
        ): Button {
            return Button(this).apply {
                text = label
                textSize = size
                isAllCaps = false
                setTextColor(Color.BLACK)
                setPadding(0, 0, 0, 0)
                minHeight = 0
                minimumHeight = 0
                setOnClickListener { action() }
            }
        }

        // TOP BAR
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            )
        }

        top.addView(makeButton("ABC", 14f) {
            alphabetMode = true
            index = 0
            setInputView(createKeyboard())
        }, LinearLayout.LayoutParams(0, -1, 1f))

        top.addView(makeButton("123", 14f) {
            alphabetMode = false
            index = 0
            setInputView(createKeyboard())
        }, LinearLayout.LayoutParams(0, -1, 1f))

        top.addView(makeButton("AUTO", 14f) {
            startAuto()
        }, LinearLayout.LayoutParams(0, -1, 1f))

        top.addView(makeButton("STOP", 14f) {
            running = false
            handler.removeCallbacks(autoTask)
        }, LinearLayout.LayoutParams(0, -1, 1f))

        root.addView(top)

        // ROW BUILDER
        fun addRow(
            keys: String,
            sidePadding: Int = 0
        ) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(sidePadding), 0, dp(sidePadding), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(keyHeight)
                )
            }

            keys.forEach { character ->
                val button = makeButton(
                    character.toString(),
                    keySize.toFloat()
                ) {
                    currentInputConnection?.commitText(
                        character.toString(), 1
                    )
                }

                val params = LinearLayout.LayoutParams(
                    0,
                    -1,
                    1f
                ).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }

                row.addView(button, params)
            }

            root.addView(row)
        }

        // KEYBOARD BODY
        if (alphabetMode) {
            addRow("QWERTYUIOP", 0)
            addRow("ASDFGHJKL", 12)
            addRow("ZXCVBNM", 35)
        } else {
            addRow("1234567890", 0)
            addRow("0987654321", 12)
            addRow("1234567890", 35)
        }

        // BOTTOM BAR
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        }

        bottom.addView(makeButton("SPACE", 15f) {
            currentInputConnection?.commitText(" ", 1)
        }, LinearLayout.LayoutParams(0, -1, 2f))

        bottom.addView(makeButton("⌫", 22f) {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }, LinearLayout.LayoutParams(0, -1, 1f))

        bottom.addView(makeButton("SEND", 15f) {
            sendAction(currentInputConnection)
        }, LinearLayout.LayoutParams(0, -1, 1.5f))

        root.addView(bottom)

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
        delayMs = prefs.getInt("delay", 500)

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
