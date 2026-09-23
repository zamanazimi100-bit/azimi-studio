package com.azimi.guardian

import android.content.Context

/**
 * AZIMI — Z Vault Authorization
 *
 * Z Vault is a protected security boundary.
 *
 * ACCESS MODEL
 *
 *     Android strong biometric
 *             +
 *     Voice Lock
 *             +
 *     Guardian owner authority
 *             =
 *     FULL Z VAULT AUTHORIZATION
 *
 * Partial authentication NEVER opens the Vault.
 *
 * IMPORTANT:
 *
 * This class does not implement or fake Voice Lock.
 * It only consumes the verified authorization state produced
 * by the real authentication system.
 *
 * Until Voice Lock is genuinely implemented and verified:
 *
 *     biometric = verified
 *     voice     = not verified
 *     Vault     = LOCKED
 *
 * Z VAULT PRINCIPLES
 *
 * - No external device is trusted automatically.
 * - No cloud provider can unlock the Vault.
 * - Atlas cannot bypass the Vault.
 * - Remote authentication cannot become local owner authority.
 * - Partial authentication cannot become full authorization.
 * - Recovery is an owner-only operation.
 * - Backup is an owner-authorized operation.
 * - Vault data must remain encrypted outside the protected
 *   Vault boundary.
 */
object ZVaultAuthorization {

    enum class VaultState {
        LOCKED,
        BIOMETRIC_VERIFIED,
        VOICE_REQUIRED,
        FULL_AUTHORIZED,
        RECOVERY_AUTHORIZED,
        BACKUP_AUTHORIZED
    }

    enum class Operation {
        OPEN_VAULT,
        READ_PROTECTED_DATA,
        WRITE_PROTECTED_DATA,
        DELETE_PROTECTED_DATA,
        BACKUP,
        RECOVERY,
        EXPORT,
        IMPORT,
        RESTORE,
        SECURITY_CONFIGURATION
    }

    data class AuthorizationStatus(
        val state: VaultState,
        val allowed: Boolean,
        val biometricVerified: Boolean,
        val voiceVerified: Boolean,
        val ownerAuthorized: Boolean,
        val message: String
    )

    data class AccessDecision(
        val allowed: Boolean,
        val operation: Operation,
        val state: VaultState,
        val reason: String
    )

    /**
     * Return the current Z Vault authorization state.
     *
     * The Vault is considered fully authorized only when
     * AtlasOwnerAuthority reports that ALL required factors
     * have been verified.
     */
    fun getStatus(
        context: Context
    ): AuthorizationStatus {

        val appContext =
            context.applicationContext

        val authority =
            AtlasOwnerAuthority.getState(
                appContext
            )

        val factors =
            AtlasOwnerAuthority.getFactorState(
                appContext
            )

        val ownerAuthorized =
            authority.ownerAuthorized

        val state =
            when {

                ownerAuthorized &&
                    factors.allRequiredFactorsVerified ->
                    VaultState.FULL_AUTHORIZED

                factors.biometricVerified &&
                    !factors.voiceVerified ->
                    VaultState.VOICE_REQUIRED

                factors.biometricVerified ->
                    VaultState.BIOMETRIC_VERIFIED

                else ->
                    VaultState.LOCKED
            }

        val allowed =
            state ==
                VaultState.FULL_AUTHORIZED

        val message =
            when (state) {

                VaultState.LOCKED ->
                    "Z Vault is locked. Complete all required owner authentication factors."

                VaultState.BIOMETRIC_VERIFIED ->
                    "Biometric factor verified. Z Vault remains locked until Voice Lock is verified."

                VaultState.VOICE_REQUIRED ->
                    "Voice Lock verification is required before Z Vault can open."

                VaultState.FULL_AUTHORIZED ->
                    "All required owner authentication factors are verified. Z Vault access is authorized."

                VaultState.RECOVERY_AUTHORIZED ->
                    "Z Vault recovery operation is authorized."

                VaultState.BACKUP_AUTHORIZED ->
                    "Z Vault backup operation is authorized."
            }

        return AuthorizationStatus(
            state =
                state,

            allowed =
                allowed,

            biometricVerified =
                factors.biometricVerified,

            voiceVerified =
                factors.voiceVerified,

            ownerAuthorized =
                ownerAuthorized,

            message =
                message
        )
    }

