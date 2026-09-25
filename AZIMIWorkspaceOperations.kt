package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Workspace Operations
 *
 * Controlled operations layer for the AZIMI-owned workspace.
 *
 * This module sits above the workspace controller and provides
 * safe, explicit operations for future AZIMI Core components.
 *
 * Security boundary:
 *
 * - Every operation is checked by AZIMIWorkspaceOperationGuard.
 * - Owner authorization is resolved through the trusted Context-based
 *   Guard path.
 * - No caller-supplied boolean authorization proof is used here.
 * - Read operations use READ_ONLY.
 * - State-changing operations use WRITE_SAFE.
 * - OWNER_AUTHORIZED operations remain unavailable until the trusted
 *   owner-authority system grants authorization.
 * - Destructive operations remain blocked.
 *
 * Design rules:
 *
 * - Operations are explicit.
 * - No automatic destructive repair.
 * - No automatic deletion.
 * - No migration.
 * - No modification of Z Continuity.
 * - No secrets are stored.
 * - No external provider is required.
 * - Failures are contained.
 * - Security failures fail closed.
 */
object AZIMIWorkspaceOperations {

    const val SUCCESS =
        "SUCCESS"

    const val DEGRADED =
        "DEGRADED"

    const val FAILED =
        "FAILED"

    /**
     * Safe operation result.
     */
    data class Result(
        val status: String,
        val operation: String,
        val controllerStatus: String,
        val healthStatus: String,
        val message: String
    )

