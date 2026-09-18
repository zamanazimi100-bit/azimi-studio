package com.azimi.guardian

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }

        val title = TextView(this).apply {
            text = "AZIMI GUARDIAN"
            textSize = 28f
        }

        val status = TextView(this).apply {
            text = "\nSECURITY CORE\n\nZ VAULT: LOCKED\nZ LAB: ISOLATED\nZ RECOVERY: READY\nZ CONTROL: PROTECTED\nZ CLOUD: SEPARATED\nZ CONNECT: AUTHORIZED ONLY\n\nAZIMI AI: RESTRICTED"
            textSize = 18f
        }

        layout.addView(title)
        layout.addView(status)

        setContentView(layout)
    }
}
