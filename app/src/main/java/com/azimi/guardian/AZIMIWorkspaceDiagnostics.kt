package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Workspace Diagnostics
 *
 * Safe diagnostic coordinator for the AZIMI-owned workspace.
 *
 * Combines:
 *
 *     AZIMIWorkspaceManifest
 *     AZIMIWorkspaceIntegrity
 *     AZIMIWorkspaceState
 *
 * into one structured diagnostic result.
 *
 * Design rules:
 *
 * - Read-only.
 * - Never deletes data.
 * - Never migrates data.
 * - Never repairs automatically.
 * - Never modifies Z Continuity.
 * - Never stores secrets.
 * - Never requires an external provider.
 * - Diagnostic failure must never crash Guardian.
 */
object AZIMIWorkspaceDiagnostics {

    const val HEALTHY =
        "HEALTHY"

    const val NOT_INITIALIZED =
        "NOT_INITIALIZED"

    const val PARTIAL =
        "PARTIAL"

    const val MANIFEST_MISSING =
        "MANIFEST_MISSING"

    const val MANIFEST_INVALID =
        "MANIFEST_INVALID"

    const val STATE_MISSING =
        "STATE_MISSING"

    const val STATE_INVALID =
        "STATE_INVALID"

    const val IDENTITY_MISMATCH =
        "IDENTITY_MISMATCH"

    const val CHECK_FAILED =
        "CHECK_FAILED"

    /**
     * Complete diagnostic result.
     *
     * Only safe metadata is exposed.
     */
    data class Result(
        val status: String,
        val workspaceStatus: String,
        val manifestStatus: String,
        val stateStatus: String,
        val integrityStatus: String,
        val workspaceIdPresent: Boolean,
        val workspaceIdConsistent: Boolean,
        val directoryCount: Int,
        val existingDirectoryCount: Int,
        val missingDirectoryCount: Int,
        val lifecycle: String?,
        val operationalStatus: String?,
        val healthy: Boolean,
        val message: String
    )

    /**
     * Runs the complete workspace diagnostic.
     *
     * This function is intentionally read-only.
     */
    fun check(
        context: Context
    ): Result {

        return runCatching {

            val workspaceStatus =
                AZIMIWorkspace.status(
                    context
                )

            val integrity =
                AZIMIWorkspaceIntegrity.check(
                    context
                )

            val manifestStatus =
                AZIMIWorkspaceManifest.status(
                    context
                )

            val stateStatus =
                AZIMIWorkspaceState.status(
                    context
                )

            val workspaceId =
                AZIMIWorkspaceManifest.workspaceId(
                    context
                )

            val state =
                AZIMIWorkspaceState.read(
                    context
                )

            val stateWorkspaceId =
                state
                    ?.optString(
                        "workspaceId",
                        ""
                    )
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }

            val workspaceIdPresent =
                !workspaceId.isNullOrBlank()

            val workspaceIdConsistent =
                workspaceIdPresent &&
                    !stateWorkspaceId.isNullOrBlank() &&
                    workspaceId ==
                    stateWorkspaceId

            val lifecycle =
                state
                    ?.optString(
                        "lifecycle",
                        ""
                    )
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }

