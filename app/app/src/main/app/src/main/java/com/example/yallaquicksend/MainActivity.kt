package com.example.yallaquicksend

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this)
        text.text = "YallaQuickSend"
        text.textSize = 24f
        text.setPadding(32, 32, 32, 32)

        setContentView(text)
    }
}
