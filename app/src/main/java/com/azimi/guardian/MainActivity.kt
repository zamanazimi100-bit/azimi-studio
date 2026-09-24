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

    // ============================================================
    // AZIMI DESIGN SYSTEM
    // ============================================================

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

    // ============================================================
    // STATE
    // ============================================================

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

    // ============================================================
    // ATLAS VOICE ENGINE
    // ============================================================

    private var atlasTts: TextToSpeech? = null
    private var atlasTtsReady = false
    private var atlasSpeechEnabled = true

    private val aiHistory =
        mutableListOf<AzimiAiClient.ChatMessage>()

    // ============================================================
    // ACTIVITY
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        // ========================================================
        // BUILD #74 DIAGNOSTIC
        // ========================================================
        //
        // Build #72:
        // GuardianDiagnosticsStartup was active.
        // App showed logo briefly and then closed.
        //
        // Build #73:
        // GuardianDiagnosticsStartup was disabled.
        // App STILL showed logo briefly and then closed.
        //
        // Build #74:
        // GuardianDiagnosticsStartup remains disabled.
        // Atlas TextToSpeech initialization is ALSO disabled.
        //
        // This isolates the Android TextToSpeech initialization
        // from the pre-UI launch path.
        //
        // No architecture is removed.
        // No security boundary is redesigned.
        // No existing feature is deleted.
        //
        // ========================================================

        configureWindow()

        // BUILD #74 DIAGNOSTIC:
        //
        // Temporarily disabled.
        //
        // initializeAtlasVoice()

        val incomingUri =
            intent?.data

        if (
            incomingUri != null &&
            isAzimiAuthCallback(incomingUri)
        ) {
            showAI()
            handleIncomingAuthIntent(intent)
        } else {
            showHome()
        }
    }

    override fun onNewIntent(
        intent: Intent?
    ) {
        super.onNewIntent(intent)

        if (intent != null) {

            setIntent(intent)

            if (
                isAzimiAuthCallback(
                    intent.data
                )
            ) {

                showAI()

                handleIncomingAuthIntent(
                    intent
                )
            }
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

        if (
            requestCode !=
            VaultAuth.REQUEST_CODE
        ) {
            return
        }

        if (resultCode == RESULT_OK) {

            val unlocked =
                GuardianStorage.unlockVault(
                    this
                )

            if (!unlocked) {

                originAuthenticationPending =
                    false

                pendingVaultAction =
                    null

                showVaultSecurityMessage(
                    "VAULT ERROR",
                    "Vault authentication succeeded but the protected Vault state could not be opened."
                )

                return
            }

            /*
             * Z SECURITY SESSION
             *
             * Unlocking the Vault creates the protected
             * Guardian session.
             *
             * Exiting the Vault later does NOT clear it.
             */
            ZSecuritySession.startProtectedSession(
                this,
                ZSecurity.AuthenticationMethod.DEVICE_CREDENTIAL
            )

            /*
             * ATLAS SESSION
             *
             * Successful Vault authentication activates
             * Atlas for the current Guardian session.
             */
            AtlasSession.start(
                this
            )

            if (
                originAuthenticationPending
            ) {

                originAuthenticationPending =
                    false

                showOrigin()

                return
            }

            when (
                pendingVaultAction
            ) {

                "MEMORY" -> {

                    pendingVaultAction =
                        null

                    showVaultSection(
                        "Z MEMORY",
                        "OWNER-APPROVED CONTEXT",
                        "Approved project context belongs to AZIMI. Secrets, credentials, recovery codes and private keys are never treated as ordinary AI memory."
                    )
                }

                "ARCHIVE" -> {

                    pendingVaultAction =
                        null

                    showVaultSection(
                        "Z ARCHIVE",
                        "CONTINUITY STORAGE",
                        "A future continuity layer for approved AZIMI backups, versions and recoverable project state."
                    )
                }

                "SOVEREIGN" -> {

                    pendingVaultAction =
                        null

                    openSovereignWithOwnerGate()
                }

                "ATLAS" -> {

                    pendingVaultAction =
                        null

                    showAI()
                }

                else -> {

                    pendingVaultAction =
                        null

                    showVault()
                }
            }

        } else {

            originAuthenticationPending =
                false

            pendingVaultAction =
                null

            showVault()
        }
    }

    // ============================================================
    // WINDOW
    // ============================================================

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

    // ============================================================
    // ROOT LAYOUT
    // ============================================================

    private fun baseLayout(): LinearLayout {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setBackgroundColor(bg)

        root.layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        root.setPadding(
            dp(18),
            dp(12),
            dp(18),
            dp(18)
        )

        applyLanguageDirection(root)

        return root
    }

    private fun screen(
        content: LinearLayout
    ): ScrollView {

        val scroll =
            ScrollView(this)

        scroll.setBackgroundColor(bg)

        scroll.isFillViewport =
            true

        scroll.layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        scroll.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        return scroll
    }

    private fun install(
        content: LinearLayout
    ) {

        setContentView(
            screen(content)
        )
    }

    // ============================================================
    // LANGUAGE
    // ============================================================

    private fun applyLanguageDirection(
        view: View
    ) {

        if (
            ZLanguage.isDari(this)
        ) {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.JELLY_BEAN_MR1
            ) {

                view.layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

            view.textDirection =
                View.TEXT_DIRECTION_RTL

        } else {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.JELLY_BEAN_MR1
            ) {

                view.layoutDirection =
                    View.LAYOUT_DIRECTION_LTR
            }

            view.textDirection =
                View.TEXT_DIRECTION_LTR
        }
    }

    private fun tr(
        english: String,
        dari: String
    ): String {

        return ZLanguage.text(
            this,
            english,
            dari
        )
    }

    // ============================================================
    // HEADER
    // ============================================================

    private fun header(
        eyebrow: String,
        title: String,
        subtitle: String
    ): LinearLayout {

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        box.setPadding(
            dp(2),
            dp(8),
            dp(2),
            dp(18)
        )

        val eyebrowView =
            text(
                eyebrow,
                10f,
                cyan
            )

        eyebrowView.letterSpacing =
            0.18f

        eyebrowView.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        val titleView =
            text(
                title,
                30f,
                white
            )

        titleView.typeface =
            Typeface.create(
                Typeface.SANS_SERIF,
                Typeface.BOLD
            )

        titleView.setPadding(
            0,
            dp(5),
            0,
            dp(4)
        )

        val subtitleView =
            text(
                subtitle,
                13f,
                gray
            )

        box.addView(
            eyebrowView
        )

        box.addView(
            titleView
        )

        box.addView(
            subtitleView
        )

        applyLanguageDirection(box)

        return box
    }

    // ============================================================
    // IDENTITY RAIL
    // ============================================================

    private fun identityRail(
        state: String,
        stateColor: Int
    ): LinearLayout {

        val rail =
            LinearLayout(this)

        rail.orientation =
            LinearLayout.HORIZONTAL

        rail.gravity =
            Gravity.CENTER_VERTICAL

        rail.background =
            rounded(
                surface,
                stateColor,
                1f,
                18f
            )

        rail.setPadding(
            dp(14),
            dp(11),
            dp(14),
            dp(11)
        )

        val mark =
            text(
                "Z",
                18f,
                stateColor
            )

        mark.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        val center =
            LinearLayout(this)

        center.orientation =
            LinearLayout.VERTICAL

        val name =
            text(
                "AZIMI",
                11f,
                white
            )

        name.letterSpacing =
            0.25f

        name.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        val sub =
            text(
                "SOVEREIGN CORE",
                9f,
                gray
            )

        sub.letterSpacing =
            0.12f

        center.addView(
            name
        )

        center.addView(
            sub
        )

        val stateView =
            text(
                state,
                10f,
                stateColor
            )

        stateView.gravity =
            Gravity.CENTER

        stateView.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        rail.addView(
            mark,
            LinearLayout.LayoutParams(
                dp(32),
                dp(40)
            )
        )

        rail.addView(
            center,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        rail.addView(
            stateView
        )

        return rail
    }

    // ============================================================
    // SECTION LABEL
    // ============================================================

    private fun sectionLabel(
        value: String
    ): TextView {

        val view =
            text(
                value,
                10f,
                darkGray
            )

        view.letterSpacing =
            0.18f

        view.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        view.setPadding(
            dp(3),
            dp(18),
            dp(3),
            dp(8)
        )

        return view
    }

    // ============================================================
    // SYSTEM STATUS
    // ============================================================

    private fun systemCard(): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.background =
            rounded(
                panel,
                darkGray,
                1f,
                radius
            )

        card.setPadding(
            dp(17),
            dp(17),
            dp(17),
            dp(17)
        )

        val title =
            text(
                tr(
                    "SYSTEM INTEGRITY",
                    "یکپارچگی سیستم"
                ),
                11f,
                green
            )

        title.letterSpacing =
            0.12f

        title.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        val main =
            text(
                tr(
                    "GUARDIAN ONLINE",
                    "گاردین آنلاین"
                ),
                21f,
                white
            )

        main.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        val description =
            text(
                tr(
                    "AZIMI Guardian is operating as the protected device layer.",
                    "AZIMI Guardian به عنوان لایه محافظ دستگاه فعال است."
                ),
                12f,
                gray
            )

        card.addView(
            title
        )

        card.addView(
            main
        )

        card.addView(
            description,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin =
                    dp(7)
            }
        )

        return card
    }

    // ============================================================
    // MODULE CARD
    // ============================================================

    private fun moduleCard(
        code: String,
        title: String,
        description: String,
        accent: Int,
        action: () -> Unit
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.background =
            rounded(
                panel,
                accent,
                1f,
                radius
            )

        card.setPadding(
            dp(16),
            dp(15),
            dp(16),
            dp(15)
        )

        card.isClickable =
            true

        card.isFocusable =
            true

        card.setOnClickListener {
            action()
        }

        val top =
            LinearLayout(this)

        top.orientation =
            LinearLayout.HORIZONTAL

        top.gravity =
            Gravity.CENTER_VERTICAL

        val codeView =
            text(
                code,
                11f,
                accent
            )

        codeView.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        codeView.letterSpacing =
            0.12f

        val arrow =
            text(
                "›",
                25f,
                darkGray
            )

        arrow.gravity =
            Gravity.CENTER

        top.addView(
            codeView,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        top.addView(
            arrow,
            LinearLayout.LayoutParams(
                dp(30),
                dp(30)
            )
        )

        val titleView =
            text(
                title,
                17f,
                white
            )

        titleView.typeface =
            Typeface.create(
                Typeface.SANS_SERIF,
                Typeface.BOLD
            )

        val descriptionView =
            text(
                description,
                11f,
                gray
            )

        card.addView(
            top
        )

        card.addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin =
                    dp(8)
            }
        )

        card.addView(
            descriptionView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin =
                    dp(4)
            }
        )

        return card
    }

    // ============================================================
    // HOME
    // ============================================================

    private fun showHome() {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "CORE ONLINE",
                green
            )
        )

        root.addView(
            header(
                "01 / AZIMI",
                "SOVEREIGN CORE",
                tr(
                    "A private command center for the AZIMI system.",
                    "مرکز فرمان خصوصی برای سیستم AZIMI."
                )
            )
        )

        root.addView(
            systemCard()
        )

        root.addView(
            sectionLabel(
                tr(
                    "COMMAND ARCHITECTURE",
                    "معماری فرمان"
                )
            )
        )

        root.addView(
            moduleCard(
                "Z01",
                "Z CONTROL",
                tr(
                    "Read-only device intelligence and diagnostics.",
                    "اطلاعات و تشخیص خواندنی دستگاه."
                ),
                cyan
            ) {
                showControl()
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "Z02",
                "Z VAULT",
                tr(
                    "Protected private storage and security boundary.",
                    "ذخیره‌سازی خصوصی و مرز امنیتی محافظت‌شده."
                ),
                purple
            ) {
                showVault()
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "Z03",
                "Z RECOVERY",
                tr(
                    "Continuity, recovery and repair foundation.",
                    "پایه تداوم، بازیابی و تعمیر."
                ),
                blue
            ) {
                showRecovery()
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "Z04",
                "Z SHIELD",
                tr(
                    "Security enforcement and protection layer.",
                    "لایه اجرای امنیت و محافظت."
                ),
                red
            ) {
                showShield()
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "Z05",
                "ATLAS AI",
                tr(
                    "Protected AI intelligence through Guardian.",
                    "هوش مصنوعی محافظت‌شده از طریق Guardian."
                ),
                green
            ) {
                showAI()
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "Z06",
                "Z LAB",
                tr(
                    "Experimental space for future AZIMI capabilities.",
                    "محیط آزمایشی برای قابلیت‌های آینده AZIMI."
                ),
                amber
            ) {
                showLab()
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "Z07",
                "Z ORIGIN",
                tr(
                    "Special owner identity architecture.",
                    "معماری ویژه هویت مالک."
                ),
                purple
            ) {
                openOrigin()
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "Z08",
                "Z CLOUD",
                tr(
                    "Encrypted backup, synchronization, recovery and portability.",
                    "پشتیبان‌گیری رمزگذاری‌شده، همگام‌سازی، بازیابی و قابلیت انتقال."
                ),
                cyan
            ) {
                showCloud()
            }
        )

        root.addView(
            sectionLabel(
                tr(
                    "CORE STATUS",
                    "وضعیت هسته"
                )
            )
        )

        root.addView(
            statusPanel(
                "VAULT",
                GuardianStorage.getVaultStatus(
                    this
                ),
                purple
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "ATLAS SESSION",
                if (
                    AtlasSession.isActive(this)
                ) {
                    "ACTIVE"
                } else {
                    "LOCKED"
                },
                if (
                    AtlasSession.isActive(this)
                ) {
                    green
                } else {
                    darkGray
                }
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "AI POLICY",
                GuardianStorage.getAIMemoryPolicy(
                    this
                ),
                green
            )
        )

        root.addView(space(8))

        val ownerState =
            AtlasOwnerAuthority.getState(
                this
            )

        val ownerStatus =
            if (
                ownerState.ownerAuthorized
            ) {
                "OWNER VERIFIED"
            } else {
                "RESTRICTED"
            }

        root.addView(
            statusPanel(
                "OWNER AREA",
                ownerStatus,
                if (
                    ownerState.ownerAuthorized
                ) {
                    green
                } else {
                    purple
                }
            )
        )

        root.addView(space(18))

        root.addView(
            actionButton(
                tr(
                    "LANGUAGE · ",
                    "زبان · "
                ) +
                    ZLanguage.languageName(
                        this
                    ),
                cyan
            ) {

                ZLanguage.toggle(
                    this
                )

                showHome()
            }
        )

        install(root)
    }

    // ============================================================
    // Z CLOUD
    // ============================================================

    private fun showCloud() {

        CloudScreen(
            this
        ).show()
    }

    fun showHomeFromCloud() {

        showHome()
    }

    // ============================================================
    // STATUS PANEL
    // ============================================================

    private fun statusPanel(
        label: String,
        value: String,
        accent: Int
    ): LinearLayout {

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        row.background =
            rounded(
                surface,
                darkGray,
                1f,
                16f
            )

        row.setPadding(
            dp(14),
            dp(12),
            dp(14),
            dp(12)
        )

        val left =
            text(
                label,
                10f,
                gray
            )

        left.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        val right =
            text(
                value,
                10f,
                accent
            )

        right.gravity =
            Gravity.CENTER

        right.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        row.addView(
            left,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        row.addView(
            right
        )

        return row
    }

    // ============================================================
    // Z CONTROL
    // ============================================================

    private fun showControl() {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "READ ONLY",
                cyan
            )
        )

        root.addView(
            header(
                "Z01 / CONTROL",
                "Z CONTROL",
                "Device intelligence without destructive control."
            )
        )

        val battery =
            getSystemService(
                BatteryManager::class.java
            ).getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )

        val stat =
            StatFs(
                Environment
                    .getDataDirectory()
                    .path
            )

        val total =
            stat.totalBytes /
                (1024.0 * 1024.0 * 1024.0)

        val free =
            stat.availableBytes /
                (1024.0 * 1024.0 * 1024.0)

        root.addView(
            statusPanel(
                "BATTERY",
                "$battery%",
                green
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "STORAGE",
                "${free.roundToInt()} GB FREE / ${total.roundToInt()} GB",
                cyan
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "ANDROID",
                Build.VERSION.RELEASE,
                blue
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "SDK",
                Build.VERSION.SDK_INT.toString(),
                purple
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "DEVICE",
                Build.MODEL ?: "UNKNOWN",
                amber
            )
        )

        root.addView(
            sectionLabel(
                "DIAGNOSTICS"
            )
        )

        root.addView(
            infoCard(
                "GUARDIAN STARTUP",
                "Startup diagnostics are recorded by GuardianDiagnosticsStartup."
            )
        )

        root.addView(space(8))

        root.addView(
            infoCard(
                "STORAGE",
                "Last storage error: ${GuardianStorage.getLastError(this)}"
            )
        )

        root.addView(space(18))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // Z VAULT
    // ============================================================

    private fun showVault() {

        val root =
            baseLayout()

        val unlocked =
            GuardianStorage.getVaultStatus(
                this
            ) == "UNLOCKED"

        val atlasActive =
            AtlasSession.isActive(
                this
            )

        val state =
            if (unlocked) {
                "VAULT OPEN"
            } else {
                "VAULT SEALED"
            }

        val stateColor =
            if (unlocked) {
                green
            } else {
                purple
            }

        root.addView(
            identityRail(
                state,
                stateColor
            )
        )

        root.addView(
            header(
                "Z02 / PRIVATE",
                "Z VAULT",
                tr(
                    "A protected boundary between AZIMI intelligence and private owner data.",
                    "مرز محافظت‌شده میان هوش AZIMI و اطلاعات خصوصی مالک."
                )
            )
        )

        val identity =
            LinearLayout(this)

        identity.orientation =
            LinearLayout.VERTICAL

        identity.background =
            rounded(
                panel,
                purple,
                1f,
                26f
            )

        identity.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        val symbol =
            text(
                "Z",
                46f,
                purple
            )

        symbol.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        val vaultState =
            text(
                state,
                19f,
                white
            )

        vaultState.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        val description =
            text(
                tr(
                    "SECURITY LEVEL · PROTECTED",
                    "سطح امنیت · محافظت‌شده"
                ),
                10f,
                gray
            )

        description.letterSpacing =
            0.12f

        identity.addView(symbol)
        identity.addView(vaultState)
        identity.addView(description)

        root.addView(identity)

        root.addView(
            space(10)
        )

        root.addView(
            statusPanel(
                "ATLAS SESSION",
                if (atlasActive) {
                    "ACTIVE"
                } else {
                    "LOCKED"
                },
                if (atlasActive) {
                    green
                } else {
                    darkGray
                }
            )
        )

        root.addView(
            sectionLabel(
                tr(
                    "PRIVATE SPACES",
                    "فضاهای خصوصی"
                )
            )
        )

        root.addView(
            moduleCard(
                "M01",
                "Z MEMORY",
                "Owner-approved AZIMI context only.",
                cyan
            ) {
                openProtectedVaultArea(
                    "MEMORY"
                )
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "M02",
                "Z ORIGIN",
                "Special owner identity architecture.",
                purple
            ) {
                openOrigin()
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "M03",
                "Z ARCHIVE",
                "Future continuity and backup storage.",
                blue
            ) {
                openProtectedVaultArea(
                    "ARCHIVE"
                )
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "M04",
                "Z SOVEREIGN",
                "Owner-controlled independence architecture.",
                amber
            ) {
                openProtectedVaultArea(
                    "SOVEREIGN"
                )
            }
        )

        root.addView(space(10))

        root.addView(
            moduleCard(
                "M05",
                "ATLAS AI",
                if (atlasActive) {
                    "ATLAS ACTIVE · AVAILABLE OUTSIDE Z VAULT."
                } else {
                    "Activate Atlas for the current Guardian session."
                },
                green
            ) {

                if (atlasActive) {
                    showAI()
                } else {
                    activateAtlasFromVault()
                }
            }
        )

        root.addView(
            sectionLabel(
                "VAULT CONTROL"
            )
        )

        if (unlocked) {

            root.addView(
                actionButton(
                    "LOCK VAULT · KEEP ATLAS ACTIVE",
                    purple
                ) {

                    val locked =
                        GuardianStorage.lockVault(
                            this
                        )

                    if (locked) {

                        AtlasSession.onVaultExit(
                            this
                        )

                        showHome()

                    } else {

                        showVaultSecurityMessage(
                            "VAULT LOCK ERROR",
                            "Guardian could not seal the protected Vault state."
                        )
                    }
                }
            )

            root.addView(
                space(10)
            )

            root.addView(
                actionButton(
                    "FULL LOCK · LOCK ATLAS",
                    red
                ) {

                    showFullLockConfirmation()
                }
            )

        } else {

            root.addView(
                actionButton(
                    "OPEN Z VAULT",
                    green
                ) {

                    pendingVaultAction =
                        null

                    requestVaultAuthentication()
                }
            )

            if (atlasActive) {

                root.addView(
                    space(10)
                )

                root.addView(
                    infoCard(
                        "ATLAS SESSION ACTIVE",
                        "Z Vault is sealed, but Atlas remains active for the current Guardian session. Use Atlas from the Home screen. Full Lock is required to terminate Atlas."
                    )
                )
            }
        }

        root.addView(space(10))

        root.addView(
            infoCard(
                "AI BOUNDARY",
                "Guardian AI policy: ${GuardianStorage.getAIMemoryPolicy(this)}"
            )
        )

        root.addView(space(18))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // ATLAS ACTIVATION
    // ============================================================

    private fun activateAtlasFromVault() {

        if (
            GuardianStorage.getVaultStatus(
                this
            ) != "UNLOCKED"
        ) {

            pendingVaultAction =
                "ATLAS"

            requestVaultAuthentication()

            return
        }

        AtlasSession.start(
            this
        )

        showAI()
    }

    // ============================================================
    // FULL LOCK
    // ============================================================

    private fun showFullLockConfirmation() {

        AlertDialog.Builder(this)
            .setTitle(
                "FULL LOCK"
            )
            .setMessage(
                "Full Lock will seal Z Vault and terminate the active Atlas session. Owner authority and the protected Guardian security session will also be revoked. Continue?"
            )
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "FULL LOCK"
            ) { _, _ ->

                performFullLock()
            }
            .show()
    }

    private fun performFullLock() {

        val locked =
            GuardianStorage.lockVault(
                this
            )

        if (!locked) {

            showVaultSecurityMessage(
                "FULL LOCK ERROR",
                "Guardian could not seal the protected Vault state. Atlas remains active because the full lock operation did not complete."
            )

            return
        }

        stopAtlasVoice()

        AtlasSession.fullLock(
            this
        )

        originAuthenticationPending =
            false

        pendingVaultAction =
            null

        showHome()
    }

    // ============================================================
    // PROTECTED VAULT AREAS
    // ============================================================

    private fun openProtectedVaultArea(
        action: String
    ) {

        if (
            GuardianStorage.getVaultStatus(
                this
            ) == "UNLOCKED"
        ) {

            when (action) {

                "MEMORY" ->
                    showVaultSection(
                        "Z MEMORY",
                        "OWNER-APPROVED CONTEXT",
                        "Approved project context belongs to AZIMI. Secrets and credentials are excluded."
                    )

                "ARCHIVE" ->
                    showVaultSection(
                        "Z ARCHIVE",
                        "CONTINUITY STORAGE",
                        "A future protected area for backups, versions and recovery material."
                    )

                "SOVEREIGN" ->
                    openSovereignWithOwnerGate()
            }

            return
        }

        pendingVaultAction =
            action

        requestVaultAuthentication()
    }

    private fun requestVaultAuthentication() {

        if (
            !VaultAuth.isDeviceSecure(
                this
            )
        ) {

            showVaultAuthenticationUnavailable()

            return
        }

        VaultAuth.requestAuthentication(
            this
        )
    }

    // ============================================================
    // VAULT SECTION
    // ============================================================

    private fun showVaultSection(
        title: String,
        subtitle: String,
        description: String
    ) {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "AUTHENTICATED",
                green
            )
        )

        root.addView(
            header(
                "Z VAULT / PRIVATE",
                title,
                subtitle
            )
        )

        root.addView(
            infoCard(
                "ACCESS BOUNDARY",
                description
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "SECURITY",
                "Device authentication established a protected Guardian session."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "AI BOUNDARY",
                "Guardian AI receives only policy-approved context. Raw protected Vault data is not directly exposed."
            )
        )

        root.addView(space(20))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // Z ORIGIN
    // ============================================================

    private fun openOrigin() {

        if (
            GuardianStorage.getVaultStatus(
                this
            ) != "UNLOCKED"
        ) {

            originAuthenticationPending =
                true

            requestVaultAuthentication()

            return
        }

        showOrigin()
    }

    private fun showOrigin() {

        val root =
            baseLayout()

        val authority =
            AtlasOwnerAuthority.getState(
                this
            )

        val ownerVerified =
            authority.ownerAuthorized &&
                authority.authorityLevel ==
                AtlasOwnerAuthority.AuthorityLevel.OWNER

        val railState =
            if (ownerVerified) {
                "OWNER VERIFIED"
            } else {
                "OWNER GATE"
            }

        val railColor =
            if (ownerVerified) {
                green
            } else {
                purple
            }

        root.addView(
            identityRail(
                railState,
                railColor
            )
        )

        root.addView(
            header(
                "Z07 / ORIGIN",
                "Z ORIGIN",
                "OWNER IDENTITY · SPECIAL ACCESS"
            )
        )

        root.addView(
            infoCard(
                "CURRENT SECURITY STATE",
                if (ownerVerified) {
                    tr(
                        "● DEVICE AUTHENTICATION PASSED · OWNER AUTHORITY VERIFIED",
                        "● تأیید هویت دستگاه موفق بود · صلاحیت مالک تأیید شد"
                    )
                } else {
                    tr(
                        "● DEVICE AUTHENTICATION PASSED · OWNER VERIFICATION REQUIRED",
                        "● تأیید هویت دستگاه موفق بود · تأیید مالک مورد نیاز است"
                    )
                }
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "OWNER SPACE",
                tr(
                    "This is a special owner-controlled architecture area. It must never be treated as an ordinary user feature.",
                    "این بخش معماری ویژه و تحت کنترل مالک است و نباید مانند یک قابلیت عادی کاربر در نظر گرفته شود."
                )
            )
        )

        root.addView(
            sectionLabel(
                "OWNER ARCHITECTURE"
            )
        )

        root.addView(
            statusPanel(
                "IDENTITY",
                if (ownerVerified) {
                    "ZAMAN AZIMI"
                } else {
                    "OWNER ONLY"
                },
                purple
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "AUTHORITY",
                if (ownerVerified) {
                    "OWNER"
                } else {
                    "NOT VERIFIED"
                },
                if (ownerVerified) {
                    green
                } else {
                    amber
                }
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "BIOMETRICS",
                if (ownerVerified) {
                    "VERIFIED"
                } else {
                    "AVAILABLE"
                },
                if (ownerVerified) {
                    green
                } else {
                    cyan
                }
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "METHOD",
                authority.authorizationMethod
                    ?: "NONE",
                if (ownerVerified) {
                    green
                } else {
                    darkGray
                }
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "VOICE LOCK",
                "FUTURE / OWNER",
                purple
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "SOVEREIGN",
                if (ownerVerified) {
                    "OWNER VERIFIED"
                } else {
                    "RESTRICTED"
                },
                amber
            )
        )

        root.addView(space(12))

        if (!ownerVerified) {

            root.addView(
                actionButton(
                    "VERIFY OWNER",
                    green
                ) {
                    requestOwnerVerification()
                }
            )

            root.addView(space(10))

            root.addView(
                infoCard(
                    "OWNER VERIFICATION",
                    "Android will display the system biometric prompt. AZIMI does not receive or store your biometric data. Successful BIOMETRIC_STRONG authentication activates the local OWNER authority state."
                )
            )

        } else {

            root.addView(
                actionButton(
                    "OWNER AUTHORITY ACTIVE",
                    green
                ) {
                    showOwnerAuthorityDetails()
                }
            )

            root.addView(space(10))

            root.addView(
                actionButton(
                    "REVOKE OWNER AUTHORITY",
                    red
                ) {

                    AtlasOwnerAuthority
                        .revokeOwnerAuthorization(
                            this
                        )

                    showOrigin()
                }
            )

            root.addView(space(10))

            root.addView(
                infoCard(
                    "VERIFIED STATE",
                    "Owner authority is active for this authenticated AZIMI session. Sensitive operations still pass through explicit operation-level authorization."
                )
            )
        }

        root.addView(space(10))

        root.addView(
            actionButton(
                "Z SOVEREIGN",
                amber
            ) {
                openSovereignWithOwnerGate()
            }
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "IMPORTANT",
                "The current owner gate uses Android BIOMETRIC_STRONG as an owner-verification factor. It does not claim that Android biometrics are legal proof of identity or ownership. Future Z Origin layers can add additional owner factors such as voice when securely supported."
            )
        )

        root.addView(space(20))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // OWNER VERIFICATION
    // ============================================================

    private fun requestOwnerVerification() {

        AtlasOwnerAuthority.verifyOwner(
            this
        ) { result ->

            if (
                result.success &&
                result.ownerAuthorized
            ) {

                showOrigin()

                showOwnerVerificationMessage(
                    "OWNER VERIFIED",
                    "Android BIOMETRIC_STRONG verification succeeded. AZIMI owner authority is now active for this protected session."
                )

            } else {

                showOrigin()

                showOwnerVerificationMessage(
                    "OWNER VERIFICATION",
                    result.message
                )
            }
        }
    }

    private fun showOwnerVerificationMessage(
        title: String,
        message: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    private fun showOwnerAuthorityDetails() {

        val authority =
            AtlasOwnerAuthority.getState(
                this
            )

        val details =
            buildString {

                appendLine(
                    "OWNER: ${AtlasKnowledge.OWNER_NAME}"
                )

                appendLine(
                    "OWNER ID: ${AtlasOwnerAuthority.getOwnerIdentity()}"
                )

                appendLine(
                    "ACTOR: ${authority.actorType}"
                )

                appendLine(
                    "AUTHORITY: ${authority.authorityLevel}"
                )

                appendLine(
                    "AUTHENTICATED: ${authority.authenticated}"
                )

                appendLine(
                    "OWNER AUTHORIZED: ${authority.ownerAuthorized}"
                )

                appendLine(
                    "METHOD: ${authority.authorizationMethod ?: "NONE"}"
                )

                appendLine(
                    "AUTHORIZED AT: ${authority.authorizedAt ?: "NONE"}"
                )

                appendLine()

                append(
                    authority.message
                )
            }

        showOwnerVerificationMessage(
            "OWNER AUTHORITY",
            details
        )
    }

    // ============================================================
    // Z SOVEREIGN
    // ============================================================

    private fun openSovereignWithOwnerGate() {

        val authority =
            AtlasOwnerAuthority.getState(
                this
            )

        if (
            authority.authorityLevel !=
            AtlasOwnerAuthority.AuthorityLevel.OWNER
        ) {

            showOwnerVerificationRequiredDialog()

            return
        }

        showSovereign()
    }

    private fun showOwnerVerificationRequiredDialog() {

        AlertDialog.Builder(this)
            .setTitle(
                "OWNER AUTHORITY REQUIRED"
            )
            .setMessage(
                "Z SOVEREIGN is an owner-controlled area. Verify owner authority through Android BIOMETRIC_STRONG before entering."
            )
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "VERIFY OWNER"
            ) { _, _ ->

                requestOwnerVerification()
            }
            .show()
    }

    private fun showSovereign() {

        val authority =
            AtlasOwnerAuthority.getState(
                this
            )

        if (
            authority.authorityLevel !=
            AtlasOwnerAuthority.AuthorityLevel.OWNER
        ) {

            showOwnerVerificationRequiredDialog()

            return
        }

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "OWNER AUTHORIZED",
                amber
            )
        )

        root.addView(
            header(
                "Z04 / SOVEREIGN",
                "Z SOVEREIGN",
                "OWNERSHIP · INDEPENDENCE · PORTABILITY"
            )
        )

        root.addView(
            infoCard(
                "OWNER AUTHORITY",
                "Zaman Azimi owner authority has been verified through the protected Guardian owner gate."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "OWNERSHIP",
                "Zaman owns the AZIMI architecture, source, approved memory model, security policy and recovery direction."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "PROVIDER INDEPENDENCE",
                "GitHub, Vercel, Cloudflare, Supabase and external AI providers are infrastructure modules — not the identity of AZIMI."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "AI INDEPENDENCE",
                "External AI engines are replaceable adapters. Atlas Core remains the coordinating intelligence architecture."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "PORTABILITY",
                "The long-term goal is recoverable source, approved memory, configuration, backups and migration paths."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "RECOVERY",
                "No single provider should be able to determine whether AZIMI can continue to exist."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "PROVENANCE",
                "Technical provenance records can identify the declared creator and owner within AZIMI's architecture. They are not a substitute for jurisdiction-specific legal registration."
            )
        )

        root.addView(space(20))

        root.addView(
            actionButton(
                "OWNER AUTHORITY",
                green
            ) {
                showOwnerAuthorityDetails()
            }
        )

        root.addView(space(10))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // Z RECOVERY
    // ============================================================

    private fun showRecovery() {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "SAFE RECOVERY",
                blue
            )
        )

        root.addView(
            header(
                "Z03 / RECOVERY",
                "Z RECOVERY",
                "CONTINUITY WITHOUT AUTOMATIC DESTRUCTIVE ACTION"
            )
        )

        root.addView(
            infoCard(
                "RECOVERY FOUNDATION",
                "Recovery operations require explicit user action. Guardian does not silently destroy, reset or overwrite protected state."
            )
        )

        root.addView(space(10))

        root.addView(
            statusPanel(
                "RECOVERY",
                "FOUNDATION READY",
                blue
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "AUTOMATIC DESTRUCTION",
                "DISABLED",
                green
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "OWNER ACTION",
                "REQUIRED",
                amber
            )
        )

        root.addView(space(20))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // Z SHIELD
    // ============================================================

    private fun showShield() {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "PROTECTION LAYER",
                red
            )
        )

        root.addView(
            header(
                "Z04 / SHIELD",
                "Z SHIELD",
                "SECURITY ENFORCEMENT"
            )
        )

        root.addView(
            statusPanel(
                "CURRENT STATE",
                "NOT CONFIGURED",
                red
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "DESIGN PRINCIPLE",
                "Security controls must be explicit, auditable and reversible where possible. Guardian must not silently bypass Android security boundaries."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "FUTURE",
                "Network protection, threat signals, policy enforcement and security diagnostics can be connected here."
            )
        )

        root.addView(space(20))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // Z LAB
    // ============================================================

    private fun showLab() {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "EXPERIMENTAL",
                amber
            )
        )

        root.addView(
            header(
                "Z06 / LAB",
                "Z LAB",
                "EXPERIMENT · TEST · VERIFY · PROMOTE"
            )
        )

        root.addView(
            infoCard(
                "LAB RULE",
                "Experimental components should be isolated from the protected core until they have been tested and explicitly promoted."
            )
        )

        root.addView(space(10))

        root.addView(
            statusPanel(
                "CORE IMPACT",
                "ISOLATED",
                green
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "PROMOTION",
                "MANUAL",
                amber
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "ROLLBACK",
                "PLANNED",
                blue
            )
        )

        root.addView(space(20))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // ATLAS VOICE ENGINE
    // ============================================================

    private fun initializeAtlasVoice() {

        atlasTtsReady =
            false

        atlasTts =
            TextToSpeech(
                this
            ) { status ->

                if (
                    status !=
                    TextToSpeech.SUCCESS
                ) {

                    atlasTtsReady =
                        false

                    aiStatus?.text =
                        "ATLAS VOICE · UNAVAILABLE"

                    aiStatus?.setTextColor(
                        amber
                    )

                    return@TextToSpeech
                }

                atlasTtsReady =
                    true

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

    private fun configureAtlasVoiceLanguage(): Boolean {

        val tts =
            atlasTts
                ?: return false

        val preferredLocale =
            if (
                ZLanguage.isDari(this)
            ) {

                Locale(
                    "fa",
                    "AF"
                )

            } else {

                Locale.ENGLISH
            }

        var result =
            tts.setLanguage(
                preferredLocale
            )

        if (
            result ==
            TextToSpeech.LANG_MISSING_DATA ||
            result ==
            TextToSpeech.LANG_NOT_SUPPORTED
        ) {

            if (
                ZLanguage.isDari(this)
            ) {

                result =
                    tts.setLanguage(
                        Locale("fa")
                    )
            }
        }

        if (
            result ==
            TextToSpeech.LANG_MISSING_DATA ||
            result ==
            TextToSpeech.LANG_NOT_SUPPORTED
        ) {

            return false
        }

        tts.setSpeechRate(
            0.95f
        )

        tts.setPitch(
            1.0f
        )

        return true
    }

    private fun speakAtlas(
        message: String
    ) {

        if (
            !atlasSpeechEnabled
        ) {
            return
        }

        if (
            message.isBlank()
        ) {
            return
        }

        if (
            !AtlasSession.isActive(
                this
            )
        ) {
            return
        }

        if (
            AzimiAuth.isProtectedCredential(
                message
            )
        ) {
            return
        }

        val tts =
            atlasTts
                ?: return

        if (
            !atlasTtsReady
        ) {
            return
        }

        val languageReady =
            configureAtlasVoiceLanguage()

        if (!languageReady) {

            aiStatus?.text =
                "ATLAS VOICE · LANGUAGE UNAVAILABLE"

            aiStatus?.setTextColor(
                amber
            )

            return
        }

        tts.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "azimi_atlas_response"
        )
    }

    private fun stopAtlasVoice() {

        atlasTts?.stop()
    }

    // ============================================================
    // ATLAS AI
    // ============================================================

    private fun showAI() {

        val root =
            baseLayout()

        val atlasActive =
            AtlasSession.isActive(
                this
            )

        val vaultOpen =
            GuardianStorage.getVaultStatus(
                this
            ) == "UNLOCKED"

        val railState =
            when {

                atlasActive &&
                    vaultOpen ->
                    "ATLAS ACTIVE · VAULT OPEN"

                atlasActive ->
                    "ATLAS ACTIVE · VAULT LOCKED"

                else ->
                    "ATLAS LOCKED"
            }

        val railColor =
            if (atlasActive) {
                green
            } else {
                darkGray
            }

        root.addView(
            identityRail(
                railState,
                railColor
            )
        )

        root.addView(
            header(
                "Z05 / INTELLIGENCE",
                "ATLAS AI",
                tr(
                    "Personal intelligence through the Guardian policy boundary.",
                    "هوش شخصی از طریق مرز سیاست Guardian."
                )
            )
        )

        aiStatus =
            text(
                if (atlasActive) {
                    "ATLAS SESSION · ACTIVE"
                } else {
                    "ATLAS SESSION · LOCKED"
                },
                10f,
                if (atlasActive) {
                    green
                } else {
                    amber
                }
            )

        aiStatus?.letterSpacing =
            0.12f

        aiStatus?.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        root.addView(
            aiStatus,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin =
                    dp(10)
            }
        )

        val conversation =
            LinearLayout(this)

        conversation.orientation =
            LinearLayout.VERTICAL

        conversation.background =
            rounded(
                surface,
                darkGray,
                1f,
                20f
            )

        conversation.setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

        aiConversation =
            conversation

        root.addView(
            conversation,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(250)
            )
        )

        root.addView(
            space(12)
        )

        aiInput =
            EditText(this)

        aiInput?.hint =
            tr(
                "Ask Atlas...",
                "از Atlas بپرسید..."
            )

        aiInput?.setTextColor(
            white
        )

        aiInput?.setHintTextColor(
            gray
        )

        aiInput?.setSingleLine(
            false
        )

        aiInput?.minLines =
            2

        aiInput?.maxLines =
            5

        aiInput?.gravity =
            Gravity.TOP or
                Gravity.START

        aiInput?.background =
            rounded(
                panel,
                darkGray,
                1f,
                18f
            )

        aiInput?.setPadding(
            dp(15),
            dp(14),
            dp(15),
            dp(14)
        )

        applyLanguageDirection(
            aiInput!!
        )

        root.addView(
            aiInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            space(10)
        )

        aiSendButton =
            actionButton(
                "SEND TO ATLAS",
                green
            ) {
                sendAIMessage()
            }

        root.addView(
            aiSendButton
        )

        root.addView(
            space(8)
        )

        aiVoiceButton =
            actionButton(
                if (
                    atlasSpeechEnabled
                ) {
                    "🦜 ATLAS VOICE · ON"
                } else {
                    "🔇 ATLAS VOICE · OFF"
                },
                cyan
            ) {

                if (
                    atlasSpeechEnabled
                ) {

                    atlasSpeechEnabled =
                        false

                    stopAtlasVoice()

                    aiVoiceButton?.text =
                        "🔇 ATLAS VOICE · OFF"

                    aiStatus?.text =
                        "ATLAS VOICE · MUTED"

                    aiStatus?.setTextColor(
                        gray
                    )

                } else {

                    val ready =
                        atlasTtsReady &&
                            configureAtlasVoiceLanguage()

                    if (!ready) {

                        atlasSpeechEnabled =
                            false

                        aiVoiceButton?.text =
                            "🔇 ATLAS VOICE · OFF"

                        aiStatus?.text =
                            "ATLAS VOICE · LANGUAGE UNAVAILABLE"

                        aiStatus?.setTextColor(
                            amber
                        )

                        addAIMessage(
                            "SYSTEM",
                            "The selected voice language is not available in the Android speech engine. Atlas text responses remain available."
                        )

                    } else {

                        atlasSpeechEnabled =
                            true

                        aiVoiceButton?.text =
                            "🦜 ATLAS VOICE · ON"

                        aiStatus?.text =
                            "ATLAS VOICE · READY"

                        aiStatus?.setTextColor(
                            cyan
                        )

                        speakAtlas(
                            if (
                                ZLanguage.isDari(this)
                            ) {
                                "Atlas voice is ready."
                            } else {
                                "Atlas voice is ready."
                            }
                        )
                    }
                }
            }

        root.addView(
            aiVoiceButton
        )

        root.addView(
            space(8)
        )

        aiVoiceLanguageButton =
            actionButton(
                "VOICE LANGUAGE · ${AtlasVoice.getLanguageName()}",
                purple
            ) {

                val changed =
                    if (
                        ZLanguage.isDari(this)
                    ) {

                        AtlasVoice.setDari()

                    } else {

                        AtlasVoice.setEnglish()
                    }

                if (
                    changed
                ) {

                    val ready =
                        configureAtlasVoiceLanguage()

                    aiVoiceLanguageButton?.text =
                        "VOICE LANGUAGE · ${AtlasVoice.getLanguageName()}"

                    if (ready) {

                        aiStatus?.text =
                            "ATLAS VOICE · ${AtlasVoice.getLanguageName()}"

                        aiStatus?.setTextColor(
                            cyan
                        )

                    } else {

                        atlasSpeechEnabled =
                            false

                        stopAtlasVoice()

                        aiVoiceButton?.text =
                            "🔇 ATLAS VOICE · OFF"

                        aiStatus?.text =
                            "ATLAS VOICE · LANGUAGE UNAVAILABLE"

                        aiStatus?.setTextColor(
                            amber
                        )

                        addAIMessage(
                            "SYSTEM",
                            "The selected voice language is not available in the Android speech engine. Atlas text responses remain available."
                        )
                    }

                } else {

                    addAIMessage(
                        "SYSTEM",
                        "The selected voice language is not available in the Android speech engine."
                    )
                }
            }

        root.addView(
            aiVoiceLanguageButton
        )

        root.addView(
            space(8)
        )

        aiLoginButton =
            actionButton(
                "AUTHENTICATE AZIMI AI",
                cyan
            ) {

                requestAIAuthentication()
            }

        root.addView(
            aiLoginButton
        )

        root.addView(
            space(8)
        )

        aiLogoutButton =
            actionButton(
                "END AI SESSION",
                red
            ) {

                AzimiAuth.signOut(
                    this
                )

                aiHistory.clear()

                aiConversation
                    ?.removeAllViews()

                refreshAIAuthUI()
            }

        root.addView(
            aiLogoutButton
        )

        root.addView(
            space(12)
        )

        root.addView(
            infoCard(
                "ATLAS SESSION",
                if (atlasActive) {

                    if (vaultOpen) {

                        "Atlas is active while Z Vault is open. You may leave the Vault and continue using Atlas."

                    } else {

                        "Z Vault is locked, but Atlas remains active for the current Guardian session. Only FULL LOCK terminates Atlas."
                    }

                } else {

                    "Atlas is locked. Open and authenticate Z Vault to activate Atlas for the current Guardian session."
                }
            )
        )

        root.addView(
            space(10)
        )

        root.addView(
            infoCard(
                "GUARDIAN POLICY",
                "Atlas requests pass through the Guardian policy boundary. Protected credential material is blocked before any external AI adapter."
            )
        )

        root.addView(
            space(10)
        )

        root.addView(
            infoCard(
                "VOICE",
                if (atlasSpeechEnabled) {
                    "Atlas voice output is enabled. Speech is generated locally through the Android speech engine."
                } else {
                    "Atlas voice output is muted. Text responses remain available."
                }
            )
        )

        root.addView(
            space(18)
        )

        root.addView(
            backButton()
        )

        install(
            root
        )

        refreshAIAuthUI()
    }

    // ============================================================
    // AI AUTH UI
    // ============================================================

    private fun refreshAIAuthUI() {

        val authenticated =
            AzimiAuth.hasSession(
                this
            )

        val atlasActive =
            AtlasSession.isActive(
                this
            )

        if (!atlasActive) {

            aiStatus?.text =
                "ATLAS LOCKED · OPEN Z VAULT TO ACTIVATE"

            aiStatus?.setTextColor(
                amber
            )

            aiInput?.isEnabled =
                false

            aiSendButton?.isEnabled =
                false

        } else {

            aiStatus?.text =
                if (authenticated) {

                    "ATLAS ACTIVE · AZIMI ONLINE AUTHENTICATED"

                } else {

                    "ATLAS ACTIVE · LOCAL READY · ONLINE LOGIN AVAILABLE"
                }

            aiStatus?.setTextColor(
                green
            )

            aiInput?.isEnabled =
                true

            aiSendButton?.isEnabled =
                true
        }

        aiVoiceButton?.isEnabled =
            atlasActive

        aiVoiceLanguageButton?.isEnabled =
            atlasActive

        aiLoginButton?.visibility =
            if (authenticated) {
                View.GONE
            } else {
                View.VISIBLE
            }

        aiLogoutButton?.visibility =
            if (authenticated) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (
            atlasActive &&
            authenticated &&
            aiConversation?.childCount ==
            0
        ) {

            addAIMessage(
                "SYSTEM",
                "Atlas Core connected through Guardian. Atlas remains active until you explicitly use FULL LOCK or the defined Guardian security lifecycle terminates the session."
            )
        }
    }

    // ============================================================
    // AI AUTHENTICATION
    // ============================================================

    private fun requestAIAuthentication() {

        val input =
            EditText(this)

        input.hint =
            "Email address"

        input.setTextColor(
            white
        )

        input.setHintTextColor(
            gray
        )

        input.setSingleLine(
            true
        )

        input.background =
            rounded(
                panel,
                darkGray,
                1f,
                18f
            )

        input.setPadding(
            dp(15),
            dp(14),
            dp(15),
            dp(14)
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "AZIMI AI AUTHENTICATION"
                )
                .setMessage(
                    "Enter your email to receive the secure magic link."
                )
                .setView(
                    input
                )
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
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val email =
                    input.text
                        .toString()
                        .trim()

                if (
                    email.isBlank()
                ) {

                    input.error =
                        "Email is required."

                    return@setOnClickListener
                }

                aiStatus?.text =
                    "AUTHENTICATION · SENDING LINK..."

                aiStatus?.setTextColor(
                    cyan
                )

                aiLoginButton?.isEnabled =
                    false

                AzimiNetwork.requestMagicLink(
                    this,
                    email
                ) { result ->

                    aiLoginButton?.isEnabled =
                        true

                    if (
                        result.success
                    ) {

                        aiStatus?.text =
                            "MAGIC LINK SENT · CHECK EMAIL"

                        aiStatus?.setTextColor(
                            green
                        )

                        addAIMessage(
                            "SYSTEM",
                            result.message
                        )

                        dialog.dismiss()

                    } else {

                        aiStatus?.text =
                            "AUTHENTICATION ERROR"

                        aiStatus?.setTextColor(
                            red
                        )

                        addAIMessage(
                            "SECURITY",
                            result.message
                        )
                    }
                }
            }
        }

        dialog.show()
    }

    // ============================================================
    // ATLAS SEND
    // ============================================================

    private fun sendAIMessage() {

        if (
            !AtlasSession.isActive(
                this
            )
        ) {

            addAIMessage(
                "SECURITY",
                "Atlas is locked. Open Z Vault and activate Atlas before sending messages."
            )

            aiStatus?.text =
                "ATLAS LOCKED"

            aiStatus?.setTextColor(
                amber
            )

            return
        }

        val input =
            aiInput
                ?: return

        val message =
            input.text
                .toString()
                .trim()

        if (
            message.isBlank()
        ) {

            aiStatus?.text =
                "ENTER A MESSAGE"

            aiStatus?.setTextColor(
                amber
            )

            input.requestFocus()

            return
        }

        if (
            AzimiAuth.isProtectedCredential(
                message
            )
        ) {

            addAIMessage(
                "SECURITY",
                "Guardian blocked protected credential material."
            )

            input.setText("")

            aiStatus?.text =
                "SECURITY BLOCK"

            aiStatus?.setTextColor(
                red
            )

            return
        }

        val safeHistory =
            aiHistory
                .filter { item ->

                    item.content.isNotBlank() &&
                        (
                            item.role == "user" ||
                                item.role == "assistant"
                            ) &&
                        !AzimiAuth.isProtectedCredential(
                            item.content
                        )
                }
                .takeLast(12)
                .toList()

        val approvedMemory =
            AtlasMemoryStore.getMemory(
                this
            )

        addAIMessage(
            "YOU",
            message
        )

        input.setText("")

        aiStatus?.text =
            "ATLAS CORE · ROUTING..."

        aiStatus?.setTextColor(
            cyan
        )

        aiSendButton?.isEnabled =
            false

        AtlasGuardianBridge.process(
            context = this,
            message = message,
            history = safeHistory,
            approvedMemory = approvedMemory
        ) { result ->

            aiSendButton?.isEnabled =
                AtlasSession.isActive(
                    this
                )

            if (
                result.success
            ) {

                AtlasMemoryStore.remember(
                    this,
                    "user",
                    message
                )

                AtlasMemoryStore.remember(
                    this,
                    "assistant",
                    result.message
                )

                addAIMessage(
                    "ATLAS",
                    result.message
                )

                speakAtlas(
                    result.message
                )

                aiStatus?.text =
                    when (
                        result.status
                    ) {

                        "LOCAL_RESPONSE_READY" ->
                            "ATLAS · LOCAL READY"

                        "HYBRID_AI_RESPONSE_READY" ->
                            "ATLAS · HYBRID READY"

                        "HYBRID_LOCAL_FALLBACK" ->
                            "ATLAS · LOCAL FALLBACK"

                        "AI_RESPONSE_READY" ->
                            "ATLAS · ONLINE READY"

                        else ->
                            "ATLAS · READY"
                    }

                aiStatus?.setTextColor(
                    green
                )

            } else {

                addAIMessage(
                    "SECURITY",
                    result.message
                )

                aiStatus?.text =
                    when (
                        result.status
                    ) {

                        "AUTHENTICATION_REQUIRED" ->
                            "AUTHENTICATION REQUIRED"

                        "SECURITY_BLOCK" ->
                            "SECURITY BLOCK"

                        "POLICY_BLOCK" ->
                            "POLICY BLOCK"

                        "AI_ENGINE_ERROR" ->
                            "AI ENGINE ERROR"

                        "LOCAL_ENGINE_ERROR" ->
                            "LOCAL ENGINE ERROR"

                        "UNAVAILABLE" ->
                            "ATLAS UNAVAILABLE"

                        "RESTRICTED" ->
                            "ATLAS RESTRICTED"

                        else ->
                            "ATLAS · REQUEST FAILED"
                    }

                aiStatus?.setTextColor(
                    when (
                        result.status
                    ) {

                        "SECURITY_BLOCK",
                        "POLICY_BLOCK",
                        "RESTRICTED" ->
                            red

                        "AUTHENTICATION_REQUIRED" ->
                            amber

                        else ->
                            red
                    }
                )
            }
        }
    }

    // ============================================================
    // AI MESSAGE
    // ============================================================

    private fun addAIMessage(
        speaker: String,
        message: String
    ) {

        if (
            message.isBlank()
        ) {
            return
        }

        val safeMessage =
            if (
                AzimiAuth.isProtectedCredential(
                    message
                )
            ) {

                "[PROTECTED CONTENT BLOCKED]"

            } else {

                message
            }

        aiHistory.add(
            AzimiAiClient.ChatMessage(
                role =
                    when (speaker) {

                        "YOU" ->
                            "user"

                        "ATLAS" ->
                            "assistant"

                        else ->
                            "system"
                    },
                content =
                    safeMessage
            )
        )

        val container =
            aiConversation
                ?: return

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.background =
            rounded(
                panel,
                when (speaker) {

                    "YOU" ->
                        cyan

                    "ATLAS" ->
                        green

                    "SECURITY" ->
                        red

                    else ->
                        darkGray
                },
                1f,
                16f
            )

        card.setPadding(
            dp(12),
            dp(10),
            dp(12),
            dp(10)
        )

        val speakerView =
            text(
                speaker,
                9f,
                when (speaker) {

                    "YOU" ->
                        cyan

                    "ATLAS" ->
                        green

                    "SECURITY" ->
                        red

                    else ->
                        gray
                }
            )

        speakerView.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        speakerView.letterSpacing =
            0.12f

        val messageView =
            text(
                safeMessage,
                12f,
                white
            )

        messageView.setPadding(
            0,
            dp(4),
            0,
            0
        )

        applyLanguageDirection(
            messageView
        )

        card.addView(
            speakerView
        )

        card.addView(
            messageView
        )

        container.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin =
                    dp(8)
            }
        )

        container.post {

            val parent =
                container.parent

            if (
                parent is ScrollView
            ) {

                parent.post {

                    parent.fullScroll(
                        View.FOCUS_DOWN
                    )
                }
            }
        }
    }

    // ============================================================
    // VAULT SECURITY MESSAGES
    // ============================================================

    private fun showVaultAuthenticationUnavailable() {

        showVaultSecurityMessage(
            "AUTHENTICATION UNAVAILABLE",
            "A secure Android device authentication method must be configured before Z Vault can be opened."
        )
    }

    private fun showVaultSecurityMessage(
        title: String,
        message: String
    ) {

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    // ============================================================
    // INCOMING AUTHENTICATION
    // ============================================================

    private fun isAzimiAuthCallback(
        uri: android.net.Uri?
    ): Boolean {

        if (
            uri == null
        ) {
            return false
        }

        return uri.scheme ==
            "azimi" &&
            uri.host ==
            "auth-callback"
    }

    private fun handleIncomingAuthIntent(
        incomingIntent: Intent?
    ) {

        val uri =
            incomingIntent?.data
                ?: return

        if (
            !isAzimiAuthCallback(
                uri
            )
        ) {
            return
        }

        AzimiNetwork.handleCallback(
            this,
            uri
        ) { result ->

            if (
                result.success
            ) {

                showAI()

                addAIMessage(
                    "SYSTEM",
                    result.message
                )

                refreshAIAuthUI()

            } else {

                showAI()

                addAIMessage(
                    "SECURITY",
                    result.message
                )

                refreshAIAuthUI()
            }
        }
    }

    // ============================================================
    // GENERIC UI HELPERS
    // ============================================================

    private fun text(
        value: String,
        size: Float,
        color: Int
    ): TextView {

        val view =
            TextView(this)

        view.text =
            value

        view.textSize =
            size

        view.setTextColor(
            color
        )

        view.setIncludeFontPadding(
            true
        )

        applyLanguageDirection(
            view
        )

        return view
    }

    private fun infoCard(
        title: String,
        message: String
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.background =
            rounded(
                panel,
                darkGray,
                1f,
                radius
            )

        card.setPadding(
            dp(16),
            dp(15),
            dp(16),
            dp(15)
        )

        val titleView =
            text(
                title,
                10f,
                cyan
            )

        titleView.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        titleView.letterSpacing =
            0.12f

        val messageView =
            text(
                message,
                12f,
                softWhite
            )

        messageView.setPadding(
            0,
            dp(6),
            0,
            0
        )

        card.addView(
            titleView
        )

        card.addView(
            messageView
        )

        return card
    }

    private fun actionButton(
        label: String,
        accent: Int,
        action: () -> Unit
    ): Button {

        val button =
            Button(this)

        button.text =
            label

        button.setTextColor(
            white
        )

        button.textSize =
            11f

        button.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        button.background =
            rounded(
                panel2,
                accent,
                1.5f,
                18f
            )

        button.setPadding(
            dp(12),
            dp(8),
            dp(12),
            dp(8)
        )

        button.isAllCaps =
            false

        button.setOnClickListener {
            action()
        }

        return button
    }

    private fun backButton(): Button {

        return actionButton(
            tr(
                "← BACK",
                "← بازگشت"
            ),
            darkGray
        ) {
            showHome()
        }
    }

    private fun space(
        dpValue: Int
    ): View {

        return View(this).apply {

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(dpValue)
                )
        }
    }

    private fun rounded(
        fill: Int,
        stroke: Int,
        strokeWidth: Float,
        corner: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                fill
            )

            setStroke(
                dp(
                    strokeWidth.roundToInt()
                ),
                stroke
            )

            cornerRadius =
                corner *
                    resources
                        .displayMetrics
                        .density
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources
                    .displayMetrics
                    .density
            ).roundToInt()
    }

    // ============================================================
    // ACTIVITY CLEANUP
    // ============================================================

    override fun onDestroy() {

        stopAtlasVoice()

        atlasTts?.shutdown()

        atlasTts =
            null

        atlasTtsReady =
            false

        super.onDestroy()
    }
}
