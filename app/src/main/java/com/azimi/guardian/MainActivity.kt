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
                    ZLanguage.text(
                        this,
                        "Authentication succeeded, but AZIMI Vault could not be unlocked.",
                        "تأیید هویت موفق بود، اما زی Vault باز نشد."
                    )
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
                        ZLanguage.text(
                            this,
                            "Z MEMORY",
                            "زی Memory"
                        ),
                        ZLanguage.text(
                            this,
                            "APPROVED ATLAS MEMORY",
                            "حافظه تأییدشده اطلس"
                        ),
                        ZLanguage.text(
                            this,
                            "Only user-approved AZIMI context belongs here.\n\n" +
                                "Passwords, API keys, recovery codes and private credentials are never stored as Atlas memory.",
                            "فقط اطلاعات ازیمی که توسط کاربر تأیید شده باشد در این بخش قرار می‌گیرد.\n\n" +
                                "رمزهای عبور، کلیدهای API، کدهای بازیابی و اطلاعات محرمانه هرگز به‌عنوان حافظه اطلس ذخیره نمی‌شوند."
                        )
                    )
                }

                "ARCHIVE" -> {

                    pendingVaultAction = null

                    showVaultSection(
                        ZLanguage.text(
                            this,
                            "Z ARCHIVE",
                            "زی Archive"
                        ),
                        ZLanguage.text(
                            this,
                            "AZIMI PROJECT ARCHIVE",
                            "آرشیف پروژه ازیمی"
                        ),
                        ZLanguage.text(
                            this,
                            "A future owner-controlled space for project history, approved decisions, backups and portable AZIMI records.",
                            "یک فضای آینده تحت کنترول مالک برای تاریخچه پروژه، تصمیم‌های تأییدشده، نسخه‌های پشتیبان و سوابق قابل انتقال ازیمی."
                        )
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
                    ZLanguage.text(
                        this,
                        "● AUTHENTICATED · AZIMI AI READY",
                        "● تأیید هویت شد · هوش مصنوعی ازیمی آماده است"
                    ),
                    green
                )

                addAIMessage(
                    "SYSTEM",
                    result.message
                )

                refreshAIAuthUI()

            } else {

                updateAIStatus(
                    ZLanguage.text(
                        this,
                        "● AUTHENTICATION FAILED",
                        "● تأیید هویت ناموفق بود"
                    ),
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
            ZLanguage.text(
                this,
                "AZIMI CORE",
                "هسته اصلی ازیمی"
            ),
            ZLanguage.text(
                this,
                "GUARDIAN · PERSONAL SYSTEM",
                "گاردین · سیستم شخصی"
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● GUARDIAN ONLINE",
                    "● گاردین آنلاین"
                ),
                green
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "LANGUAGE · ${ZLanguage.getLanguage(this).name}",
                    "زبان · ${if (ZLanguage.isDari(this)) "دری" else "انگلیسی"}"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "SECURITY MODEL · UNIVERSAL POLICY",
                    "مدل امنیتی · پالیسی عمومی"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "OWNER TOOLS · RESTRICTED",
                    "ابزارهای مالک · محدود"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Z VAULT · ${GuardianStorage.getVaultStatus(this)}",
                    "زی Vault · ${if (GuardianStorage.getVaultStatus(this) == "UNLOCKED") "باز" else "قفل"}"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "AI POLICY · ${GuardianStorage.getAIMemoryPolicy(this)}",
                    "پالیسی هوش مصنوعی · ${GuardianStorage.getAIMemoryPolicy(this)}"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Z SHIELD · NOT CONFIGURED",
                    "زی Shield · تنظیم نشده"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Z CONNECT · AUTHORIZATION REQUIRED",
                    "زی Connect · اجازه دسترسی لازم است"
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "LANGUAGE · SWITCH LANGUAGE",
                    "زبان · تغییر زبان"
                )
            ) {

                ZLanguage.toggle(this)

                showHome()
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z CONTROL",
                    "زی Control"
                )
            ) {
                showControl()
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z VAULT",
                    "زی Vault"
                )
            ) {
                showVault()
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z RECOVERY",
                    "زی Recovery"
                )
            ) {
                showRecovery()
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z SHIELD",
                    "زی Shield"
                )
            ) {
                showShield()
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "AZIMI AI",
                    "هوش مصنوعی ازیمی"
                )
            ) {
                showAI()
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z LAB",
                    "زی Lab"
                )
            ) {
                showLab()
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z ORIGIN · OWNER AREA",
                    "زی Origin · بخش مالک"
                )
            ) {

                originAuthenticationPending = true

                if (!requestVaultAuthentication()) {
                    originAuthenticationPending = false
                    showVaultAuthenticationUnavailable()
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
            ZLanguage.text(
                this,
                "Z CONTROL",
                "زی Control"
            ),
            ZLanguage.text(
                this,
                "DEVICE CONTROL · READ ONLY",
                "کنترول دستگاه · فقط خواندنی"
            )
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
            info(
                ZLanguage.text(
                    this,
                    "BATTERY · $battery%",
                    "باتری · $battery%"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "STORAGE · ${total - free} GB USED / $total GB TOTAL",
                    "ذخیره‌سازی · ${total - free} گیگابایت استفاده‌شده / $total گیگابایت مجموع"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "ANDROID · ${Build.VERSION.RELEASE}",
                    "اندروید · ${Build.VERSION.RELEASE}"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "SDK · ${Build.VERSION.SDK_INT}",
                    "SDK · ${Build.VERSION.SDK_INT}"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "DEVICE · ${Build.MODEL}",
                    "دستگاه · ${Build.MODEL}"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "SECURITY · DEVICE AUTHENTICATION FOUNDATION",
                    "امنیت · بنیاد تأیید هویت دستگاه"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "DIAGNOSTICS · ${GuardianDiagnostics.getLastStatus(this)}",
                    "تشخیص خطا · ${GuardianDiagnostics.getLastStatus(this)}"
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO AZIMI CORE",
                    "← بازگشت به هسته ازیمی"
                )
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
            ZLanguage.text(
                this,
                "Z VAULT",
                "زی Vault"
            ),
            ZLanguage.text(
                this,
                "PRIVATE SYSTEM · OWNER CONTROLLED",
                "سیستم خصوصی · تحت کنترول مالک"
            )
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
                        ZLanguage.text(
                            this@MainActivity,
                            "VAULT OPEN",
                            "Vault باز است"
                        )
                    } else {
                        ZLanguage.text(
                            this@MainActivity,
                            "VAULT SEALED",
                            "Vault مهر و موم است"
                        )
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
                        ZLanguage.text(
                            this@MainActivity,
                            "Protected AZIMI spaces are available.",
                            "بخش‌های محافظت‌شده ازیمی قابل دسترسی هستند."
                        )
                    } else {
                        ZLanguage.text(
                            this@MainActivity,
                            "Authentication required before protected spaces can be opened.",
                            "پیش از بازکردن بخش‌های محافظت‌شده، تأیید هویت لازم است."
                        )
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
                    ZLanguage.text(
                        this,
                        "● Z VAULT · UNLOCKED",
                        "● زی Vault · باز است"
                    )
                } else {
                    ZLanguage.text(
                        this,
                        "● Z VAULT · LOCKED",
                        "● زی Vault · قفل است"
                    )
                },
                if (vaultUnlocked) {
                    green
                } else {
                    purple
                }
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "SECURITY LEVEL · PROTECTED",
                    "سطح امنیتی · محافظت‌شده"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "OWNER AREAS REQUIRE ADDITIONAL AUTHORIZATION.",
                    "بخش‌های مالک به اجازه اضافی نیاز دارند."
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z MEMORY\nApproved Atlas memory",
                    "زی Memory\nحافظه تأییدشده اطلس"
                )
            ) {
                openProtectedVaultArea("MEMORY")
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z ORIGIN\nOwner identity space",
                    "زی Origin\nفضای هویت مالک"
                )
            ) {

                originAuthenticationPending = true

                if (!vaultUnlocked) {

                    if (!requestVaultAuthentication()) {
                        originAuthenticationPending = false
                        showVaultAuthenticationUnavailable()
                    }

                } else {

                    showOrigin()
                }
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z ARCHIVE\nProjects · decisions · history",
                    "زی Archive\nپروژه‌ها · تصمیم‌ها · تاریخچه"
                )
            ) {
                openProtectedVaultArea("ARCHIVE")
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z RECOVERY\nBackup · restore · portability",
                    "زی Recovery\nنسخه پشتیبان · بازیابی · انتقال‌پذیری"
                )
            ) {
                showRecovery()
            }
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "Z SOVEREIGN\nOwnership · independence · control",
                    "زی Sovereign\nمالکیت · استقلال · کنترول"
                )
            ) {
                openProtectedVaultArea("SOVEREIGN")
            }
        )

        if (vaultUnlocked) {

            layout.addView(
                actionButton(
                    ZLanguage.text(
                        this,
                        "SEAL Z VAULT",
                        "بستن زی Vault"
                    )
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
                    ZLanguage.text(
                        this,
                        "AUTHENTICATE & OPEN",
                        "تأیید هویت و بازکردن"
                    )
                ) {

                    if (!requestVaultAuthentication()) {
                        showVaultAuthenticationUnavailable()
                    }
                }
            )
        }

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Guardian policy · ${GuardianStorage.getAIMemoryPolicy(this)}\n" +
                        "Android Keystore protected storage\n" +
                        "AI access · policy controlled",

                    "پالیسی گاردین · ${GuardianStorage.getAIMemoryPolicy(this)}\n" +
                        "ذخیره‌سازی محافظت‌شده توسط Android Keystore\n" +
                        "دسترسی هوش مصنوعی · تحت کنترول پالیسی"
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO AZIMI CORE",
                    "← بازگشت به هسته ازیمی"
                )
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
                        ZLanguage.text(
                            this,
                            "Z MEMORY",
                            "زی Memory"
                        ),
                        ZLanguage.text(
                            this,
                            "APPROVED ATLAS MEMORY",
                            "حافظه تأییدشده اطلس"
                        ),
                        ZLanguage.text(
                            this,
                            "Only user-approved AZIMI context belongs here.\n\n" +
                                "Passwords, API keys, recovery codes and private credentials are never stored as Atlas memory.",

                            "فقط اطلاعات ازیمی که توسط کاربر تأیید شده باشد در این بخش قرار می‌گیرد.\n\n" +
                                "رمزهای عبور، کلیدهای API، کدهای بازیابی و اطلاعات محرمانه هرگز به‌عنوان حافظه اطلس ذخیره نمی‌شوند."
                        )
                    )
                }

                "ARCHIVE" -> {

                    showVaultSection(
                        ZLanguage.text(
                            this,
                            "Z ARCHIVE",
                            "زی Archive"
                        ),
                        ZLanguage.text(
                            this,
                            "AZIMI PROJECT ARCHIVE",
                            "آرشیف پروژه ازیمی"
                        ),
                        ZLanguage.text(
                            this,
                            "A future owner-controlled space for project history, approved decisions, backups and portable AZIMI records.",

                            "یک فضای آینده تحت کنترول مالک برای تاریخچه پروژه، تصمیم‌های تأییدشده، نسخه‌های پشتیبان و سوابق قابل انتقال ازیمی."
                        )
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

        showVaultSecurityMessage(
            ZLanguage.text(
                this,
                "AZIMI cannot open protected Vault areas until this device has a secure authentication method configured.\n\n" +
                    "Configure a secure device lock such as PIN, password or pattern.",

                "ازیمی نمی‌تواند بخش‌های محافظت‌شده Vault را باز کند تا زمانی که یک روش امن تأیید هویت در این دستگاه تنظیم شود.\n\n" +
                    "یک قفل امن مانند PIN، رمز عبور یا الگو تنظیم کنید."
            )
        )
    }

    private fun showVaultSecurityMessage(
        message: String
    ) {

        val layout =
            baseLayout()

        header(
            layout,
            ZLanguage.text(
                this,
                "VAULT SECURITY",
                "امنیت Vault"
            ),
            ZLanguage.text(
                this,
                "AUTHENTICATION REQUIRED",
                "تأیید هویت لازم است"
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● SECURITY ACTION REQUIRED",
                    "● اقدام امنیتی لازم است"
                ),
                red
            )
        )

        layout.addView(
            info(message)
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Future AZIMI platform adapters can use the strongest secure authentication methods supported by each device.",
                    "آداپترهای آینده ازیمی می‌توانند از امن‌ترین روش‌های تأیید هویت پشتیبانی‌شده توسط هر دستگاه استفاده کنند."
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO Z VAULT",
                    "← بازگشت به زی Vault"
                )
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
                ZLanguage.text(
                    this,
                    "● Z VAULT · AUTHENTICATED",
                    "● زی Vault · تأیید هویت‌شده"
                ),
                green
            )
        )

        layout.addView(
            info(description)
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "ACCESS BOUNDARY\n" +
                        "Guardian authorization required\n\n" +
                        "AI BOUNDARY\n" +
                        "AI cannot directly access protected Vault storage.",

                    "مرز دسترسی\n" +
                        "اجازه گاردین لازم است\n\n" +
                        "مرز هوش مصنوعی\n" +
                        "هوش مصنوعی نمی‌تواند مستقیماً به ذخیره‌سازی محافظت‌شده Vault دسترسی داشته باشد."
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO Z VAULT",
                    "← بازگشت به زی Vault"
                )
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
            ZLanguage.text(
                this,
                "Z ORIGIN",
                "زی Origin"
            ),
            ZLanguage.text(
                this,
                "OWNER IDENTITY · SPECIAL ACCESS",
                "هویت مالک · دسترسی ویژه"
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● DEVICE AUTHENTICATION PASSED · OWNER VERIFICATION PENDING",
                    "● تأیید هویت دستگاه موفق بود · تأیید هویت مالک باقی مانده است"
                ),
                green
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Z Origin is the special owner-controlled area of AZIMI.\n\n" +
                        "This space is intentionally different from normal user security.",

                    "زی Origin بخش ویژه ازیمی تحت کنترول مالک است.\n\n" +
                        "این بخش عمداً با امنیت عادی کاربران متفاوت طراحی شده است."
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "OWNER-ONLY ARCHITECTURE\n\n" +
                        "• Owner identity\n" +
                        "• Main Kingdom\n" +
                        "• Sovereign controls\n" +
                        "• Special Voice Lock\n" +
                        "• Owner recovery authority\n" +
                        "• AZIMI core administration",

                    "معماری مخصوص مالک\n\n" +
                        "• هویت مالک\n" +
                        "• قلمرو اصلی\n" +
                        "• کنترول‌های مستقل\n" +
                        "• قفل صوتی ویژه\n" +
                        "• صلاحیت بازیابی مالک\n" +
                        "• مدیریت هسته ازیمی"
                )
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● SPECIAL OWNER TOOLS · RESTRICTED",
                    "● ابزارهای ویژه مالک · محدود"
                ),
                purple
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Current Android foundation uses secure device authentication. Future versions can add additional owner factors such as biometric and voice authentication when securely supported.",

                    "بنیاد فعلی اندروید از تأیید هویت امن دستگاه استفاده می‌کند. نسخه‌های آینده می‌توانند در صورت پشتیبانی امن، عوامل اضافی مانند بیومتریک و تأیید صوتی مالک را اضافه کنند."
                )
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
                ZLanguage.text(
                    this,
                    "← BACK TO Z VAULT",
                    "← بازگشت به زی Vault"
                )
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
            ZLanguage.text(
                this,
                "Z SOVEREIGN",
                "زی Sovereign"
            ),
            ZLanguage.text(
                this,
                "AZIMI OWNERSHIP · INDEPENDENCE",
                "مالکیت ازیمی · استقلال"
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● SOVEREIGN FOUNDATION",
                    "● بنیاد استقلال"
                ),
                purple
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "ACCESS LEVEL · SOVEREIGN",
                    "سطح دسترسی · مستقل و مالکیتی"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "AUTHORIZATION · OWNER AREA",
                    "اجازه دسترسی · بخش مخصوص مالک"
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Z Sovereign is the owner-control architecture of AZIMI.\n\n" +
                        "AZIMI is designed to remain portable and recoverable rather than permanently dependent on one cloud platform, database, deployment service or AI provider.",

                    "زی Sovereign معماری مالکیت و کنترول ازیمی است.\n\n" +
                        "ازیمی طوری طراحی می‌شود که قابل انتقال و بازیابی باشد و برای همیشه به یک پلتفرم ابری، دیتابیس، سرویس نشر یا ارائه‌دهنده هوش مصنوعی وابسته نماند."
                )
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "OWNERSHIP\n" +
                        "Source · identity · policies · approved memory\n\n" +
                        "PORTABILITY\n" +
                        "Exportable data · replaceable providers · migration\n\n" +
                        "RECOVERY\n" +
                        "Backups · restore procedures · repairability\n\n" +
                        "AI INDEPENDENCE\n" +
                        "External AI engines remain replaceable modules.",

                    "مالکیت\n" +
                        "سورس · هویت · پالیسی‌ها · حافظه تأییدشده\n\n" +
                        "قابل انتقال بودن\n" +
                        "داده قابل استخراج · ارائه‌دهندگان قابل تعویض · مهاجرت\n\n" +
                        "بازیابی\n" +
                        "نسخه‌های پشتیبان · روش‌های بازگردانی · قابلیت ترمیم\n\n" +
                        "استقلال هوش مصنوعی\n" +
                        "موتورهای خارجی هوش مصنوعی همچنان ماژول‌های قابل تعویض هستند."
                )
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● OWNER CONTROL · ENABLED BY ARCHITECTURE",
                    "● کنترول مالک · فعال در معماری"
                ),
                green
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO Z ORIGIN",
                    "← بازگشت به زی Origin"
                )
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
            ZLanguage.text(
                this,
                "Z RECOVERY",
                "زی Recovery"
            ),
            ZLanguage.text(
                this,
                "SAFE RECOVERY CENTER",
                "مرکز امن بازیابی"
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● RECOVERY FOUNDATION READY",
                    "● بنیاد بازیابی آماده است"
                ),
                green
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Recovery operations require explicit user action. No destructive operation is performed automatically.",

                    "عملیات بازیابی به اقدام واضح کاربر نیاز دارد. هیچ عملیات مخربی به‌صورت خودکار اجرا نمی‌شود."
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO AZIMI CORE",
                    "← بازگشت به هسته ازیمی"
                )
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
            ZLanguage.text(
                this,
                "Z SHIELD",
                "زی Shield"
            ),
            ZLanguage.text(
                this,
                "SECURITY FOUNDATION",
                "بنیاد امنیت"
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● Z SHIELD · NOT CONFIGURED",
                    "● زی Shield · تنظیم نشده"
                ),
                red
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Security controls will only operate within permissions explicitly granted to Guardian.",

                    "کنترول‌های امنیتی فقط در محدوده اجازه‌هایی اجرا می‌شوند که به‌صورت واضح به گاردین داده شده‌اند."
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO AZIMI CORE",
                    "← بازگشت به هسته ازیمی"
                )
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
            ZLanguage.text(
                this,
                "AZIMI AI",
                "هوش مصنوعی ازیمی"
            ),
            ZLanguage.text(
                this,
                "PERSONAL INTELLIGENCE · AUTHENTICATED",
                "هوش شخصی · تأیید هویت‌شده"
            )
        )

        aiStatus =
            status(
                ZLanguage.text(
                    this,
                    "● CHECKING AUTHENTICATION...",
                    "● بررسی تأیید هویت..."
                ),
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
                    ZLanguage.text(
                        this@MainActivity,
                        "Ask AZIMI AI...",
                        "از هوش مصنوعی ازیمی بپرسید..."
                    )

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
            actionButton(
                ZLanguage.text(
                    this,
                    "SEND",
                    "ارسال"
                )
            ) {
                sendAIMessage()
            }

        sendButton.tag =
            "ai_send_button"

        layout.addView(sendButton)

        val loginButton =
            actionButton(
                ZLanguage.text(
                    this,
                    "SIGN IN WITH EMAIL",
                    "ورود با ایمیل"
                )
            ) {
                requestAIAuthentication()
            }

        loginButton.tag =
            "ai_login_button"

        layout.addView(loginButton)

        val logoutButton =
            actionButton(
                ZLanguage.text(
                    this,
                    "SIGN OUT",
                    "خروج"
                )
            ) {

                AzimiAuth.signOut(this)

                aiHistory.clear()

                updateAIStatus(
                    ZLanguage.text(
                        this,
                        "● AUTHENTICATION REQUIRED",
                        "● تأیید هویت لازم است"
                    ),
                    red
                )

                addAIMessage(
                    "SYSTEM",
                    ZLanguage.text(
                        this,
                        "Signed out successfully.",
                        "با موفقیت خارج شدید."
                    )
                )

                refreshAIAuthUI()
            }

        logoutButton.tag =
            "ai_logout_button"

        layout.addView(logoutButton)

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO AZIMI CORE",
                    "← بازگشت به هسته ازیمی"
                )
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
                ZLanguage.text(
                    this,
                    "● AUTHENTICATION REQUIRED",
                    "● تأیید هویت لازم است"
                ),
                red
            )

            aiInput?.hint =
                ZLanguage.text(
                    this,
                    "Sign in before using AZIMI AI",
                    "پیش از استفاده از هوش مصنوعی ازیمی وارد شوید"
                )

        } else {

            updateAIStatus(
                ZLanguage.text(
                    this,
                    "● AUTHENTICATED · AZIMI AI READY",
                    "● تأیید هویت شد · هوش مصنوعی ازیمی آماده است"
                ),
                green
            )

            aiInput?.hint =
                ZLanguage.text(
                    this,
                    "Ask AZIMI AI...",
                    "از هوش مصنوعی ازیمی بپرسید..."
                )
        }

        aiConversation?.let {

            if (
                it.childCount == 0 &&
                authenticated
            ) {

                addAIMessage(
                    "SYSTEM",
                    ZLanguage.text(
                        this,
                        "AZIMI AI authenticated. You may now send a message.",
                        "هوش مصنوعی ازیمی تأیید هویت شد. اکنون می‌توانید پیام ارسال کنید."
                    )
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
                    ZLanguage.text(
                        this,
                        "AZIMI AI SIGN IN",
                        "ورود به هوش مصنوعی ازیمی"
                    )
                )
                .setMessage(
                    ZLanguage.text(
                        this,
                        "Enter your email. AZIMI will request a secure magic link.",
                        "ایمیل خود را وارد کنید. ازیمی یک لینک امن ورود درخواست می‌کند."
                    )
                )
                .setView(input)
                .setNegativeButton(
                    ZLanguage.text(
                        this,
                        "CANCEL",
                        "لغو"
                    ),
                    null
                )
                .setPositiveButton(
                    ZLanguage.text(
                        this,
                        "SEND LINK",
                        "ارسال لینک"
                    ),
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
                        ZLanguage.text(
                            this,
                            "● EMAIL REQUIRED",
                            "● ایمیل لازم است"
                        ),
                        red
                    )

                    return@setOnClickListener
                }

                updateAIStatus(
                    ZLanguage.text(
                        this,
                        "● REQUESTING MAGIC LINK...",
                        "● درخواست لینک ورود..."
                    ),
                    purple
                )

                AzimiNetwork.requestMagicLink(
                    this,
                    email
                ) { result ->

                    if (result.success) {

                        updateAIStatus(
                            ZLanguage.text(
                                this,
                                "● CHECK YOUR EMAIL",
                                "● ایمیل خود را بررسی کنید"
                            ),
                            green
                        )

                        addAIMessage(
                            "SYSTEM",
                            result.message
                        )

                        dialog.dismiss()

                    } else {

                        updateAIStatus(
                            ZLanguage.text(
                                this,
                                "● AUTHENTICATION ERROR",
                                "● خطای تأیید هویت"
                            ),
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
                ZLanguage.text(
                    this,
                    "This message appears to contain protected credential material and was blocked before reaching AZIMI AI.",
                    "این پیام ممکن است شامل اطلاعات محرمانه باشد و پیش از رسیدن به هوش مصنوعی ازیمی مسدود شد."
                )
            )

            aiInput?.setText("")

            return
        }

        val session =
            AzimiAuth.getSession(this)

        if (session == null) {

            updateAIStatus(
                ZLanguage.text(
                    this,
                    "● AUTHENTICATION REQUIRED",
                    "● تأیید هویت لازم است"
                ),
                red
            )

            addAIMessage(
                "SYSTEM",
                ZLanguage.text(
                    this,
                    "Please authenticate before using AZIMI AI.",
                    "لطفاً پیش از استفاده از هوش مصنوعی ازیمی تأیید هویت شوید."
                )
            )

            return
        }

        /*
         * Capture previous history before adding
         * the current user message.
         *
         * This prevents duplicate current messages.
         */
        val safeHistory =
            aiHistory.toList()

        addAIMessage(
            "YOU",
            message
        )

        aiInput?.setText("")

        updateAIStatus(
            ZLanguage.text(
                this,
                "● AZIMI AI THINKING...",
                "● هوش مصنوعی ازیمی در حال پردازش..."
            ),
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
                    ZLanguage.text(
                        this,
                        "● AZIMI AI ONLINE · ${response.engine}",
                        "● هوش مصنوعی ازیمی آنلاین · ${response.engine}"
                    ),
                    green
                )

            } else {

                addAIMessage(
                    "AZIMI",
                    response.reply
                )

                updateAIStatus(
                    ZLanguage.text(
                        this,
                        "● AI REQUEST FAILED",
                        "● درخواست هوش مصنوعی ناموفق بود"
                    ),
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
            ZLanguage.text(
                this,
                "Z LAB",
                "زی Lab"
            ),
            ZLanguage.text(
                this,
                "AZIMI EXPERIMENTAL SPACE",
                "فضای آزمایشی ازیمی"
            )
        )

        layout.addView(
            status(
                ZLanguage.text(
                    this,
                    "● LAB FOUNDATION READY",
                    "● بنیاد Lab آماده است"
                ),
                purple
            )
        )

        layout.addView(
            info(
                ZLanguage.text(
                    this,
                    "Experimental AZIMI capabilities will be developed here without bypassing Guardian security boundaries.",

                    "قابلیت‌های آزمایشی ازیمی در این بخش توسعه می‌یابند، بدون عبور از مرزهای امنیتی گاردین."
                )
            )
        )

        layout.addView(
            actionButton(
                ZLanguage.text(
                    this,
                    "← BACK TO AZIMI CORE",
                    "← بازگشت به هسته ازیمی"
                )
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
