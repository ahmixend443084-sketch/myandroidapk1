
package com.example.yallaquicksend

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private lateinit var autoDrop: CheckBox
    private lateinit var speedBased: CheckBox
    private lateinit var enterSend: CheckBox
    private lateinit var digitCount: EditText
    private lateinit var alphabetCount: EditText
    private lateinit var numberSequence: EditText
    private lateinit var alphabetSequence: EditText
    private lateinit var delay: EditText
    private lateinit var keyboardSize: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF101018.toInt())
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "𝙁𝙇𝘼𝙎𝙃🌙"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(0xFFB69CFF.toInt())
            setPadding(8, 20, 8, 8)
        }
        content.addView(title)

        val subtitle = TextView(this).apply {
            text = "QUICK SEND KEYBOARD"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(8, 0, 8, 24)
        }
        content.addView(subtitle)

        keyboardSize = Spinner(this)
        keyboardSize.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Small keyboard", "Medium keyboard", "Large keyboard")
        )
        content.addView(label("Keyboard Size"))
        content.addView(keyboardSize)

        enterSend = CheckBox(this).apply {
            text = "Enter key sends message"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = prefs.getBoolean("enterSend", true)
        }
        content.addView(enterSend)

        autoDrop = CheckBox(this).apply {
            text = "Auto Drop"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = prefs.getBoolean("autoDrop", false)
        }
        content.addView(autoDrop)

        speedBased = CheckBox(this).apply {
            text = "Speed Based"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = prefs.getBoolean("speedBased", false)
        }
        content.addView(speedBased)

        digitCount = editField(
            "Number count",
            prefs.getInt("digitCount", 10).toString()
        )
        content.addView(label("Number Count"))
        content.addView(digitCount)

        alphabetCount = editField(
            "Alphabet count",
            prefs.getInt("alphabetCount", 26).toString()
        )
        content.addView(label("Alphabet Count"))
        content.addView(alphabetCount)

        numberSequence = editField(
            "Numbers sequence",
            prefs.getString(
                "numberSequence",
                "1234567890"
            ) ?: "1234567890"
        )
        content.addView(label("Numbers Sequence"))
        content.addView(numberSequence)

        alphabetSequence = editField(
            "Alphabet sequence",
            prefs.getString(
                "alphabetSequence",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            ) ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )
        content.addView(label("Alphabet Sequence"))
        content.addView(alphabetSequence)

        delay = editField(
            "Delay in milliseconds",
            prefs.getInt("delay", 300).toString()
        )
        content.addView(label("Auto Drop Delay (ms)"))
        content.addView(delay)

        content.addView(makeButton("SAVE SETTINGS") {
            saveSettings()
        })

        content.addView(makeButton("ENABLE FLASH KEYBOARD") {
            try {
                startActivity(
                    Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                )
            } catch (e: Exception) {
                toast("Keyboard settings open nahi ho sakin")
            }
        })

        content.addView(makeButton("CHOOSE FLASH KEYBOARD") {
            val imm = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            imm.showInputMethodPicker()
        })

        content.addView(makeButton("ALLOW FLOATING PANEL") {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                toast("Floating permission pehle se allowed hai")
            }
        })

        content.addView(makeButton("START FLOATING PANEL") {
            saveSettings()
            if (Settings.canDrawOverlays(this)) {
                try {
                    startService(
                        Intent(this, ModOverlayService::class.java)
                    )
                    toast("FLASH panel start request bhej di")
                } catch (e: Exception) {
                    toast("Panel start nahi hua: ${e.message}")
                }
            } else {
                toast("Pehle floating permission allow karo")
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        })

        content.addView(makeButton("ACCESSIBILITY SETTINGS") {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        })

        scroll.addView(content)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun label(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(0xFFDDDDFF.toInt())
            setPadding(4, 16, 4, 4)
        }
    }

    private fun editField(
        hintText: String,
        initialValue: String
    ): EditText {
        return EditText(this).apply {
            hint = hintText
            setSingleLine(true)
            setText(initialValue)
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF999999.toInt())
        }
    }

    private fun makeButton(
        textValue: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = textValue
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun saveSettings() {
        val digits = digitCount.text.toString()
            .toIntOrNull()?.coerceIn(1, 100) ?: 10

        val letters = alphabetCount.text.toString()
            .toIntOrNull()?.coerceIn(1, 100) ?: 26

        val delayMs = delay.text.toString()
            .toIntOrNull()?.coerceIn(0, 60000) ?: 300

        prefs.edit()
            .putBoolean("enterSend", enterSend.isChecked)
            .putBoolean("autoDrop", autoDrop.isChecked)
            .putBoolean("speedBased", speedBased.isChecked)
            .putInt("keyboardSize", keyboardSize.selectedItemPosition)
            .putInt("digitCount", digits)
            .putInt("alphabetCount", letters)
            .putString(
                "numberSequence",
                numberSequence.text.toString()
            )
            .putString(
                "alphabetSequence",
                alphabetSequence.text.toString()
            )
            .putInt("delay", delayMs)
            .apply()

        toast("FLASH settings saved")
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onPause() {
        super.onPause()
        // Settings are saved when SAVE SETTINGS is pressed.
    }
}