    /**
     * Determine whether the Vault can currently be opened.
     */
    fun canOpen(
        context: Context
    ): Boolean {

        return getStatus(
            context
        ).state ==
            VaultState.FULL_AUTHORIZED
    }

    /**
     * Authorize a specific Vault operation.
     *
     * All protected Vault operations require FULL OWNER
     * AUTHORIZATION.
     */
    fun authorize(
        context: Context,
        operation: Operation
    ): AccessDecision {

        val status =
            getStatus(
                context
            )

        if (
            status.state !=
            VaultState.FULL_AUTHORIZED
        ) {

            return AccessDecision(
                allowed = false,

                operation =
                    operation,

                state =
                    status.state,

                reason =
                    when (status.state) {

                        VaultState.LOCKED ->
                            "Z Vault denied access: owner authentication has not been completed."

                        VaultState.BIOMETRIC_VERIFIED ->
                            "Z Vault denied access: Voice Lock is still required."

                        VaultState.VOICE_REQUIRED ->
                            "Z Vault denied access: all required owner factors are not verified."

                        else ->
                            "Z Vault denied access: full owner authorization is not active."
                    }
            )
        }

        /*
         * Extra owner-authority check.
         *
         * The Vault never relies only on local Vault state.
         */
        if (!status.ownerAuthorized) {

            return AccessDecision(
                allowed = false,

                operation =
                    operation,

                state =
                    status.state,

                reason =
                    "Z Vault denied access: active Guardian owner authority is unavailable."
            )
        }

        return AccessDecision(
            allowed = true,

            operation =
                operation,

            state =
                VaultState.FULL_AUTHORIZED,

            reason =
                "Z Vault operation authorized by the active AZIMI owner security session."
        )
    }

    /**
     * Open permission check.
     *
     * This does not expose Vault data.
     *
     * It only answers whether the protected Vault boundary
     * may be entered.
     */
    fun requestOpen(
        context: Context
    ): AccessDecision {

        return authorize(
            context,
            Operation.OPEN_VAULT
        )
    }

    /**
     * Protected-data read permission.
     */
    fun requestRead(
        context: Context
    ): AccessDecision {

        return authorize(
            context,
            Operation.READ_PROTECTED_DATA
        )
    }

    /**
     * Protected-data write permission.
     */
    fun requestWrite(
        context: Context
    ): AccessDecision {

        return authorize(
            context,
            Operation.WRITE_PROTECTED_DATA
        )
    }

    /**
     * Backup permission.
     *
     * Backup is owner-authorized because it concerns the
     * protected continuity of Z Vault.
     */
    fun requestBackup(
        context: Context
    ): AccessDecision {

        val decision =
            authorize(
                context,
                Operation.BACKUP
            )

        if (!decision.allowed) {
            return decision
        }

        return decision.copy(
            state =
                VaultState.BACKUP_AUTHORIZED,

            reason =
                "Z Vault backup is authorized. Only encrypted Vault payloads may leave the Vault boundary."
        )
    }

    /**
     * Recovery permission.
     *
     * Recovery NEVER bypasses owner authorization.
     *
     * A recovery package may be present without being
     * sufficient to unlock the Vault.
     */
    fun requestRecovery(
        context: Context
    ): AccessDecision {

        val decision =
            authorize(
                context,
                Operation.RECOVERY
            )

        if (!decision.allowed) {
            return decision
        }

        return decision.copy(
            state =
                VaultState.RECOVERY_AUTHORIZED,

            reason =
                "Z Vault recovery is authorized. Recovery data must still pass integrity and cryptographic validation."
        )
    }

