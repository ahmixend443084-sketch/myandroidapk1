package com.example.yallaquicksend

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast

object KeyboardBridge {
    fun insert(context: Context, value: String) {
        // Overlay key inserts text through clipboard; user may paste into the focused chat field.
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Yalla Quick Send", value))
        Toast.makeText(context, "Copied: $value — tap chat field and paste if needed", Toast.LENGTH_SHORT).show()
    }
}
