package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Workspace Health
 *
 * Safe health snapshot built from the existing AZIMI Workspace
 * diagnostic layer.
 *
 * Architecture:
 *
 *     AZIMIWorkspace
 *            │
 *            ├── Manifest
 *            ├── Integrity
 *            ├── State
 *            │
 *            ▼
 *     Workspace Diagnostics
 *            │
 *            ▼
 *     Workspace Health
 *
 * Design rules:
 *
 * - Read-only.
 * - Does not create or modify workspace data.
 * - Does not delete anything.
 * - Does not migrate anything.
 * - Does not modify Z Continuity.
 * - Does not store secrets.
 * - Does not require an external provider.
 * - Health failure must never crash Guardian.
 * - Uses existing diagnostic sources as the source of truth.
 */
object AZIMIWorkspaceHealth {

    const val HEALTHY =
        "HEALTHY"

    const val DEGRADED =
        "DEGRADED"

    const val UNAVAILABLE =
        "UNAVAILABLE"

    const val CHECK_FAILED =
        "CHECK_FAILED"

    /**
     * Safe workspace health snapshot.
     *
     * This contains metadata only.
     *
     * No encrypted file contents,
     * credentials, tokens, or secrets are exposed.
     */
    data class Snapshot(
        val status: String,
        val diagnosticStatus: String,
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
     * Creates a safe health snapshot.
     *
     * This function is intentionally read-only.
     */
    fun check(
        context: Context
    ): Snapshot {

        return runCatching {

            val diagnostics =
                AZIMIWorkspaceDiagnostics.check(
                    context
                )

            val healthStatus =
                determineHealthStatus(
                    diagnosticStatus =
                        diagnostics.status,
                    healthy =
                        diagnostics.healthy
                )

            Snapshot(
                status =
                    healthStatus,
                diagnosticStatus =
                    diagnostics.status,
                workspaceStatus =
                    diagnostics.workspaceStatus,
                manifestStatus =
                    diagnostics.manifestStatus,
                stateStatus =
                    diagnostics.stateStatus,
                integrityStatus =
                    diagnostics.integrityStatus,
                workspaceIdPresent =
                    diagnostics.workspaceIdPresent,
                workspaceIdConsistent =
                    diagnostics.workspaceIdConsistent,
                directoryCount =
                    diagnostics.directoryCount,
                existingDirectoryCount =
                    diagnostics.existingDirectoryCount,
                missingDirectoryCount =
                    diagnostics.missingDirectoryCount,
                lifecycle =
                    diagnostics.lifecycle,
                operationalStatus =
                    diagnostics.operationalStatus,
                healthy =
                    diagnostics.healthy,
                message =
                    createMessage(
                        healthStatus,
                        diagnostics.status
                    )
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace health check failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            Snapshot(
                status =
                    CHECK_FAILED,
                diagnosticStatus =
                    "UNKNOWN",
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
                    "AZIMI Workspace health could not be determined."
            )
        }
    }

    /**
     * Returns true only when the complete workspace diagnostic
     * reports a healthy and internally consistent workspace.
     */
    fun isHealthy(
        context: Context
    ): Boolean {

        return runCatching {

            check(context).status ==
                HEALTHY

        }.getOrDefault(false)
    }

    /**
     * Returns only the current health status.
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
     * Returns the complete safe health snapshot.
     */
    fun snapshot(
        context: Context
    ): Snapshot {

        return check(context)
    }

    /**
     * Returns a concise human-readable health report.
     *
     * Only safe metadata is exposed.
     */
    fun summary(
        context: Context
    ): String {

        return runCatching {

            val result =
                check(context)

            buildString {

                append(
                    "AZIMI Workspace Health"
                )

                append("\nStatus: ")
                append(result.status)

                append("\nDiagnostics: ")
                append(result.diagnosticStatus)

                append("\nWorkspace: ")
                append(result.workspaceStatus)

                append("\nManifest: ")
                append(result.manifestStatus)

                append("\nState: ")
                append(result.stateStatus)

                append("\nIntegrity: ")
                append(result.integrityStatus)

                append("\nIdentity Present: ")
                append(result.workspaceIdPresent)

                append("\nIdentity Consistent: ")
                append(result.workspaceIdConsistent)

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

            "AZIMI Workspace Health: CHECK_FAILED"
        }
    }

    /**
     * Converts the detailed diagnostic state into a smaller
     * health classification.
     *
     * HEALTHY:
     *     Everything required is valid and consistent.
     *
     * DEGRADED:
     *     The workspace exists but one or more required
     *     components are incomplete or invalid.
     *
     * UNAVAILABLE:
     *     The workspace has not been initialized.
     *
     * CHECK_FAILED:
     *     The health check itself could not complete safely.
     */
    private fun determineHealthStatus(
        diagnosticStatus: String,
        healthy: Boolean
    ): String {

        if (healthy &&
            diagnosticStatus ==
            AZIMIWorkspaceDiagnostics.HEALTHY
        ) {
            return HEALTHY
        }

        return when (diagnosticStatus) {

            AZIMIWorkspaceDiagnostics.NOT_INITIALIZED ->
                UNAVAILABLE

            AZIMIWorkspaceDiagnostics.CHECK_FAILED ->
                CHECK_FAILED

            else ->
                DEGRADED
        }
    }

    /**
     * Creates a safe human-readable health message.
     */
    private fun createMessage(
        healthStatus: String,
        diagnosticStatus: String
    ): String {

        return when (healthStatus) {

            HEALTHY ->
                "AZIMI Workspace is healthy and ready."

            UNAVAILABLE ->
                "AZIMI Workspace is not initialized."

            CHECK_FAILED ->
                "AZIMI Workspace health check failed safely."

            DEGRADED ->
                when (diagnosticStatus) {

                    AZIMIWorkspaceDiagnostics.PARTIAL ->
                        "AZIMI Workspace is partially available."

                    AZIMIWorkspaceDiagnostics.MANIFEST_MISSING ->
                        "AZIMI Workspace Manifest is unavailable."

                    AZIMIWorkspaceDiagnostics.MANIFEST_INVALID ->
                        "AZIMI Workspace Manifest requires attention."

                    AZIMIWorkspaceDiagnostics.STATE_MISSING ->
                        "AZIMI Workspace State is unavailable."

                    AZIMIWorkspaceDiagnostics.STATE_INVALID ->
                        "AZIMI Workspace State requires attention."

                    AZIMIWorkspaceDiagnostics.IDENTITY_MISMATCH ->
                        "AZIMI Workspace identity requires attention."

                    else ->
                        "AZIMI Workspace is available but degraded."
                }

            else ->
                "AZIMI Workspace health status is unavailable."
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
