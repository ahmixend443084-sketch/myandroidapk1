
package com.example.yallaquicksend

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable

class QuickKeyboardService : InputMethodService() {

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

        when (prefs.getInt("keyboardSize", 1).coerceIn(0, 2)) {
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
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            isAllCaps = false
            setTextColor(Color.rgb(35, 40, 48))

            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(8).toFloat()
            }

            setPadding(0, 0, 0, 0)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            elevation = 0f

            setOnClickListener { action() }
        }
    }

    // Manual key typing: no extra delay
    private fun typeCharacter(value: String) {

        val ic = currentInputConnection ?: return

        ic.commitText(value, 1)

        if (autoDrop) {
            sendAction(ic)
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

            val button = makeButton(
                key,
                if (key == "⌫") 22f else keySize.toFloat()
            ) {

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
            setBackgroundColor(Color.rgb(225, 229, 236))
        }

        // TOP BAR

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        top.addView(
            makeButton("ABC", 12f) {
                alphabetMode = true
                setInputView(createKeyboard())
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        top.addView(
            makeButton("123", 12f) {
                alphabetMode = false
                setInputView(createKeyboard())
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        top.addView(
            makeButton(
                if (autoDrop) "DROP ON" else "DROP OFF",
                11f,
                if (autoDrop) Color.rgb(120, 205, 145)
                else Color.LTGRAY
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

        // AUTO button retained for compatibility
        top.addView(
            makeButton("AUTO", 11f) {
                // Manual typing mode: no automatic sequence
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        top.addView(
            makeButton("STOP", 11f) {
                // No automatic task running
            },
            LinearLayout.LayoutParams(0, dp(44), 1f)
        )

        root.addView(top)

        if (alphabetMode) {

            // ALPHABET KEYBOARD

            addKeyRow(root, "QWERTYUIOP".map { it.toString() })
            addKeyRow(root, "ASDFGHJKL".map { it.toString() })
            addKeyRow(root, "ZXCVBNM".map { it.toString() })

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
                    currentInputConnection
                        ?.deleteSurroundingText(1, 0)
                },
                LinearLayout.LayoutParams(0, dp(52), 1f)
            )

            bottom.addView(
                makeButton("SEND", 14f, Color.rgb(120, 205, 145)) {
                    sendAction(currentInputConnection)
                },
                LinearLayout.LayoutParams(0, dp(52), 1.5f)
            )

            root.addView(bottom)

        } else {

            // NUMBER PAD

            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val numberSide = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val numberRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "")
            )

            numberRows.forEach { keys ->

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                keys.forEach { key ->

                    if (key.isEmpty()) {
                        row.addView(
                            View(this),
                            LinearLayout.LayoutParams(0, dp(55), 1f)
                        )
                    } else {
                        row.addView(
                            makeButton(key, 25f) {
                                typeCharacter(key)
                            },
                            LinearLayout.LayoutParams(0, dp(55), 1f)
                                .apply {
                                    setMargins(dp(2), dp(2), dp(2), dp(2))
                                }
                        )
                    }
                }

                numberSide.addView(
                    row,
                    LinearLayout.LayoutParams(-1, dp(58))
                )
            }

            panel.addView(
                numberSide,
                LinearLayout.LayoutParams(0, -2, 3f)
            )

            // DELETE TOP, SEND BOTTOM

            val actionSide = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            actionSide.addView(
                makeButton("DELETE", 13f, Color.LTGRAY) {
                    currentInputConnection
                        ?.deleteSurroundingText(1, 0)
                },
                LinearLayout.LayoutParams(-1, 0, 1f).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                }
            )

            actionSide.addView(
                makeButton("SEND", 15f, Color.rgb(120, 205, 145)) {
                    sendAction(currentInputConnection)
                },
                LinearLayout.LayoutParams(-1, 0, 1f).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                }
            )

            panel.addView(
                actionSide,
                LinearLayout.LayoutParams(0, dp(232), 1.3f)
            )

            root.addView(panel)
        }

        return root
    }

    private fun sendAction(ic: InputConnection?) {

        if (ic == null) return

        if (YallaAccessibilityService.instance
                ?.clickVisibleSend() == true
        ) {
            return
        }

        val action = currentInputEditorInfo?.imeOptions
            ?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_UNSPECIFIED

        if (
            action == EditorInfo.IME_ACTION_SEND ||
            action == EditorInfo.IME_ACTION_GO ||
            action == EditorInfo.IME_ACTION_DONE
        ) {
            if (ic.performEditorAction(action)) return
        }

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

    override fun onFinishInput() {
        super.onFinishInput()
    }
}
