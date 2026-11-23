
package com.example.yallaquicksend

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.Gravity
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
    private var alphabetMode = false
    private var keyHeight = 68

    private lateinit var root: LinearLayout

    override fun onCreateInputView(): View {
        return buildKeyboard()
    }

    private fun buildKeyboard(): View {

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val savedHeight = prefs.getInt("keyHeight", 68)
        keyHeight = savedHeight.coerceIn(55, 100)

        val sequence = if (alphabetMode) {
            prefs.getString("letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        } else {
            prefs.getString("numbers", "12456") ?: "12456"
        }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 6, 5, 6)
            setBackgroundColor(Color.rgb(225, 230, 238))
        }

        // TOP CONTROLS
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun controlButton(label: String, action: () -> Unit): Button {
            return Button(this).apply {
                text = label
                textSize = 13f
                setOnClickListener { action() }
            }
        }

        top.addView(controlButton("123") {
            alphabetMode = false
            index = 0
            setInputView(buildKeyboard())
        }, LinearLayout.LayoutParams(0, 52, 1f))

        top.addView(controlButton("ABC") {
            alphabetMode = true
            index = 0
            setInputView(buildKeyboard())
        }, LinearLayout.LayoutParams(0, 52, 1f))

        top.addView(controlButton("AUTO START") {
            startAuto()
        }, LinearLayout.LayoutParams(0, 52, 1.5f))

        top.addView(controlButton("STOP") {
            running = false
            handler.removeCallbacks(autoTask)
        }, LinearLayout.LayoutParams(0, 52, 1f))

        main.addView(top)

        // RESIZE CONTROLS
        val resize = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        resize.addView(controlButton("−") {
            keyHeight = (keyHeight - 5).coerceAtLeast(55)
            prefs.edit().putInt("keyHeight", keyHeight).apply()
            setInputView(buildKeyboard())
        }, LinearLayout.LayoutParams(0, 45, 1f))

        resize.addView(TextView(this).apply {
            text = "KEY SIZE: $keyHeight"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
        }, LinearLayout.LayoutParams(0, 45, 2f))

        resize.addView(controlButton("+") {
            keyHeight = (keyHeight + 5).coerceAtMost(100)
            prefs.edit().putInt("keyHeight", keyHeight).apply()
            setInputView(buildKeyboard())
        }, LinearLayout.LayoutParams(0, 45, 1f))

        main.addView(resize)

        // KEY GRID
        val grid = GridLayout(this).apply {
            columnCount = 5
        }

        val keys = if (alphabetMode) {
            prefs.getString("letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        } else {
            prefs.getString("numbers", "12456") ?: "12456"
        }

        keys.forEach { character ->

            val button = Button(this).apply {
                text = character.toString()
                textSize = 28f
                isAllCaps = false
                setTextColor(Color.BLACK)
                setOnClickListener {
                    currentInputConnection?.commitText(character.toString(), 1)
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = keyHeight
                columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED, 1f
                )
                setMargins(3, 3, 3, 3)
            }

            grid.addView(button, params)
        }

        main.addView(grid)

        // BOTTOM BUTTONS
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun bottomButton(label: String, action: () -> Unit): Button {
            return Button(this).apply {
                text = label
                textSize = 17f
                setOnClickListener { action() }
            }
        }

        bottom.addView(bottomButton("SPACE") {
            currentInputConnection?.commitText(" ", 1)
        }, LinearLayout.LayoutParams(0, 58, 1f))

        bottom.addView(bottomButton("⌫") {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }, LinearLayout.LayoutParams(0, 58, 1f))

        bottom.addView(bottomButton("SEND") {
            sendAction(currentInputConnection)
        }, LinearLayout.LayoutParams(0, 58, 1.5f))

        main.addView(bottom)

        return ScrollView(this).apply {
            isFillViewport = true
            addView(main)
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

            val seq = if (alphabetMode) {
                prefs.getString("letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                    ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            } else {
                prefs.getString("numbers", "12456") ?: "12456"
            }

            if (seq.isNotEmpty()) {
                val count = if (twoChars) 2 else 1
                val value = buildString {
                    repeat(count) {
                        append(seq[index++ % seq.length])
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
