package com.azimi.guardian

/**
 * Coordinates AZIMI capability authorization.
 *
 * The coordinator connects:
 *
 * Request
 *   ↓
 * Authorizer
 *   ↓
 * Registry
 *   ↓
 * Result
 *
 * It does not:
 * - authenticate requesters
 * - create authorized sessions
 * - grant persistent permissions
 * - execute capabilities
 * - perform consequential actions
 */
object AzimiCapabilityAuthorizationCoordinator {

    /**
     * Authorizes a capability request and records the
     * resulting decision for inspection.
     *
     * The authorization result is returned even when recording
     * the result is unsuccessful.
     */
    fun authorize(
        request: AzimiCapabilityAuthorization
    ): AzimiCapabilityAuthorizationResult {

        val result =
            AzimiCapabilityAuthorizer.authorize(
                request
            )

        AzimiCapabilityAuthorizationRegistry.record(
            result
        )

        return result
    }

    /**
     * Checks whether a capability request is currently
     * authorized.
     */
    fun isAuthorized(
        request: AzimiCapabilityAuthorization
    ): Boolean {

        return authorize(request)
            .isAuthorized()
    }

    /**
     * Returns the most recent authorization decision.
     */
    fun latest():
        AzimiCapabilityAuthorizationResult? {

        return AzimiCapabilityAuthorizationRegistry.latest()
    }

    /**
     * Returns all recorded authorization decisions,
     * newest first.
     */
    fun history():
        List<AzimiCapabilityAuthorizationResult> {

        return AzimiCapabilityAuthorizationRegistry.all()
    }

    /**
     * Returns authorization history for a requester.
     */
    fun historyForRequester(
        requesterId: String
    ): List<AzimiCapabilityAuthorizationResult> {

        return AzimiCapabilityAuthorizationRegistry
            .forRequester(requesterId)
    }

    /**
     * Returns authorization history for a capability.
     */
    fun historyForCapability(
        capabilityId: String
    ): List<AzimiCapabilityAuthorizationResult> {

        return AzimiCapabilityAuthorizationRegistry
            .forCapability(capabilityId)
    }

    /**
     * Returns authorization history for a requester component.
     */
    fun historyForComponent(
        componentId: String
    ): List<AzimiCapabilityAuthorizationResult> {

        return AzimiCapabilityAuthorizationRegistry
            .forComponent(componentId)
    }

    /**
     * Returns authorization history for a specific decision state.
     */
    fun historyForState(
        state: AzimiCapabilityAuthorizationState
    ): List<AzimiCapabilityAuthorizationResult> {

        return AzimiCapabilityAuthorizationRegistry
            .forState(state)
    }

    /**
     * Returns the number of recorded authorization decisions.
     */
    fun historySize(): Int {

        return AzimiCapabilityAuthorizationRegistry.size()
    }

    /**
     * Clears the in-memory authorization history.
     *
     * This affects diagnostic history only and does not change
     * authentication, authorization policy, or component state.
     */
    fun clearHistory() {

        AzimiCapabilityAuthorizationRegistry.clear()
    }
}
