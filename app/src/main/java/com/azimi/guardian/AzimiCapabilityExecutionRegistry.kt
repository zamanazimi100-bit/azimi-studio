package com.azimi.guardian

/**
 * Bounded in-memory registry of AZIMI capability execution
 * results.
 *
 * This registry exists for diagnostics, inspection, and
 * execution-history tracking only.
 *
 * It does not:
 * - execute capabilities
 * - authorize requests
 * - authenticate requesters
 * - create authorized sessions
 * - grant permissions
 * - persist secrets or credentials
 */
object AzimiCapabilityExecutionRegistry {

    private const val MAX_RESULTS = 100

    private val results =
        ArrayDeque<AzimiCapabilityExecutionResult>()

    /**
     * Records a valid execution result.
     *
     * The registry is bounded so execution history cannot grow
     * without limit during the active process.
     */
    @Synchronized
    fun record(
        result: AzimiCapabilityExecutionResult
    ): Boolean {

        val normalized =
            result.normalized()

        if (!normalized.isValid()) {
            return false
        }

        if (results.size >= MAX_RESULTS) {
            results.removeFirst()
        }

        results.addLast(normalized)

        return true
    }

    /**
     * Returns the most recent execution result.
     */
    @Synchronized
    fun latest():
        AzimiCapabilityExecutionResult? {

        return results.lastOrNull()
    }

    /**
     * Returns execution results from newest to oldest.
     */
    @Synchronized
    fun all():
        List<AzimiCapabilityExecutionResult> {

        return results
            .asReversed()
            .toList()
    }

    /**
     * Returns execution history for a requester.
     */
    @Synchronized
    fun forRequester(
        requesterId: String
    ): List<AzimiCapabilityExecutionResult> {

        val requester =
            requesterId.trim()

        if (requester.isEmpty()) {
            return emptyList()
        }

        return results
            .asReversed()
            .filter {
                it.requesterId.trim() == requester
            }
            .toList()
    }

    /**
     * Returns execution history for a capability.
     */
    @Synchronized
    fun forCapability(
        capabilityId: String
    ): List<AzimiCapabilityExecutionResult> {

        val capability =
            capabilityId.trim()

        if (capability.isEmpty()) {
            return emptyList()
        }

        return results
            .asReversed()
            .filter {
                it.capabilityId.trim() == capability
            }
            .toList()
    }

    /**
     * Returns execution history for a requester component.
     */
    @Synchronized
    fun forComponent(
        componentId: String
    ): List<AzimiCapabilityExecutionResult> {

        val component =
            componentId.trim()

        if (component.isEmpty()) {
            return emptyList()
        }

        return results
            .asReversed()
            .filter {
                it.requesterComponentId?.trim() == component
            }
            .toList()
    }

    /**
     * Returns execution history for a specific execution state.
     */
    @Synchronized
    fun forState(
        state: AzimiCapabilityExecutionState
    ): List<AzimiCapabilityExecutionResult> {

        return results
            .asReversed()
            .filter {
                it.state == state
            }
            .toList()
    }

    /**
     * Returns whether at least one execution result exists.
     */
    @Synchronized
    fun hasResults(): Boolean {
        return results.isNotEmpty()
    }

    /**
     * Returns the number of stored execution results.
     */
    @Synchronized
    fun size(): Int {
        return results.size
    }

    /**
     * Clears the in-memory execution history.
     *
     * This affects diagnostic history only.
     * It does not change capability state or authorization state.
     */
    @Synchronized
    fun clear() {
        results.clear()
    }
}
