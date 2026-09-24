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
     */
    fun initialize(
        context: Context
    ): Result {

        return runCatching {

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
                    "INITIALIZE",
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
     */
    fun inspect(
        context: Context
    ): Result {

        return runCatching {

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
                    "INSPECT",
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
     * This is an explicit operation.
     *
     * It does not repair invalid state automatically.
     */
    fun refreshState(
        context: Context
    ): Result {

        return runCatching {

            val currentState =
                AZIMIWorkspaceState.read(
                    context
                )

            if (currentState == null) {
                return@runCatching failureResult(
                    operation =
                        "REFRESH_STATE"
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
                        "REFRESH_STATE"
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
                    "REFRESH_STATE",
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
     * The value is stored through AZIMIWorkspaceState, which
     * performs its own validation and encrypted commit.
     */
    fun setOperationalStatus(
        context: Context,
        status: String
    ): Result {

        return runCatching {

            val updated =
                AZIMIWorkspaceState.setOperationalStatus(
                    context = context,
                    status =
                        status
                )

            if (!updated) {
                return@runCatching failureResult(
                    operation =
                        "SET_OPERATIONAL_STATUS"
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
                    "SET_OPERATIONAL_STATUS",
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
