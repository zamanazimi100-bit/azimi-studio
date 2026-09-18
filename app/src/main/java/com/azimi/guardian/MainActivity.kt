package com.azimi.guardian

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private fun textView(text: String, size: Float): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 16)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showDashboard()
    }

    private fun showDashboard() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.BLACK)
        }

        val title = textView("AZIMI GUARDIAN", 28f)

        val vaultButton = Button(this).apply {
            text = "Z VAULT — LOCKED"
            setOnClickListener {
                showVault()
            }
        }

        val status = textView(
            """
            
SECURITY CORE

Z LAB: ISOLATED
Z RECOVERY: READY
Z CONTROL: PROTECTED
Z CLOUD: SEPARATED
Z CONNECT: AUTHORIZED ONLY

AZIMI AI: RESTRICTED
            """.trimIndent(),
            18f
        )

        layout.addView(title)
        layout.addView(
            vaultButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        layout.addView(status)

        setContentView(layout)
    }

    private fun showVault() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.BLACK)
        }

        val title = textView("Z VAULT", 28f)

        val state = textView(
            "\nVAULT STATUS\n\n🔒 LOCKED\n\nNo secrets are stored or collected by this screen.",
            18f
        )

        val backButton = Button(this).apply {
            text = "← BACK TO GUARDIAN"
            setOnClickListener {
                showDashboard()
            }
        }

        layout.addView(title)
        layout.addView(state)
        layout.addView(backButton)

        setContentView(layout)
    }
}
