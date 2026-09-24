package com.azimi.guardian

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private var originAuthenticationPending = false
    private var pendingVaultAction: String? = null

    private var aiInput: EditText? = null
    private var aiConversation: LinearLayout? = null
    private var aiStatus: TextView? = null
    private var aiLoginButton: Button? = null
    private var aiLogoutButton: Button? = null
    private var aiSendButton: Button? = null

    private var aiVoiceButton: Button? = null
    private var aiVoiceLanguageButton: Button? = null

    private var atlasTts: TextToSpeech? = null
    private var atlasTtsReady = false
    private var atlasSpeechEnabled = true

    private val aiHistory =
        mutableListOf<AzimiAiClient.ChatMessage>()

    private val bg = 0xFF050607.toInt()
    private val surface = 0xFF0B0E11.toInt()
    private val panel = 0xFF10151A.toInt()
    private val panel2 = 0xFF151B21.toInt()

    private val white = 0xFFF5F7FA.toInt()
    private val softWhite = 0xFFD8DEE6.toInt()
    private val gray = 0xFF89929D.toInt()
    private val darkGray = 0xFF4E5965.toInt()

    private val green = 0xFF00E676.toInt()
    private val cyan = 0xFF00D9FF.toInt()
    private val purple = 0xFFB388FF.toInt()
    private val blue = 0xFF64B5FF.toInt()
    private val amber = 0xFFFFC857.toInt()
    private val red = 0xFFFF5252.toInt()

    private val radius = 22f

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        // BUILD #76 — CONFIGURE WINDOW ISOLATION
        configureWindow()

        // Everything else remains disabled for this diagnostic build.
        val testScreen =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(bg)
                setPadding(32, 32, 32, 32)
            }

        val testTitle =
            TextView(this).apply {
                text = "AZIMI GUARDIAN"
                textSize = 24f
                setTextColor(white)
                gravity = Gravity.CENTER
            }

        val testStatus =
            TextView(this).apply {
                text =
                    "\nBUILD #76\nCONFIGURE WINDOW TEST\n\nGUARDIAN ACTIVITY ONLINE"
                textSize = 16f
                setTextColor(softWhite)
                gravity = Gravity.CENTER
            }

        testScreen.addView(testTitle)

        testScreen.addView(
            testStatus,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(testScreen)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
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

        /*
         * Disabled from the launch path for Build #76.
         * Original runtime implementation remains preserved
         * in the project architecture for later isolation.
         */
    }

    private fun configureWindow() {

        window.setNavigationBarColor(bg)
        window.setStatusBarColor(bg)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.R
        ) {

            window.setDecorFitsSystemWindows(
                false
            )

            window.decorView.setOnApplyWindowInsetsListener {
                    view,
                    insets ->

                val bars =
                    insets.getInsets(
                        WindowInsets.Type.systemBars()
                    )

                view.setPadding(
                    0,
                    bars.top,
                    0,
                    bars.bottom
                )

                insets
            }
        }
    }

    /*
     * BUILD #76 DIAGNOSTIC NOTE
     *
     * The full Guardian UI/runtime methods remain below so this file
     * preserves the existing MainActivity architecture.
     *
     * They are intentionally NOT called from onCreate() in Build #76.
     */

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private fun makeText(
        text: String,
        size: Float = 14f,
        color: Int = white
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
        }
    }

    private fun makeButton(
        text: String
    ): Button {
        return Button(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(white)
            isAllCaps = false
        }
    }

    private fun makePanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
            )

            background =
                GradientDrawable().apply {
                    setColor(panel)
                    cornerRadius = dp(18).toFloat()
                }
        }
    }

    private fun addGap(
        parent: LinearLayout,
        height: Int = 12
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    private fun setModuleStatus(
        view: TextView,
        value: String,
        color: Int
    ) {
        view.text = value
        view.setTextColor(color)
    }

    private fun isAzimiAuthCallback(
        uri: android.net.Uri?
    ): Boolean {
        if (uri == null) return false

        return uri.scheme == "azimi" &&
            uri.host == "auth-callback"
    }

    private fun showHome() {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        val title =
            makeText(
                "AZIMI GUARDIAN",
                26f,
                white
            )

        title.typeface =
            Typeface.DEFAULT_BOLD

        root.addView(title)

        addGap(root, 8)

        val subtitle =
            makeText(
                "GUARDIAN CORE",
                13f,
                cyan
            )

        root.addView(subtitle)

        addGap(root, 20)

        val statusPanel =
            makePanel()

        statusPanel.addView(
            makeText(
                "SYSTEM STATUS",
                12f,
                gray
            )
        )

        addGap(statusPanel, 8)

        statusPanel.addView(
            makeText(
                "GUARDIAN ONLINE",
                18f,
                green
            )
        )

        root.addView(
            statusPanel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addGap(root, 14)

        val vaultButton =
            makeButton("Z VAULT")

        vaultButton.setOnClickListener {
            showVault()
        }

        root.addView(vaultButton)

        val originButton =
            makeButton("Z ORIGIN")

        originButton.setOnClickListener {
            showOrigin()
        }

        root.addView(originButton)

        val aiButton =
            makeButton("AZIMI AI")

        aiButton.setOnClickListener {
            showAI()
        }

        root.addView(aiButton)

        val cloudButton =
            makeButton("Z CLOUD")

        cloudButton.setOnClickListener {
            showCloud()
        }

        root.addView(cloudButton)

        val controlButton =
            makeButton("Z CONTROL")

        controlButton.setOnClickListener {
            showControl()
        }

        root.addView(controlButton)

        val recoveryButton =
            makeButton("Z RECOVERY")

        recoveryButton.setOnClickListener {
            showRecovery()
        }

        root.addView(recoveryButton)

        val shieldButton =
            makeButton("Z SHIELD")

        shieldButton.setOnClickListener {
            showShield()
        }

        root.addView(shieldButton)

        val labButton =
            makeButton("Z LAB")

        labButton.setOnClickListener {
            showLab()
        }

        root.addView(labButton)

        setContentView(root)
    }

    private fun showCloud() {
        setContentView(
            CloudScreen(
                this
            ).create()
        )
    }

    private fun showControl() {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        root.addView(
            makeText(
                "Z CONTROL",
                24f,
                white
            )
        )

        addGap(root, 16)

        val battery =
            getSystemService(
                BATTERY_SERVICE
            ) as BatteryManager

        val batteryPercent =
            battery.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )

        val storage =
            StatFs(
                Environment.getDataDirectory()
                    .path
            )

        val total =
            storage.totalBytes /
                (1024L * 1024L * 1024L)

        val free =
            storage.availableBytes /
                (1024L * 1024L * 1024L)

        root.addView(
            makeText(
                "BATTERY\n${batteryPercent}%",
                16f,
                green
            )
        )

        addGap(root, 12)

        root.addView(
            makeText(
                "STORAGE\n${free} GB FREE / ${total} GB TOTAL",
                16f,
                softWhite
            )
        )

        addGap(root, 20)

        val back =
            makeButton("BACK")

        back.setOnClickListener {
            showHome()
        }

        root.addView(back)

        setContentView(root)
    }

    private fun showVault() {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        root.addView(
            makeText(
                "Z VAULT",
                24f,
                white
            )
        )

        addGap(root, 12)

        root.addView(
            makeText(
                "VAULT STATUS",
                12f,
                gray
            )
        )

        addGap(root, 8)

        root.addView(
            makeText(
                runCatching {
                    GuardianStorage.getVaultStatus(
                        this
                    )
                }.getOrDefault(
                    "UNKNOWN"
                ),
                18f,
                cyan
            )
        )

        addGap(root, 18)

        val lockAtlas =
            makeButton(
                "LOCK VAULT · KEEP ATLAS ACTIVE"
            )

        lockAtlas.setOnClickListener {
            GuardianStorage.lockVault(this)
            showVault()
        }

        root.addView(lockAtlas)

        val fullLock =
            makeButton(
                "FULL LOCK · LOCK ATLAS"
            )

        fullLock.setOnClickListener {
            performFullLock()
        }

        root.addView(fullLock)

        val back =
            makeButton("BACK")

        back.setOnClickListener {
            showHome()
        }

        root.addView(back)

        setContentView(root)
    }

    private fun performFullLock() {
        runCatching {
            GuardianStorage.lockVault(this)
        }

        runCatching {
            AtlasVoice.stop(this)
        }

        runCatching {
            AtlasSession.fullLock(this)
        }

        originAuthenticationPending = false
        pendingVaultAction = null

        showHome()
    }

    private fun showOrigin() {
        val state =
            runCatching {
                AtlasOwnerAuthority.getState(
                    this
                )
            }.getOrNull()

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        root.addView(
            makeText(
                "Z ORIGIN",
                24f,
                white
            )
        )

        addGap(root, 12)

        root.addView(
            makeText(
                "OWNER AUTHORITY",
                12f,
                gray
            )
        )

        addGap(root, 8)

        root.addView(
            makeText(
                state?.toString()
                    ?: "AUTHORITY NOT VERIFIED",
                17f,
                amber
            )
        )

        addGap(root, 18)

        val verify =
            makeButton(
                "VERIFY OWNER"
            )

        verify.setOnClickListener {
            requestOwnerVerification()
        }

        root.addView(verify)

        val back =
            makeButton("BACK")

        back.setOnClickListener {
            showHome()
        }

        root.addView(back)

        setContentView(root)
    }

    private fun requestOwnerVerification() {
        originAuthenticationPending = true

        runCatching {
            AtlasOwnerAuthority.verifyOwner(
                this
            )
        }.onFailure {
            originAuthenticationPending = false

            AlertDialog.Builder(this)
                .setTitle("Z ORIGIN")
                .setMessage(
                    it.message
                        ?: "Owner verification could not start."
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .show()
        }
    }

    private fun showSovereign() {
        if (
            !AtlasOwnerAuthority
                .isOwnerAuthorized(this)
        ) {
            showOrigin()
            return
        }

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        root.addView(
            makeText(
                "Z SOVEREIGN",
                24f,
                white
            )
        )

        addGap(root, 12)

        root.addView(
            makeText(
                "OWNER-CONTROLLED SPACE",
                14f,
                purple
            )
        )

        addGap(root, 20)

        root.addView(
            makeText(
                "AUTHORIZED OWNER SESSION ACTIVE.",
                16f,
                green
            )
        )

        addGap(root, 20)

        val back =
            makeButton("BACK")

        back.setOnClickListener {
            showOrigin()
        }

        root.addView(back)

        setContentView(root)
    }

    private fun showRecovery() {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        root.addView(
            makeText(
                "Z RECOVERY",
                24f,
                white
            )
        )

        addGap(root, 14)

        root.addView(
            makeText(
                "RECOVERY SYSTEM",
                14f,
                cyan
            )
        )

        addGap(root, 10)

        root.addView(
            makeText(
                "Owner-controlled recovery and restoration.",
                15f,
                softWhite
            )
        )

        addGap(root, 22)

        val back =
            makeButton("BACK")

        back.setOnClickListener {
            showHome()
        }

        root.addView(back)

        setContentView(root)
    }

    private fun showShield() {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        root.addView(
            makeText(
                "Z SHIELD",
                24f,
                white
            )
        )

        addGap(root, 14)

        root.addView(
            makeText(
                "SECURITY STATUS",
                12f,
                gray
            )
        )

        addGap(root, 8)

        root.addView(
            makeText(
                "NOT CONFIGURED",
                18f,
                amber
            )
        )

        addGap(root, 20)

        val back =
            makeButton("BACK")

        back.setOnClickListener {
            showHome()
        }

        root.addView(back)

        setContentView(root)
    }

    private fun showLab() {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        root.addView(
            makeText(
                "Z LAB",
                24f,
                white
            )
        )

        addGap(root, 14)

        root.addView(
            makeText(
                "EXPERIMENTAL AZIMI WORKSPACE",
                13f,
                purple
            )
        )

        addGap(root, 20)

        val back =
            makeButton("BACK")

        back.setOnClickListener {
            showHome()
        }

        root.addView(back)

        setContentView(root)
    }

    private fun initializeAtlasVoice() {
        atlasTtsReady = false

        atlasTts =
            TextToSpeech(this) { status ->

                if (
                    status !=
                    TextToSpeech.SUCCESS
                ) {
                    atlasTtsReady = false

                    aiStatus?.text =
                        "ATLAS VOICE · UNAVAILABLE"

                    aiStatus?.setTextColor(
                        amber
                    )

                    return@TextToSpeech
                }

                atlasTtsReady = true

                configureAtlasVoiceLanguage()

                if (
                    aiStatus != null &&
                    AtlasSession.isActive(this)
                ) {
                    aiStatus?.text =
                        "ATLAS VOICE · READY"

                    aiStatus?.setTextColor(
                        cyan
                    )
                }
            }
    }

    private fun configureAtlasVoiceLanguage() {
        val dari =
            runCatching {
                ZLanguage.isDari(this)
            }.getOrDefault(false)

        val locale =
            if (dari) {
                Locale(
                    "fa",
                    "AF"
                )
            } else {
                Locale.ENGLISH
            }

        val result =
            atlasTts?.setLanguage(
                locale
            )

        if (
            result ==
            TextToSpeech.LANG_MISSING_DATA ||
            result ==
            TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            atlasTts?.setLanguage(
                if (dari) {
                    Locale("fa")
                } else {
                    Locale.ENGLISH
                }
            )
        }
    }

    private fun showAI() {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    dp(18),
                    dp(18),
                    dp(18),
                    dp(18)
                )
            }

        root.addView(
            makeText(
                "AZIMI AI",
                24f,
                white
            )
        )

        addGap(root, 8)

        aiStatus =
            makeText(
                "ATLAS AI · READY",
                12f,
                cyan
            )

        root.addView(aiStatus)

        addGap(root, 14)

        aiConversation =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        val scroll =
            ScrollView(this).apply {
                addView(
                    aiConversation
                )
            }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        addGap(root, 10)

        aiInput =
            EditText(this).apply {
                hint =
                    "Ask Atlas..."
                setTextColor(white)
                setHintTextColor(gray)
                setBackgroundColor(
                    panel
                )
            }

        root.addView(
            aiInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        aiSendButton =
            makeButton("SEND")

        aiSendButton?.setOnClickListener {
            sendAtlasMessage()
        }

        root.addView(
            aiSendButton
        )

        aiVoiceButton =
            makeButton(
                "ATLAS VOICE"
            )

        aiVoiceButton?.setOnClickListener {
            speakLastAtlasMessage()
        }

        root.addView(
            aiVoiceButton
        )

        aiVoiceLanguageButton =
            makeButton(
                "VOICE LANGUAGE"
            )

        aiVoiceLanguageButton?.setOnClickListener {
            configureAtlasVoiceLanguage()
        }

        root.addView(
            aiVoiceLanguageButton
        )

        aiLoginButton =
            makeButton(
                "AI LOGIN"
            )

        aiLoginButton?.setOnClickListener {
            showAiLogin()
        }

        root.addView(
            aiLoginButton
        )

        aiLogoutButton =
            makeButton(
                "AI LOGOUT"
            )

        aiLogoutButton?.setOnClickListener {
            runCatching {
                AzimiAuth.clearSession(
                    this
                )
            }

            aiStatus?.text =
                "ATLAS AI · LOGGED OUT"

            aiStatus?.setTextColor(
                amber
            )
        }

        root.addView(
            aiLogoutButton
        )

        val back =
            makeButton("BACK")

        back.setOnClickListener {
            showHome()
        }

        root.addView(back)

        setContentView(root)
    }

    private fun showAiLogin() {
        val input =
            EditText(this).apply {
                hint =
                    "Email"
                setTextColor(white)
            }

        AlertDialog.Builder(this)
            .setTitle(
                "AZIMI AI LOGIN"
            )
            .setView(input)
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "SEND LINK"
            ) { _, _ ->

                val email =
                    input.text
                        .toString()
                        .trim()

                if (email.isEmpty()) {
                    aiStatus?.text =
                        "LOGIN · EMAIL REQUIRED"

                    aiStatus?.setTextColor(
                        amber
                    )

                    return@setPositiveButton
                }

                runCatching {
                    AzimiAuth.requestMagicLink(
                        this,
                        email
                    )
                }.onSuccess {
                    aiStatus?.text =
                        "LOGIN LINK REQUESTED"

                    aiStatus?.setTextColor(
                        green
                    )
                }.onFailure {
                    aiStatus?.text =
                        "LOGIN FAILED"

                    aiStatus?.setTextColor(
                        red
                    )
                }
            }
            .show()
    }

    private fun sendAtlasMessage() {
        val message =
            aiInput
                ?.text
                ?.toString()
                ?.trim()
                ?: return

        if (message.isEmpty()) {
            return
        }

        if (
            AzimiAuth.isProtectedCredential(
                message
            )
        ) {
            addAIMessage(
                "SYSTEM",
                "Protected credentials are not accepted by AZIMI AI."
            )
            return
        }

        addAIMessage(
            "ZAMAN",
            message
        )

        aiInput?.setText("")

        aiStatus?.text =
            "ATLAS AI · PROCESSING"

        aiStatus?.setTextColor(
            cyan
        )

        val result =
            runCatching {
                AtlasGuardianBridge.process(
                    this,
                    message,
                    aiHistory.toList()
                )
            }

        result.onSuccess {
            val reply =
                it?.toString()
                    ?: "Atlas returned no response."

            if (
                AzimiAuth.isProtectedCredential(
                    reply
                )
            ) {
                addAIMessage(
                    "ATLAS",
                    "Response blocked by credential safety policy."
                )
            } else {
                addAIMessage(
                    "ATLAS",
                    reply
                )
            }

            aiStatus?.text =
                "ATLAS AI · READY"

            aiStatus?.setTextColor(
                green
            )
        }

        result.onFailure {
            addAIMessage(
                "ATLAS",
                "AI request failed safely: ${
                    it.message
                        ?: "Unknown error"
                }"
            )

            aiStatus?.text =
                "ATLAS AI · ERROR"

            aiStatus?.setTextColor(
                red
            )
        }
    }

    private fun addAIMessage(
        sender: String,
        message: String
    ) {
        if (
            AzimiAuth.isProtectedCredential(
                message
            )
        ) {
            return
        }

        aiHistory.add(
            AzimiAiClient.ChatMessage(
                role =
                    if (
                        sender == "ZAMAN"
                    ) {
                        "user"
                    } else {
                        "assistant"
                    },
                content = message
            )
        )

        val container =
            aiConversation
                ?: return

        val text =
            makeText(
                "$sender\n$message",
                14f,
                if (
                    sender == "ATLAS"
                ) {
                    cyan
                } else {
                    softWhite
                }
            )

        text.setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

        container.addView(text)

        container.post {
            (
                container.parent
                    as? ScrollView
                )?.fullScroll(
                    View.FOCUS_DOWN
                )
            }
    }

    private fun speakLastAtlasMessage() {
        if (!atlasSpeechEnabled) {
            return
        }

        val last =
            aiHistory
                .lastOrNull {
                    it.role ==
                        "assistant"
                }
                ?.content
                ?: return

        if (!atlasTtsReady) {
            initializeAtlasVoice()
            return
        }

        atlasTts?.speak(
            last,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "AZIMI_ATLAS_MESSAGE"
        )
    }

    private fun handleIncomingAuthIntent(
        incomingIntent: Intent?
    ) {
        val uri =
            incomingIntent?.data
                ?: return

        runCatching {
            AzimiAuth.handleAuthCallback(
                this,
                uri
            )
        }.onSuccess {
            aiStatus?.text =
                "AUTHENTICATION COMPLETE"

            aiStatus?.setTextColor(
                green
            )
        }.onFailure {
            aiStatus?.text =
                "AUTHENTICATION FAILED"

            aiStatus?.setTextColor(
                red
            )
        }
    }

    override fun onDestroy() {
        runCatching {
            atlasTts?.stop()
            atlasTts?.shutdown()
        }

        atlasTts = null
        atlasTtsReady = false

        super.onDestroy()
    }
}
