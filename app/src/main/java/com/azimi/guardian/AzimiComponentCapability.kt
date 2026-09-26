package com.azimi.guardian

/**
 * Declares a capability provided by an AZIMI component.
 *
 * A capability describes what a component can provide
 * without exposing its internal implementation.
 *
 * This is architectural metadata only.
 *
 * Declaring a capability does not:
 *
 * - execute the capability
 * - grant permission to use it
 * - resolve a provider
 * - start a component
 * - stop a component
 * - execute recovery
 * - change security authority
 */
data class AzimiComponentCapability(

    /**
     * Component providing the capability.
     */
    val componentId: String,

    /**
     * Stable machine-readable capability identifier.
     */
    val capabilityId: String,

    /**
     * Human-readable description of the capability.
     */
    val description: String = "NONE"
) {

    /**
     * Validates the capability declaration.
     */
    fun isValid(): Boolean {

        return componentId.trim().isNotEmpty() &&
            capabilityId.trim().isNotEmpty()
    }

    /**
     * Returns a normalized copy of this declaration.
     */
    fun normalized(): AzimiComponentCapability {

        return copy(
            componentId =
                componentId.trim(),

            capabilityId =
                capabilityId.trim(),

            description =
                description.trim().ifEmpty {
                    "NONE"
                }
        )
    }
}
