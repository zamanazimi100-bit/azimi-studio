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

class MainActivity : Activity() {

    private val bg = 0xFF050505.toInt()
    private val panel = 0xFF101010.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val gray = 0xFF9E9E9E.toInt()
    private val green = 0xFF00E676.toInt()
    private val red = 0xFFFF5252.toInt()
    private val purple = 0xFFBB86FC.toInt()

    private var originAuthenticationPending = false

    private var pendingVaultAction: String? = null

    private var aiInput: EditText? = null
    private var aiConversation: LinearLayout? = null
    private var aiStatus: TextView? = null

    private val aiHistory =
        mutableListOf<AzimiAiClient.ChatMessage>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        GuardianDiagnosticsStartup.start(this)

        handleIncomingAuthIntent(intent)

        showHome()
    }

    override fun onNewIntent(
        intent: Intent?
    ) {
        super.onNewIntent(intent)

        if (intent != null) {
            setIntent(intent)
            handleIncomingAuthIntent(intent)
        }
    }

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

        if (requestCode != VaultAuth.REQUEST_CODE) {
            return
        }

        if (resultCode == RESULT_OK) {

            val unlocked =
                GuardianStorage.unlockVault(this)

            if (!unlocked) {

                originAuthenticationPending = false
                pendingVaultAction = null

                addAIMessage(
                    "SECURITY",
                    "Authentication succeeded, but AZIMI Vault could not be unlocked."
                )

                showVault()

                return
            }

            if (originAuthenticationPending) {

                originAuthenticationPending = false

                showOrigin()

                return
            }

            when (pendingVaultAction) {

                "MEMORY" -> {
                    pendingVaultAction = null

                    showVaultSection(
                        "Z MEMORY",
                        "APPROVED ATLAS MEMORY",
                        "Only user-approved AZIMI context belongs here.\n\n" +
                            "Passwords, API keys, recovery codes and private credentials are never stored as Atlas memory."
                    )
                }

                "ARCHIVE" -> {
                    pendingVaultAction = null

                    showVaultSection(
                        "Z ARCHIVE",
                        "AZIMI PROJECT ARCHIVE",
                        "A future owner-controlled space for project history, approved decisions, backups and portable AZIMI records."
                    )
                }

                "SOVEREIGN" -> {
                    pendingVaultAction = null

                    showSovereign()
                }

                else -> {
                    pendingVaultAction = null
                    showVault()
                }
            }

        } else {

            originAuthenticationPending = false
            pendingVaultAction = null

            showVault()
        }
    }

    private fun handleIncomingAuthIntent(
        intent: Intent
    ) {
        val uri: Uri =
            intent.data ?: return

        if (
            uri.scheme != "azimi" ||
            uri.host != "auth-callback"
        ) {
            return
        }

        showAI()

        AzimiNetwork.handleCallback(
            this,
            uri
        ) { result ->

            if (result.success) {

                updateAIStatus(
                    "● AUTHENTICATED · AZIMI AI READY",
                    green
                )

                addAIMessage(
                    "SYSTEM",
                    result.message
                )

                refreshAIAuthUI()

            } else {

                updateAIStatus(
                    "● AUTHENTICATION FAILED",
                    red
                )

                addAIMessage(
                    "SYSTEM",
                    result.message
                )

                refreshAIAuthUI()
            }
        }
    }

    private fun baseLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL

            setPadding(
                28,
                28,
                28,
                28
            )

            setBackgroundColor(bg)
        }
    }

    private fun screen(
        layout: LinearLayout
    ): ScrollView {
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

                setPadding(
                    0,
                    0,
                    0,
                    6
                )
            }
        )

        layout.addView(
            TextView(this).apply {
                text = subtitle
                textSize = 12f
                setTextColor(gray)

                setPadding(
                    0,
                    0,
                    0,
                    24
                )
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

            setPadding(
                0,
                12,
                0,
                18
            )
        }
    }

    private fun info(
        text: String
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(gray)

            setPadding(
                0,
                8,
                0,
                16
            )
        }
    }

    private fun actionButton(
        text: String,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text

            setOnClickListener {
                action()
            }
        }
    }

    private fun showHome() {

        val layout =
            baseLayout()

        header(
            layout,
            "AZIMI CORE",
            "GUARDIAN · PERSONAL SYSTEM"
        )

        layout.addView(
            status(
                "● GUARDIAN ONLINE",
                green
            )
        )

        layout.addView(
            info(
                "Z VAULT · ${GuardianStorage.getVaultStatus(this)}"
            )
        )

        layout.addView(
            info(
                "AI POLICY · ${GuardianStorage.getAIMemoryPolicy(this)}"
            )
        )

        layout.addView(
            info(
                "Z SHIELD · NOT CONFIGURED"
            )
        )

        layout.addView(
            info(
                "Z CONNECT · AUTHORIZATION REQUIRED"
            )
        )

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

                if (!requestVaultAuthentication()) {
                    originAuthenticationPending = false
                }
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun showControl() {

        val layout =
            baseLayout()

        header(
            layout,
            "Z CONTROL",
            "DEVICE CONTROL · READ ONLY"
        )

        val batteryManager =
            getSystemService(
                BATTERY_SERVICE
            ) as BatteryManager

        val battery =
            batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )

        val statFs =
            StatFs(
                Environment
                    .getDataDirectory()
                    .path
            )

        val total =
            statFs.totalBytes /
                (1024 * 1024 * 1024)

        val free =
            statFs.availableBytes /
                (1024 * 1024 * 1024)

        layout.addView(
            info("BATTERY · $battery%")
        )

        layout.addView(
            info(
                "STORAGE · ${total - free} GB USED / $total GB TOTAL"
            )
        )

        layout.addView(
            info(
                "ANDROID · ${Build.VERSION.RELEASE}"
            )
        )

        layout.addView(
            info(
                "SDK · ${Build.VERSION.SDK_INT}"
            )
        )

        layout.addView(
            info(
                "DEVICE · ${Build.MODEL}"
            )
        )

        layout.addView(
            info(
                "DIAGNOSTICS · ${GuardianDiagnostics.getLastStatus(this)}"
            )
        )

        layout.addView(
            actionButton(
                "← BACK TO AZIMI CORE"
            ) {
                showHome()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun showVault() {

        val layout =
            baseLayout()

        header(
            layout,
            "Z VAULT",
            "PRIVATE SYSTEM · OWNER CONTROLLED"
        )

        val vaultStatus =
            GuardianStorage.getVaultStatus(this)

        val vaultUnlocked =
            vaultStatus == "UNLOCKED"

        val identityPanel =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    24,
                    24,
                    24
                )

                setBackgroundColor(panel)
            }

        identityPanel.addView(
            TextView(this).apply {

                text =
                    if (vaultUnlocked) {
                        "◉"
                    } else {
                        "◆"
                    }

                textSize = 38f

                setTextColor(
                    if (vaultUnlocked) {
                        green
                    } else {
                        purple
                    }
                )
            }
        )

        identityPanel.addView(
            TextView(this).apply {

                text =
                    if (vaultUnlocked) {
                        "VAULT OPEN"
                    } else {
                        "VAULT SEALED"
                    }

                textSize = 20f
                setTextColor(white)

                setPadding(
                    0,
                    8,
                    0,
                    0
                )
            }
        )

        identityPanel.addView(
            TextView(this).apply {

                text =
                    if (vaultUnlocked) {
                        "Protected AZIMI spaces are available."
                    } else {
                        "Authentication required before protected spaces can be opened."
                    }

                textSize = 13f
                setTextColor(gray)

                setPadding(
                    0,
                    8,
                    0,
                    0
                )
            }
        )

        layout.addView(identityPanel)

        layout.addView(
            status(
                if (vaultUnlocked) {
                    "● Z VAULT · UNLOCKED"
                } else {
                    "● Z VAULT · LOCKED"
                },
                if (vaultUnlocked) {
                    green
                } else {
                    purple
                }
            )
        )

        layout.addView(
            actionButton(
                "Z MEMORY\nApproved Atlas memory"
            ) {
                openProtectedVaultArea("MEMORY")
            }
        )

        layout.addView(
            actionButton(
                "Z ORIGIN\nOwner identity space"
            ) {

                originAuthenticationPending =
                    true

                if (!vaultUnlocked) {

                    if (!requestVaultAuthentication()) {
                        originAuthenticationPending =
                            false
                    }

                } else {
                    showOrigin()
                }
            }
        )

        layout.addView(
            actionButton(
                "Z ARCHIVE\nProjects · decisions · history"
            ) {
                openProtectedVaultArea("ARCHIVE")
            }
        )

        layout.addView(
            actionButton(
                "Z RECOVERY\nBackup · restore · portability"
            ) {
                showRecovery()
            }
        )

        layout.addView(
            actionButton(
                "Z SOVEREIGN\nOwnership · independence · control"
            ) {
                openProtectedVaultArea("SOVEREIGN")
            }
        )

        if (vaultUnlocked) {

            layout.addView(
                actionButton(
                    "SEAL Z VAULT"
                ) {

                    if (
                        GuardianStorage.lockVault(
                            this
                        )
                    ) {
                        showVault()
                    }
                }
            )

        } else {

            layout.addView(
                actionButton(
                    "AUTHENTICATE & OPEN"
                ) {

                    if (!requestVaultAuthentication()) {

                        showVaultAuthenticationUnavailable()
                    }
                }
            )
        }

        layout.addView(
            info(
                "Guardian policy · ${GuardianStorage.getAIMemoryPolicy(this)}\n" +
                    "Android Keystore protected storage\n" +
                    "AI access · policy controlled"
            )
        )

        layout.addView(
            actionButton(
                "← BACK TO AZIMI CORE"
            ) {
                showHome()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun openProtectedVaultArea(
        area: String
    ) {

        if (
            GuardianStorage.getVaultStatus(this) ==
            "UNLOCKED"
        ) {

            when (area) {

                "MEMORY" -> {
                    showVaultSection(
                        "Z MEMORY",
                        "APPROVED ATLAS MEMORY",
                        "Only user-approved AZIMI context belongs here.\n\n" +
                            "Passwords, API keys, recovery codes and private credentials are never stored as Atlas memory."
                    )
                }

                "ARCHIVE" -> {
                    showVaultSection(
                        "Z ARCHIVE",
                        "AZIMI PROJECT ARCHIVE",
                        "A future owner-controlled space for project history, approved decisions, backups and portable AZIMI records."
                    )
                }

                "SOVEREIGN" -> {
                    showSovereign()
                }
            }

            return
        }

        pendingVaultAction =
            area

        if (!requestVaultAuthentication()) {

            pendingVaultAction = null

            showVaultAuthenticationUnavailable()
        }
    }

    private fun requestVaultAuthentication(): Boolean {

        if (!VaultAuth.isDeviceSecure(this)) {
            return false
        }

        return VaultAuth.requestAuthentication(
            this
        )
    }

    private fun showVaultAuthenticationUnavailable() {

        val layout =
            baseLayout()

        header(
            layout,
            "VAULT SECURITY",
            "AUTHENTICATION REQUIRED"
        )

        layout.addView(
            status(
                "● DEVICE SECURITY NOT AVAILABLE",
                red
            )
        )

        layout.addView(
            info(
                "AZIMI cannot open protected Vault areas until this device has a secure authentication method configured."
            )
        )

        layout.addView(
            info(
                "Configure a secure device lock such as PIN, password or pattern. Future AZIMI platform adapters can use the strongest secure authentication methods supported by each device."
            )
        )

        layout.addView(
            actionButton(
                "← BACK TO Z VAULT"
            ) {
                showVault()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun showVaultSection(
        title: String,
        subtitle: String,
        description: String
    ) {

        val layout =
            baseLayout()

        header(
            layout,
            title,
            subtitle
        )

        layout.addView(
            status(
                "● Z VAULT · AUTHENTICATED",
                green
            )
        )

        layout.addView(
            info(description)
        )

        layout.addView(
            info(
                "ACCESS BOUNDARY\n" +
                    "Guardian authorization required\n\n" +
                    "AI BOUNDARY\n" +
                    "AI cannot directly access protected Vault storage."
            )
        )

        layout.addView(
            actionButton(
                "← BACK TO Z VAULT"
            ) {
                showVault()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun showOrigin() {

        val layout =
            baseLayout()

        header(
            layout,
            "Z ORIGIN",
            "OWNER IDENTITY · SPECIAL ACCESS"
        )

        layout.addView(
            status(
                "● OWNER GATE PASSED · CURRENT DEVICE",
                green
            )
        )

        layout.addView(
            info(
                "Z Origin is the special owner-controlled area of AZIMI.\n\n" +
                    "This space is intentionally different from normal user security."
            )
        )

        layout.addView(
            info(
                "OWNER-ONLY ARCHITECTURE\n\n" +
                    "• Owner identity\n" +
                    "• Main Kingdom\n" +
                    "• Sovereign controls\n" +
                    "• Special Voice Lock\n" +
                    "• Owner recovery authority\n" +
                    "• AZIMI core administration"
            )
        )

        layout.addView(
            status(
                "● SPECIAL OWNER TOOLS · RESTRICTED",
                purple
            )
        )

        layout.addView(
            info(
                "Current Android foundation uses secure device authentication. Future versions can add additional owner factors such as biometric and voice authentication when securely supported."
            )
        )

        layout.addView(
            actionButton(
                "Z SOVEREIGN"
            ) {
                showSovereign()
            }
        )

        layout.addView(
            actionButton(
                "← BACK TO Z VAULT"
            ) {
                showVault()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun showSovereign() {

        val layout =
            baseLayout()

        header(
            layout,
            "Z SOVEREIGN",
            "AZIMI OWNERSHIP · INDEPENDENCE"
        )

        layout.addView(
            status(
                "● SOVEREIGN FOUNDATION",
                purple
            )
        )

        layout.addView(
            info(
                "Z Sovereign is the owner-control architecture of AZIMI.\n\n" +
                    "AZIMI is designed to remain portable and recoverable rather than permanently dependent on one cloud platform, database, deployment service or AI provider."
            )
        )

        layout.addView(
            info(
                "OWNERSHIP\n" +
                    "Source · identity · policies · approved memory\n\n" +
                    "PORTABILITY\n" +
                    "Exportable data · replaceable providers · migration\n\n" +
                    "RECOVERY\n" +
                    "Backups · restore procedures · repairability\n\n" +
                    "AI INDEPENDENCE\n" +
                    "External AI engines remain replaceable modules."
            )
        )

        layout.addView(
            status(
                "● OWNER CONTROL · ENABLED BY ARCHITECTURE",
                green
            )
        )

        layout.addView(
            actionButton(
                "← BACK TO Z ORIGIN"
            ) {
                showOrigin()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun showRecovery() {

        val layout =
            baseLayout()

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
            actionButton(
                "← BACK TO AZIMI CORE"
            ) {
                showHome()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun showShield() {

        val layout =
            baseLayout()

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
            actionButton(
                "← BACK TO AZIMI CORE"
            ) {
                showHome()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    private fun showAI() {

        val layout =
            baseLayout()

        header(
            layout,
            "AZIMI AI",
            "PERSONAL INTELLIGENCE · AUTHENTICATED"
        )

        aiStatus =
            status(
                "● CHECKING AUTHENTICATION...",
                purple
            )

        layout.addView(aiStatus)

        aiConversation =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        layout.addView(aiConversation)

        aiInput =
            EditText(this).apply {

                hint =
                    "Ask AZIMI AI..."

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

        val sendButton =
            actionButton("SEND") {
                sendAIMessage()
            }

        sendButton.tag =
            "ai_send_button"

        layout.addView(sendButton)

        val loginButton =
            actionButton(
                "SIGN IN WITH EMAIL"
            ) {
                requestAIAuthentication()
            }

        loginButton.tag =
            "ai_login_button"

        layout.addView(loginButton)

        val logoutButton =
            actionButton(
                "SIGN OUT"
            ) {

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

        logoutButton.tag =
            "ai_logout_button"

        layout.addView(logoutButton)

        layout.addView(
            actionButton(
                "← BACK TO AZIMI CORE"
            ) {
                showHome()
            }
        )

        setContentView(
            screen(layout)
        )

        refreshAIAuthUI()
    }

    private fun refreshAIAuthUI() {

        val authenticated =
            AzimiAuth.hasSession(this)

        aiInput?.isEnabled =
            authenticated

        val root =
            aiInput?.parent?.parent
                as? LinearLayout

        root?.let { container ->

            for (
                i in 0 until container.childCount
            ) {

                val child =
                    container.getChildAt(i)

                if (child is Button) {

                    when (child.tag) {

                        "ai_login_button" -> {

                            child.isEnabled =
                                !authenticated

                            child.visibility =
                                if (authenticated) {
                                    Button.GONE
                                } else {
                                    Button.VISIBLE
                                }
                        }

                        "ai_logout_button" -> {

                            child.isEnabled =
                                authenticated

                            child.visibility =
                                if (authenticated) {
                                    Button.VISIBLE
                                } else {
                                    Button.GONE
                                }
                        }

                        "ai_send_button" -> {

                            child.isEnabled =
                                authenticated
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

            aiInput?.hint =
                "Sign in before using AZIMI AI"

        } else {

            updateAIStatus(
                "● AUTHENTICATED · AZIMI AI READY",
                green
            )

            aiInput?.hint =
                "Ask AZIMI AI..."
        }

        aiConversation?.let {

            if (
                it.childCount == 0 &&
                authenticated
            ) {

                addAIMessage(
                    "SYSTEM",
                    "AZIMI AI authenticated. You may now send a message."
                )
            }
        }
    }

    private fun requestAIAuthentication() {

        val input =
            EditText(this).apply {

                hint =
                    "your@email.com"

                setSingleLine(true)

                setTextColor(white)
                setHintTextColor(gray)
            }

        val dialog =
            android.app.AlertDialog.Builder(this)
                .setTitle(
                    "AZIMI AI SIGN IN"
                )
                .setMessage(
                    "Enter your email. AZIMI will request a secure magic link."
                )
                .setView(input)
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .setPositiveButton(
                    "SEND LINK",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val email =
                    input.text
                        .toString()
                        .trim()

                if (email.isEmpty()) {

                    updateAIStatus(
                        "● EMAIL REQUIRED",
                        red
                    )

                    return@setOnClickListener
                }

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
            aiInput
                ?.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (message.isEmpty()) {
            return
        }

        if (
            AzimiAuth.isProtectedCredential(
                message
            )
        ) {

            addAIMessage(
                "SECURITY",
                "This message appears to contain protected credential material and was blocked before reaching AZIMI AI."
            )

            aiInput?.setText("")

            return
        }

        val session =
            AzimiAuth.getSession(this)

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

        /*
         * Capture history BEFORE adding the current
         * user message.
         *
         * This prevents the current message from
         * being duplicated in the request.
         */
        val safeHistory =
            aiHistory.toList()

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
            safeHistory
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

                text =
                    "$speaker\n$message"

                textSize = 14f

                setTextColor(white)

                setPadding(
                    0,
                    12,
                    0,
                    18
                )
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

        aiStatus?.text =
            text

        aiStatus?.setTextColor(
            color
        )
    }

    private fun showLab() {

        val layout =
            baseLayout()

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
            actionButton(
                "← BACK TO AZIMI CORE"
            ) {
                showHome()
            }
        )

        setContentView(
            screen(layout)
        )
    }

    override fun onBackPressed() {
        showHome()
    }
}
