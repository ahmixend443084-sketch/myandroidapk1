
package com.example.yallaquicksend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val bgColor = Color.rgb(15, 13, 27)
    private val cardColor = Color.rgb(30, 26, 47)
    private val purple = Color.rgb(174, 132, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(190, 183, 210)

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
            setPadding(dp(18), dp(12), dp(18), dp(16))
            setBackgroundColor(bgColor)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(12))
        }

        val title = TextView(this).apply {
            text = "FLASH🌙"
            textSize = 34f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(purple)
            setPadding(0, dp(20), 0, dp(4))
        }
        content.addView(title)

        val subtitle = TextView(this).apply {
            text = "QUICK SEND KEYBOARD"
            textSize = 13f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, 0, 0, dp(20))
        }
        content.addView(subtitle)

        content.addView(sectionTitle("KEYBOARD"))

        keyboardSize = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "Small keyboard",
                    "Medium keyboard",
                    "Large keyboard"
                )
            ).also {
                it.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
            }

            setSelection(
                prefs.getInt("keyboardSize", 1).coerceIn(0, 2)
            )
            background = roundedBackground(cardColor, 12)
        }
        content.addView(
            keyboardSize,
            fieldLayout()
        )

        content.addView(sectionTitle("SEND OPTIONS"))

        enterSend = makeCheckBox(
            "Enter key sends message",
            prefs.getBoolean("enterSend", true)
        )
        content.addView(enterSend)

        autoDrop = makeCheckBox(
            "Auto Drop",
            prefs.getBoolean("autoDrop", false)
        )
        content.addView(autoDrop)

        speedBased = makeCheckBox(
            "Speed Based",
            prefs.getBoolean("speedBased", true)
        )
        content.addView(speedBased)

        content.addView(sectionTitle("SEQUENCE SETTINGS"))

        digitCount = editField(
            "Number count",
            prefs.getInt("digitCount", 10).toString(),
            false
        )
        content.addView(label("Number Count"))
        content.addView(digitCount, fieldLayout())

        alphabetCount = editField(
            "Alphabet count",
            prefs.getInt("alphabetCount", 26).toString(),
            false
        )
        content.addView(label("Alphabet Count"))
        content.addView(alphabetCount, fieldLayout())

        numberSequence = editField(
            "Numbers sequence",
            prefs.getString("numberSequence", "1234567890")
                ?: "1234567890",
            true
        )
        content.addView(label("Numbers Sequence"))
        content.addView(numberSequence, fieldLayout())

        alphabetSequence = editField(
            "Alphabet sequence",
            prefs.getString(
                "alphabetSequence",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            ) ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            true
        )
        content.addView(label("Alphabet Sequence"))
        content.addView(alphabetSequence, fieldLayout())

        content.addView(sectionTitle("SPEED CONTROL"))

        delay = editField(
            "Delay in milliseconds",
            prefs.getInt("delay", 100).toString(),
            false
        )
        content.addView(label("Auto Drop Delay (ms)"))
        content.addView(delay, fieldLayout())

        content.addView(
            makeButton("SAVE SETTINGS", true) {
                saveSettings()
            },
            buttonLayout()
        )

        content.addView(
            makeButton("ENABLE FLASH KEYBOARD") {
                openSettings(Settings.ACTION_INPUT_METHOD_SETTINGS)
            },
            buttonLayout()
        )

        content.addView(
            makeButton("CHOOSE FLASH KEYBOARD") {
                try {
                    val imm = getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager
                    imm.showInputMethodPicker()
                } catch (e: Exception) {
                    toast("Keyboard picker open nahi ho saka")
                }
            },
            buttonLayout()
        )

        content.addView(
            makeButton("ALLOW FLOATING PANEL") {
                if (!Settings.canDrawOverlays(this)) {
                    openOverlayPermission()
                } else {
                    toast("Floating permission pehle se allowed hai")
                }
            },
            buttonLayout()
        )

        content.addView(
            makeButton("START FLOATING PANEL", true) {
                saveSettings(showToast = false)

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
                    openOverlayPermission()
                }
            },
            buttonLayout()
        )

        content.addView(
            makeButton("ACCESSIBILITY SETTINGS") {
                openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            },
            buttonLayout()
        )

        val footer = TextView(this).apply {
            text = "FLASH🌙  •  Quick Send Tools"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, dp(22), 0, dp(8))
        }
        content.addView(footer)

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

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 12f
            letterSpacing = 0.1f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(purple)
            setPadding(dp(4), dp(22), dp(4), dp(8))
        }
    }

    private fun label(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(white)
            setPadding(dp(4), dp(10), dp(4), dp(4))
        }
    }

    private fun makeCheckBox(
        textValue: String,
        checked: Boolean
    ): CheckBox {
        return CheckBox(this).apply {
            text = textValue
            textSize = 15f
            setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(purple)
            isChecked = checked
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }
    }

    private fun editField(
        hintText: String,
        initialValue: String,
        allowText: Boolean
    ): EditText {
        return EditText(this).apply {
            hint = hintText
            setSingleLine(true)
            setText(initialValue)
            textSize = 15f
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = roundedBackground(cardColor, 12)

            if (!allowText) {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            } else {
                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            }
        }
    }

    private fun makeButton(
        textValue: String,
        highlighted: Boolean = false,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = textValue
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (highlighted) white else purple)
            background = roundedBackground(
                if (highlighted) Color.rgb(105, 66, 190) else cardColor,
                14
            )
            setPadding(dp(12), dp(8), dp(12), dp(8))
            stateListAnimator = null
            setOnClickListener { action() }
        }
    }

    private fun roundedBackground(
        color: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun fieldLayout(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(8)
        }
    }

    private fun buttonLayout(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(6)
            bottomMargin = dp(4)
        }
    }

    private fun saveSettings(showToast: Boolean = true) {
        val digits = digitCount.text.toString()
            .toIntOrNull()?.coerceIn(1, 100) ?: 10

        val letters = alphabetCount.text.toString()
            .toIntOrNull()?.coerceIn(1, 100) ?: 26

        val delayMs = delay.text.toString()
            .toIntOrNull()?.coerceIn(0, 60000) ?: 100

        prefs.edit()
            .putBoolean("enterSend", enterSend.isChecked)
            .putBoolean("autoDrop", autoDrop.isChecked)
            .putBoolean("speedBased", speedBased.isChecked)
            .putInt("keyboardSize", keyboardSize.selectedItemPosition)
            .putInt("digitCount", digits)
            .putInt("alphabetCount", letters)
            .putString("numberSequence", numberSequence.text.toString())
            .putString("alphabetSequence", alphabetSequence.text.toString())
            .putInt("delay", delayMs)
            .apply()

        if (showToast) {
            toast("FLASH settings saved")
        }
    }

    private fun openOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            toast("Overlay settings open nahi ho sakin")
        }
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            toast("Settings open nahi ho sakin")
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
