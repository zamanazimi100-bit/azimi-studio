package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Workspace Controller
 *
 * Single safe coordination point for the AZIMI-owned workspace.
 *
 * Architecture:
 *
 *     AZIMI CORE
 *          │
 *          ▼
 *     Workspace Controller
 *          │
 *     ┌────┼───────────────┐
 *     ▼    ▼       ▼       ▼
 *  Workspace Manifest  State  Diagnostics
 *                              │
 *                              ▼
 *                            Health
 *
 * Design rules:
 *
 * - Keeps workspace coordination in one dedicated module.
 * - Does not replace the underlying workspace components.
 * - Does not delete data.
 * - Does not migrate data.
 * - Does not automatically repair corrupted data.
 * - Does not modify Z Continuity.
 * - Does not store secrets.
 * - Does not require an external provider.
 * - Every operation is failure-safe.
 * - Initialization is idempotent.
 */
object AZIMIWorkspaceController {

    const val READY =
        "READY"

    const val NOT_INITIALIZED =
        "NOT_INITIALIZED"

    const val DEGRADED =
        "DEGRADED"

    const val FAILED =
        "FAILED"

    /**
     * Controller result.
     *
     * Contains safe metadata only.
     */
    data class Result(
        val status: String,
        val initialized: Boolean,
        val complete: Boolean,
        val workspaceStatus: String,
        val diagnosticStatus: String,
        val healthStatus: String,
        val workspaceId: String?,
        val message: String
    )

    /**
     * Initializes the AZIMI workspace safely.
     *
     * This creates only the required workspace foundation
     * and manifest/state structures through their existing
     * dedicated components.
     *
     * Existing data is never deleted.
     */
    fun initialize(
        context: Context
    ): Result {

        return runCatching {

            val workspaceInitialized =
                AZIMIWorkspace.initialize(
                    context
                )

            if (!workspaceInitialized) {
                return@runCatching failureResult(
                    message =
                        "AZIMI Workspace initialization failed."
                )
            }

            val manifestInitialized =
                AZIMIWorkspaceManifest.initialize(
                    context
                )

            if (!manifestInitialized) {
                return@runCatching failureResult(
                    message =
                        "AZIMI Workspace Manifest initialization failed."
                )
            }

            val stateInitialized =
                AZIMIWorkspaceState.initialize(
                    context
                )

            if (!stateInitialized) {
                return@runCatching failureResult(
                    message =
                        "AZIMI Workspace State initialization failed."
                )
            }

            inspect(
                context
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace Controller initialization failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            failureResult(
                message =
                    "AZIMI Workspace Controller initialization failed safely."
            )
        }
    }

    /**
     * Inspects the current workspace without modifying it.
     */
    fun inspect(
        context: Context
    ): Result {

        return runCatching {

            val diagnostics =
                AZIMIWorkspaceDiagnostics.check(
                    context
                )

            val health =
                AZIMIWorkspaceHealth.check(
                    context
                )

            val initialized =
                AZIMIWorkspace.isInitialized(
                    context
                )

            val complete =
                AZIMIWorkspace.isComplete(
                    context
                )

            val workspaceId =
                if (
                    diagnostics.workspaceIdPresent &&
                    diagnostics.workspaceIdConsistent
                ) {
                    AZIMIWorkspaceManifest.workspaceId(
                        context
                    )
                } else {
                    null
                }

            val controllerStatus =
                determineControllerStatus(
                    diagnosticsStatus =
                        diagnostics.status,
                    healthStatus =
                        health.status,
                    initialized =
                        initialized,
                    complete =
                        complete
                )

            Result(
                status =
                    controllerStatus,
                initialized =
                    initialized,
                complete =
                    complete,
                workspaceStatus =
                    diagnostics.workspaceStatus,
                diagnosticStatus =
                    diagnostics.status,
                healthStatus =
                    health.status,
                workspaceId =
                    workspaceId,
                message =
                    createMessage(
                        controllerStatus
                    )
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace Controller inspection failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            failureResult(
                message =
                    "AZIMI Workspace inspection failed safely."
            )
        }
    }

    /**
     * Returns the current workspace health through the
     * centralized controller.
     */
    fun health(
        context: Context
    ): AZIMIWorkspaceHealth.Snapshot {

        return runCatching {

            AZIMIWorkspaceHealth.check(
                context
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace Controller health request failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            AZIMIWorkspaceHealth.Snapshot(
                status =
                    AZIMIWorkspaceHealth.CHECK_FAILED,
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
                    "AZIMI Workspace health request failed safely."
            )
        }
    }

    /**
     * Returns only the controller status.
     */
    fun status(
        context: Context
    ): String {

        return runCatching {

            inspect(
                context
            ).status

        }.getOrDefault(
            FAILED
        )
    }

    /**
     * Returns the workspace identity only when the
     * workspace has a consistent identity.
     */
    fun workspaceId(
        context: Context
    ): String? {

        return runCatching {

            inspect(
                context
            ).workspaceId

        }.getOrNull()
    }

    /**
     * Returns a concise controller report.
     *
     * No encrypted file contents are exposed.
     */
    fun summary(
        context: Context
    ): String {

        return runCatching {

            val result =
                inspect(
                    context
                )

            buildString {

                append(
                    "AZIMI Workspace Controller"
                )

                append("\nStatus: ")
                append(result.status)

                append("\nInitialized: ")
                append(result.initialized)

                append("\nComplete: ")
                append(result.complete)

                append("\nWorkspace: ")
                append(result.workspaceStatus)

                append("\nDiagnostics: ")
                append(result.diagnosticStatus)

                append("\nHealth: ")
                append(result.healthStatus)

                append("\nWorkspace ID: ")
                append(
                    result.workspaceId
                        ?: "UNAVAILABLE"
                )

                append("\nMessage: ")
                append(result.message)
            }

        }.getOrElse {

            "AZIMI Workspace Controller: FAILED"
        }
    }

    /**
     * Determines the controller-level state.
     */
    private fun determineControllerStatus(
        diagnosticsStatus: String,
        healthStatus: String,
        initialized: Boolean,
        complete: Boolean
    ): String {

        if (!initialized) {
            return NOT_INITIALIZED
        }

        if (
            diagnosticsStatus ==
            AZIMIWorkspaceDiagnostics.CHECK_FAILED ||
            healthStatus ==
            AZIMIWorkspaceHealth.CHECK_FAILED
        ) {
            return FAILED
        }

        if (
            !complete ||
            healthStatus !=
            AZIMIWorkspaceHealth.HEALTHY ||
            diagnosticsStatus !=
            AZIMIWorkspaceDiagnostics.HEALTHY
        ) {
            return DEGRADED
        }

        return READY
    }

    /**
     * Creates a safe human-readable controller message.
     */
    private fun createMessage(
        status: String
    ): String {

        return when (status) {

            READY ->
                "AZIMI Workspace is ready."

            NOT_INITIALIZED ->
                "AZIMI Workspace has not been initialized."

            DEGRADED ->
                "AZIMI Workspace is available but requires attention."

            FAILED ->
                "AZIMI Workspace Controller could not complete the operation."

            else ->
                "AZIMI Workspace Controller status is unavailable."
        }
    }

    /**
     * Creates a safe failure result.
     */
    private fun failureResult(
        message: String
    ): Result {

        return Result(
            status =
                FAILED,
            initialized =
                false,
            complete =
                false,
            workspaceStatus =
                "UNKNOWN",
            diagnosticStatus =
                "UNKNOWN",
            healthStatus =
                AZIMIWorkspaceHealth.CHECK_FAILED,
            workspaceId =
                null,
            message =
                message
        )
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
