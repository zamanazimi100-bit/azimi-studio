package com.azimi.guardian

/**
 * Explicit outcome of inspecting an AZIMI component dependency.
 *
 * This contract describes dependency state only.
 *
 * It does not:
 *
 * - resolve providers
 * - start components
 * - stop components
 * - enforce dependencies
 * - execute recovery
 * - grant security authority
 */
enum class AzimiComponentDependencyState {

    AVAILABLE,

    UNAVAILABLE,

    NOT_DECLARED,

    INVALID_COMPONENT_ID,

    COMPONENT_NOT_REGISTERED
}

/**
 * Result of inspecting a declared component dependency.
 */
data class AzimiComponentDependencyResult(

    /**
     * Component requesting the dependency.
     */
    val componentId: String,

    /**
     * Component identified as the dependency.
     */
    val dependencyComponentId: String,

    /**
     * Whether the declared dependency is required.
     */
    val required: Boolean = false,

    /**
     * Current dependency inspection state.
     */
    val state: AzimiComponentDependencyState,

    /**
     * Optional resolved dependency component.
     *
     * This is observational data only.
     */
    val dependency: AzimiComponent? = null,

    /**
     * Human-readable diagnostic message.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the component identities.
     */
    fun isValid(): Boolean {

        val source =
            componentId.trim()

        val dependencyId =
            dependencyComponentId.trim()

        return source.isNotEmpty() &&
            dependencyId.isNotEmpty() &&
            source != dependencyId
    }

    /**
     * Returns true when the dependency is currently
     * available as a registered component.
     */
    fun isAvailable(): Boolean {

        return state ==
            AzimiComponentDependencyState.AVAILABLE &&
            dependency != null &&
            dependency.componentId.trim() ==
            dependencyComponentId.trim()
    }
}
