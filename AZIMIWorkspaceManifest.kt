package com.azimi.guardian

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * AZIMI Workspace Manifest
 *
 * Self-describing identity and structural metadata for the
 * AZIMI-owned workspace.
 *
 * Location:
 *
 *     /filesDir/azimi_workspace/core/workspace.manifest
 *
 * Security:
 *
 * - Manifest is encrypted with VaultCrypto.
 * - No passwords are stored.
 * - No API keys are stored.
 * - No access or refresh tokens are stored.
 * - No recovery codes are stored.
 * - No private credentials are stored.
 * - No biometric information is stored.
 *
 * Architecture rule:
 *
 *     AZIMI Workspace
 *            │
 *            ▼
 *     Workspace Manifest
 *            │
 *            ├── identity
 *            ├── schema
 *            ├── architecture
 *            └── structural state
 *
 * The manifest describes the workspace.
 * It is not the source of truth for project history.
 *
 * ZContinuityStorage remains the source of truth for
 * continuity records until a deliberate migration is designed.
 */
object AZIMIWorkspaceManifest {

    private const val FILE_NAME =
        "workspace.manifest"

    private const val MANIFEST_SCHEMA_VERSION =
        1

    private const val ARCHITECTURE_VERSION =
        1

    private const val WORKSPACE_NAME =
        "AZIMI"

    private const val MAX_MANIFEST_SIZE =
        50_000

    private const val MAX_WORKSPACE_ID_LENGTH =
        100

