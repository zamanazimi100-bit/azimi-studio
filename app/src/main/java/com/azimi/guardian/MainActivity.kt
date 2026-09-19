package com.azimi.guardian

import android.app.Activity
import android.content.Intent
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
    private val blue = 0xFF4DA6FF.toInt()
    private val purple = 0xFFB56CFF.toInt()
    private val cyan = 0xFF40E0D0.toInt()
    private val red = 0xFFFF6B6B.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GuardianStorage.lockVault(this)

        showHome()
    }

    private fun baseLayout(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 38, 28, 30)
            setBackgroundColor(bg)
        }

    private fun title(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 28f
            setTextColor(white)
            setPadding(0, 0, 0, 8)
        }

    private fun subtitle(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(gray)
            setPadding(0, 0, 0, 18)
        }

    private fun section(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(gray)
            setPadding(0, 22, 0, 8)
        }

    private fun info(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(white)
            setPadding(0, 8, 0, 12)
        }

    private fun status(text: String, color: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(color)
            setPadding(0, 7, 0, 7)
        }

    private fun actionButton(
        text: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            this.text = text
            textSize = 15f
            isAllCaps = false

            setTextColor(
                when {
                    text.contains("Z CONTROL") -> blue
                    text.contains("Z VAULT") -> purple
                    text.contains("Z RECOVERY") -> amber
                    text.contains("Z SHIELD") -> cyan
                    text.contains("AZIMI AI") -> purple
                    text.contains("Z LAB") -> green
                    else -> white
                }
            )

            setOnClickListener {
                action()
            }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 6)
            }
        }

    private fun screen(content: LinearLayout): ScrollView =
        ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(content)
        }

    private fun header(
        layout: LinearLayout,
        name: String,
        description: String
    ) {
        layout.addView(title(name))
        layout.addView(subtitle(description))
        layout.addView(status("------------------------------", gray))
    }

    private fun showHome() {
        val layout = baseLayout()

        header(
            layout,
            "AZIMI CORE",
            "AZIMI GUARDIAN · PERSONAL COMMAND CENTER"
        )

        layout.addView(
            status(
                "● GUARDIAN ONLINE",
                green
            )
        )

        layout.addView(
            info(
                "Privacy-first device control and security foundation."
            )
        )

        layout.addView(section("LIVE SYSTEM STATE"))

        layout.addView(
            status(
                "● Z VAULT     ${GuardianStorage.getVaultStatus(this)}",
                if (GuardianStorage.getVaultStatus(this) == "UNLOCKED") {
                    green
                } else {
                    amber
                }
            )
        )

        layout.addView(
            status(
                "● AI POLICY   ${GuardianStorage.getAIMemoryPolicy(this)}",
                amber
            )
        )

        layout.addView(
            status(
                "● Z SHIELD    NOT CONFIGURED",
                amber
            )
        )

        layout.addView(
            status(
                "● Z CONNECT   AUTHORIZATION REQUIRED",
                amber
            )
        )

        layout.addView(section("AZIMI SPACES"))

        layout.addView(
            actionButton("🛡  Z CONTROL") {
                showControl()
            }
        )

        layout.addView(
            actionButton("🔐  Z VAULT") {
                showVault()
            }
        )

        layout.addView(
            actionButton("🛠  Z RECOVERY") {
                showRecovery()
            }
        )

        layout.addView(
            actionButton("🛡  Z SHIELD") {
                showShield()
            }
        )

        layout.addView(
            actionButton("🤖  AZIMI AI") {
                showAI()
            }
        )

        layout.addView(
            actionButton("🧪  Z LAB") {
                showLab()
            }
        )

        layout.addView(section("SECURITY PRINCIPLE"))

        layout.addView(
            info(
                "Guardian never silently accesses passwords, " +
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

        layout.addView(
            status(
                "● CONTROL ENGINE ACTIVE",
                green
            )
        )

        layout.addView(section("DEVICE"))

        layout.addView(
            info("Manufacturer: ${Build.MANUFACTURER}")
        )

        layout.addView(
            info("Model: ${Build.MODEL}")
        )

        layout.addView(
            info("Android: ${Build.VERSION.RELEASE}")
        )

        layout.addView(
            info("SDK: ${Build.VERSION.SDK_INT}")
        )

        layout.addView(section("STORAGE"))

        val statFs = StatFs(
            Environment.getDataDirectory().path
        )

        val totalBytes = statFs.totalBytes
        val freeBytes = statFs.availableBytes
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

        val batteryLevel =
            batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )

        layout.addView(
            status(
                "● BATTERY: ${
                    if (batteryLevel >= 0) {
                        "$batteryLevel%"
                    } else {
                        "UNKNOWN"
                    }
                }",
                if (batteryLevel >= 20) {
                    green
                } else {
                    amber
                }
            )
        )

        layout.addView(section("GUARDIAN STATE"))

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
                    "and Guardian local state."
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

        val vaultStatus =
            GuardianStorage.getVaultStatus(this)

        layout.addView(
            status(
                "● VAULT: $vaultStatus",
                if (vaultStatus == "UNLOCKED") {
                    green
                } else {
                    amber
                }
            )
        )

        layout.addView(
            info(
                "Protected local storage is encrypted with " +
                    "Android Keystore. Authentication is required " +
                    "before Vault access."
            )
        )

        if (!VaultAuth.isDeviceSecure(this)) {

            layout.addView(
                status(
                    "● DEVICE LOCK: NOT CONFIGURED",
                    red
                )
            )

            layout.addView(
                info(
                    "Set a screen lock, PIN, pattern, or supported " +
                        "device authentication in Android Settings " +
                        "before using Z VAULT."
                )
            )

        } else {

            layout.addView(
                status(
                    "● DEVICE AUTHENTICATION: AVAILABLE",
                    green
                )
            )

            if (vaultStatus == "LOCKED") {

                layout.addView(
                    actionButton("🔓  UNLOCK Z VAULT") {

                        if (VaultAuth.requestAuthentication(this)) {
                            return@actionButton
                        }

                        message(
                            "Z VAULT",
                            "Android device authentication could not be started."
                        )
                    }
                )

            } else {

                layout.addView(
                    actionButton("🔒  LOCK Z VAULT") {

                        GuardianStorage.lockVault(this)

                        showVault()
                    }
                )
            }
        }

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
                        "Device authentication: ${
                            if (VaultAuth.isDeviceSecure(this)) {
                                "AVAILABLE"
                            } else {
                                "NOT CONFIGURED"
                            }
                        }\n\n" +
                        "Encryption: ANDROID KEYSTORE + AES/GCM"
                )
            }
        )

        back(layout)

        setContentView(screen(layout))
    }

    private fun showRecovery() {
        val layout = baseLayout()

        header(
            layout,
            "Z RECOVERY",
            "SAFE RECOVERY CENTER"
        )

        layout.addView(
            status(
                "● RECOVERY FOUNDATION READY",
                green
            )
        )

        layout.addView(
            info(
                "Recovery operations require explicit user action. " +
                    "No destructive operation is performed automatically."
            )
        )

        layout.addView(
            actionButton("Recovery Status") {

                message(
                    "Z RECOVERY",
                    "Recovery foundation is available.\n\n" +
                        "No backup or restore operation has been started."
                )
            }
        )

        back(layout)

        setContentView(screen(layout))
    }

    private fun showShield() {
        val layout = baseLayout()

        header(
            layout,
            "Z SHIELD",
            "NETWORK PROTECTION"
        )

        layout.addView(
            status(
                "● VPN: NOT CONFIGURED",
                amber
            )
        )

        layout.addView(
            info(
                "Guardian does not claim VPN protection until " +
                    "a real encrypted tunnel is configured."
            )
        )

        layout.addView(
            actionButton("Shield Status") {

                message(
                    "Z SHIELD",
                    "VPN status: NOT CONFIGURED\n\n" +
                        "No encrypted tunnel is currently active."
                )
            }
        )

        back(layout)

        setContentView(screen(layout))
    }

    private fun showAI() {
        val layout = baseLayout()

        header(
            layout,
            "AZIMI AI",
            "INTELLIGENCE LAYER"
        )

        layout.addView(
            status(
                "● AI: RESTRICTED",
                amber
            )
        )

        layout.addView(
            info(
                "AI can use authorized information only. " +
                    "Protected credentials remain outside AI memory."
            )
        )

        layout.addView(
            actionButton("AI Memory Policy") {

                message(
                    "AZIMI AI",
                    "Current policy:\n\n" +
                        GuardianStorage.getAIMemoryPolicy(this) +
                        "\n\nCredentials are excluded."
                )
            }
        )

        back(layout)

        setContentView(screen(layout))
    }

    private fun showLab() {
        val layout = baseLayout()

        header(
            layout,
            "Z LAB",
            "BUILD · TEST · EXPERIMENT"
        )

        layout.addView(
            status(
                "● LOCAL WORKSPACE FOUNDATION",
                green
            )
        )

        layout.addView(
            info(
                "Z LAB is prepared for real project tools, " +
                    "experiments and local workflows."
            )
        )

        layout.addView(
            actionButton("System Snapshot") {
                showControl()
            }
        )

        back(layout)

        setContentView(screen(layout))
    }

    private fun message(
        heading: String,
        body: String
    ) {
        val layout = baseLayout()

        layout.addView(
            title("AZIMI CORE")
        )

        layout.addView(
            subtitle(heading)
        )

        layout.addView(
            info(body)
        )

        layout.addView(
            actionButton("← BACK TO AZIMI CORE") {
                showHome()
            }
        )

        setContentView(screen(layout))
    }

    private fun back(layout: LinearLayout) {

        layout.addView(
            section("NAVIGATION")
        )

        layout.addView(
            actionButton("← BACK TO AZIMI CORE") {
                showHome()
            }
        )
    }

    private fun formatBytes(bytes: Long): String {

        if (bytes <= 0) {
            return "0 B"
        }

        val units = arrayOf(
            "B",
            "KB",
            "MB",
            "GB",
            "TB"
        )

        var value = bytes.toDouble()
        var index = 0

        while (
            value >= 1024.0 &&
            index < units.lastIndex
        ) {
            value /= 1024.0
            index++
        }

        return String.format(
            Locale.US,
            "%.1f %s",
            value,
            units[index]
        )
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == VaultAuth.REQUEST_CODE) {

            if (resultCode == RESULT_OK) {

                GuardianStorage.unlockVault(this)

            } else {

                GuardianStorage.lockVault(this)
            }

            showVault()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        showHome()
    }
}
