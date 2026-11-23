package com.example.yallaquicksend

import android.inputmethodservice.InputMethodService
import android.view.View
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
    private var sequence = "12456"
    private var delayMs = 500
    private var twoChars = false
    private var alphabetMode = false

    override fun onCreateInputView(): View {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        sequence = prefs.getString("numbers", "12456") ?: "12456"
        delayMs = prefs.getInt("delay", 500)
        twoChars = prefs.getInt("mode", 0) == 1

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(6, 6, 6, 6)
            setBackgroundColor(Color.rgb(239, 240, 245))
        }
        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val nums = Button(this).apply { text = "123"; setOnClickListener { alphabetMode = false; sequence = prefs.getString("numbers", "12456") ?: "12456"; index = 0 } }
        val abc = Button(this).apply { text = "ABC"; setOnClickListener { alphabetMode = true; sequence = prefs.getString("letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ") ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; index = 0 } }
        val start = Button(this).apply { text = "AUTO START"; setOnClickListener { startAuto() } }
        val stop = Button(this).apply { text = "STOP"; setOnClickListener { running = false; handler.removeCallbacks(autoTask) } }
        listOf(nums, abc, start, stop).forEach { modeRow.addView(it, LinearLayout.LayoutParams(0, 48, 1f)) }
        root.addView(modeRow)

        val grid = GridLayout(this).apply { columnCount = 5 }
        val keys = listOf("1","2","4","5","6","A","B","C","D","E","F","G","H","I","J")
        keys.forEach { key ->
            val b = Button(this).apply {
                text = key
                textSize = 20f
                setOnClickListener { currentInputConnection?.commitText(key, 1) }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0; height = 54; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(3, 3, 3, 3)
            }
            grid.addView(b, lp)
        }
        root.addView(grid)
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val space = Button(this).apply { text = "SPACE"; setOnClickListener { currentInputConnection?.commitText(" ", 1) } }
        val del = Button(this).apply { text = "⌫"; setOnClickListener { currentInputConnection?.deleteSurroundingText(1, 0) } }
        val send = Button(this).apply {
            text = "➤ SEND"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(0, 125, 190))
            setOnClickListener { sendAction(currentInputConnection) }
        }
        bottom.addView(space, LinearLayout.LayoutParams(0, 52, 1f))
        bottom.addView(del, LinearLayout.LayoutParams(0, 52, 1f))
        bottom.addView(send, LinearLayout.LayoutParams(0, 52, 1.5f))
        root.addView(bottom)
        return root
    }

    private fun sendAction(ic: InputConnection?) {
        if (YallaAccessibilityService.instance?.clickVisibleSend() == true) return
        if (ic == null) return
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_UNSPECIFIED
        if (action == EditorInfo.IME_ACTION_SEND || action == EditorInfo.IME_ACTION_GO ||
            action == EditorInfo.IME_ACTION_DONE) {
            ic.performEditorAction(action)
        } else {
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
        }
    }

    private val autoTask = object : Runnable {
        override fun run() {
            if (!running) return
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val seq = if (alphabetMode) prefs.getString("letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ") ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                      else prefs.getString("numbers", "12456") ?: "12456"
            if (seq.isNotEmpty()) {
                val n = if (twoChars) 2 else 1
                val value = buildString { repeat(n) { append(seq[index++ % seq.length]) } }
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
