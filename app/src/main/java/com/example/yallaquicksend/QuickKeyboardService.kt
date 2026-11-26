
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

    // Characters are committed immediately. Only Send actions are queued.
    private var autoDropCount = 0
    private var pendingAutoSends = 0
    private var autoSendInProgress = false

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val green = Color.rgb(0, 195, 105)
    private val keyColor = Color.rgb(48, 49, 62)
    private val textColor = Color.WHITE

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
        stopped = false
        return buildKeyboard()
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInputView(info, restarting)

        loadSettings()
        stopped = false

        if (::keyboardRoot.isInitialized) {
            renderKeys()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        stopPendingActions()
        stopped = true
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        stopPendingActions()
        stopped = true
        super.onDestroy()
    }

    private fun stopPendingActions() {
        handler.removeCallbacksAndMessages(null)
        autoDropCount = 0
        pendingAutoSends = 0
        autoSendInProgress = false
    }

    private fun buildKeyboard(): View {
        keyboardRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(5), dp(5), dp(5))

            background = GradientDrawable().apply {
                cornerRadius = dp(17).toFloat()
                setColor(Color.rgb(20, 21, 27))
                setStroke(dp(2), Color.rgb(49, 210, 91))
            }
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

        addToolbar(rows)

        if (numbersMode) {
            renderNumberPad(rows)
        } else {
            renderAlphabetKeys(rows)
        }
    }

    private fun addToolbar(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        addToolbarButton(row, "ABC", !numbersMode, 1f) {
            numbersMode = false
            renderKeys()
        }

        addToolbarButton(row, "123", numbersMode, 1f) {
            numbersMode = true
            renderKeys()
        }

        addToolbarButton(row, "😊", false, 0.8f) {
            typeText("😊")
        }

        addToolbarButton(row, "⚙", false, 0.8f) {
            showToast("FLASH keyboard settings app mein kholen")
        }

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(keyHeight() + 4)
            )
        )
    }

    private fun addToolbarButton(
        row: LinearLayout,
        label: String,
        active: Boolean,
        weight: Float,
        action: () -> Unit
    ) {
        val button = makeButton(
            label = label,
            textSize = if (label.length > 2) 14f else 19f,
            highlighted = active,
            action = action
        )

        row.addView(
            button,
            LinearLayout.LayoutParams(
                0,
                dp(keyHeight()),
                weight
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )
    }

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
            listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "⌫"),
            weights = listOf(
                1.35f, 1f, 1f, 1f, 1f,
                1f, 1f, 1f, 1.25f
            )
        ) { key ->
            when (key) {
                "SHIFT" -> {
                    uppercaseMode = !uppercaseMode
                    prefs.edit()
                        .putBoolean("uppercaseMode", uppercaseMode)
                        .apply()
                    renderKeys()
                }

                "⌫" -> deleteCharacter()

                else -> typeText(
                    if (uppercaseMode) key.uppercase()
                    else key.lowercase()
                )
            }
        }

        addKeyRow(
            rows,
            listOf("?123", ",", "SPACE", ".", "SEND"),
            weights = listOf(1.35f, 0.75f, 3.5f, 0.7f, 1.5f)
        ) { key ->
            when (key) {
                "?123" -> {
                    numbersMode = true
                    renderKeys()
                }

                "," -> typeText(",")
                "SPACE" -> typeText(" ")
                "." -> typeText(".")
                "SEND" -> handleEnter()
            }
        }
    }

    private fun renderNumberPad(rows: LinearLayout) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val numberGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        addKeyRow(numberGrid, listOf("1", "2", "3")) {
            typeText(it)
        }

        addKeyRow(numberGrid, listOf("4", "5", "6")) {
            typeText(it)
        }

        addKeyRow(numberGrid, listOf("7", "8", "9")) {
            typeText(it)
        }

        addKeyRow(numberGrid, listOf("-", "0", ".")) {
            typeText(it)
        }

        content.addView(
            numberGrid,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                3f
            )
        )

        val actionColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val deleteButton = makeButton(
            label = "⌫",
            textSize = 25f,
            action = { deleteCharacter() }
        )

        actionColumn.addView(
            deleteButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(keyHeight())
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        val sendButton = makeButton(
            label = "➤\nSEND",
            textSize = 21f,
            highlighted = true,
            action = { handleEnter() }
        )

        actionColumn.addView(
            sendButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                3f
            ).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        )

        content.addView(
            actionColumn,
            LinearLayout.LayoutParams(
                0,
                dp((keyHeight() + 4) * 4),
                1.25f
            )
        )

        rows.addView(
            content,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp((keyHeight() + 4) * 4)
            )
        )
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
            val displayKey = when (key) {
                "SHIFT" -> if (uppercaseMode) "⬆" else "⇧"
                "SPACE" -> "English"

                else -> if (
                    key.length == 1 && key[0].isLetter()
                ) {
                    if (uppercaseMode) key.uppercase()
                    else key.lowercase()
                } else {
                    key
                }
            }

            val size = when (key) {
                "SPACE" -> 11f
                "SEND" -> 12f
                "?123" -> 12f
                "SHIFT", "⌫" -> 19f
                else -> keyTextSize()
            }

            val button = makeButton(
                label = displayKey,
                textSize = size,
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
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER

            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0

            setPadding(dp(1), dp(1), dp(1), dp(1))
            stateListAnimator = null

            background = GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(if (highlighted) green else keyColor)
            }

            setOnClickListener {
                if (!stopped) action()
            }
        }
    }

    /**
     * Har key ka text foran field mein insert hota hai.
     * Auto Drop sirf Send actions ko queue karta hai.
     */
    private fun typeText(value: String) {
        if (stopped || value.isEmpty()) return

        loadSettings()

        val connection = currentInputConnection ?: return

        // Typing ko Send delay se block nahi karna.
        connection.commitText(value, 1)

        if (!autoDrop) {
            autoDropCount = 0
            return
        }

        // Spaces ko type karo, magar unhein character count mein na gino.
        val countableCharacters = value.count { !it.isWhitespace() }
        if (countableCharacters == 0) return

        autoDropCount += countableCharacters

        while (autoDropCount >= alphabetCount) {
            autoDropCount -= alphabetCount
            queueAutomaticSend()
        }
    }

    private fun queueAutomaticSend() {
        if (stopped) return

        pendingAutoSends++
        processAutoSendQueue()
    }

    private fun processAutoSendQueue() {
        if (stopped || autoSendInProgress || pendingAutoSends <= 0) {
            return
        }

        loadSettings()

        if (!autoDrop) {
            pendingAutoSends = 0
            autoDropCount = 0
            return
        }

        autoSendInProgress = true

        handler.postDelayed({
            try {
                if (!stopped && autoDrop) {
                    sendAutomaticAction()
                }
            } finally {
                pendingAutoSends = (pendingAutoSends - 1).coerceAtLeast(0)
                autoSendInProgress = false
                processAutoSendQueue()
            }
        }, delayMs.coerceIn(0, 1000).toLong())
    }

    private fun deleteCharacter() {
        if (stopped) return

        val connection = currentInputConnection ?: return

        // Agar Auto Drop active ho, deletion count ko bhi adjust karo.
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

        // Saved target fail ho to visible Send button try karo.
        val sent = if (service.hasSavedSendPosition()) {
            service.tapSavedSendPosition() || service.clickVisibleSend()
        } else {
            service.clickVisibleSend()
        }

        if (!sent) {
            showToast("Send nahi hua: Yalla screen aur saved position check karo")
        }
    }

    private fun sendAutomaticAction() {
        if (stopped || !autoDrop) return

        val service = YallaAccessibilityService.instance ?: return

        if (service.hasSavedSendPosition()) {
            service.tapSavedSendPosition() || service.clickVisibleSend()
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
