
package com.example.yallaquicksend

import android.content.Context
import android.widget.Toast

object KeyboardBridge {

    fun insert(context: Context, value: String): Boolean {
        val service = YallaAccessibilityService.instance

        if (service == null) {
            showMessage(
                context,
                "FLASH Accessibility enable karo"
            )
            return false
        }

        val success = service.insertText(value)

        if (!success) {
            showMessage(
                context,
                "Yalla ka message field select karo"
            )
        }

        return success
    }

    private fun showMessage(context: Context, message: String) {
        Toast.makeText(
            context.applicationContext,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
