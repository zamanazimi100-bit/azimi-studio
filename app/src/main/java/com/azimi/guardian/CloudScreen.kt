package com.azimi.guardian

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import kotlin.math.roundToInt

/**
 * Z Cloud Screen
 *
 * UI layer only.
 *
 * Responsibilities:
 * - display local Z Cloud state
 * - enable / disable Z Cloud
 * - configure provider adapter identity
 * - display diagnostics
 *
 * Network operations are NOT performed here.
 * Actual cloud coordination belongs to ZCloud.
 */
class CloudScreen(
    private val activity: Activity
) {

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
    // ENTRY
    // ============================================================

    fun show() {

        val root =
            baseLayout()

        val status =
            ZCloud.getStatus(
                activity
            )

        val stateColor =
            when (status.state) {

                ZCloud.CloudState.READY ->
                    green

                ZCloud.CloudState.RECOVERY_READY ->
                    blue

                ZCloud.CloudState.BACKUP_REQUIRED,
                ZCloud.CloudState.SYNC_REQUIRED ->
                    amber

                ZCloud.CloudState.PROVIDER_UNAVAILABLE ->
                    amber

                ZCloud.CloudState.SECURITY_BLOCKED ->
                    red

                ZCloud.CloudState.DISABLED ->
                    darkGray
            }

        val stateText =
            when (status.state) {

                ZCloud.CloudState.READY ->
                    "READY"

                ZCloud.CloudState.RECOVERY_READY ->
                    "RECOVERY READY"

                ZCloud.CloudState.BACKUP_REQUIRED ->
                    "BACKUP REQUIRED"

                ZCloud.CloudState.SYNC_REQUIRED ->
                    "SYNC REQUIRED"

                ZCloud.CloudState.PROVIDER_UNAVAILABLE ->
                    "PROVIDER UNAVAILABLE"

                ZCloud.CloudState.SECURITY_BLOCKED ->
                    "SECURITY BLOCKED"

                ZCloud.CloudState.DISABLED ->
                    "DISABLED"
            }

        root.addView(
            identityRail(
                stateText,
                stateColor
            )
        )

        root.addView(
            header(
                "Z08 / CLOUD",
                "Z CLOUD",
                "ENCRYPTED CONTINUITY · PORTABILITY · RECOVERY"
            )
        )

        root.addView(
            infoCard(
                "Z CLOUD CORE",
                "Z Cloud coordinates encrypted backup, synchronization, recovery, integrity and provider portability. The core itself does not perform network operations."
            )
        )

        root.addView(space(10))

        // ========================================================
        // CURRENT STATE
        // ========================================================

        root.addView(
            sectionLabel(
                "CLOUD STATUS"
            )
        )

        root.addView(
            statusPanel(
                "STATE",
                stateText,
                stateColor
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "ENABLED",
                if (status.enabled) {
                    "YES"
                } else {
                    "NO"
                },
                if (status.enabled) {
                    green
                } else {
                    darkGray
                }
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "PROVIDER",
                if (status.providerId.isBlank()) {
                    "NONE"
                } else {
                    status.providerId
                },
                if (status.providerId.isBlank()) {
                    amber
                } else {
                    cyan
                }
            )
        )

        root.addView(space(8))

        root.addView(
            statusPanel(
                "VERSION",
                status.version,
                purple
            )
        )

        // ========================================================
        // ENABLE / DISABLE
        // ========================================================

        root.addView(
            sectionLabel(
                "CLOUD CONTROL"
            )
        )

        if (status.enabled) {

            root.addView(
                actionButton(
                    "DISABLE Z CLOUD",
                    red
                ) {

                    ZCloud.disable(
                        activity
                    )

                    show()
                }
            )

        } else {

            root.addView(
                actionButton(
                    "ENABLE Z CLOUD",
                    green
                ) {

                    ZCloud.enable(
                        activity
                    )

                    show()
                }
            )
        }

        root.addView(space(10))

        // ========================================================
        // PROVIDER
        // ========================================================

        root.addView(
            actionButton(
                "CONFIGURE PROVIDER",
                cyan
            ) {

                showProviderDialog()
            }
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "PROVIDER BOUNDARY",
                "A provider identifier selects an adapter. Provider credentials and network implementation do not belong in ZCloud.kt or this UI layer."
            )
        )

        root.addView(space(10))

        // ========================================================
        // BACKUP / SYNC / RECOVERY
        // ========================================================

        root.addView(
            sectionLabel(
                "CLOUD OPERATIONS"
            )
        )

        root.addView(
            actionButton(
                "BACKUP",
                blue
            ) {

                showOperationInfo(
                    "BACKUP",
                    "Backup execution is intentionally not performed by the UI. Z Cloud requires an encrypted payload and provider adapter before a real backup can be executed."
                )
            }
        )

        root.addView(space(8))

        root.addView(
            actionButton(
                "SYNC",
                cyan
            ) {

                showOperationInfo(
                    "SYNC",
                    "Synchronization will require an explicit encrypted data package and a configured provider adapter."
                )
            }
        )

        root.addView(space(8))

        root.addView(
            actionButton(
                "RECOVERY",
                amber
            ) {

                showOperationInfo(
                    "RECOVERY",
                    "Recovery remains an explicit operation. Z Cloud does not silently unlock Z Vault or overwrite protected local state."
                )
            }
        )

        root.addView(space(8))

        root.addView(
            actionButton(
                "INTEGRITY",
                purple
            ) {

                showOperationInfo(
                    "INTEGRITY",
                    "Z Cloud can calculate and verify SHA-256 integrity hashes for encrypted payloads without exposing their plaintext."
                )
            }
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "VAULT BOUNDARY",
                "Z Vault remains owner-controlled. Cloud operations must use encrypted payloads only. Z Cloud never silently unlocks the Vault."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "AI MEMORY",
                "AI memory remains SAFE_CONTEXT_ONLY. Passwords, API keys, recovery codes and private credentials are not ordinary AI memory."
            )
        )

        root.addView(space(10))

        root.addView(
            infoCard(
                "NETWORK",
                "NETWORK: NOT USED BY Z CLOUD CORE"
            )
        )

        // ========================================================
        // DIAGNOSTICS
        // ========================================================

        root.addView(
            sectionLabel(
                "DIAGNOSTICS"
            )
        )

        root.addView(
            infoCard(
                "Z CLOUD DIAGNOSTICS",
                ZCloud.diagnostics(
                    activity
                )
            )
        )

        root.addView(space(20))

        root.addView(
            actionButton(
                "← BACK",
                darkGray
            ) {

                if (
                    activity is MainActivity
                ) {
                    activity.showHomeFromCloud()
                } else {
                    activity.finish()
                }
            }
        )

        activity.setContentView(
            screen(root)
        )
    }

    // ============================================================
    // PROVIDER DIALOG
    // ============================================================

    private fun showProviderDialog() {

        val input =
            EditText(activity)

        input.hint =
            "Provider adapter ID"

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

        val current =
            ZCloud.getStatus(
                activity
            ).providerId

        if (current.isNotBlank()) {
            input.setText(current)
            input.setSelection(
                input.text.length
            )
        }

        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(
                    "Z CLOUD PROVIDER"
                )
                .setMessage(
                    "Enter the provider adapter identifier. This only records the adapter identity locally; it does not connect to the network."
                )
                .setView(
                    input
                )
                .setNegativeButton(
                    "CANCEL",
                    null
                )
                .setPositiveButton(
                    "SAVE",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val providerId =
                    input.text
                        .toString()
                        .trim()

                if (providerId.isBlank()) {

                    input.error =
                        "Provider ID is required."

                    return@setOnClickListener
                }

                ZCloud.setProvider(
                    activity,
                    providerId
                )

                dialog.dismiss()

                show()
            }
        }

        dialog.show()
    }

    // ============================================================
    // OPERATION INFORMATION
    // ============================================================

    private fun showOperationInfo(
        title: String,
        message: String
    ) {

        AlertDialog.Builder(activity)
            .setTitle(
                "Z CLOUD · $title"
            )
            .setMessage(
                message
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    // ============================================================
    // BASE LAYOUT
    // ============================================================

    private fun baseLayout(): LinearLayout {

        val root =
            LinearLayout(activity)

        root.orientation =
            LinearLayout.VERTICAL

        root.setBackgroundColor(
            bg
        )

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

        applyLanguageDirection(
            root
        )

        return root
    }

    private fun screen(
        content: LinearLayout
    ): ScrollView {

        val scroll =
            ScrollView(activity)

        scroll.setBackgroundColor(
            bg
        )

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

    // ============================================================
    // LANGUAGE
    // ============================================================

    private fun applyLanguageDirection(
        view: View
    ) {

        if (
            ZLanguage.isDari(activity)
        ) {

            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
            ) {

                view.layoutDirection =
                    View.LAYOUT_DIRECTION_RTL
            }

            view.textDirection =
                View.TEXT_DIRECTION_RTL

        } else {

            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
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
            activity,
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
            LinearLayout(activity)

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

        applyLanguageDirection(
            box
        )

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
            LinearLayout(activity)

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
            LinearLayout(activity)

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
    // STATUS PANEL
    // ============================================================

    private fun statusPanel(
        label: String,
        value: String,
        accent: Int
    ): LinearLayout {

        val row =
            LinearLayout(activity)

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
    // INFO CARD
    // ============================================================

    private fun infoCard(
        title: String,
        message: String
    ): LinearLayout {

        val card =
            LinearLayout(activity)

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

        applyLanguageDirection(
            messageView
        )

        card.addView(
            titleView
        )

        card.addView(
            messageView
        )

        return card
    }

    // ============================================================
    // ACTION BUTTON
    // ============================================================

    private fun actionButton(
        label: String,
        accent: Int,
        action: () -> Unit
    ): Button {

        val button =
            Button(activity)

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

    // ============================================================
    // GENERIC HELPERS
    // ============================================================

    private fun text(
        value: String,
        size: Float,
        color: Int
    ): TextView {

        val view =
            TextView(activity)

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

    private fun space(
        dpValue: Int
    ): View {

        return View(activity).apply {

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
                    activity.resources
                        .displayMetrics
                        .density
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                activity.resources
                    .displayMetrics
                    .density
            ).roundToInt()
    }
}
