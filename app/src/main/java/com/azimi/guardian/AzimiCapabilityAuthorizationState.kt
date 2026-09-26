package com.azimi.guardian

/**
 * Explicit authorization decision states for AZIMI
 * capability requests.
 *
 * These states describe an authorization decision.
 * They do not perform authentication or capability execution.
 */
enum class AzimiCapabilityAuthorizationState {

    /**
     * The request has been authorized by the
     * applicable authorization policy.
     */
    AUTHORIZED,

    /**
     * The request has been denied.
     */
    DENIED,

    /**
     * The authorization request is malformed or
     * contains an invalid identity.
     */
    INVALID_REQUEST,

    /**
     * The requester does not currently have an
     * active authorized session.
     */
    NO_ACTIVE_SESSION,

    /**
     * The requested capability is not currently available.
     */
    CAPABILITY_UNAVAILABLE,

    /**
     * The requested capability has not been declared.
     */
    CAPABILITY_NOT_DECLARED,

    /**
     * The requester component is not currently registered.
     */
    REQUESTER_COMPONENT_UNAVAILABLE,

    /**
     * Authorization state cannot safely be determined.
     */
    UNKNOWN
}
