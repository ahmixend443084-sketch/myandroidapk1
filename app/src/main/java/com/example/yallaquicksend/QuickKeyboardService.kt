
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
    private lateinit var statusText: TextView

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val autoSendTask = Runnable {
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
        keyboardSize = prefs.getInt("keyboardSize", 1)
            .coerceIn(0, 2)
        delayMs = prefs.getInt("delay", 300)
            .coerceIn(0, 60000)
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
        refreshStatus()
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
            setPadding(5, 6, 5, 6)
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
            LinearLayout.LayoutParams(0, dp(42), 1f)
        )

        header.addView(
            makeButton("ABC", 12f) {
                numbersMode = false
                renderKeys()
            },
            buttonParams()
        )

        header.addView(
            makeButton("123", 12f) {
                numbersMode = true
                renderKeys()
            },
            buttonParams()
        )

        keyboardRoot.addView(header)

        statusText = TextView(this).apply {
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(2, 2, 2, 4)
        }
        keyboardRoot.addView(statusText)

        renderKeys()
        refreshStatus()

        return keyboardRoot
    }

    private fun renderKeys() {
        while (keyboardRoot.childCount > 2) {
            keyboardRoot.removeViewAt(2)
        }

        if (numbersMode) {
            addKeyRow(listOf("1", "2", "3"))
            addKeyRow(listOf("4", "5", "6"))
            addKeyRow(listOf("7", "8", "9"))
            addKeyRow(listOf("-", "0", "."))
        } else {
            addKeyRow(listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"))
            addKeyRow(listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"))
            addKeyRow(listOf("Z", "X", "C", "V", "B", "N", "M"))
        }

        val editRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        editRow.addView(
            makeButton("DELETE", 11f) {
                val connection = currentInputConnection
                connection?.deleteSurroundingText(1, 0)
            },
            LinearLayout.LayoutParams(0, dp(43), 1.5f).apply {
                setMargins(2, 2, 2, 2)
            }
        )

        editRow.addView(
            makeButton("SPACE", 11f) {
                typeText(" ")
            },
            LinearLayout.LayoutParams(0, dp(43), 2f).apply {
                setMargins(2, 2, 2, 2)
            }
        )

        editRow.addView(
            makeButton("DROP ${if (autoDrop && !stopped) "ON" else "OFF"}", 10f) {
                autoDrop = !autoDrop
                stopped = false
                prefs.edit().putBoolean("autoDrop", autoDrop).apply()
                refreshStatus()
                renderKeys()
            },
            LinearLayout.LayoutParams(0, dp(43), 1.5f).apply {
                setMargins(2, 2, 2, 2)
            }
        )

        keyboardRoot.addView(editRow)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actionRow.addView(
            makeButton("STOP", 12f) {
                stopped = true
                autoDrop = false
                handler.removeCallbacks(autoSendTask)
                prefs.edit().putBoolean("autoDrop", false).apply()
                refreshStatus()
                renderKeys()
            },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(2, 2, 2, 2)
            }
        )

        actionRow.addView(
            makeButton("ENTER", 12f) {
                val connection = currentInputConnection
                if (enterSend) {
                    sendAction()
                } else {
                    connection?.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_ENTER
                        )
                    )
                    connection?.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_ENTER
                        )
                    )
                }
            },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                setMargins(2, 2, 2, 2)
            }
        )

        actionRow.addView(
            makeButton("SEND", 12f, true) {
                sendAction()
            },
            LinearLayout.LayoutParams(0, dp(44), 1.3f).apply {
                setMargins(2, 2, 2, 2)
            }
        )

        keyboardRoot.addView(actionRow)
    }

    private fun addKeyRow(keys: List<String>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        for (key in keys) {
            row.addView(
                makeButton(key, keyTextSize()) {
                    typeText(key)
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(keyHeight()),
                    1f
                ).apply {
                    setMargins(2, 2, 2, 2)
                }
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
            setPadding(1, 1, 1, 1)

            background = GradientDrawable().apply {
                cornerRadius = dp(7).toFloat()
                setColor(
                    if (highlighted) {
                        Color.rgb(103, 66, 185)
                    } else {
                        Color.rgb(48, 47, 63)
                    }
            }

            setOnClickListener { action() }
        }
    }

    private fun buttonParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            dp(49),
            dp(40)
        ).apply {
            setMargins(2, 1, 2, 1)
        }
    }

    private fun typeText(value: String) {
        val connection: InputConnection =
            currentInputConnection ?: return

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

    private fun sendAction() {
        handler.removeCallbacks(autoSendTask)

        val connection = currentInputConnection ?: return
        val info = currentInputEditorInfo

        val actionId = info?.imeOptions?.and(
            EditorInfo.IME_MASK_ACTION
        ) ?: EditorInfo.IME_ACTION_UNSPECIFIED

        val performed = when (actionId) {
            EditorInfo.IME_ACTION_GO,
            EditorInfo.IME_ACTION_SEARCH,
            EditorInfo.IME_ACTION_SEND,
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.IME_ACTION_DONE,
            EditorInfo.IME_ACTION_PREVIOUS -> {
                connection.performEditorAction(actionId)
            }
            else -> false
        }

        if (!performed) {
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

        refreshStatus()
    }

    private fun refreshStatus() {
        if (!::statusText.isInitialized) return

        statusText.text = buildString {
            append(if (numbersMode) "123" else "ABC")
            append("  •  Auto Drop: ")
            append(if (autoDrop && !stopped) "ON" else "OFF")
            append("  •  Delay: ")
            append(delayMs)
            append(" ms")
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
            2 -> 52
            else -> 44
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