    /**
     * Import permission.
     *
     * External data is never trusted merely because it comes
     * from Android, Apple, Windows, USB, cloud, or another
     * device.
     */
    fun requestImport(
        context: Context
    ): AccessDecision {

        val decision =
            authorize(
                context,
                Operation.IMPORT
            )

        if (!decision.allowed) {
            return decision
        }

        return decision.copy(
            reason =
                "Import is authorized only after owner authentication. Imported data must still pass quarantine, integrity, provenance, and security validation."
        )
    }

    /**
     * Export permission.
     *
     * Export remains owner-only.
     */
    fun requestExport(
        context: Context
    ): AccessDecision {

        return authorize(
            context,
            Operation.EXPORT
        )
    }

    /**
     * Security configuration permission.
     */
    fun requestSecurityConfiguration(
        context: Context
    ): AccessDecision {

        return authorize(
            context,
            Operation.SECURITY_CONFIGURATION
        )
    }

    /**
     * Determine whether an external source should be trusted.
     *
     * Current policy:
     *
     *     NEVER TRUST BY DEVICE TYPE.
     *
     * Android, Apple, Windows, Mac, Linux, USB, cloud
     * and other sources all begin as untrusted.
     */
    fun isExternalSourceTrusted(
        source: String
    ): Boolean {

        /*
         * No external source receives implicit Vault trust.
         *
         * Future trusted-device enrollment must be a separate
         * cryptographic authorization system.
         */
        return false
    }

    /**
     * Security policy description.
     */
    fun securityPolicy(): Map<String, String> {

        return mapOf(
            "vault_boundary" to
                "PROTECTED",

            "default_external_trust" to
                "NONE",

            "android_trust" to
                "NONE",

            "apple_trust" to
                "NONE",

            "windows_trust" to
                "NONE",

            "cloud_trust" to
                "NONE",

            "atlas_direct_unlock" to
                "DENIED",

            "remote_unlock" to
                "DENIED",

            "partial_authentication_unlock" to
                "DENIED",

            "recovery_bypass" to
                "DENIED",

            "biometric_template_storage" to
                "ANDROID_CONTROLLED",

            "voice_template_storage" to
                "NOT_IMPLEMENTED",

            "backup_policy" to
                "ENCRYPTED_PAYLOAD_ONLY",

            "import_policy" to
                "QUARANTINE_AND_VALIDATE",

            "export_policy" to
                "OWNER_AUTHORIZATION_REQUIRED"
        )
    }

    /**
     * Human-readable diagnostics.
     */
    fun diagnostics(
        context: Context
    ): String {

        val status =
            getStatus(
                context
            )

        return buildString {

            appendLine(
                "Z VAULT AUTHORIZATION"
            )

            appendLine(
                "STATE: ${status.state}"
            )

            appendLine(
                "VAULT ACCESS ALLOWED: ${status.allowed}"
            )

            appendLine(
                "BIOMETRIC FACTOR: ${
                    status.biometricVerified
                }"
            )

            appendLine(
                "VOICE FACTOR: ${
                    status.voiceVerified
                }"
            )

            appendLine(
                "OWNER AUTHORITY: ${
                    status.ownerAuthorized
                }"
            )

            appendLine()

            appendLine(
                "EXTERNAL DEVICE TRUST: NONE"
            )

            appendLine(
                "ANDROID TRUST: NONE"
            )

            appendLine(
                "APPLE TRUST: NONE"
            )

            appendLine(
                "WINDOWS TRUST: NONE"
            )

            appendLine(
                "CLOUD TRUST: NONE"
            )

            appendLine()

            appendLine(
                "ATLAS DIRECT UNLOCK: DENIED"
            )

            appendLine(
                "REMOTE UNLOCK: DENIED"
            )

            appendLine(
                "PARTIAL AUTH UNLOCK: DENIED"
            )

            appendLine(
                "RECOVERY BYPASS: DENIED"
            )

            appendLine()

            appendLine(
                "BACKUP: ENCRYPTED PAYLOAD ONLY"
            )

            appendLine(
                "IMPORT: QUARANTINE + VALIDATION"
            )

            appendLine(
                "EXPORT: OWNER AUTHORIZATION REQUIRED"
            )

            appendLine()

            appendLine(
                "STATUS: ${status.message}"
            )
        }
    }
}
