package com.azimi.guardian

/**
 * Explicit outcome of inspecting an AZIMI capability.
 *
 * This result describes capability availability only.
 *
 * It does not:
 *
 * - execute the capability
 * - grant authorization
 * - start a component
 * - stop a component
 * - execute recovery
 * - change security authority
 */
enum class AzimiCapabilityResolutionState {

    AVAILABLE,

    NOT_DECLARED,

    PROVIDER_COMPONENT_UNAVAILABLE,

    INVALID_CAPABILITY_ID,

    INVALID_COMPONENT_ID
}

/**
 * Result of inspecting a component capability.
 */
data class AzimiCapabilityResolutionResult(

    /**
     * Requested capability identifier.
     */
    val capabilityId: String,

    /**
     * Optional component requested as the provider.
     *
     * When null, the capability may be searched across
     * all declared providers.
     */
    val requestedComponentId: String? = null,

    /**
     * Current capability inspection state.
     */
    val state: AzimiCapabilityResolutionState,

    /**
     * Component currently identified as the provider.
     *
     * This is observational data only.
     */
    val providerComponent: AzimiComponent? = null,

    /**
     * Human-readable diagnostic message.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the capability identity.
     */
    fun isValid(): Boolean {

        val capability =
            capabilityId.trim()

        val component =
            requestedComponentId
                ?.trim()

        return capability.isNotEmpty() &&
            (
                component == null ||
                    component.isNotEmpty()
            )
    }

    /**
     * Returns true when a usable provider component
     * has been identified.
     */
    fun isAvailable(): Boolean {

        val provider =
            providerComponent
                ?: return false

        val providerId =
            provider.componentId.trim()

        if (providerId.isEmpty()) {
            return false
        }

        val requested =
            requestedComponentId?.trim()

        if (requested != null &&
            requested != providerId
        ) {
            return false
        }

        return state ==
            AzimiCapabilityResolutionState.AVAILABLE
    }
}
