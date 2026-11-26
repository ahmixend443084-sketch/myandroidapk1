
package com.example.yallaquicksend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val bg = Color.rgb(12, 10, 23)
    private val card = Color.rgb(29, 23, 48)
    private val purple = Color.rgb(155, 75, 255)
    private val lightPurple = Color.rgb(205, 170, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(178, 167, 202)

    private lateinit var panelSwitch: Switch
    private lateinit var autoDrop: CheckBox
    private lateinit var speedBased: CheckBox
    private lateinit var enterSend: CheckBox
    private lateinit var keyboardSize: Spinner
    private lateinit var digitCount: EditText
    private lateinit var alphabetCount: EditText
    private lateinit var numberSequence: EditText
    private lateinit var alphabetSequence: EditText
    private lateinit var delay: EditText

    private var updatingPanelSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            clipToPadding = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(24))
        }

        scroll.addView(root)
        setContentView(scroll)

        // BRAND HEADER
        val logo = TextView(this).apply {
            text = "🌙"
            textSize = 48f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }
        root.addView(logo)

        val title = TextView(this).apply {
            text = "FLASH"
            textSize = 34f
            gravity = Gravity.CENTER
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.BOLD
            )
            setTextColor(lightPurple)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "QUICK SEND KEYBOARD"
            textSize = 12f
            letterSpacing = 0.18f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, dp(2), 0, dp(20))
        }
        root.addView(subtitle)

        // PANEL MASTER ON / OFF
        val panelCard = makeCard()
        panelCard.orientation = LinearLayout.VERTICAL

        val panelTitle = TextView(this).apply {
            text = "FLOATING PANEL"
            textSize = 12f
            letterSpacing = 0.12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(lightPurple)
        }
        panelCard.addView(panelTitle)

        val panelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(4))
        }

        val panelInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        panelInfo.addView(TextView(this).apply {
            text = "FLASH Panel"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
        })

        panelInfo.addView(TextView(this).apply {
            text = "Show or hide the floating controls"
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(4), 0, 0)
        })

        panelRow.addView(
            panelInfo,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        panelSwitch = Switch(this).apply {
            text = ""
            showText = false
            isChecked = prefs.getBoolean("panelEnabled", false)
            thumbTintList = ColorStateList.valueOf(white)
            trackTintList = ColorStateList.valueOf(purple)
        }
        panelRow.addView(panelSwitch)
        panelCard.addView(panelRow)

        val panelStatus = TextView(this).apply {
            text = if (panelSwitch.isChecked) {
                "● PANEL ON"
            } else {
                "● PANEL OFF"
            }
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(
                if (panelSwitch.isChecked) {
                    Color.rgb(100, 235, 160)
                } else {
                    muted
                }
            )
            setPadding(0, dp(8), 0, 0)
        }
        panelCard.addView(panelStatus)
        root.addView(panelCard, blockParams(dp(12)))

        panelSwitch.setOnCheckedChangeListener { _, enabled ->
            if (updatingPanelSwitch) return@setOnCheckedChangeListener

            if (enabled) {
                if (!Settings.canDrawOverlays(this)) {
                    updatingPanelSwitch = true
                    panelSwitch.isChecked = false
                    updatingPanelSwitch = false

                    prefs.edit().putBoolean("panelEnabled", false).apply()
                    panelStatus.text = "● PANEL OFF"
                    panelStatus.setTextColor(muted)

                    toast("Pehle floating permission allow karo")
                    openOverlayPermission()
                    return@setOnCheckedChangeListener
                }

                saveSettings(false)

                try {
                    startService(
                        Intent(this, ModOverlayService::class.java)
                    )
                    prefs.edit().putBoolean("panelEnabled", true).apply()
                    panelStatus.text = "● PANEL ON"
                    panelStatus.setTextColor(
                        Color.rgb(100, 235, 160)
                    )
                    toast("FLASH Panel ON")
                } catch (e: Exception) {
                    updatingPanelSwitch = true
                    panelSwitch.isChecked = false
                    updatingPanelSwitch = false
                    prefs.edit().putBoolean("panelEnabled", false).apply()
                    panelStatus.text = "● PANEL OFF"
                    panelStatus.setTextColor(muted)
                    toast("Panel start nahi hua: ${e.message}")
                }
            } else {
                // Sirf overlay service band hoti hai.
                // Keyboard aur saved settings ko touch nahi karte.
                stopService(
                    Intent(this, ModOverlayService::class.java)
                )
                prefs.edit().putBoolean("panelEnabled", false).apply()
                panelStatus.text = "● PANEL OFF"
                panelStatus.setTextColor(muted)
                toast("Floating panel OFF")
            }
        }

        // KEYBOARD SETTINGS
        root.addView(section("⌨  KEYBOARD SETTINGS"))

        val keyboardCard = makeCard()

        keyboardCard.addView(label("Keyboard Size"))

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
            background = rounded(card, 12)
        }
        keyboardCard.addView(keyboardSize, fieldParams())

        enterSend = makeCheckBox(
            "Enter key sends message",
            prefs.getBoolean("enterSend", true)
        )
        keyboardCard.addView(enterSend)

        root.addView(keyboardCard, blockParams(dp(12)))

        // AUTO DROP OPTIONS
        root.addView(section("⚡  AUTOMATION"))

        val automationCard = makeCard()

        autoDrop = makeCheckBox(
            "Auto Drop",
            prefs.getBoolean("autoDrop", false)
        )
        automationCard.addView(autoDrop)

        speedBased = makeCheckBox(
            "Speed Based",
            prefs.getBoolean("speedBased", true)
        )
        automationCard.addView(speedBased)

        root.addView(automationCard, blockParams(dp(12)))

        // SEQUENCE SETTINGS
        root.addView(section("Aa  SEQUENCE SETTINGS"))

        val sequenceCard = makeCard()

        sequenceCard.addView(label("Number Count (1-100)"))
        digitCount = editField(
            prefs.getInt("digitCount", 10).toString(),
            false
        )
        sequenceCard.addView(digitCount, fieldParams())

        sequenceCard.addView(label("Alphabet Count (1-3)"))
        alphabetCount = editField(
            prefs.getInt("alphabetCount", 1).coerceIn(1, 3).toString(),
            false
        )
        sequenceCard.addView(alphabetCount, fieldParams())

        sequenceCard.addView(label("Numbers Sequence"))
        numberSequence = editField(
            prefs.getString("numberSequence", "1234567890")
                ?: "1234567890",
            true
        )
        sequenceCard.addView(numberSequence, fieldParams())

        sequenceCard.addView(label("Alphabet Sequence (ABC / abc)"))
        alphabetSequence = editField(
            prefs.getString(
                "alphabetSequence",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            ) ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            true
        )
        sequenceCard.addView(alphabetSequence, fieldParams())

        root.addView(sequenceCard, blockParams(dp(12)))

        // DELAY
        root.addView(section("◷  SPEED CONTROL"))

        val speedCard = makeCard()
        speedCard.addView(label("Auto Drop Delay (milliseconds)"))

        delay = editField(
            prefs.getInt("delay", 0).toString(),
            false
        )
        speedCard.addView(delay, fieldParams())

        speedCard.addView(TextView(this).apply {
            text = "0 ms = no intentional waiting. Delay Auto Drop sending par apply hota hai."
            textSize = 12f
            setTextColor(muted)
            setPadding(dp(4), dp(4), dp(4), dp(6))
        })

        root.addView(speedCard, blockParams(dp(12)))

        // ACTION BUTTONS
        root.addView(section("⚙  QUICK ACTIONS"))

        root.addView(
            makeButton("SAVE SETTINGS", true) {
                saveSettings()
            },
            blockParams(dp(8))
        )

        root.addView(
            makeButton("ENABLE FLASH KEYBOARD") {
                openSettings(Settings.ACTION_INPUT_METHOD_SETTINGS)
            },
            blockParams(dp(8))
        )

        root.addView(
            makeButton("CHOOSE FLASH KEYBOARD") {
                try {
                    val manager = getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager
                    manager.showInputMethodPicker()
                } catch (e: Exception) {
                    toast("Keyboard picker open nahi ho saka")
                }
            },
            blockParams(dp(8))
        )

        root.addView(
            makeButton("ALLOW FLOATING PERMISSION") {
                if (!Settings.canDrawOverlays(this)) {
                    openOverlayPermission()
                } else {
                    toast("Floating permission already allowed")
                }
            },
            blockParams(dp(8))
        )

        root.addView(
            makeButton("ACCESSIBILITY SETTINGS") {
                openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            },
            blockParams(dp(8))
        )

        root.addView(TextView(this).apply {
            text = "FLASH🌙  •  Quick Send Tools"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, dp(24), 0, dp(8))
        })
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = rounded(card, 18)
        }
    }

    private fun section(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 13f
            letterSpacing = 0.08f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(lightPurple)
            setPadding(dp(4), dp(18), dp(4), dp(8))
        }
    }

    private fun label(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(white)
            setPadding(dp(4), dp(8), dp(4), dp(5))
        }
    }

    private fun makeCheckBox(
        value: String,
        checked: Boolean
    ): CheckBox {
        return CheckBox(this).apply {
            text = value
            textSize = 14f
            setTextColor(white)
            isChecked = checked
            buttonTintList = ColorStateList.valueOf(lightPurple)
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }
    }

    private fun editField(
        initial: String,
        allowText: Boolean
    ): EditText {
        return EditText(this).apply {
            setText(initial)
            setSingleLine(true)
            textSize = 15f
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = rounded(
                Color.rgb(20, 16, 34),
                12
            )

            inputType = if (allowText) {
                android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            } else {
                android.text.InputType.TYPE_CLASS_NUMBER
            }
        }
    }

    private fun makeButton(
        value: String,
        highlighted: Boolean = false,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = value
            isAllCaps = false
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            background = rounded(
                if (highlighted) purple else card,
                14
            )
            setPadding(dp(12), dp(10), dp(12), dp(10))
            stateListAnimator = null
            setOnClickListener { action() }
        }
    }

    private fun rounded(
        color: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), Color.rgb(72, 49, 105))
        }
    }

    private fun fieldParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(8)
        }
    }

    private fun blockParams(
        topMargin: Int = 0
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            this.topMargin = topMargin
        }
    }

    private fun saveSettings(showToast: Boolean = true) {
        val digits = digitCount.text.toString()
            .toIntOrNull()?.coerceIn(1, 100) ?: 10

        val letters = alphabetCount.text.toString()
            .toIntOrNull()?.coerceIn(1, 3) ?: 1

        val delayMs = delay.text.toString()
            .toIntOrNull()?.coerceIn(0, 60000) ?: 0

        val numbers = numberSequence.text.toString()
            .ifBlank { "1234567890" }

        val alphabets = alphabetSequence.text.toString()
            .ifBlank { "ABCDEFGHIJKLMNOPQRSTUVWXYZ" }

        prefs.edit()
            .putBoolean("enterSend", enterSend.isChecked)
            .putBoolean("autoDrop", autoDrop.isChecked)
            .putBoolean("speedBased", speedBased.isChecked)
            .putInt(
                "keyboardSize",
                keyboardSize.selectedItemPosition.coerceIn(0, 2)
            )
            .putInt("digitCount", digits)
            .putInt("alphabetCount", letters)
            .putString("numberSequence", numbers)
            .putString("alphabetSequence", alphabets)
            .putInt("delay", delayMs)
            .apply()

        if (showToast) {
            toast("FLASH settings saved")
        }
    }

    private fun openOverlayPermission() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
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
