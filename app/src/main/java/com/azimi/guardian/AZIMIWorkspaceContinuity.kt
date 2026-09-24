package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Workspace Continuity
 *
 * Connects the AZIMI Workspace foundation to the existing
 * Z Continuity event system.
 *
 * Architecture:
 *
 * AZIMI Workspace
 *       ↓
 * Workspace Readiness
 *       ↓
 * Workspace Operation Guard
 *       ↓
 * Z Continuity Events
 *       ↓
 * Z Continuity Storage
 *
 * Important:
 * - ZContinuityStorage remains the source of truth.
 * - Existing Z Continuity APIs are preserved.
 * - Workspace does not replace existing storage.
 * - Protected credentials must never be recorded.
 * - Failed continuity recording must never crash the caller.
 */
object AZIMIWorkspaceContinuity {

    data class Result(
        val success: Boolean,
        val workspaceReady: Boolean,
        val authorized: Boolean,
        val message: String
    )

    /**
     * Records a general project event through the protected
     * AZIMI Workspace → Z Continuity path.
     */
    fun project(
        context: Context,
        title: String,
        content: String,
        category: String = "PROJECT"
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.project(
                context = context,
                title = title,
                content = content,
                category = category
            )
        }
    }

    /**
     * Records a build event.
     */
    fun build(
        context: Context,
        buildNumber: String,
        status: String,
        details: String = ""
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.build(
                context = context,
                buildNumber = buildNumber,
                status = status,
                details = details
            )
        }
    }

    /**
     * Records a failure event.
     */
    fun failure(
        context: Context,
        component: String,
        file: String,
        function: String,
        stage: String,
        message: String
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.failure(
                context = context,
                component = component,
                file = file,
                function = function,
                stage = stage,
                message = message
            )
        }
    }

    /**
     * Records an architectural or project decision.
     */
    fun decision(
        context: Context,
        title: String,
        decision: String,
        reason: String = ""
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.decision(
                context = context,
                title = title,
                decision = decision,
                reason = reason
            )
        }
    }

    /**
     * Records a diagnostic event.
     */
    fun diagnostic(
        context: Context,
        title: String,
        details: String
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.diagnostic(
                context = context,
                title = title,
                details = details
            )
        }
    }

    /**
     * Records a recovery point.
     */
    fun recoveryPoint(
        context: Context,
        title: String,
        details: String = ""
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.recoveryPoint(
                context = context,
                title = title,
                details = details
            )
        }
    }

    /**
     * Records configuration information.
     */
    fun configuration(
        context: Context,
        title: String,
        details: String
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.configuration(
                context = context,
                title = title,
                details = details
            )
        }
    }

    /**
     * Records a tool/provider event.
     */
    fun tool(
        context: Context,
        toolName: String,
        action: String,
        details: String
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.tool(
                context = context,
                toolName = toolName,
                action = action,
                details = details
            )
        }
    }

    /**
     * Records explicitly approved Atlas context.
     */
    fun atlasContext(
        context: Context,
        title: String,
        approvedContext: String
    ): Result {

        return execute(
            context = context,
            operation = "WRITE"
        ) {
            ZContinuityEvents.atlasContext(
                context = context,
                title = title,
                approvedContext = approvedContext
            )
        }
    }

    /**
     * Common protected execution path.
     *
     * Workspace readiness is checked first.
     * Workspace write policy is checked second.
     * Only then is the existing Z Continuity gateway called.
     *
     * Any failure is contained and returned as a Result.
     */
    private fun execute(
        context: Context,
        operation: String,
        action: () -> Unit
    ): Result {

        return try {

            val readiness = AZIMIWorkspaceReadiness.check(context)

            if (!readiness.status.equals("READY", ignoreCase = true)) {
                return Result(
                    success = false,
                    workspaceReady = false,
                    authorized = false,
                    message = "AZIMI Workspace is not ready: ${readiness.message}"
                )
            }

            val decision = AZIMIWorkspaceOperationGuard.check(
                operation = operation,
                level = AZIMIWorkspaceAccessPolicy.WRITE_SAFE
            )

            if (!decision.allowed) {
                return Result(
                    success = false,
                    workspaceReady = true,
                    authorized = false,
                    message = decision.message
                )
            }

            action()

            Result(
                success = true,
                workspaceReady = true,
                authorized = true,
                message = "Workspace continuity event recorded."
            )

        } catch (securityException: SecurityException) {

            Result(
                success = false,
                workspaceReady = true,
                authorized = false,
                message = "Workspace continuity security failure: ${
                    securityException.message ?: "unknown security error"
                }"
            )

        } catch (exception: Exception) {

            Result(
                success = false,
                workspaceReady = true,
                authorized = true,
                message = "Workspace continuity recording failed safely: ${
                    exception.message ?: exception.javaClass.simpleName
                }"
            )
        }
    }
}
