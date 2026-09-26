package com.azimi.guardian

/**
 * Executes an AZIMI capability only after the supplied
 * execution request has passed the authorization boundary.
 *
 * The actual capability operation is supplied by the caller.
 * This keeps the execution boundary independent from individual
 * capability implementations.
 *
 * AzimiCapabilityExecutor does not:
 * - authenticate requesters
 * - create authorized sessions
 * - elevate authority
 * - bypass authorization
 * - persist permissions
 * - decide what a capability is allowed to do
 *
 * A capability implementation remains responsible for its
 * own operation-specific safety checks.
 */
object AzimiCapabilityExecutor {

    /**
     * Executes an authorized capability operation.
     *
     * The operation must return a structured execution result.
     *
     * Authorization is checked before the operation is invoked.
     * If authorization fails, the operation is never called.
     */
    fun execute(
        request: AzimiCapabilityExecutionRequest,
        operation: () -> AzimiCapabilityExecutionResult
    ): AzimiCapabilityExecutionResult {

        val normalized =
            request.normalized()

        if (!normalized.isValid()) {
            return deniedResult(
                request = normalized,
                state =
                    AzimiCapabilityExecutionState
                        .DENIED,
                message =
                    "INVALID_EXECUTION_REQUEST"
            )
        }

        if (!normalized.authorizationMatchesRequest()) {
            return deniedResult(
                request = normalized,
                state =
                    AzimiCapabilityExecutionState
                        .DENIED,
                message =
                    "AUTHORIZATION_REQUEST_MISMATCH"
            )
        }

        if (!normalized.isAuthorizedForExecution()) {
            return deniedResult(
                request = normalized,
                state =
                    AzimiCapabilityExecutionState
                        .DENIED,
                message =
                    "EXECUTION_NOT_AUTHORIZED"
            )
        }

        return runCatching {

            val result =
                operation()

            val normalizedResult =
                result.normalized()

            if (!normalizedResult.isValid()) {

                AzimiCapabilityExecutionResult(
                    requesterId =
                        normalized.requesterId,

                    capabilityId =
                        normalized.capabilityId,

                    requesterComponentId =
                        normalized.requesterComponentId,

                    state =
                        AzimiCapabilityExecutionState
                            .FAILED,

                    message =
                        "INVALID_EXECUTION_RESULT"
                )

            } else if (
                !resultMatchesRequest(
                    normalized,
                    normalizedResult
                )
            ) {

                AzimiCapabilityExecutionResult(
                    requesterId =
                        normalized.requesterId,

                    capabilityId =
                        normalized.capabilityId,

                    requesterComponentId =
                        normalized.requesterComponentId,

                    state =
                        AzimiCapabilityExecutionState
                            .FAILED,

                    message =
                        "EXECUTION_RESULT_MISMATCH"
                )

            } else {

                normalizedResult
            }

        }.getOrElse {

            AzimiCapabilityExecutionResult(
                requesterId =
                    normalized.requesterId,

                capabilityId =
                    normalized.capabilityId,

                requesterComponentId =
                    normalized.requesterComponentId,

                state =
                    AzimiCapabilityExecutionState
                        .FAILED,

                message =
                    "CAPABILITY_EXECUTION_FAILED"
            )
        }
    }

    /**
     * Checks whether an execution request is permitted without
     * invoking the supplied capability operation.
     */
    fun canExecute(
        request: AzimiCapabilityExecutionRequest
    ): Boolean {

        val normalized =
            request.normalized()

        return normalized.isValid() &&
            normalized.authorizationMatchesRequest() &&
            normalized.isAuthorizedForExecution()
    }

    /**
     * Creates a safe denial result.
     */
    private fun deniedResult(
        request: AzimiCapabilityExecutionRequest,
        state: AzimiCapabilityExecutionState,
        message: String
    ): AzimiCapabilityExecutionResult {

        return AzimiCapabilityExecutionResult(
            requesterId =
                request.requesterId.trim(),

            capabilityId =
                request.capabilityId.trim(),

            requesterComponentId =
                request.requesterComponentId
                    ?.trim()
                    ?.ifEmpty {
                        null
                    },

            state = state,

            message =
                message.trim().ifEmpty {
                    "NONE"
                }
        )
    }

    /**
     * Ensures the capability operation cannot return a result
     * belonging to a different execution request.
     */
    private fun resultMatchesRequest(
        request: AzimiCapabilityExecutionRequest,
        result: AzimiCapabilityExecutionResult
    ): Boolean {

        return request.requesterId.trim() ==
            result.requesterId.trim() &&

            request.capabilityId.trim() ==
                result.capabilityId.trim() &&

            request.requesterComponentId
                ?.trim() ==
                result.requesterComponentId
                    ?.trim()
    }
}
