
package com.example.yallaquicksend

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.text.InputType
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

        layout.addView(Button(this).apply {
            text = "1. Enable keyboard"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        })

        layout.addView(Button(this).apply {
            text = "2. Choose Yalla Quick Send"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showInputMethodPicker()
            }
        })

        // KEYBOARD SIZE
        layout.addView(TextView(this).apply {
            text = "Keyboard Size"
            textSize = 20f
            setPadding(0, 24, 0, 8)
        })

        val sizeSpinner = Spinner(this)

        sizeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Small", "Medium", "Large")
        )

        sizeSpinner.setSelection(
            prefs.getInt("keyboardSize", 1).coerceIn(0, 2)
        )

        layout.addView(sizeSpinner)

        // AUTO DIGIT COUNT
        layout.addView(TextView(this).apply {
            text = "Auto Drop - Digits per send"
            textSize = 17f
            setPadding(0, 20, 0, 8)
        })

        val digitSpinner = Spinner(this)

        digitSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("1 Digit", "2 Digits", "3 Digits", "4 Digits", "5 Digits")
        )

        digitSpinner.setSelection(
            prefs.getInt("digitCount", 1).coerceIn(1, 5) - 1
        )

        layout.addView(digitSpinner)

        // AUTO ALPHABET COUNT
        layout.addView(TextView(this).apply {
            text = "Auto Drop - Alphabets per send"
            textSize = 17f
            setPadding(0, 20, 0, 8)
        })

        val alphaSpinner = Spinner(this)

        alphaSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                "1 Alphabet",
                "2 Alphabets",
                "3 Alphabets",
                "4 Alphabets",
                "5 Alphabets"
            )
        )

        alphaSpinner.setSelection(
            prefs.getInt("alphaCount", 1).coerceIn(1, 5) - 1
        )

        layout.addView(alphaSpinner)

        // AUTO SEQUENCES
        layout.addView(TextView(this).apply {
            text = "Auto Sequence Settings"
            textSize = 20f
            setPadding(0, 24, 0, 8)
        })

        val nums = EditText(this).apply {
            hint = "Numbers e.g. 1234567890"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getString("numbers", "1234567890"))
        }

        layout.addView(nums)

        val letters = EditText(this).apply {
            hint = "Letters e.g. ABCXYZ"
            setText(
                prefs.getString(
                    "letters",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                )
            )
        }

        layout.addView(letters)

        // DELAY
        layout.addView(TextView(this).apply {
            text = "Auto Drop Delay (milliseconds)"
            textSize = 17f
            setPadding(0, 20, 0, 8)
        })

        val speed = EditText(this).apply {
            hint = "Delay e.g. 500"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("delay", 500).toString())
        }

        layout.addView(speed)

        // SAVE SETTINGS
        layout.addView(Button(this).apply {
            text = "SAVE SETTINGS"

            setOnClickListener {

                prefs.edit()
                    .putInt("keyboardSize", sizeSpinner.selectedItemPosition)
                    .putInt("digitCount", digitSpinner.selectedItemPosition + 1)
                    .putInt("alphaCount", alphaSpinner.selectedItemPosition + 1)

                    .putString(
                        "numbers",
                        nums.text.toString()
                            .filter { it.isDigit() }
                            .ifBlank { "1234567890" }
                    )

                    .putString(
                        "letters",
                        letters.text.toString()
                            .filter { it.isLetter() }
                            .uppercase()
                            .ifBlank { "ABCDEFGHIJKLMNOPQRSTUVWXYZ" }
                    )

                    .putInt(
                        "delay",
                        (speed.text.toString().toIntOrNull() ?: 500)
                            .coerceIn(150, 5000)
                    )
                    .apply()

                Toast.makeText(
                    this@MainActivity,
                    "Settings Saved Successfully!",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        // MOD PANEL
        layout.addView(Button(this).apply {
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
                        Intent(
                            this@MainActivity,
                            ModOverlayService::class.java
                        )
                    )
                }
            }
        })

        // ACCESSIBILITY
        layout.addView(Button(this).apply {
            text = "4. Enable Send Accessibility"

            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            }
        })

        layout.addView(TextView(this).apply {
            text = "Important: Accessibility can view screen content while enabled. Enable it only if you understand and want this feature."
            textSize = 14f
            setPadding(0, 20, 0, 0)
        })

        setContentView(
            ScrollView(this).apply {
                addView(layout)
            }
        )
    }
}
