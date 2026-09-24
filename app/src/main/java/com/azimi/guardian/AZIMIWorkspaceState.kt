package com.azimi.guardian

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * AZIMI Workspace State
 *
 * Persistent, encrypted operational state for the AZIMI-owned
 * workspace.
 *
 * Location:
 *
 *     /filesDir/azimi_workspace/core/workspace.state
 *
 * Purpose:
 *
 *     AZIMI Workspace
 *            │
 *            ├── Manifest  → identity and architecture
 *            ├── Integrity → structural health
 *            └── State     → operational state
 *
 * Design rules:
 *
 * - State is encrypted with VaultCrypto.
 * - No passwords.
 * - No API keys.
 * - No tokens.
 * - No recovery codes.
 * - No private credentials.
 * - No biometric information.
 * - Existing state is preserved unless explicitly updated.
 * - Writes use a temporary file before commit.
 * - State failures must never crash Guardian.
 * - Z Continuity remains untouched.
 * - External providers are not required.
 */
object AZIMIWorkspaceState {

    private const val FILE_NAME =
        "workspace.state"

    private const val MAX_STATE_SIZE =
        50_000

    private const val STATE_SCHEMA_VERSION =
        1

    private const val DEFAULT_LIFECYCLE =
        "ACTIVE"

    private const val DEFAULT_OPERATIONAL_STATUS =
        "READY"

    /**
     * Returns the encrypted state file.
     */
    fun file(
        context: Context
    ): File {

        return File(
            AZIMIWorkspace.core(context),
            FILE_NAME
        )
    }