            val operationalStatus =
                state
                    ?.optString(
                        "operationalStatus",
                        ""
                    )
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }

            val finalStatus =
                determineStatus(
                    workspaceStatus =
                        workspaceStatus,
                    manifestStatus =
                        manifestStatus,
                    stateStatus =
                        stateStatus,
                    integrityStatus =
                        integrity.status,
                    workspaceIdPresent =
                        workspaceIdPresent,
                    workspaceIdConsistent =
                        workspaceIdConsistent
                )

            val healthy =
                finalStatus ==
                    HEALTHY

            Result(
                status =
                    finalStatus,
                workspaceStatus =
                    workspaceStatus,
                manifestStatus =
                    manifestStatus,
                stateStatus =
                    stateStatus,
                integrityStatus =
                    integrity.status,
                workspaceIdPresent =
                    workspaceIdPresent,
                workspaceIdConsistent =
                    workspaceIdConsistent,
                directoryCount =
                    integrity.requiredDirectoryCount,
                existingDirectoryCount =
                    integrity.existingDirectoryCount,
                missingDirectoryCount =
                    integrity.missingDirectoryCount,
                lifecycle =
                    lifecycle,
                operationalStatus =
                    operationalStatus,
                healthy =
                    healthy,
                message =
                    createMessage(
                        finalStatus
                    )
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace diagnostics failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            Result(
                status =
                    CHECK_FAILED,
                workspaceStatus =
                    "UNKNOWN",
                manifestStatus =
                    "UNKNOWN",
                stateStatus =
                    "UNKNOWN",
                integrityStatus =
                    "UNKNOWN",
                workspaceIdPresent =
                    false,
                workspaceIdConsistent =
                    false,
                directoryCount =
                    0,
                existingDirectoryCount =
                    0,
                missingDirectoryCount =
                    0,
                lifecycle =
                    null,
                operationalStatus =
                    null,
                healthy =
                    false,
                message =
                    "AZIMI Workspace diagnostics could not be completed."
            )
        }
    }

    /**
     * Returns true only when every required workspace component
     * is healthy and the workspace identity is consistent.
     */
    fun isHealthy(
        context: Context
    ): Boolean {

        return runCatching {

            check(context).healthy

        }.getOrDefault(false)
    }

    /**
     * Returns only the diagnostic status.
     */
    fun status(
        context: Context
    ): String {

        return runCatching {

            check(context).status

        }.getOrDefault(
            CHECK_FAILED
        )
    }

    /**
     * Returns the current workspace identity when diagnostics
     * can safely establish it.
     */
    fun workspaceId(
        context: Context
    ): String? {

        return runCatching {

            val result =
                check(context)

            if (
                result.workspaceIdPresent &&
                result.workspaceIdConsistent
            ) {
                AZIMIWorkspaceManifest.workspaceId(
                    context
                )
            } else {
                null
            }

        }.getOrNull()
    }

    /**
     * Returns a safe human-readable diagnostic report.
     *
     * No encrypted file contents are exposed.
     */
    fun summary(
        context: Context
    ): String {

        return runCatching {

            val result =
                check(context)

            buildString {

                append(
                    "AZIMI Workspace Diagnostics"
                )

                append("\nStatus: ")
                append(result.status)

                append("\nWorkspace: ")
                append(result.workspaceStatus)

                append("\nManifest: ")
                append(result.manifestStatus)

                append("\nState: ")
                append(result.stateStatus)

                append("\nIntegrity: ")
                append(result.integrityStatus)

                append("\nWorkspace ID: ")
                append(
                    when {
                        !result.workspaceIdPresent ->
                            "UNAVAILABLE"

                        !result.workspaceIdConsistent ->
                            "MISMATCH"

                        else ->
                            "CONSISTENT"
                    }
                )

                append("\nDirectories: ")
                append(
                    result.existingDirectoryCount
                )

                append("/")
                append(
                    result.directoryCount
                )

                append("\nMissing: ")
                append(
                    result.missingDirectoryCount
                )

                append("\nLifecycle: ")
                append(
                    result.lifecycle
                        ?: "UNKNOWN"
                )

                append("\nOperational Status: ")
                append(
                    result.operationalStatus
                        ?: "UNKNOWN"
                )

                append("\nHealthy: ")
                append(result.healthy)

                append("\nMessage: ")
                append(result.message)
            }

        }.getOrElse {

            "AZIMI Workspace Diagnostics: CHECK_FAILED"
        }
    }

    /**
     * Determines the final diagnostic state.
     *
     * The order is deliberate:
     *
     * 1. Workspace existence
     * 2. Structural integrity
     * 3. Manifest
     * 4. State
     * 5. Identity consistency
     */
    private fun determineStatus(
        workspaceStatus: String,
        manifestStatus: String,
        stateStatus: String,
        integrityStatus: String,
        workspaceIdPresent: Boolean,
        workspaceIdConsistent: Boolean
    ): String {

        if (
            workspaceStatus ==
            "NOT_INITIALIZED"
        ) {
            return NOT_INITIALIZED
        }

        if (
            integrityStatus ==
            AZIMIWorkspaceIntegrity.CHECK_FAILED
        ) {
            return CHECK_FAILED
        }

        if (
            workspaceStatus ==
            "PARTIAL" ||
            integrityStatus ==
            AZIMIWorkspaceIntegrity.WORKSPACE_PARTIAL
        ) {
            return PARTIAL
        }

        if (
            manifestStatus ==
            AZIMIWorkspaceManifest.statusDummyMissing()
        ) {
            return MANIFEST_MISSING
        }

        if (
            manifestStatus ==
            "INVALID" ||
            integrityStatus ==
            AZIMIWorkspaceIntegrity.MANIFEST_INVALID
        ) {
            return MANIFEST_INVALID
        }

        if (
            stateStatus ==
            "MISSING"
        ) {
            return STATE_MISSING
        }

        if (
            stateStatus ==
            "INVALID"
        ) {
            return STATE_INVALID
        }

        if (
            !workspaceIdPresent ||
            !workspaceIdConsistent
        ) {
            return IDENTITY_MISMATCH
        }

        if (
            !AZIMIWorkspaceManifest.validate(
                context = DummyContextHolder.context
            )
        ) {
            return MANIFEST_INVALID
        }

        return HEALTHY
    }

    /**
     * Creates a user-safe message for each diagnostic state.
     */
    private fun createMessage(
        status: String
    ): String {

        return when (status) {

            HEALTHY ->
                "AZIMI Workspace is healthy and internally consistent."

            NOT_INITIALIZED ->
                "AZIMI Workspace has not been initialized."

            PARTIAL ->
                "AZIMI Workspace structure is incomplete."

            MANIFEST_MISSING ->
                "AZIMI Workspace Manifest is missing."

            MANIFEST_INVALID ->
                "AZIMI Workspace Manifest is invalid."

            STATE_MISSING ->
                "AZIMI Workspace State is missing."

            STATE_INVALID ->
                "AZIMI Workspace State is invalid."

            IDENTITY_MISMATCH ->
                "AZIMI Workspace identity is inconsistent."

            else ->
                "AZIMI Workspace diagnostic check failed."
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

    /**
     * Internal placeholder used only to keep the diagnostic
     * coordinator independent from UI and application state.
     *
     * It is never intended to execute a real validation.
     */
    private object DummyContextHolder {
        lateinit var context: Context
    }
}
