
package com.example.yallaquicksend

import android.content.Context
import android.widget.Toast

object KeyboardBridge {

    fun insert(context: Context, value: String) {
        val service = YallaAccessibilityService.instance

        if (service == null) {
            Toast.makeText(
                context,
                "Enable Yalla Quick Send Accessibility first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val success = service.insertText(value)

        if (!success) {
            Toast.makeText(
                context,
                "Chat field not detected. Tap the chat field first.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