    /**
     * Initializes workspace state if it does not already exist.
     *
     * Existing state is never overwritten automatically.
     */
    fun initialize(
        context: Context
    ): Boolean {

        return runCatching {

            if (!AZIMIWorkspace.initialize(context)) {
                return false
            }

            if (!AZIMIWorkspaceManifest.initialize(context)) {
                return false
            }

            if (file(context).isFile) {
                return validate(context)
            }

            val now =
                System.currentTimeMillis()

            val workspaceId =
                AZIMIWorkspaceManifest.workspaceId(
                    context
                ) ?: return false

            val state =
                createInitialState(
                    workspaceId = workspaceId,
                    now = now
                )

            writeEncrypted(
                context = context,
                state = state
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace State initialization failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            false
        }
    }

    /**
     * Returns true when the state file exists.
     *
     * This does not create anything.
     */
    fun exists(
        context: Context
    ): Boolean {

        return runCatching {

            file(context).isFile

        }.getOrDefault(false)
    }

    /**
     * Reads the decrypted workspace state.
     *
     * Returns null when the state cannot safely be read.
     */
    fun read(
        context: Context
    ): JSONObject? {

        return runCatching {

            val stateFile =
                file(context)

            if (!stateFile.isFile) {
                return null
            }

            if (stateFile.length() <= 0L) {
                return null
            }

            if (stateFile.length() > MAX_STATE_SIZE) {
                return null
            }

            val encrypted =
                stateFile.readText(
                    Charsets.UTF_8
                )

            if (encrypted.isBlank()) {
                return null
            }

            val decrypted =
                VaultCrypto.decrypt(
                    encrypted
                )

            if (decrypted.isBlank()) {
                return null
            }

            if (decrypted.length > MAX_STATE_SIZE) {
                return null
            }

            JSONObject(
                decrypted
            )

        }.getOrNull()
    }

    /**
     * Validates the state structure.
     *
     * This does not modify the state.
     */
    fun validate(
        context: Context
    ): Boolean {

        return runCatching {

            val state =
                read(context)
                    ?: return false

            validateState(
                context = context,
                state = state
            )

        }.getOrDefault(false)
    }

    /**
     * Returns a compact state status.
     */
    fun status(
        context: Context
    ): String {

        return runCatching {

            when {
                !exists(context) ->
                    "MISSING"

                validate(context) ->
                    "READY"

                else ->
                    "INVALID"
            }

        }.getOrDefault(
            "UNAVAILABLE"
        )
    }

    /**
     * Returns the workspace lifecycle state.
     */
    fun lifecycle(
        context: Context
    ): String? {

        return runCatching {

            read(context)
                ?.optString(
                    "lifecycle",
                    ""
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        }.getOrNull()
    }

    /**
     * Returns the operational state.
     */
    fun operationalStatus(
        context: Context
    ): String? {

        return runCatching {

            read(context)
                ?.optString(
                    "operationalStatus",
                    ""
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        }.getOrNull()
    }

    /**
     * Returns the last state update timestamp.
     */
    fun updatedAt(
        context: Context
    ): Long? {

        return runCatching {

            read(context)
                ?.optLong(
                    "updatedAt",
                    -1L
                )
                ?.takeIf {
                    it > 0L
                }

        }.getOrNull()
    }

    /**
     * Updates only the operational status.
     *
     * This is intentionally narrow so unrelated components cannot
     * arbitrarily rewrite workspace identity or architecture data.
     */
    fun setOperationalStatus(
        context: Context,
        status: String
    ): Boolean {

        return runCatching {

            val safeStatus =
                sanitizeValue(
                    status
                )

            if (safeStatus.isBlank()) {
                return false
            }

            val current =
                read(context)
                    ?: return false

            if (!validateState(context, current)) {
                return false
            }

            current.put(
                "operationalStatus",
                safeStatus
            )

            current.put(
                "updatedAt",
                System.currentTimeMillis()
            )

            writeEncrypted(
                context = context,
                state = current
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace State update failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            false
        }
    }

    /**
     * Creates the initial workspace state.
     */
    private fun createInitialState(
        workspaceId: String,
        now: Long
    ): JSONObject {

        return JSONObject().apply {

            put(
                "schemaVersion",
                STATE_SCHEMA_VERSION
            )

            put(
                "workspaceId",
                workspaceId
            )

            put(
                "lifecycle",
                DEFAULT_LIFECYCLE
            )

            put(
                "operationalStatus",
                DEFAULT_OPERATIONAL_STATUS
            )

            put(
                "createdAt",
                now
            )

            put(
                "updatedAt",
                now
            )
        }
    }

    /**
     * Writes encrypted state using a temporary file.
     */
    private fun writeEncrypted(
        context: Context,
        state: JSONObject
    ): Boolean {

        return runCatching {

            val serialized =
                state.toString()

            if (
                serialized.isBlank() ||
                serialized.length > MAX_STATE_SIZE
            ) {
                return false
            }

            val encrypted =
                VaultCrypto.encrypt(
                    serialized
                )

            if (encrypted.isBlank()) {
                return false
            }

            val stateFile =
                file(context)

            val temporaryFile =
                File(
                    stateFile.parentFile,
                    "$FILE_NAME.tmp"
                )

            temporaryFile.writeText(
                encrypted,
                Charsets.UTF_8
            )

            if (
                !temporaryFile.isFile ||
                temporaryFile.length() <= 0L
            ) {

                runCatching {
                    temporaryFile.delete()
                }

                return false
            }

            val committed =
                temporaryFile.renameTo(
                    stateFile
                )

            if (!committed) {

                runCatching {
                    temporaryFile.delete()
                }

                false

            } else {

                validate(context)
            }

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace State write failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            false
        }
    }

    /**
     * Validates state structure and workspace identity.
     */
    private fun validateState(
        context: Context,
        state: JSONObject
    ): Boolean {

        val schemaVersion =
            state.optInt(
                "schemaVersion",
                -1
            )

        if (
            schemaVersion !=
            STATE_SCHEMA_VERSION
        ) {
            return false
        }

        val workspaceId =
            state.optString(
                "workspaceId",
                ""
            ).trim()

        if (workspaceId.isBlank()) {
            return false
        }

        val manifestWorkspaceId =
            AZIMIWorkspaceManifest.workspaceId(
                context
            )

        if (
            manifestWorkspaceId.isNullOrBlank() ||
            manifestWorkspaceId != workspaceId
        ) {
            return false
        }

        val lifecycle =
            state.optString(
                "lifecycle",
                ""
            ).trim()

        if (lifecycle.isBlank()) {
            return false
        }

        val operationalStatus =
            state.optString(
                "operationalStatus",
                ""
            ).trim()

        if (operationalStatus.isBlank()) {
            return false
        }

        val createdAt =
            state.optLong(
                "createdAt",
                -1L
            )

        if (createdAt <= 0L) {
            return false
        }

        val updatedAt =
            state.optLong(
                "updatedAt",
                -1L
            )

        if (updatedAt <= 0L) {
            return false
        }

        return true
    }

    /**
     * Prevents uncontrolled state values.
     *
     * Workspace state is metadata, not a free-form data store.
     */
    private fun sanitizeValue(
        value: String
    ): String {

        return value
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
            .take(200)
    }

    /**
     * Failure handling must never become a new failure.
     */
    private fun safeRecordError(
        context: Context,
        message: String
    ) {

        runCatching {

            GuardianStorage.recordError(
                context,
                message
            )

        }
    }
}
