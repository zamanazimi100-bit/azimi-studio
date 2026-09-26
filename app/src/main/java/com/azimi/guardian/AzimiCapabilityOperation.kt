package com.azimi.guardian

/**
 * Defines one executable AZIMI capability operation.
 *
 * An operation provides the capability-specific behavior.
 * It does not own the authorization system or execution
 * orchestration.
 *
 * Implementations must not:
 * - authenticate requesters
 * - bypass authorization
 * - elevate authority
 * - silently grant permissions
 * - store protected credentials
 *
 * Authorization is enforced before an operation is invoked
 * by AzimiCapabilityExecutor.
 */
interface AzimiCapabilityOperation {

    /**
     * Stable identifier of the capability implemented
     * by this operation.
     */
    val capabilityId: String

    /**
     * Executes the capability operation for the supplied
     * execution request.
     *
     * The executor is responsible for enforcing the
     * authorization boundary before this method is called.
     */
    fun execute(
        request: AzimiCapabilityExecutionRequest
    ): AzimiCapabilityExecutionResult
}
