
package com.example.yallaquicksend

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper

class QuickKeyboardService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var index = 0
    private var delayMs = 500
    private var twoChars = false
    private var alphabetMode = true

    private val keyHeight = 58

    override fun onCreateInputView(): View {
        return createKeyboard()
    }

    private fun createKeyboard(): View {

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(3, 5, 3, 5)
            setBackgroundColor(Color.rgb(215, 220, 230))
        }

        fun makeButton(
            textValue: String,
            size: Float,
            action: () -> Unit
        ): Button {
            return Button(this).apply {
                text = textValue
                textSize = size
                isAllCaps = false
                setTextColor(Color.BLACK)
                setPadding(0, 0, 0, 0)
                setOnClickListener { action() }
            }
        }

        // TOP BAR
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        top.addView(makeButton("ABC") {
            alphabetMode = true
            index = 0
            setInputView(createKeyboard())
        }, LinearLayout.LayoutParams(0, 48, 1f))

        top.addView(makeButton("123") {
            alphabetMode = false
            index = 0
            setInputView(createKeyboard())
        }, LinearLayout.LayoutParams(0, 48, 1f))

        top.addView(makeButton("AUTO") {
            startAuto()
        }, LinearLayout.LayoutParams(0, 48, 1f))

        top.addView(makeButton("STOP") {
            running = false
            handler.removeCallbacks(autoTask)
        }, LinearLayout.LayoutParams(0, 48, 1f))

        root.addView(top)

        // KEY ROW CREATOR
        fun addKeyRow(keys: String, height: Int = keyHeight) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            keys.forEach { character ->
                val button = makeButton(character.toString(), 23f) {
                    currentInputConnection?.commitText(character.toString(), 1)
                }

                val params = LinearLayout.LayoutParams(
                    0,
                    height,
                    1f
                )
                params.setMargins(2, 3, 2, 3)
                row.addView(button, params)
            }

            root.addView(row)
        }

        // ALPHABET KEYBOARD
        if (alphabetMode) {
            addKeyRow("QWERTYUIOP")
            addKeyRow("ASDFGHJKL")

            val third = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            addViewToThirdRow(third, "ZXCVBNM")

            root.addView(third)
        } else {
            // NUMBER KEYBOARD
            addKeyRow("123")
            addKeyRow("456")
            addKeyRow("789")

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val zero = makeButton("0", 26f) {
                currentInputConnection?.commitText("0", 1)
            }

            row.addView(
                zero,
                LinearLayout.LayoutParams(0, keyHeight, 2f)
            )

            root.addView(row)
        }

        // BOTTOM CONTROLS
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        bottom.addView(makeButton("SPACE", 16f) {
            currentInputConnection?.commitText(" ", 1)
        }, LinearLayout.LayoutParams(0, 55, 2f))

        bottom.addView(makeButton("⌫", 22f) {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }, LinearLayout.LayoutParams(0, 55, 1f))

        bottom.addView(makeButton("➤ SEND", 16f) {
            sendAction(currentInputConnection)
        }, LinearLayout.LayoutParams(0, 55, 1.5f))

        root.addView(bottom)

        return ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
    }

    private fun addViewToThirdRow(row: LinearLayout, keys: String) {
        keys.forEach { character ->
            val button = Button(this).apply {
                text = character.toString()
                textSize = 23f
                setTextColor(Color.BLACK)
                setPadding(0, 0, 0, 0)
                setOnClickListener {
                    currentInputConnection?.commitText(character.toString(), 1)
                }
            }

            row.addView(
                button,
                LinearLayout.LayoutParams(0, keyHeight, 1f).apply {
                    setMargins(2, 3, 2, 3)
                }
            )
        }
    }

    private fun sendAction(ic: InputConnection?) {
        if (YallaAccessibilityService.instance?.clickVisibleSend() == true) return
        if (ic == null) return

        val action = currentInputEditorInfo?.imeOptions
            ?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_UNSPECIFIED

        if (action == EditorInfo.IME_ACTION_SEND ||
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
                prefs.getString("letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                    ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
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
