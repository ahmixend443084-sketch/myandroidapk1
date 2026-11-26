
package com.example.yallaquicksend

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    private val bg = Color.rgb(15, 12, 27)
    private val card = Color.rgb(30, 24, 48)
    private val purple = Color.rgb(151, 91, 255)
    private val lightPurple = Color.rgb(207, 177, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(176, 164, 198)

    private lateinit var root: LinearLayout

    private lateinit var panelSwitch: CheckBox
    private lateinit var autoDrop: CheckBox
    private lateinit var speedBased: CheckBox
    private lateinit var enterSend: CheckBox

    private lateinit var keyboardSize: EditText
    private lateinit var digitCount: EditText
    private lateinit var alphabetCount: EditText
    private lateinit var numberSequence: EditText
    private lateinit var alphabetSequence: EditText
    private lateinit var delay: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildScreen()
        loadSettings()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(28))
            setBackgroundColor(bg)
        }

        scroll.addView(
            root,
            ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(scroll)

        // XIMENA BRAND HEADER
        val logoFrame = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ximena_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Ximena moon logo"

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(card)
                setStroke(dp(2), purple)
            }

            clipToOutline = true
            elevation = dp(8).toFloat()
        }

        logoFrame.addView(
            logo,
            LinearLayout.LayoutParams(dp(100), dp(100))
        )
        root.addView(logoFrame)

        val title = TextView(this).apply {
            text = "FLASH 🌙"
            textSize = 32f
            gravity = Gravity.CENTER
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.BOLD
            )
            setTextColor(lightPurple)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "XIMENA • QUICK SEND"
            textSize = 12f
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, dp(4), 0, dp(20))
        }
        root.addView(subtitle)

        // MAIN PANEL
        section("MAIN CONTROL")

        val panelCard = makeCard()

        panelSwitch = makeCheckBox(
            "FLASH Floating Panel ON/OFF"
        )
        panelCard.addView(panelSwitch)

        val panelHint = label(
            "Panel OFF sirf floating panel hide karega. " +
                "Keyboard aur saved settings alag rahengi."
        )
        panelHint.textSize = 12f
        panelCard.addView(panelHint)

        panelSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("panelSwitch", checked).apply()

            if (checked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.canDrawOverlays(this)
                ) {
                    toast("Pehle overlay permission allow karo")
                    openOverlayPermission()
                    panelSwitch.isChecked = false
                    return@setOnCheckedChangeListener
                }

                startOverlay()
            } else {
                stopOverlay()
            }
        }

        root.addView(panelCard)

        // KEYBOARD SETTINGS
        section("KEYBOARD SETTINGS")

        val keyboardCard = makeCard()

        enterSend = makeCheckBox("Enter key se Send")
        keyboardCard.addView(enterSend)

        autoDrop = makeCheckBox("Auto Drop")
        keyboardCard.addView(autoDrop)

        speedBased = makeCheckBox("Speed Based Typing")
        keyboardCard.addView(speedBased)

        keyboardSize = editField(
            keyboardCard,
            "Keyboard size (default 100)",
            "100"
        )

        root.addView(keyboardCard)

        // NUMBER AND ALPHABET SETTINGS
        section("NUMBER & ALPHABET")

        val sequenceCard = makeCard()

        digitCount = editField(
            sequenceCard,
            "Number count",
            "10"
        )

        alphabetCount = editField(
            sequenceCard,
            "Alphabet count",
            "26"
        )

        numberSequence = editField(
            sequenceCard,
            "Number sequence",
            "1234567890"
        )

        alphabetSequence = editField(
            sequenceCard,
            "Alphabet sequence",
            "abcdefghijklmnopqrstuvwxyz"
        )

        root.addView(sequenceCard)

        // SPEED SETTINGS
        section("SPEED CONTROL")

        val speedCard = makeCard()

        delay = editField(
            speedCard,
            "Typing delay in milliseconds",
            "50"
        )

        val speedHint = label(
            "Kam delay se typing tez ho sakti hai. " +
                "App aur device ke response ke mutabiq speed farq ho sakti hai."
        )
        speedHint.textSize = 12f
        speedCard.addView(speedHint)

        root.addView(speedCard)

        // SAVE
        section("SAVE SETTINGS")

        val saveCard = makeCard()

        saveCard.addView(
            makeButton("SAVE ALL SETTINGS") {
                saveSettings()
            }
        )

        saveCard.addView(
            makeButton("RESTORE SAVED SETTINGS") {
                loadSettings()
                toast("Saved settings load ho gayi")
            }
        )

        root.addView(saveCard)

        // KEYBOARD ACTIVATION
        section("ENABLE KEYBOARD")

        val keyboardCard2 = makeCard()

        keyboardCard2.addView(
            makeButton("OPEN KEYBOARD SETTINGS") {
                openInputSettings()
            }
        )

        keyboardCard2.addView(
            makeButton("CHOOSE FLASH KEYBOARD") {
                chooseKeyboard()
            }
        )

        root.addView(keyboardCard2)

        // PERMISSIONS
        section("PERMISSIONS")

        val permissionsCard = makeCard()

        permissionsCard.addView(
            makeButton("OVERLAY PERMISSION") {
                openOverlayPermission()
            }
        )

        permissionsCard.addView(
            makeButton("ACCESSIBILITY SETTINGS") {
                openAccessibilitySettings()
            }
        )

        root.addView(permissionsCard)

        // FOOTER
        val footer = TextView(this).apply {
            text = "FLASH 🌙 QUICK SEND\nXIMENA EDITION"
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(24), 0, dp(8))
        }
        root.addView(footer)
    }

    private fun loadSettings() {
        panelSwitch.isChecked =
            prefs.getBoolean("panelSwitch", false)

        autoDrop.isChecked =
            prefs.getBoolean("autoDrop", false)

        speedBased.isChecked =
            prefs.getBoolean("speedBased", false)

        enterSend.isChecked =
            prefs.getBoolean("enterSend", true)

        keyboardSize.setText(
            prefs.getInt("keyboardSize", 100).toString()
        )

        digitCount.setText(
            prefs.getInt("digitCount", 10).toString()
        )

        alphabetCount.setText(
            prefs.getInt("alphabetCount", 26).toString()
        )

        numberSequence.setText(
            prefs.getString("numberSequence", "1234567890")
                ?: "1234567890"
        )

        alphabetSequence.setText(
            prefs.getString(
                "alphabetSequence",
                "abcdefghijklmnopqrstuvwxyz"
            ) ?: "abcdefghijklmnopqrstuvwxyz"
        )

        delay.setText(
            prefs.getInt("delay", 50).toString()
        )
    }

    private fun saveSettings() {
        val keyboardSizeValue =
            keyboardSize.text.toString().toIntOrNull() ?: 100

        val digitCountValue =
            digitCount.text.toString().toIntOrNull() ?: 10

        val alphabetCountValue =
            alphabetCount.text.toString().toIntOrNull() ?: 26

        val delayValue =
            delay.text.toString().toIntOrNull() ?: 50

        prefs.edit()
            .putBoolean("panelSwitch", panelSwitch.isChecked)
            .putBoolean("autoDrop", autoDrop.isChecked)
            .putBoolean("speedBased", speedBased.isChecked)
            .putBoolean("enterSend", enterSend.isChecked)
            .putInt("keyboardSize", keyboardSizeValue.coerceIn(50, 200))
            .putInt("digitCount", digitCountValue.coerceIn(1, 100))
            .putInt("alphabetCount", alphabetCountValue.coerceIn(1, 100))
            .putString(
                "numberSequence",
                numberSequence.text.toString()
            )
            .putString(
                "alphabetSequence",
                alphabetSequence.text.toString()
            )
            .putInt("delay", delayValue.coerceIn(0, 5000))
            .apply()

        toast("Settings save ho gayi")
    }

    private fun startOverlay() {
        try {
            val intent = Intent(this, ModOverlayService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            toast("Floating panel start nahi hua: ${e.message}")
        }
    }

    private fun stopOverlay() {
        try {
            stopService(
                Intent(this, ModOverlayService::class.java)
            )
        } catch (_: Exception) {
            toast("Panel band nahi ho saka")
        }
    }

    private fun openOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (_: Exception) {
                toast("Overlay settings open nahi hui")
            }
        }
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        } catch (_: Exception) {
            toast("Accessibility settings open nahi hui")
        }
    }

    private fun openInputSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            )
        } catch (_: Exception) {
            toast("Keyboard settings open nahi hui")
        }
    }

    private fun chooseKeyboard() {
        try {
            val imm = getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

            imm.showInputMethodPicker()
        } catch (_: Exception) {
            toast("Keyboard picker open nahi hua")
        }
    }

    private fun section(text: String) {
        val heading = TextView(this).apply {
            this.text = text
            textSize = 13f
            letterSpacing = 0.08f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(lightPurple)
            setPadding(dp(4), dp(18), dp(4), dp(8))
        }

        root.addView(heading)
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(
                card,
                dp(18),
                Color.rgb(65, 48, 95)
            )

            elevation = dp(3).toFloat()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(4)
            }
        }
    }

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(5), 0, dp(7))
        }
    }

    private fun makeCheckBox(text: String): CheckBox {
        return CheckBox(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(
                purple
            )
            setPadding(dp(2), dp(5), 0, dp(5))
        }
    }

    private fun editField(
        parent: LinearLayout,
        hintText: String,
        defaultValue: String
    ): EditText {
        val caption = label(hintText)
        parent.addView(caption)

        val field = EditText(this).apply {
            hint = defaultValue
            setTextColor(white)
            setHintTextColor(muted)
            textSize = 14f
            singleLine = true
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(
                Color.rgb(21, 17, 34),
                dp(10),
                Color.rgb(76, 58, 106)
            )
        }

        parent.addView(
            field,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                bottomMargin = dp(10)
            }
        )

        return field
    }

    private fun makeButton(
        text: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            textSize = 12f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            background = rounded(purple, dp(12), purple)

            setOnClickListener {
                action()
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {
                bottomMargin = dp(8)
            }
        }
    }

    private fun rounded(
        color: Int,
        radius: Int,
        strokeColor: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            setStroke(dp(1), strokeColor)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
