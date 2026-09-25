package com.azimi.guardian

import android.content.Context
import java.security.MessageDigest

/**
 * Z Cloud
 *
 * AZIMI's provider-independent cloud coordination layer.
 *
 * Z Cloud is NOT a cloud provider.
 * It is the AZIMI-controlled system that coordinates:
 *
 * - encrypted backup
 * - synchronization
 * - recovery
 * - integrity verification
 * - portability
 * - provider adapters
 *
 * Security principles:
 *
 * - Z Cloud never requires plaintext Vault secrets.
 * - Z Cloud never stores passwords, API keys, or recovery codes
 *   as AI memory.
 * - Z Vault remains owner-controlled.
 * - Cloud providers are replaceable.
 * - No cloud operation silently unlocks Z Vault.
 * - No network operation is performed by this core.
 *
 * This first version establishes the Z Cloud contract and
 * local state model. Provider/network adapters will be added
 * later without changing the core architecture.
 */
object ZCloud {

    const val VERSION = "1.0.0"

    private const val PREFS_NAME =
        "azimi_z_cloud"

    private const val KEY_ENABLED =
        "enabled"

    private const val KEY_PROVIDER =
        "provider"

    private const val KEY_LAST_BACKUP =
        "last_backup"

    private const val KEY_LAST_SYNC =
        "last_sync"

    private const val KEY_LAST_RECOVERY =
        "last_recovery"

    enum class CloudState {
        DISABLED,
        READY,
        BACKUP_REQUIRED,
        SYNC_REQUIRED,
        RECOVERY_READY,
        PROVIDER_UNAVAILABLE,
        SECURITY_BLOCKED
    }

    enum class Operation {
        BACKUP,
        SYNC,
        RECOVERY,
        INTEGRITY_CHECK
    }

    enum class DataClass {
        ATLAS_MEMORY,
        PROJECT_DATA,
        GUARDIAN_CONFIGURATION,
        VAULT_ENCRYPTED_BACKUP
    }

    data class CloudStatus(
        val enabled: Boolean,
        val state: CloudState,
        val providerId: String,
        val version: String,
        val lastBackup: Long,
        val lastSync: Long,
        val lastRecovery: Long
    )

    data class CloudRequest(
        val operation: Operation,
        val dataClass: DataClass,
        val encryptedPayload: ByteArray? = null,
        val integrityHash: String = ""
    )

    data class CloudResult(
        val success: Boolean,
        val operation: Operation,
        val message: String,
        val providerId: String = "",
        val integrityHash: String = "",
        val status: CloudState = CloudState.DISABLED
    )

    /**
     * Returns the current local Z Cloud state.
     *
     * This does not contact a cloud provider.
     */
    fun getStatus(
        context: Context
    ): CloudStatus {

        val appContext =
            context.applicationContext

        val prefs =
            appContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val enabled =
            prefs.getBoolean(
                KEY_ENABLED,
                false
            )

        val provider =
            prefs.getString(
                KEY_PROVIDER,
                ""
            ) ?: ""

        val lastBackup =
            prefs.getLong(
                KEY_LAST_BACKUP,
                0L
            )

        val lastSync =
            prefs.getLong(
                KEY_LAST_SYNC,
                0L
            )

        val lastRecovery =
            prefs.getLong(
                KEY_LAST_RECOVERY,
                0L
            )

        val state =
            when {
                !enabled ->
                    CloudState.DISABLED

                provider.isBlank() ->
                    CloudState.PROVIDER_UNAVAILABLE

                lastRecovery > 0L ->
                    CloudState.RECOVERY_READY

                lastBackup == 0L ->
                    CloudState.BACKUP_REQUIRED

                else ->
                    CloudState.READY
            }

        return CloudStatus(
            enabled = enabled,
            state = state,
            providerId = provider,
            version = VERSION,
            lastBackup = lastBackup,
            lastSync = lastSync,
            lastRecovery = lastRecovery
        )
    }

    /**
     * Enables Z Cloud locally.
     *
     * Enabling Z Cloud does NOT connect to the network.
     */
    fun enable(
        context: Context
    ): CloudStatus {

        val appContext =
            context.applicationContext

        appContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_ENABLED,
                true
            )
            .apply()