    /**
     * Returns the manifest file.
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
     * Initializes the workspace manifest if it does not exist.
     *
     * Existing manifest data is preserved.
     */
    fun initialize(
        context: Context
    ): Boolean {

        return runCatching {

            if (!AZIMIWorkspace.initialize(context)) {
                return false
            }

            if (file(context).isFile) {
                return validate(context)
            }

            val now =
                System.currentTimeMillis()

            val workspaceId =
                generateWorkspaceId()

            val manifest =
                createManifest(
                    workspaceId = workspaceId,
                    createdAt = now,
                    updatedAt = now
                )

            writeEncrypted(
                context = context,
                manifest = manifest
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace Manifest initialization failed: " +
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
     * Checks whether the manifest exists.
     *
     * This does not create or modify anything.
     */
    fun exists(
        context: Context
    ): Boolean {

        return runCatching {

            file(context).isFile

        }.getOrDefault(false)
    }

    /**
     * Reads the decrypted manifest.
     *
     * Returns null when the manifest cannot safely be read.
     */
    fun read(
        context: Context
    ): JSONObject? {

        return runCatching {

            val manifestFile =
                file(context)

            if (!manifestFile.isFile) {
                return null
            }

            if (manifestFile.length() <= 0L) {
                return null
            }

            if (manifestFile.length() > MAX_MANIFEST_SIZE) {
                return null
            }

            val encrypted =
                manifestFile.readText(
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

            if (decrypted.length > MAX_MANIFEST_SIZE) {
                return null
            }

            JSONObject(
                decrypted
            )

        }.getOrNull()
    }

    /**
     * Validates the complete manifest structure.
     *
     * This performs structural validation only.
     */
    fun validate(
        context: Context
    ): Boolean {

        return runCatching {

            val manifest =
                read(context)
                    ?: return false

            validateManifest(
                manifest
            )

        }.getOrDefault(false)
    }

    /**
     * Returns the stable AZIMI workspace ID.
     *
     * Returns null when no valid manifest exists.
     */
    fun workspaceId(
        context: Context
    ): String? {

        return runCatching {

            val manifest =
                read(context)
                    ?: return null

            val value =
                manifest.optString(
                    "workspaceId",
                    ""
                ).trim()

            if (
                value.isBlank() ||
                value.length > MAX_WORKSPACE_ID_LENGTH
            ) {
                null
            } else {
                value
            }

        }.getOrNull()
    }

    /**
     * Returns the manifest schema version.
     */
    fun schemaVersion(
        context: Context
    ): Int? {

        return runCatching {

            val manifest =
                read(context)
                    ?: return null

            manifest.optInt(
                "schemaVersion",
                -1
            ).takeIf {
                it > 0
            }

        }.getOrNull()
    }

    /**
     * Returns the AZIMI architecture version.
     */
    fun architectureVersion(
        context: Context
    ): Int? {

        return runCatching {

            val manifest =
                read(context)
                    ?: return null

            manifest.optInt(
                "architectureVersion",
                -1
            ).takeIf {
                it > 0
            }

        }.getOrNull()
    }

    /**
     * Returns the manifest creation timestamp.
     */
    fun createdAt(
        context: Context
    ): Long? {

        return runCatching {

            val manifest =
                read(context)
                    ?: return null

            manifest.optLong(
                "createdAt",
                -1L
            ).takeIf {
                it > 0L
            }

        }.getOrNull()
    }

    /**
     * Returns the last manifest update timestamp.
     */
    fun updatedAt(
        context: Context
    ): Long? {

        return runCatching {

            val manifest =
                read(context)
                    ?: return null

            manifest.optLong(
                "updatedAt",
                -1L
            ).takeIf {
                it > 0L
            }

        }.getOrNull()
    }

    /**
     * Returns a compact manifest status.
     *
     * Possible values:
     *
     *     READY
     *     MISSING
     *     INVALID
     *     UNAVAILABLE
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
     * Creates the initial manifest structure.
     */
    private fun createManifest(
        workspaceId: String,
        createdAt: Long,
        updatedAt: Long
    ): JSONObject {

        return JSONObject().apply {

            put(
                "schemaVersion",
                MANIFEST_SCHEMA_VERSION
            )

            put(
                "workspaceName",
                WORKSPACE_NAME
            )

            put(
                "workspaceId",
                workspaceId
            )

            put(
                "architectureVersion",
                ARCHITECTURE_VERSION
            )

            put(
                "createdAt",
                createdAt
            )

            put(
                "updatedAt",
                updatedAt
            )

            put(
                "workspaceStatus",
                "ACTIVE"
            )

            put(
                "storageModel",
                "APP_PRIVATE_ENCRYPTED"
            )

            put(
                "continuitySource",
                "ZContinuityStorage"
            )

            put(
                "providerDependency",
                "NONE"
            )

            put(
                "requiredDirectories",
                requiredDirectoryNames()
            )
        }
    }

    /**
     * Writes the manifest using VaultCrypto.
     *
     * The temporary file prevents an incomplete write from becoming
     * the active manifest.
     */
    private fun writeEncrypted(
        context: Context,
        manifest: JSONObject
    ): Boolean {

        return runCatching {

            val serialized =
                manifest.toString()

            if (
                serialized.isBlank() ||
                serialized.length > MAX_MANIFEST_SIZE
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

            val manifestFile =
                file(context)

            val temporaryFile =
                File(
                    manifestFile.parentFile,
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
                    manifestFile
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
                        "AZIMI Workspace Manifest write failed: " +
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
     * Validates the manifest without modifying it.
     */
    private fun validateManifest(
        manifest: JSONObject
    ): Boolean {

        val schemaVersion =
            manifest.optInt(
                "schemaVersion",
                -1
            )

        if (
            schemaVersion != MANIFEST_SCHEMA_VERSION
        ) {
            return false
        }

        val workspaceName =
            manifest.optString(
                "workspaceName",
                ""
            )

        if (
            workspaceName != WORKSPACE_NAME
        ) {
            return false
        }

        val workspaceId =
            manifest.optString(
                "workspaceId",
                ""
            ).trim()

        if (
            workspaceId.isBlank() ||
            workspaceId.length > MAX_WORKSPACE_ID_LENGTH
        ) {
            return false
        }

        val architectureVersion =
            manifest.optInt(
                "architectureVersion",
                -1
            )

        if (
            architectureVersion <= 0
        ) {
            return false
        }

        val createdAt =
            manifest.optLong(
                "createdAt",
                -1L
            )

        if (
            createdAt <= 0L
        ) {
            return false
        }

        val updatedAt =
            manifest.optLong(
                "updatedAt",
                -1L
            )

        if (
            updatedAt <= 0L
        ) {
            return false
        }

        val storageModel =
            manifest.optString(
                "storageModel",
                ""
            )

        if (
            storageModel !=
            "APP_PRIVATE_ENCRYPTED"
        ) {
            return false
        }

        val continuitySource =
            manifest.optString(
                "continuitySource",
                ""
            )

        if (
            continuitySource !=
            "ZContinuityStorage"
        ) {
            return false
        }

        val providerDependency =
            manifest.optString(
                "providerDependency",
                ""
            )

        if (
            providerDependency !=
            "NONE"
        ) {
            return false
        }

        return true
    }

    /**
     * Generates a stable workspace identifier.
     *
     * This identifier is not a secret.
     */
    private fun generateWorkspaceId(): String {

        return "AZW-" +
            UUID.randomUUID()
                .toString()
                .uppercase()
    }

    /**
     * Returns the logical directory names expected by the workspace.
     */
    private fun requiredDirectoryNames(): org.json.JSONArray {

        return org.json.JSONArray().apply {

            put("core")
            put("guardian")
            put("atlas")
            put("storage")
            put("history")
            put("diagnostics")
            put("builds")
            put("backups")
            put("recovery")
            put("projects")
            put("config")
            put("providers")
        }
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
