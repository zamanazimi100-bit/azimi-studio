package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Workspace Readiness
 *
 * Read-only safety gate used by future AZIMI modules before
 * performing workspace-dependent operations.
 *
 * Design rules:
 *
 * - Read-only.
 * - Does not initialize or modify the workspace.
 * - Does not repair automatically.
 * - Does not delete anything.
 * - Does not migrate anything.
 * - Does not modify Z Continuity.
 * - Does not store secrets.
 * - Does not require external providers.
 * - Readiness failures must never crash Guardian.
 */
object AZIMIWorkspaceReadiness {

    const val READY =
        "READY"

    const val NOT_READY =
        "NOT_READY"

    const val NOT_INITIALIZED =
        "NOT_INITIALIZED"

    const val CHECK_FAILED =
        "CHECK_FAILED"

    /**
     * Safe readiness result.
     */
    data class Result(
        val status: String,
        val workspaceInitialized: Boolean,
        val workspaceComplete: Boolean,
        val manifestValid: Boolean,
        val stateValid: Boolean,
        val identityPresent: Boolean,
        val identityConsistent: Boolean,
        val healthStatus: String,
        val diagnosticStatus: String,
        val controllerStatus: String,
        val message: String
    )

    /**
     * Checks whether the workspace is ready for use.
     *
     * This method is intentionally read-only.
     */
    fun check(
        context: Context
    ): Result {

        return runCatching {

            val initialized =
                AZIMIWorkspace.isInitialized(
                    context
                )

            if (!initialized) {
                return@runCatching Result(
                    status =
                        NOT_INITIALIZED,
                    workspaceInitialized =
                        false,
                    workspaceComplete =
                        false,
                    manifestValid =
                        false,
                    stateValid =
                        false,
                    identityPresent =
                        false,
                    identityConsistent =
                        false,
                    healthStatus =
                        AZIMIWorkspaceHealth.UNAVAILABLE,
                    diagnosticStatus =
                        AZIMIWorkspaceDiagnostics.NOT_INITIALIZED,
                    controllerStatus =
                        AZIMIWorkspaceController.NOT_INITIALIZED,
                    message =
                        "AZIMI Workspace is not initialized."
                )
            }

            val complete =
                AZIMIWorkspace.isComplete(
                    context
                )

            val manifestValid =
                AZIMIWorkspaceManifest.validate(
                    context
                )

            val stateValid =
                AZIMIWorkspaceState.validate(
                    context
                )

            val diagnostics =
                AZIMIWorkspaceDiagnostics.check(
                    context
                )

            val health =
                AZIMIWorkspaceHealth.check(
                    context
                )

            val controller =
                AZIMIWorkspaceController.inspect(
                    context
                )

            val identityPresent =
                diagnostics.workspaceIdPresent

            val identityConsistent =
                diagnostics.workspaceIdConsistent

            val ready =
                complete &&
                    manifestValid &&
                    stateValid &&
                    identityPresent &&
                    identityConsistent &&
                    diagnostics.status ==
                    AZIMIWorkspaceDiagnostics.HEALTHY &&
                    health.status ==
                    AZIMIWorkspaceHealth.HEALTHY &&
                    controller.status ==
                    AZIMIWorkspaceController.READY

            Result(
                status =
                    if (ready) {
                        READY
                    } else {
                        NOT_READY
                    },
                workspaceInitialized =
                    initialized,
                workspaceComplete =
                    complete,
                manifestValid =
                    manifestValid,
                stateValid =
                    stateValid,
                identityPresent =
                    identityPresent,
                identityConsistent =
                    identityConsistent,
                healthStatus =
                    health.status,
                diagnosticStatus =
                    diagnostics.status,
                controllerStatus =
                    controller.status,
                message =
                    if (ready) {
                        "AZIMI Workspace is ready for authorized operations."
                    } else {
                        "AZIMI Workspace is not ready for authorized operations."
                    }
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace readiness check failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            Result(
                status =
                    CHECK_FAILED,
                workspaceInitialized =
                    false,
                workspaceComplete =
                    false,
                manifestValid =
                    false,
                stateValid =
                    false,
                identityPresent =
                    false,
                identityConsistent =
                    false,
                healthStatus =
                    AZIMIWorkspaceHealth.CHECK_FAILED,
                diagnosticStatus =
                    AZIMIWorkspaceDiagnostics.CHECK_FAILED,
                controllerStatus =
                    AZIMIWorkspaceController.FAILED,
                message =
                    "AZIMI Workspace readiness could not be determined safely."
            )
        }
    }

    /**
     * Returns true only when all readiness requirements pass.
     */
    fun isReady(
        context: Context
    ): Boolean {

        return runCatching {

            check(context).status ==
                READY

        }.getOrDefault(false)
    }

    /**
     * Returns only the readiness status.
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
     * Returns a safe human-readable readiness report.
     */
    fun summary(
        context: Context
    ): String {

        return runCatching {

            val result =
                check(context)

            buildString {

                append(
                    "AZIMI Workspace Readiness"
                )

                append("\nStatus: ")
                append(result.status)

                append("\nInitialized: ")
                append(result.workspaceInitialized)

                append("\nComplete: ")
                append(result.workspaceComplete)

                append("\nManifest Valid: ")
                append(result.manifestValid)

                append("\nState Valid: ")
                append(result.stateValid)

                append("\nIdentity Present: ")
                append(result.identityPresent)

                append("\nIdentity Consistent: ")
                append(result.identityConsistent)

                append("\nHealth: ")
                append(result.healthStatus)

                append("\nDiagnostics: ")
                append(result.diagnosticStatus)

                append("\nController: ")
                append(result.controllerStatus)

                append("\nMessage: ")
                append(result.message)
            }

        }.getOrElse {

            "AZIMI Workspace Readiness: CHECK_FAILED"
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
