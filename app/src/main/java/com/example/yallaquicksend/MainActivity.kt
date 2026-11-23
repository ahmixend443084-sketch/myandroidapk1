
package com.example.yallaquicksend

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
        }

        layout.addView(TextView(this).apply {
            text = "YALLA QUICK SEND"
            textSize = 25f
        })

        layout.addView(TextView(this).apply {
            text = "Keyboard setup"
            textSize = 18f
            setPadding(0, 24, 0, 8)
        })

        val enable = Button(this).apply {
            text = "1. Enable keyboard"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }
        layout.addView(enable)

        val choose = Button(this).apply {
            text = "2. Choose Yalla Quick Send"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        }
        layout.addView(choose)

        layout.addView(TextView(this).apply {
            text = "Auto sequence settings"
            textSize = 18f
            setPadding(0, 24, 0, 8)
        })

        val nums = EditText(this).apply {
            hint = "Numbers e.g. 12456"
            setText(prefs.getString("numbers", "12456"))
        }
        layout.addView(nums)

        val letters = EditText(this).apply {
            hint = "Letters e.g. ABCXYZ"
            setText(prefs.getString("letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
        }
        layout.addView(letters)

        val mode = Spinner(this)
        mode.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("One digit / one letter", "Two digits / two letters")
        )
        mode.setSelection(prefs.getInt("mode", 0))
        layout.addView(mode)

        val speed = EditText(this).apply {
            hint = "Delay in milliseconds (e.g. 500)"
            inputType = 2
            setText(prefs.getInt("delay", 500).toString())
        }
        layout.addView(speed)

        val save = Button(this).apply {
            text = "Save settings"
            setOnClickListener {
                prefs.edit()
                    .putString(
                        "numbers",
                        nums.text.toString().filter { it.isDigit() }.ifBlank { "12456" }
                    )
                    .putString(
                        "letters",
                        letters.text.toString().filter { it.isLetter() }
                            .uppercase().ifBlank { "ABCDEFGHIJKLMNOPQRSTUVWXYZ" }
                    )
                    .putInt("mode", mode.selectedItemPosition)
                    .putInt(
                        "delay",
                        (speed.text.toString().toIntOrNull() ?: 500).coerceIn(150, 5000)
                    )
                    .apply()

                Toast.makeText(
                    this@MainActivity,
                    "Settings saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        layout.addView(save)

        val overlay = Button(this).apply {
            text = "3. Enable MOD panel"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 23 &&
                    !Settings.canDrawOverlays(this@MainActivity)
                ) {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                } else {
                    startService(
                        Intent(this@MainActivity, ModOverlayService::class.java)
                    )
                }
            }
        }
        layout.addView(overlay)

        val access = Button(this).apply {
            text = "4. Enable Send Accessibility"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(access)

        layout.addView(TextView(this).apply {
            text = "Important: enable the accessibility service yourself. SEND will try to click a visible control named Send. If Yalla does not expose a detectable Send control, sending may not work. Accessibility can view screen content while enabled."
            textSize = 14f
            setPadding(0, 20, 0, 0)
        })

        setContentView(ScrollView(this).apply {
            addView(layout)
        })
    }
}
