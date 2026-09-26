package com.azimi.guardian

/**
 * Bounded in-memory registry of AZIMI capability authorization
 * decisions.
 *
 * This registry exists for inspection, diagnostics, and audit
 * context only.
 *
 * It does not:
 * - authenticate requesters
 * - grant permissions
 * - create authorized sessions
 * - execute capabilities
 * - persist secrets or credentials
 */
object AzimiCapabilityAuthorizationRegistry {

    private const val MAX_RESULTS = 100

    private val results =
        ArrayDeque<AzimiCapabilityAuthorizationResult>()

    /**
     * Records a valid authorization result.
     *
     * The registry is bounded so authorization history cannot
     * grow without limit during the active process.
     */
    @Synchronized
    fun record(
        result: AzimiCapabilityAuthorizationResult
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
     * Returns the most recent authorization result.
     */
    @Synchronized
    fun latest():
        AzimiCapabilityAuthorizationResult? {

        return results.lastOrNull()
    }

    /**
     * Returns authorization results from newest to oldest.
     */
    @Synchronized
    fun all():
        List<AzimiCapabilityAuthorizationResult> {

        return results
            .asReversed()
            .toList()
    }

    /**
     * Returns authorization results for a requester.
     */
    @Synchronized
    fun forRequester(
        requesterId: String
    ): List<AzimiCapabilityAuthorizationResult> {

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
     * Returns authorization results for a capability.
     */
    @Synchronized
    fun forCapability(
        capabilityId: String
    ): List<AzimiCapabilityAuthorizationResult> {

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
     * Returns authorization results for a requester component.
     */
    @Synchronized
    fun forComponent(
        componentId: String
    ): List<AzimiCapabilityAuthorizationResult> {

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
     * Returns authorization results matching a decision state.
     */
    @Synchronized
    fun forState(
        state: AzimiCapabilityAuthorizationState
    ): List<AzimiCapabilityAuthorizationResult> {

        return results
            .asReversed()
            .filter {
                it.state == state
            }
            .toList()
    }

    /**
     * Returns whether the registry contains at least one result.
     */
    @Synchronized
    fun hasResults(): Boolean {
        return results.isNotEmpty()
    }

    /**
     * Returns the number of stored authorization results.
     */
    @Synchronized
    fun size(): Int {
        return results.size
    }

    /**
     * Clears the in-memory authorization history.
     */
    @Synchronized
    fun clear() {
        results.clear()
    }
}
