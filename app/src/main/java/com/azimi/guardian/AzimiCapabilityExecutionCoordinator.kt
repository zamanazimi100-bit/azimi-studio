package com.azimi.guardian

/**
 * Coordinates AZIMI capability execution.
 *
 * Execution flow:
 *
 * Execution Request
 *        ↓
 * Capability Executor
 *        ↓
 * Execution Result
 *        ↓
 * Execution Registry
 *
 * This coordinator does not:
 * - authenticate requesters
 * - authorize requests independently
 * - create authorized sessions
 * - elevate authority
 * - bypass the execution gate
 */
object AzimiCapabilityExecutionCoordinator {

    /**
     * Executes a capability through the central execution
     * boundary and records the resulting execution state.
     *
     * Authorization remains enforced by
     * AzimiCapabilityExecutor.
     */
    fun execute(
        request: AzimiCapabilityExecutionRequest,
        operation: () -> AzimiCapabilityExecutionResult
    ): AzimiCapabilityExecutionResult {

        val result =
            AzimiCapabilityExecutor.execute(
                request = request,
                operation = operation
            )

        AzimiCapabilityExecutionRegistry.record(
            result
        )

        return result
    }

    /**
     * Checks whether the supplied request currently passes
     * the execution gate without invoking the capability.
     */
    fun canExecute(
        request: AzimiCapabilityExecutionRequest
    ): Boolean {

        return AzimiCapabilityExecutor.canExecute(
            request
        )
    }

    /**
     * Returns the most recent execution result.
     */
    fun latest():
        AzimiCapabilityExecutionResult? {

        return AzimiCapabilityExecutionRegistry.latest()
    }

    /**
     * Returns all execution results, newest first.
     */
    fun history():
        List<AzimiCapabilityExecutionResult> {

        return AzimiCapabilityExecutionRegistry.all()
    }

    /**
     * Returns execution history for a requester.
     */
    fun historyForRequester(
        requesterId: String
    ): List<AzimiCapabilityExecutionResult> {

        return AzimiCapabilityExecutionRegistry
            .forRequester(requesterId)
    }

    /**
     * Returns execution history for a capability.
     */
    fun historyForCapability(
        capabilityId: String
    ): List<AzimiCapabilityExecutionResult> {

        return AzimiCapabilityExecutionRegistry
            .forCapability(capabilityId)
    }

    /**
     * Returns execution history for a requester component.
     */
    fun historyForComponent(
        componentId: String
    ): List<AzimiCapabilityExecutionResult> {

        return AzimiCapabilityExecutionRegistry
            .forComponent(componentId)
    }

    /**
     * Returns execution history for a specific state.
     */
    fun historyForState(
        state: AzimiCapabilityExecutionState
    ): List<AzimiCapabilityExecutionResult> {

        return AzimiCapabilityExecutionRegistry
            .forState(state)
    }

    /**
     * Returns the number of recorded execution results.
     */
    fun historySize(): Int {

        return AzimiCapabilityExecutionRegistry.size()
    }

    /**
     * Clears the in-memory execution history.
     *
     * This affects diagnostic history only.
     * It does not alter authorization, security state,
     * or capability state.
     */
    fun clearHistory() {

        AzimiCapabilityExecutionRegistry.clear()
    }
}