    /**
     * Initializes the workspace through the centralized
     * workspace controller.
     *
     * Workspace initialization is classified as WRITE_SAFE
     * because it may create or update controlled workspace state.
     *
     * No destructive repair or deletion is performed.
     */
    fun initialize(
        context: Context
    ): Result {

        return runCatching {

            val operation =
                "INITIALIZE"

            if (
                !AZIMIWorkspaceOperationGuard.isAllowed(
                    context = context,
                    operation = operation,
                    level = AZIMIWorkspaceAccessPolicy.WRITE_SAFE
                )
            ) {

                safeRecordError(
                    context = context,
                    message =
                        "AZIMI Workspace initialize operation blocked by Workspace Operation Guard."
                )

                return@runCatching blockedResult(
                    operation = operation,
                    message =
                        "AZIMI Workspace initialization blocked by access policy."
                )
            }

            val controller =
                AZIMIWorkspaceController.initialize(
                    context
                )

            val operationStatus =
                when (controller.status) {

                    AZIMIWorkspaceController.READY ->
                        SUCCESS

                    AZIMIWorkspaceController.DEGRADED ->
                        DEGRADED

                    else ->
                        FAILED
                }

            Result(
                status =
                    operationStatus,
                operation =
                    operation,
                controllerStatus =
                    controller.status,
                healthStatus =
                    controller.healthStatus,
                message =
                    controller.message
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace initialize operation failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            failureResult(
                operation =
                    "INITIALIZE"
            )
        }
    }

    /**
     * Performs a read-only workspace inspection.
     *
     * This operation must remain available under READ_ONLY
     * access because it does not intentionally modify workspace
     * state.
     */
    fun inspect(
        context: Context
    ): Result {

        return runCatching {

            val operation =
                "INSPECT"

            if (
                !AZIMIWorkspaceOperationGuard.isAllowed(
                    context = context,
                    operation = operation,
                    level = AZIMIWorkspaceAccessPolicy.READ_ONLY
                )
            ) {

                safeRecordError(
                    context = context,
                    message =
                        "AZIMI Workspace inspect operation blocked by Workspace Operation Guard."
                )

                return@runCatching blockedResult(
                    operation = operation,
                    message =
                        "AZIMI Workspace inspection blocked by access policy."
                )
            }

            val controller =
                AZIMIWorkspaceController.inspect(
                    context
                )

            val operationStatus =
                when (controller.status) {

                    AZIMIWorkspaceController.READY ->
                        SUCCESS

                    AZIMIWorkspaceController.DEGRADED,
                    AZIMIWorkspaceController.NOT_INITIALIZED ->
                        DEGRADED

                    else ->
                        FAILED
                }

            Result(
                status =
                    operationStatus,
                operation =
                    operation,
                controllerStatus =
                    controller.status,
                healthStatus =
                    controller.healthStatus,
                message =
                    controller.message
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace inspect operation failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            failureResult(
                operation =
                    "INSPECT"
            )
        }
    }

    /**
     * Refreshes the workspace state timestamp and confirms
     * that the resulting state remains valid.
     *
     * This is an explicit WRITE_SAFE operation.
     *
     * It does not repair invalid state automatically.
     */
    fun refreshState(
        context: Context
    ): Result {

        return runCatching {

            val operation =
                "REFRESH_STATE"

            if (
                !AZIMIWorkspaceOperationGuard.isAllowed(
                    context = context,
                    operation = operation,
                    level = AZIMIWorkspaceAccessPolicy.WRITE_SAFE
                )
            ) {

                safeRecordError(
                    context = context,
                    message =
                        "AZIMI Workspace state refresh blocked by Workspace Operation Guard."
                )

                return@runCatching blockedResult(
                    operation = operation,
                    message =
                        "AZIMI Workspace state refresh blocked by access policy."
                )
            }

            val currentState =
                AZIMIWorkspaceState.read(
                    context
                )

            if (currentState == null) {
                return@runCatching failureResult(
                    operation =
                        operation
                )
            }

            val currentOperationalStatus =
                AZIMIWorkspaceState.operationalStatus(
                    context
                )
                    ?: "READY"

            val updated =
                AZIMIWorkspaceState.setOperationalStatus(
                    context = context,
                    status =
                        currentOperationalStatus
                )

            if (!updated) {
                return@runCatching failureResult(
                    operation =
                        operation
                )
            }

            val health =
                AZIMIWorkspaceHealth.check(
                    context
                )

            val operationStatus =
                if (health.healthy) {
                    SUCCESS
                } else {
                    DEGRADED
                }

            Result(
                status =
                    operationStatus,
                operation =
                    operation,
                controllerStatus =
                    AZIMIWorkspaceController.status(
                        context
                    ),
                healthStatus =
                    health.status,
                message =
                    if (health.healthy) {
                        "AZIMI Workspace State refreshed successfully."
                    } else {
                        "AZIMI Workspace State was refreshed but workspace health remains degraded."
                    }
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace state refresh failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            failureResult(
                operation =
                    "REFRESH_STATE"
            )
        }
    }

    /**
     * Explicitly changes the workspace operational status.
     *
     * This is a WRITE_SAFE operation.
     *
     * The value is stored through AZIMIWorkspaceState, which
     * performs its own validation and encrypted commit.
     */
    fun setOperationalStatus(
        context: Context,
        status: String
    ): Result {

        return runCatching {

            val operation =
                "SET_OPERATIONAL_STATUS"

            if (
                !AZIMIWorkspaceOperationGuard.isAllowed(
                    context = context,
                    operation = operation,
                    level = AZIMIWorkspaceAccessPolicy.WRITE_SAFE
                )
            ) {

                safeRecordError(
                    context = context,
                    message =
                        "AZIMI Workspace operational status update blocked by Workspace Operation Guard."
                )

                return@runCatching blockedResult(
                    operation = operation,
                    message =
                        "AZIMI Workspace operational status update blocked by access policy."
                )
            }

            val updated =
                AZIMIWorkspaceState.setOperationalStatus(
                    context = context,
                    status =
                        status
                )

            if (!updated) {
                return@runCatching failureResult(
                    operation =
                        operation
                )
            }

            val health =
                AZIMIWorkspaceHealth.check(
                    context
                )

            Result(
                status =
                    if (health.healthy) {
                        SUCCESS
                    } else {
                        DEGRADED
                    },
                operation =
                    operation,
                controllerStatus =
                    AZIMIWorkspaceController.status(
                        context
                    ),
                healthStatus =
                    health.status,
                message =
                    "AZIMI Workspace operational status updated."
            )

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace operational status update failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            failureResult(
                operation =
                    "SET_OPERATIONAL_STATUS"
            )
        }
    }

    /**
     * Returns a safe operation report.
     *
     * Summary is read-only and therefore uses the same
     * READ_ONLY boundary as inspect().
     */
    fun summary(
        context: Context
    ): String {

        return runCatching {

            val inspection =
                inspect(
                    context
                )

            buildString {

                append(
                    "AZIMI Workspace Operations"
                )

                append("\nStatus: ")
                append(
                    inspection.status
                )

                append("\nOperation: ")
                append(
                    inspection.operation
                )

                append("\nController: ")
                append(
                    inspection.controllerStatus
                )

                append("\nHealth: ")
                append(
                    inspection.healthStatus
                )

                append("\nMessage: ")
                append(
                    inspection.message
                )
            }

        }.getOrElse {

            "AZIMI Workspace Operations: FAILED"
        }
    }

    /**
     * Creates a safe failure result.
     */
    private fun failureResult(
        operation: String
    ): Result {

        return Result(
            status =
                FAILED,
            operation =
                operation,
            controllerStatus =
                AZIMIWorkspaceController.FAILED,
            healthStatus =
                AZIMIWorkspaceHealth.CHECK_FAILED,
            message =
                "AZIMI Workspace operation failed safely."
        )
    }

    /**
     * Creates a safe access-policy failure result.
     *
     * Authorization failures are deliberately reported without
     * exposing internal security state or authentication details.
     */
    private fun blockedResult(
        operation: String,
        message: String
    ): Result {

        return Result(
            status =
                FAILED,
            operation =
                operation,
            controllerStatus =
                AZIMIWorkspaceController.FAILED,
            healthStatus =
                AZIMIWorkspaceHealth.CHECK_FAILED,
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
