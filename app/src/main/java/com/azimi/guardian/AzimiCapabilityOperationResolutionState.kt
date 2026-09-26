package com.azimi.guardian

/**
 * Explicit states describing AZIMI capability-operation
 * resolution.
 *
 * These states describe resolution only.
 * They do not authorize, authenticate, or execute an operation.
 */
enum class AzimiCapabilityOperationResolutionState {

    /**
     * A valid operation was resolved successfully.
     */
    RESOLVED,

    /**
     * The requested capability ID is empty or otherwise invalid.
     */
    INVALID_CAPABILITY_ID,

    /**
     * No operation is currently registered for the requested
     * capability.
     */
    OPERATION_NOT_FOUND,

    /**
     * A registered operation was found, but its own capability
     * ID does not match the requested ID.
     */
    CAPABILITY_ID_MISMATCH,

    /**
     * An unexpected failure occurred while resolving the
     * operation.
     */
    RESOLUTION_FAILED
}
