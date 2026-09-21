package com.azimi.guardian

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {

    private val bg = 0xFF050505.toInt()
    private val panel = 0xFF101010.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val gray = 0xFF9E9E9E.toInt()
    private val green = 0xFF00E676.toInt()
    private val red = 0xFFFF5252.toInt()
    private val purple = 0xFFBB86FC.toInt()

    private var originAuthenticationPending = false

    private var aiInput: EditText? = null
    private var aiConversation: LinearLayout? = null
    private var aiStatus: TextView? = null

    private val aiHistory = mutableListOf<AzimiAiClient.ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GuardianDiagnosticsStartup.start(this)
        handleIncomingAuthIntent(intent)
        showHome()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            setIntent(intent)
            handleIncomingAuthIntent(intent)
        }
    }

    private fun handleIncomingAuthIntent(intent: Intent) {
        val uri: Uri = intent.data ?: return

        if (uri.scheme != "azimi" || uri.host != "auth-callback") {
            return
        }

        showAI()

        AzimiNetwork.handleCallback(
            this,
            uri
        ) { result ->

            if (result.success) {
                updateAIStatus("● AUTHENTICATED · AZIMI AI READY", green)
                addAIMessage("SYSTEM", result.message)
                refreshAIAuthUI()
            } else {
                updateAIStatus("● AUTHENTICATION FAILED", red)
                addAIMessage("SYSTEM", result.message)
                refreshAIAuthUI()
            }
        }
    }

    private fun baseLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(bg)
        }
    }

    private fun screen(layout: LinearLayout): ScrollView {
        return ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(
                layout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun header(
        layout: LinearLayout,
        title: String,
        subtitle: String
    ) {
        layout.addView(
            TextView(this).apply {
                text = title
                textSize = 28f
                setTextColor(white)
                setPadding(0, 0, 0, 6)
            }
        )

        layout.addView(
            TextView(this).apply {
                text = subtitle
                textSize = 12f
                setTextColor(gray)
                setPadding(0, 0, 0, 24)
            }
        )
    }

    private fun status(
        text: String,
        color: Int
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(color)
            setPadding(0, 12, 0, 18)
        }
    }

    private fun info(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(gray)
            setPadding(0, 8, 0, 16)
        }
    }

    private fun actionButton(
        text: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }
    }

    private fun showHome() {
        val layout = baseLayout()

        header(
            layout,
            "AZIMI CORE",
            "GUARDIAN · PERSONAL SYSTEM"
        )

        layout.addView(status("● GUARDIAN ONLINE", green))
        layout.addView(info("Z VAULT · ${GuardianStorage.getVaultStatus(this)}"))
        layout.addView(info("AI POLICY · ${GuardianStorage.getAiMemoryPolicy(this)}"))
        layout.addView(info("Z SHIELD · NOT CONFIGURED"))
        layout.addView(info("Z CONNECT · AUTHORIZATION REQUIRED"))

        layout.addView(
            actionButton("Z CONTROL") {
                showControl()
            }
        )

        layout.addView(
            actionButton("Z VAULT") {
                showVault()
            }
        )

        layout.addView(
            actionButton("Z RECOVERY") {
                showRecovery()
            }
        )

        layout.addView(
            actionButton("Z SHIELD") {
                showShield()
            }
        )

        layout.addView(
            actionButton("AZIMI AI") {
                showAI()
            }
        )

        layout.addView(
            actionButton("Z LAB") {
                showLab()
            }
        )

        layout.addView(
            actionButton("Z ORIGIN") {
                originAuthenticationPending = true
                VaultAuth.requestAuthentication(this)
            }
        )

        setContentView(screen(layout))
    }

    private fun showControl() {
        val layout = baseLayout()

        header(
            layout,
            "Z CONTROL",
            "DEVICE CONTROL · READ ONLY"
        )

        val batteryManager =
            getSystemService(BATTERY_SERVICE) as BatteryManager

        val battery =
            batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )

        val statFs = StatFs(
            Environment.getDataDirectory().path
        )

        val total =
            statFs.totalBytes / (1024 * 1024 * 1024)

        val free =
            statFs.availableBytes / (1024 * 1024 * 1024)

        layout.addView(info("BATTERY · $battery%"))
        layout.addView(info("STORAGE · ${total - free} GB USED / $total GB TOTAL"))
        layout.addView(info("ANDROID · ${Build.VERSION.RELEASE}"))
        layout.addView(info("SDK · ${Build.VERSION.SDK_INT}"))
        layout.addView(info("DEVICE · ${Build.MODEL}"))
        layout.addView(info("DIAGNOSTICS · ${GuardianDiagnostics.getLastStatus(this)}"))

        layout.addView(
            actionButton("← BACK TO AZIMI CORE") {
                showHome()
            }
        )

        setContentView(screen(layout))
    }

    private fun showVault() {
        val layout = baseLayout()

        header(
            layout,
            "Z VAULT",
            "SECURE STORAGE"
        )

        layout.addView(
            status(
                "● VAULT · ${GuardianStorage.getVaultStatus(this)}",
                purple
            )
        )

        layout.addView(
            info(
                "Vault authentication uses the Android secure authentication mechanism currently configured by Guardian."
            )
        )

        layout.addView(
            actionButton("AUTHENTICATE") {
                VaultAuth.requestAuthentication(this)
            }
        )

        layout.addView(
            actionButton("← BACK TO AZIMI CORE") {
                showHome()
            }
        )

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
                "Recovery operations require explicit user action. No destructive operation is performed automatically."
            )
        )

        layout.addView(
            actionButton("← BACK TO AZIMI CORE") {
                showHome()
            }
        )

        setContentView(screen(layout))
    }

    private fun showShield() {
        val layout = baseLayout()

        header(
            layout,
            "Z SHIELD",
            "SECURITY FOUNDATION"
        )

        layout.addView(
            status(
                "● Z SHIELD · NOT CONFIGURED",
                red
            )
        )

        layout.addView(
            info(
                "Security controls will only operate within permissions explicitly granted to Guardian."
            )
        )

        layout.addView(
            actionButton("← BACK TO AZIMI CORE") {
                showHome()
            }
        )

        setContentView(screen(layout))
    }

    private fun showAI() {
        val layout = baseLayout()

        header(
            layout,
            "AZIMI AI",
            "PERSONAL INTELLIGENCE · AUTHENTICATED"
        )

        aiStatus = status(
            "● CHECKING AUTHENTICATION...",
            purple
        )

        layout.addView(aiStatus)

        aiConversation = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        layout.addView(aiConversation)

        aiInput = EditText(this).apply {
            hint = "Ask AZIMI AI..."
            setTextColor(white)
            setHintTextColor(gray)
            setSingleLine(false)
            minLines = 2
            maxLines = 5
        }

        layout.addView(
            aiInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val sendButton = actionButton("SEND") {
            sendAIMessage()
        }

        layout.addView(sendButton)

        val loginButton = actionButton("SIGN IN WITH EMAIL") {
            requestAIAuthentication()
        }

        loginButton.tag = "ai_login_button"
        layout.addView(loginButton)

        val logoutButton = actionButton("SIGN OUT") {
            AzimiAuth.signOut(this)
            aiHistory.clear()
            updateAIStatus(
                "● AUTHENTICATION REQUIRED",
                red
            )
            addAIMessage(
                "SYSTEM",
                "Signed out successfully."
            )
            refreshAIAuthUI()
        }

        logoutButton.tag = "ai_logout_button"
        layout.addView(logoutButton)

        layout.addView(
            actionButton("← BACK TO AZIMI CORE") {
                showHome()
            }
        )

        setContentView(screen(layout))

        refreshAIAuthUI()
    }

    private fun refreshAIAuthUI() {
        val authenticated = AzimiAuth.hasSession(this)

        aiInput?.isEnabled = authenticated

        val sendButtonEnabled = authenticated

        aiConversation?.let {
            if (it.childCount == 0 && authenticated) {
                addAIMessage(
                    "SYSTEM",
                    "AZIMI AI authenticated. You may now send a message."
                )
            }
        }

        val root = aiInput?.parent?.parent as? LinearLayout
        root?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)

                if (child is Button) {
                    when (child.tag) {
                        "ai_login_button" -> {
                            child.isEnabled = !authenticated
                            child.visibility =
                                if (authenticated) Button.GONE
                                else Button.VISIBLE
                        }

                        "ai_logout_button" -> {
                            child.isEnabled = authenticated
                            child.visibility =
                                if (authenticated) Button.VISIBLE
                                else Button.GONE
                        }
                    }
                }
            }
        }

        if (!authenticated) {
            updateAIStatus(
                "● AUTHENTICATION REQUIRED",
                red
            )
        } else {
            updateAIStatus(
                "● AUTHENTICATED · AZIMI AI READY",
                green
            )
        }

        if (!sendButtonEnabled) {
            aiInput?.hint = "Sign in before using AZIMI AI"
        } else {
            aiInput?.hint = "Ask AZIMI AI..."
        }
    }

    private fun requestAIAuthentication() {
        val input = EditText(this).apply {
            hint = "your@email.com"
            setSingleLine(true)
            setTextColor(white)
            setHintTextColor(gray)
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("AZIMI AI SIGN IN")
            .setMessage(
                "Enter your email. AZIMI will request a secure magic link."
            )
            .setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SEND LINK", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val email = input.text.toString().trim()

                updateAIStatus(
                    "● REQUESTING MAGIC LINK...",
                    purple
                )

                AzimiNetwork.requestMagicLink(
                    this,
                    email
                ) { result ->

                    if (result.success) {
                        updateAIStatus(
                            "● CHECK YOUR EMAIL",
                            green
                        )

                        addAIMessage(
                            "SYSTEM",
                            result.message
                        )

                        dialog.dismiss()
                    } else {
                        updateAIStatus(
                            "● AUTHENTICATION ERROR",
                            red
                        )

                        addAIMessage(
                            "SYSTEM",
                            result.message
                        )
                    }
                }
            }
        }

        dialog.show()
    }

    private fun sendAIMessage() {
        val message =
            aiInput?.text?.toString()?.trim().orEmpty()

        if (message.isEmpty()) {
            return
        }

        if (AzimiAuth.isProtectedCredential(message)) {
            addAIMessage(
                "SECURITY",
                "This message appears to contain protected credential material and was blocked before reaching AZIMI AI."
            )
            aiInput?.setText("")
            return
        }

        val session = AzimiAuth.getSession(this)

        if (session == null) {
            updateAIStatus(
                "● AUTHENTICATION REQUIRED",
                red
            )
            addAIMessage(
                "SYSTEM",
                "Please authenticate before using AZIMI AI."
            )
            return
        }

        addAIMessage(
            "YOU",
            message
        )

        aiInput?.setText("")

        updateAIStatus(
            "● AZIMI AI THINKING...",
            purple
        )

        AzimiNetwork.askAI(
            session.accessToken,
            message,
            aiHistory.toList()
        ) { response ->

            if (response.success) {

                addAIMessage(
                    "AZIMI",
                    response.reply
                )

                updateAIStatus(
                    "● AZIMI AI ONLINE · ${response.engine}",
                    green
                )

            } else {

                addAIMessage(
                    "AZIMI",
                    response.reply
                )

                updateAIStatus(
                    "● AI REQUEST FAILED",
                    red
                )
            }
        }
    }

    private fun addAIMessage(
        speaker: String,
        message: String
    ) {
        aiConversation?.addView(
            TextView(this).apply {
                text = "$speaker\n$message"
                textSize = 14f
                setTextColor(white)
                setPadding(0, 12, 0, 18)
            }
        )

        if (speaker == "YOU") {
            aiHistory.add(
                AzimiAiClient.ChatMessage(
                    role = "user",
                    content = message
                )
            )
        }

        if (speaker == "AZIMI") {
            aiHistory.add(
                AzimiAiClient.ChatMessage(
                    role = "assistant",
                    content = message
                )
            )
        }
    }

    private fun updateAIStatus(
        text: String,
        color: Int
    ) {
        aiStatus?.text = text
        aiStatus?.setTextColor(color)
    }

    private fun showLab() {
        val layout = baseLayout()

        header(
            layout,
            "Z LAB",
            "AZIMI EXPERIMENTAL SPACE"
        )

        layout.addView(
            status(
                "● LAB FOUNDATION READY",
                purple
            )
        )

        layout.addView(
            info(
                "Experimental AZIMI capabilities will be developed here without bypassing Guardian security boundaries."
            )
        )

        layout.addView(
            actionButton("← BACK TO AZIMI CORE") {
                showHome()
            }
        )

        setContentView(screen(layout))
    }

    override fun onBackPressed() {
        showHome()
    }
}