        return getStatus(appContext)
    }

    /**
     * Disables Z Cloud locally.
     *
     * Existing cloud data is not deleted by this operation.
     */
    fun disable(
        context: Context
    ): CloudStatus {

        val appContext =
            context.applicationContext

        appContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_ENABLED,
                false
            )
            .apply()

        return getStatus(appContext)
    }

    /**
     * Selects a cloud provider adapter.
     *
     * The provider itself is intentionally not implemented here.
     */
    fun setProvider(
        context: Context,
        providerId: String
    ): CloudStatus {

        val appContext =
            context.applicationContext

        val normalized =
            providerId.trim()

        if (normalized.isBlank()) {
            return getStatus(appContext)
        }

        appContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_PROVIDER,
                normalized
            )
            .apply()

        return getStatus(appContext)
    }

    /**
     * Creates an integrity hash for encrypted data.
     *
     * The payload remains encrypted.
     * The hash is only used to detect corruption or unexpected changes.
     */
    fun calculateIntegrityHash(
        encryptedPayload: ByteArray
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        val result =
            digest.digest(
                encryptedPayload
            )

        return result.joinToString("") {
            "%02x".format(it)
        }
    }

    /**
     * Validates an encrypted payload against its expected hash.
     */
    fun verifyIntegrity(
        encryptedPayload: ByteArray,
        expectedHash: String
    ): Boolean {

        if (encryptedPayload.isEmpty()) {
            return false
        }

        if (expectedHash.isBlank()) {
            return false
        }

        val actualHash =
            calculateIntegrityHash(
                encryptedPayload
            )

        return actualHash.equals(
            expectedHash.trim(),
            ignoreCase = true
        )
    }

    /**
     * Validates whether a cloud operation is allowed by
     * the current Guardian security boundary.
     *
     * This does not perform the operation.
     */
    fun validateRequest(
        context: Context,
        request: CloudRequest
    ): CloudResult {

        val appContext =
            context.applicationContext

        val status =
            getStatus(appContext)

        if (!status.enabled) {
            return CloudResult(
                success = false,
                operation = request.operation,
                message =
                    "Z Cloud is disabled.",
                status =
                    CloudState.DISABLED
            )
        }

        if (status.providerId.isBlank()) {
            return CloudResult(
                success = false,
                operation = request.operation,
                message =
                    "No Z Cloud provider adapter is configured.",
                status =
                    CloudState.PROVIDER_UNAVAILABLE
            )
        }

        if (
            request.dataClass ==
            DataClass.VAULT_ENCRYPTED_BACKUP
        ) {

            if (
                request.encryptedPayload == null ||
                request.encryptedPayload.isEmpty()
            ) {
                return CloudResult(
                    success = false,
                    operation = request.operation,
                    message =
                        "Vault cloud operation requires encrypted payload data.",
                    providerId =
                        status.providerId,
                    status =
                        CloudState.SECURITY_BLOCKED
                )
            }

            if (
                request.integrityHash.isBlank()
            ) {
                return CloudResult(
                    success = false,
                    operation = request.operation,
                    message =
                        "Vault cloud operation requires an integrity hash.",
                    providerId =
                        status.providerId,
                    status =
                        CloudState.SECURITY_BLOCKED
                )
            }

            if (
                !verifyIntegrity(
                    request.encryptedPayload,
                    request.integrityHash
                )
            ) {
                return CloudResult(
                    success = false,
                    operation = request.operation,
                    message =
                        "Encrypted Vault payload failed integrity verification.",
                    providerId =
                        status.providerId,
                    status =
                        CloudState.SECURITY_BLOCKED
                )
            }
        }

        return CloudResult(
            success = true,
            operation = request.operation,
            message =
                "Z Cloud request passed local security validation.",
            providerId =
                status.providerId,
            integrityHash =
                request.integrityHash,
            status =
                CloudState.READY
        )
    }

    /**
     * Records a successful local cloud lifecycle event.
     *
     * This is state bookkeeping only.
     * It does not perform network operations.
     */
    fun recordSuccess(
        context: Context,
        operation: Operation
    ): CloudStatus {

        val appContext =
            context.applicationContext

        val now =
            System.currentTimeMillis()

        val editor =
            appContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()

        when (operation) {

            Operation.BACKUP ->
                editor.putLong(
                    KEY_LAST_BACKUP,
                    now
                )

            Operation.SYNC ->
                editor.putLong(
                    KEY_LAST_SYNC,
                    now
                )

            Operation.RECOVERY ->
                editor.putLong(
                    KEY_LAST_RECOVERY,
                    now
                )

            Operation.INTEGRITY_CHECK -> {
                // No lifecycle timestamp required.
            }
        }

        editor.apply()

        return getStatus(appContext)
    }

    /**
     * Human-readable diagnostic information.
     */
    fun diagnostics(
        context: Context
    ): String {

        val status =
            getStatus(
                context.applicationContext
            )

        return buildString {

            appendLine(
                "Z CLOUD"
            )

            appendLine(
                "VERSION: ${status.version}"
            )

            appendLine(
                "ENABLED: ${status.enabled}"
            )

            appendLine(
                "STATE: ${status.state}"
            )

            appendLine(
                "PROVIDER: ${
                    if (status.providerId.isBlank()) {
                        "NONE"
                    } else {
                        status.providerId
                    }
                }"
            )

            appendLine(
                "LAST BACKUP: ${status.lastBackup}"
            )

            appendLine(
                "LAST SYNC: ${status.lastSync}"
            )

            appendLine(
                "LAST RECOVERY: ${status.lastRecovery}"
            )

            appendLine(
                "NETWORK: NOT USED BY CORE"
            )

            appendLine(
                "VAULT: ENCRYPTED PAYLOADS ONLY"
            )

            appendLine(
                "AI MEMORY: OWNER-APPROVED CONTEXT ONLY"
            )

            appendLine(
                "PROVIDER LOCK-IN: NOT REQUIRED"
            )
        }
    }
}
