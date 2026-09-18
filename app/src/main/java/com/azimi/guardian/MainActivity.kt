package com.azimi.guardian

import android.app.Activity
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {

    private val bg = 0xFF050505.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val gray = 0xFFAAAAAA.toInt()
    private val green = 0xFF50DC8C.toInt()
    private val amber = 0xFFF0BE50.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GuardianStorage.initializeVault(this)

        showHome()
    }

    private fun baseLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 38, 28, 30)
            setBackgroundColor(bg)
        }
    }

    private fun title(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 28f
            setTextColor(white)
            setPadding(0, 0, 0, 8)
        }
    }

    private fun subtitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(gray)
            setPadding(0, 0, 0, 18)
        }
    }

    private fun section(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(gray)
            setPadding(0, 22, 0, 8)
        }
    }

    private fun info(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(white)
            setPadding(0, 8, 0, 12)
        }
    }

    private fun status(text: String, color: Int): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(color)
            setPadding(0, 7, 0, 7)
        }
    }

    private fun actionButton(
        text: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 15f
            setOnClickListener { action() }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 6)
            }
        }
    }

    private fun screen(content: LinearLayout): ScrollView {
        return ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(content)
        }
    }

    private fun header(
        layout: LinearLayout,
        name: String,
        description: String
    ) {
        layout.addView(title(name))
        layout.addView(subtitle(description))
        layout.addView(status("────────────────────────────", gray))
    }

    private fun showHome() {
        val layout = baseLayout()

        header(
            layout,
            "AZIMI CORE",
            "AZIMI GUARDIAN · PERSONAL COMMAND CENTER"
        )

        layout.addView(status("● GUARDIAN ONLINE", green))

        layout.addView(
            info(
                "Live device control and privacy-first security foundation."
            )
        )

        layout.addView(section("LIVE SYSTEM STATE"))

        layout.addView(
            status(
                "● Z VAULT        ${GuardianStorage.getVaultStatus(this)}",
                amber
            )
        )

        layout.addView(
            status(
                "● AI POLICY      ${GuardianStorage.getAIMemoryPolicy(this)}",
                amber
            )
        )

        layout.addView(status("● Z SHIELD       NOT CONFIGURED", amber))
        layout.addView(status("● Z CONNECT      AUTHORIZATION REQUIRED", amber))

        layout.addView(section("AZIMI SPACES"))

        layout.addView(actionButton("🛡  Z CONTROL") {
            showControl()
        })

        layout.addView(actionButton("🔐  Z VAULT") {
            showVault()
        })

        layout.addView(actionButton("🛠  Z RECOVERY") {
            showRecovery()
        })

        layout.addView(actionButton("🛡  Z SHIELD") {
            showShield()
        })

        layout.addView(actionButton("🤖  AZIMI AI") {
            showAI()
        })

        layout.addView(actionButton("🧪  Z LAB") {
            showLab()
        })

        layout.addView(section("SECURITY PRINCIPLE"))

        layout.addView(
            info(
                "Guardian does not silently access passwords, " +
                    "verification codes, recovery codes, API keys " +
                    "or private credentials."
            )
        )

        setContentView(screen(layout))
    }

    private fun showControl() {
        val layout = baseLayout()

        header(
            layout,
            "Z CONTROL",
            "LIVE DEVICE & SECURITY STATE"
        )

        layout.addView(status("● CONTROL ENGINE: ACTIVE", green))

        layout.addView(section("DEVICE"))

        layout.addView(info("Manufacturer: ${Build.MANUFACTURER}"))
        layout.addView(info("Model: ${Build.MODEL}"))
        layout.addView(info("Android: ${Build.VERSION.RELEASE}"))
        layout.addView(info("SDK: ${Build.VERSION.SDK_INT}"))

        layout.addView(section("STORAGE"))

        val stat = StatFs(Environment.getDataDirectory().path)

        val totalBytes = stat.totalBytes
        val freeBytes = stat.availableBytes
        val usedBytes = totalBytes - freeBytes

        layout.addView(
            info(
                "Used: ${formatBytes(usedBytes)}\n" +
                    "Free: ${formatBytes(freeBytes)}\n" +
                    "Total: ${formatBytes(totalBytes)}"
            )
        )

        layout.addView(section("BATTERY"))

        val batteryManager =
            getSystemService(BATTERY_SERVICE) as BatteryManager

        val battery = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        )

        layout.addView(
            status(
                "● BATTERY: ${if (battery >= 0) "$battery%" else "UNKNOWN"}",
                if (battery >= 20) green else amber
            )
        )

        layout.addView(section("GUARDIAN SECURITY"))

        layout.addView(
            status(
                "● VAULT: ${GuardianStorage.getVaultStatus(this)}",
                amber
            )
        )

        layout.addView(
            status(
                "● AI MEMORY: ${GuardianStorage.getAIMemoryPolicy(this)}",
                amber
            )
        )

        layout.addView(
            status(
                "● VPN: NOT CONFIGURED",
                amber
            )
        )

        layout.addView(
            status(
                "● EXTERNAL ACCESS: RESTRICTED",
                amber
            )
        )

        layout.addView(
            info(
                "These values are read from the Android device " +
                    "and Guardian's local state."
            )
        )

        back(layout)

        setContentView(screen(layout))
    }

    private fun showVault() {
        val layout = baseLayout()

        header(
            layout,
            "Z VAULT",
            "LOCAL PROTECTED STORAGE"
        )

        layout.addView(
            status(
                "● VAULT: ${GuardianStorage.getVaultStatus(this)}",
                amber
            )
        )

        layout.addView(
            info(
                "Vault initialization is active. " +
                    "The next layer will add real user-controlled " +
                    "unlock protection and encrypted records."
            )
        )

        layout.addView(
            actionButton("Vault Status") {
                message(
                    "Z VAULT",
                    "Initialized: ${
                        GuardianStorage.isVaultInitialized(this)
                    }\n\n" +
                        "Status: ${
                            GuardianStorage.getVaultStatus(this)
                        }\n\n" +
                        "Memory policy: ${
                            GuardianStorage.getAIMemoryPolicy(this)
                        }"
                )
            }
        )

        back(layout)

        setContentView(screen
