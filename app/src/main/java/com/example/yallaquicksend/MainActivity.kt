
package com.example.yallaquicksend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        root.addView(TextView(this).apply {
            text = "YALLA QUICK SEND"
            textSize = 25f
            setPadding(0, 0, 0, 20)
        })

        root.addView(TextView(this).apply {
            text = "KEYBOARD SETUP"
            textSize = 18f
        })

        addButton(root, "1. Enable Keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        addButton(root, "2. Choose Yalla Quick Send") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        }

        addHeading(root, "Keyboard Settings")

        val sizeSpinner = addSpinner(
            root,
            listOf("Small", "Medium", "Large"),
            prefs.getInt("keyboardSize", 1).coerceIn(0, 2)
        )

        val enterSend = addCheckBox(
            root,
            "ENTER = SEND (Green Enter key)",
            prefs.getBoolean("enterSend", true)
        )

        val autoDrop = addCheckBox(
            root,
            "Auto Drop",
            prefs.getBoolean("autoDrop", false)
        )

        val speedBased = addCheckBox(
            root,
            "Speed Based",
            prefs.getBoolean("speedBased", false)
        )

        addHeading(root, "Auto Send Settings")

        val digitSpinner = addSpinner(
            root,
            listOf("1 Digit", "2 Digits", "3 Digits", "4 Digits", "5 Digits"),
            prefs.getInt("digitCount", 1).coerceIn(1, 5) - 1
        )

        val alphaSpinner = addSpinner(
            root,
            listOf(
                "1 Alphabet", "2 Alphabets", "3 Alphabets",
                "4 Alphabets", "5 Alphabets", "6 Alphabets"
            ),
            prefs.getInt("alphaCount", 1).coerceIn(1, 6) - 1
        )

        val numbers = EditText(this).apply {
            hint = "Numbers sequence"
            setSingleLine()
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getString("numbers", "1234567890"))
        }
        root.addView(numbers)

        val letters = EditText(this).apply {
            hint = "Alphabet sequence"
            setSingleLine()
            setText(prefs.getString("letters", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
        }
        root.addView(letters)

        addHeading(root, "Delay Settings")

        val delay = EditText(this).apply {
            hint = "Delay in milliseconds"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("delay", 500).toString())
        }
        root.addView(delay)

        addButton(root, "SAVE SETTINGS") {
            prefs.edit()
                .putInt("keyboardSize", sizeSpinner.selectedItemPosition)
                .putBoolean("enterSend", enterSend.isChecked)
                .putBoolean("autoDrop", autoDrop.isChecked)
                .putBoolean("speedBased", speedBased.isChecked)
                .putInt("digitCount", digitSpinner.selectedItemPosition + 1)
                .putInt("alphaCount", alphaSpinner.selectedItemPosition + 1)
                .putString(
                    "numbers",
                    numbers.text.toString().filter { it.isDigit() }
                        .ifBlank { "1234567890" }
                )
                .putString(
                    "letters",
                    letters.text.toString().filter { it.isLetter() }
                        .uppercase().ifBlank { "ABCDEFGHIJKLMNOPQRSTUVWXYZ" }
                )
                .putInt(
                    "delay",
                    (delay.text.toString().toIntOrNull() ?: 500)
                        .coerceIn(150, 5000)
                )
                .apply()

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        }

        addHeading(root, "Floating Panel")

        addButton(root, "3. Enable Overlay Permission") {
            if (Build.VERSION.SDK_INT >= 23 &&
                !Settings.canDrawOverlays(this)
            ) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                startOverlay()
            }
        }

        addButton(root, "4. Start Floating Panel") {
            if (Build.VERSION.SDK_INT >= 23 &&
                !Settings.canDrawOverlays(this)
            ) {
                Toast.makeText(
                    this,
                    "First enable overlay permission",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                startOverlay()
            }
        }

        addButton(root, "5. Enable Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        root.addView(TextView(this).apply {
            text = "Accessibility can read visible screen content. Enable it only if you want to use the send-control feature."
            textSize = 13f
            setPadding(0, 18, 0, 0)
        })

        setContentView(ScrollView(this).apply {
            addView(root)
        })
    }

    private fun addHeading(root: LinearLayout, title: String) {
        root.addView(TextView(this).apply {
            text = title
            textSize = 19f
            setPadding(0, 22, 0, 8)
        })
    }

    private fun addButton(
        root: LinearLayout,
        title: String,
        action: () -> Unit
    ) {
        root.addView(Button(this).apply {
            text = title
            setOnClickListener { action() }
        })
    }

    private fun addSpinner(
        root: LinearLayout,
        items: List<String>,
        selected: Int
    ): Spinner {
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            items
        )
        spinner.setSelection(selected)
        root.addView(spinner)
        return spinner
    }

    private fun addCheckBox(
        root: LinearLayout,
        title: String,
        checked: Boolean
    ): CheckBox {
        val checkBox = CheckBox(this).apply {
            text = title
            isChecked = checked
        }
        root.addView(checkBox)
        return checkBox
    }

    private fun startOverlay() {
        val intent = Intent(this, ModOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
