package com.azimi.guardian

/**
 * Small contract shared by AZIMI building blocks.
 *
 * This contract describes a component without owning
 * the component's internal implementation.
 *
 * Components should remain independently understandable,
 * testable, and replaceable.
 */
interface AzimiComponent {

    /**
     * Stable machine-readable component identifier.
     *
     * Examples:
     * ATLAS
     * Z_VAULT
     * Z_SHIELD
     * Z_RECOVERY
     */
    val componentId: String

    /**
     * Human-readable component name.
     */
    val componentName: String

    /**
     * Current lifecycle state of the component.
     */
    fun state(): AzimiComponentState

    /**
     * Lightweight health check.
     *
     * This must not perform consequential actions.
     */
    fun health(): AzimiComponentHealth
}

/**
 * Lifecycle state of an AZIMI component.
 */
enum class AzimiComponentState {

    CREATED,

    STARTING,

    READY,

    DEGRADED,

    RECOVERING,

    UNAVAILABLE,

    STOPPED
}

/**
 * Lightweight health result for an AZIMI component.
 */
data class AzimiComponentHealth(

    val healthy: Boolean,

    val state: AzimiComponentState,

    val message: String = "NONE"
)
