package com.azimi.guardian

/**
 * Explicit outcome of resolving an AZIMI component.
 *
 * This contract allows the architecture to distinguish
 * different resolution conditions without relying only
 * on a null component.
 *
 * Resolution results describe observation only.
 * They do not start, stop, register, recover, or execute
 * a component.
 */
enum class AzimiComponentResolutionState {

    RESOLVED,

    INVALID_COMPONENT_ID,

    PROVIDER_NOT_FOUND,

    PROVIDER_UNAVAILABLE,

    COMPONENT_ID_MISMATCH,

    PROVIDER_FAILURE
}

/**
 * Result returned by the AZIMI component resolver.
 */
data class AzimiComponentResolutionResult(

    val requestedComponentId: String,

    val state: AzimiComponentResolutionState,

    val component: AzimiComponent? = null,

    val message: String = "NONE"
) {

    /**
     * Checks whether the result contains a valid
     * requested component identity.
     */
    fun isValid(): Boolean {

        return requestedComponentId
            .trim()
            .isNotEmpty()
    }

    /**
     * Returns true only when resolution produced
     * a usable component.
     */
    fun isResolved(): Boolean {

        return state ==
            AzimiComponentResolutionState.RESOLVED &&
            component != null &&
            component.componentId.trim() ==
            requestedComponentId.trim()
    }
}
