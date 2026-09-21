package com.azimi.guardian

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
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

    private val aiHistory =
        mutableListOf<AzimiAiClient.ChatMessage>()

    // ============================================================
    // ACTIVITY
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        GuardianDiagnosticsStartup.start(this)

        configureWindow()

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

            if (unlocked) {
                ZSecuritySession.startProtectedSession(
                    this,
                    ZSecurity.AuthenticationMethod.DEVICE_CREDENTIAL
                )
            }

            if (!unlocked) {

                originAuthenticationPending = false
                pendingVaultAction = null

                showVaultSecurityMessage(
                    "VAULT ERROR",
                    "Vault authentication succeeded but the protected Vault state could not be opened."
                )

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
                        "OWNER-APPROVED CONTEXT",
                        "Approved project context belongs to AZIMI. Secrets, credentials, recovery codes and private keys are never treated as ordinary AI memory."
                    )
                }

                "ARCHIVE" -> {
                    pendingVaultAction = null
                    showVaultSection(
                        "Z ARCHIVE",
                        "CONTINUITY STORAGE",
                        "A future continuity layer for approved AZIMI backups, versions and recoverable project state."
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

    // ============================================================
    // WINDOW
    // ============================================================

    private fun configureWindow() {

        window.setNavigationBarColor(bg)
        window.setStatusBarColor(bg)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false)

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

        scroll.isFillViewport = true

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

        if (ZLanguage.isDari(this)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                view.layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

            view.textDirection =
                View.TEXT_DIRECTION_RTL

        } else {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
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

        box.addView(eyebrowView)
        box.addView(titleView)
        box.addView(subtitleView)

        applyLanguageDirection(box)

        return box
    }

    // ============================================================
    // TOP IDENTITY RAIL
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

        center.addView(name)
        center.addView(sub)

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
            stateView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
    // SYSTEM STATUS CARD
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

        card.addView(title)
        card.addView(main)

        card.addView(
            description,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(7)
            }
        )

        return card
    }

    // ============================================================
    // CORE MODULE CARD
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

        card.isClickable = true
        card.isFocusable = true

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

        card.addView(top)

        card.addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        card.addView(
            descriptionView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
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

        root.addView(
            sectionLabel(
                tr(
                    "CORE STATUS",
                    "وضعیت هسته"
                )
            )
        )

        // FIXED: Context passed to GuardianStorage
        root.addView(
            statusPanel(
                "VAULT",
                GuardianStorage.getVaultStatus(this),
                purple
            )
        )

        root.addView(space(8))

        // FIXED: Context passed to GuardianStorage
        root.addView(
            statusPanel(
                "AI POLICY",
                GuardianStorage.getAIMemoryPolicy(this),
                green
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "OWNER AREA",
                "RESTRICTED",
                purple
            )
        )

        root.addView(space(18))

        root.addView(
            actionButton(
                tr(
                    "LANGUAGE · ",
                    "زبان · "
                ) + ZLanguage.languageName(this),
                cyan
            ) {
                ZLanguage.toggle(this)
                showHome()
            }
        )

        install(root)
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

        row.addView(right)

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
            )
                .getIntProperty(
                    BatteryManager.BATTERY_PROPERTY_CAPACITY
                )

        val stat =
            StatFs(
                Environment.getDataDirectory().path
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
            sectionLabel("DIAGNOSTICS")
        )

        root.addView(
            infoCard(
                "GUARDIAN STARTUP",
                "Startup diagnostics are recorded by GuardianDiagnosticsStartup."
            )
        )

        root.addView(space(8))

        // FIXED: Context passed to GuardianStorage
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

        // FIXED: Context passed to GuardianStorage
        val unlocked =
            GuardianStorage.getVaultStatus(this) ==
                "UNLOCKED"

        val state =
            if (unlocked) {
                "VAULT OPEN"
            } else {
                "VAULT SEALED"
            }

        val stateColor =
            if (unlocked) green else purple

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
                openProtectedVaultArea("MEMORY")
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
                openProtectedVaultArea("ARCHIVE")
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
                openProtectedVaultArea("SOVEREIGN")
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
                    "SEAL Z VAULT",
                    purple
                ) {

                    val locked =
                        GuardianStorage.lockVault(this)

                    if (locked) {
                        ZSecuritySession.clear(this)
                    }

                    showVault()
                }
            )

        } else {

            root.addView(
                actionButton(
                    "OPEN Z VAULT",
                    green
                ) {
                    pendingVaultAction = null
                    requestVaultAuthentication()
                }
            )
        }

        root.addView(space(10))

        // FIXED: Context passed to GuardianStorage
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
    // PROTECTED VAULT AREAS
    // ============================================================

    private fun openProtectedVaultArea(
        action: String
    ) {

        // FIXED: Context passed to GuardianStorage
        if (
            GuardianStorage.getVaultStatus(this) ==
            "UNLOCKED"
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
                    showSovereign()
            }

            return
        }

        pendingVaultAction =
            action

        requestVaultAuthentication()
    }

    private fun requestVaultAuthentication() {

        if (!VaultAuth.isDeviceSecure(this)) {

            showVaultAuthenticationUnavailable()

            return
        }

        VaultAuth.requestAuthentication(this)
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

        // FIXED: Context passed to GuardianStorage
        if (
            GuardianStorage.getVaultStatus(this) !=
            "UNLOCKED"
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

        root.addView(
            identityRail(
                "OWNER GATE",
                purple
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
                tr(
                    "● DEVICE AUTHENTICATION PASSED · OWNER VERIFICATION PENDING",
                    "● تأیید هویت دستگاه موفق بود · تأیید هویت مالک باقی مانده است"
                )
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
                "OWNER ONLY",
                purple
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
                "BIOMETRICS",
                "ANDROID SECURE APIs",
                green
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "SOVEREIGN",
                "RESTRICTED",
                amber
            )
        )

        root.addView(space(12))

        root.addView(
            infoCard(
                "IMPORTANT",
                "The current Guardian foundation uses secure device authentication. Fingerprint + face + voice owner verification remains a future controlled layer when securely supported."
            )
        )

        root.addView(space(20))

        root.addView(
            actionButton(
                "Z SOVEREIGN",
                amber
            ) {
                showSovereign()
            }
        )

        root.addView(space(10))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // Z SOVEREIGN
    // ============================================================

    private fun showSovereign() {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "OWNER ARCHITECTURE",
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

        root.addView(space(20))

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
    // ATLAS AI
    // ============================================================

    private fun showAI() {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "ATLAS READY",
                green
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
                "AUTHENTICATION REQUIRED",
                10f,
                amber
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
                bottomMargin = dp(10)
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

        root.addView(space(12))

        aiInput =
            EditText(this)

        aiInput?.hint =
            tr(
                "Ask Atlas...",
                "از Atlas بپرسید..."
            )

        aiInput?.setTextColor(white)
        aiInput?.setHintTextColor(gray)

        aiInput?.setSingleLine(false)

        aiInput?.minLines = 2
        aiInput?.maxLines = 5

        aiInput?.gravity =
            Gravity.TOP or Gravity.START

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

        root.addView(space(10))

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

        root.addView(space(8))

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

        root.addView(space(8))

        aiLogoutButton =
            actionButton(
                "END AI SESSION",
                red
            ) {

                // FIXED: AzimiAuth uses signOut(), not clearSession()
                AzimiAuth.signOut(this)

                // Clear the in-memory conversation when the
                // authenticated AI session ends.
                aiHistory.clear()

                aiConversation?.removeAllViews()

                refreshAIAuthUI()
            }

        root.addView(
            aiLogoutButton
        )

        root.addView(space(12))

        root.addView(
            infoCard(
                "GUARDIAN POLICY",
                "AI requests pass through GuardianAiBridge. Protected credential material is blocked before the network layer."
            )
        )

        root.addView(space(18))

        root.addView(
            backButton()
        )

        install(root)

        refreshAIAuthUI()
    }

    private fun refreshAIAuthUI() {

        val authenticated =
            AzimiAuth.hasSession(this)

        aiStatus?.text =
            if (authenticated) {
                "AUTHENTICATED · GUARDIAN AI GATE ACTIVE"
            } else {
                "AUTHENTICATION REQUIRED"
            }

        aiStatus?.setTextColor(
            if (authenticated) green else amber
        )

        aiInput?.isEnabled =
            authenticated

        aiSendButton?.isEnabled =
            authenticated

        aiLoginButton?.visibility =
            if (authenticated) View.GONE
            else View.VISIBLE

        aiLogoutButton?.visibility =
            if (authenticated) View.VISIBLE
            else View.GONE

        if (authenticated) {

            addAIMessage(
                "SYSTEM",
                "Atlas AI connected through Guardian."
            )
        }
    }

    private fun requestAIAuthentication() {

        val input =
            EditText(this)

        input.hint =
            "your@email.com"

        input.setTextColor(white)
        input.setHintTextColor(gray)

        input.setPadding(
            dp(14),
            dp(12),
            dp(14),
            dp(12)
        )

        input.background =
            rounded(
                panel,
                darkGray,
                1f,
                16f
            )

        val dialog =
            android.app.AlertDialog.Builder(this)
                .setTitle("AZIMI AI AUTHENTICATION")
                .setMessage(
                    "Enter your email to receive the secure magic link."
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

                if (email.isBlank()) {
                    input.error =
                        "Email required"
                    return@setOnClickListener
                }

                dialog.dismiss()

                AzimiNetwork.requestMagicLink(
                    this,
                    email
                ) { result ->

                    if (result.success) {

                        aiStatus?.text =
                            "MAGIC LINK SENT"

                        aiStatus?.setTextColor(
                            cyan
                        )

                    } else {

                        aiStatus?.text =
                            result.message

                        aiStatus?.setTextColor(
                            red
                        )
                    }
                }
            }
        }

        dialog.show()
    }

   private fun sendAIMessage() {

    val input =
        aiInput ?: return

    val message =
        input.text
            .toString()
            .trim()

    if (message.isBlank()) {
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

        return
    }

    if (!AzimiAuth.hasSession(this)) {

        addAIMessage(
            "SECURITY",
            "Authentication is required before Atlas Core can be used."
        )

        return
    }

    val safeHistory =
        aiHistory
            .filter {
                !AzimiAuth.isProtectedCredential(
                    it.content
                )
            }
            .toList()

    addAIMessage(
        "YOU",
        message
    )

    input.setText("")

    aiStatus?.text =
        "ATLAS CORE · ANALYZING..."

    aiStatus?.setTextColor(
        cyan
    )

    aiSendButton?.isEnabled =
        false

    AtlasGuardianBridge.process(
        this,
        message,
        safeHistory
    ) { result ->

        aiSendButton?.isEnabled =
            AzimiAuth.hasSession(this)

        if (result.success) {

            addAIMessage(
                "ATLAS",
                result.message
            )

            aiStatus?.text =
                "ATLAS CORE · PLAN READY"

            aiStatus?.setTextColor(
                green
            )

        } else {

            addAIMessage(
                "SECURITY",
                result.message
            )

            aiStatus?.text =
                when (result.status) {

                    "AUTHENTICATION_REQUIRED" ->
                        "AUTHENTICATION REQUIRED"

                    "SECURITY_BLOCK" ->
                        "SECURITY BLOCK"

                    "POLICY_BLOCK" ->
                        "POLICY BLOCK"

                    else ->
                        "ATLAS CORE · REQUEST BLOCKED"
                }

            aiStatus?.setTextColor(
                red
            )
        }
    }
} 

        val input =
            aiInput ?: return

        val message =
            input.text
                .toString()
                .trim()

        if (message.isBlank()) {
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

            return
        }

        if (!AzimiAuth.hasSession(this)) {

            addAIMessage(
                "SECURITY",
                "Authentication is required before Atlas AI can be used."
            )

            return
        }

        val safeHistory =
            aiHistory
                .filter {
                    !AzimiAuth.isProtectedCredential(
                        it.content
                    )
                }
                .toList()

        addAIMessage(
            "YOU",
            message
        )

        input.setText("")

        aiStatus?.text =
            "ATLAS PROCESSING..."

        aiStatus?.setTextColor(
            cyan
        )

        aiSendButton?.isEnabled =
            false

        GuardianAiBridge.ask(
            this,
            message,
            safeHistory
        ) { result ->

            aiSendButton?.isEnabled =
                AzimiAuth.hasSession(this)

            if (result.success) {

                addAIMessage(
                    "ATLAS",
                    result.message
                )

                aiStatus?.text =
                    "ATLAS READY · ${result.engine}"

                aiStatus?.setTextColor(
                    green
                )

            } else {

                addAIMessage(
                    "SECURITY",
                    result.message
                )

                aiStatus?.text =
                    "REQUEST BLOCKED / FAILED"

                aiStatus?.setTextColor(
                    red
                )
            }
        }
    }

    private fun addAIMessage(
        speaker: String,
        message: String
    ) {

        val conversation =
            aiConversation ?: return

        val color =
            when (speaker) {
                "YOU" -> cyan
                "ATLAS" -> green
                "SECURITY" -> red
                else -> gray
            }

        val bubble =
            LinearLayout(this)

        bubble.orientation =
            LinearLayout.VERTICAL

        bubble.background =
            rounded(
                panel,
                color,
                1f,
                16f
            )

        bubble.setPadding(
            dp(12),
            dp(10),
            dp(12),
            dp(10)
        )

        val label =
            text(
                speaker,
                9f,
                color
            )

        label.typeface =
            Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )

        label.letterSpacing =
            0.15f

        val body =
            text(
                message,
                12f,
                softWhite
            )

        bubble.addView(label)

        bubble.addView(
            body,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(5)
            }
        )

        conversation.addView(
            bubble,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        )

        if (
            speaker == "YOU" ||
            speaker == "ATLAS"
        ) {

            aiHistory.add(
                AzimiAiClient.ChatMessage(
                    role =
                        if (speaker == "YOU")
                            "user"
                        else
                            "assistant",
                    content = message
                )
            )
        }
    }

    // ============================================================
    // AUTH CALLBACK
    // ============================================================

    private fun handleIncomingAuthIntent(
        intent: Intent?
    ) {

        val uri =
            intent?.data
                ?: return

        if (
            uri.scheme != "azimi" ||
            uri.host != "auth-callback"
        ) {
            return
        }

        AzimiNetwork.handleCallback(
            this,
            uri
        ) { result ->

            if (result.success) {

                showAI()

                aiStatus?.text =
                    "AUTHENTICATED · ATLAS READY"

                aiStatus?.setTextColor(
                    green
                )

            } else {

                showAI()

                aiStatus?.text =
                    result.message

                aiStatus?.setTextColor(
                    red
                )
            }
        }
    }

    // ============================================================
    // SECURITY MESSAGES
    // ============================================================

    private fun showVaultAuthenticationUnavailable() {

        showVaultSecurityMessage(
            "AUTHENTICATION UNAVAILABLE",
            "A secure device authentication method is not currently available on this device."
        )
    }

    private fun showVaultSecurityMessage(
        title: String,
        message: String
    ) {

        val root =
            baseLayout()

        root.addView(
            identityRail(
                "SECURITY EVENT",
                red
            )
        )

        root.addView(
            header(
                "Z VAULT / SECURITY",
                title,
                "Guardian protected boundary"
            )
        )

        root.addView(
            infoCard(
                "EVENT",
                message
            )
        )

        root.addView(space(20))

        root.addView(
            backButton()
        )

        install(root)
    }

    // ============================================================
    // UI HELPERS
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

        view.includeFontPadding =
            true

        applyLanguageDirection(view)

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
                18f
            )

        card.setPadding(
            dp(15),
            dp(14),
            dp(15),
            dp(14)
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
            0.10f

        val messageView =
            text(
                message,
                12f,
                softWhite
            )

        card.addView(titleView)

        card.addView(
            messageView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
            }
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

        button.letterSpacing =
            0.06f

        button.isAllCaps =
            false

        button.background =
            rounded(
                panel2,
                accent,
                1.5f,
                17f
            )

        button.setPadding(
            dp(10),
            dp(8),
            dp(10),
            dp(8)
        )

        button.setOnClickListener {
            action()
        }

        return button
    }

    private fun backButton(): Button {

        return actionButton(
            tr(
                "← BACK TO AZIMI CORE",
                "→ بازگشت به هسته AZIMI"
            ),
            darkGray
        ) {
            showHome()
        }
    }

    private fun rounded(
        fill: Int,
        stroke: Int,
        strokeWidth: Float,
        corner: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(fill)

            setStroke(
                dp(strokeWidth),
                stroke
            )

            cornerRadius =
                dp(corner).toFloat()
        }
    }

    private fun space(
        height: Int
    ): View {

        return View(this).apply {

            layoutParams =
                LinearLayout.LayoutParams(
                    1,
                    dp(height)
                )
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    private fun dp(
        value: Float
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    // ============================================================
    // BACK
    // ============================================================

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        showHome()
    }
}
