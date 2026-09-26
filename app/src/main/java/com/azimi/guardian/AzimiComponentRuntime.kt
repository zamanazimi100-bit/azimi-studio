package com.azimi.guardian

/**
 * Controlled runtime contract for an AZIMI component.
 *
 * This contract defines lifecycle boundaries without owning
 * component-specific implementation.
 *
 * A runtime may start, stop, or report the condition of a
 * component, but consequential behavior must remain inside
 * the component's own implementation and security boundaries.
 */
interface AzimiComponentRuntime {

    /**
     * Stable identifier of the component controlled by
     * this runtime.
     */
    val componentId: String

    /**
     * Starts the component.
     *
     * Implementations must not silently bypass security,
     * authorization, or component-specific safety rules.
     */
    fun start(): Boolean

    /**
     * Stops the component.
     *
     * Implementations should leave the component in a
     * predictable stopped state.
     */
    fun stop(): Boolean

    /**
     * Returns the current component lifecycle state.
     *
     * This operation must not perform consequential actions.
     */
    fun state(): AzimiComponentState

    /**
     * Returns a lightweight health report.
     *
     * This operation must not perform consequential actions.
     */
    fun health(): AzimiComponentHealth
}

/**
 * Result of a controlled component lifecycle operation.
 *
 * The result describes what happened; it does not itself
 * authorize any future operation.
 */
data class AzimiRuntimeResult(

    /**
     * Component associated with the operation.
     */
    val componentId: String,

    /**
     * Whether the requested operation succeeded.
     */
    val successful: Boolean,

    /**
     * Component state after the operation.
     */
    val state: AzimiComponentState,

    /**
     * Short diagnostic message.
     *
     * Secrets and protected credentials must never
     * be placed in this field.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the minimum identity required
     * for a useful runtime result.
     */
    fun isValid(): Boolean {

        return componentId
            .trim()
            .isNotEmpty()
    }
}

/**
 * Small helper for safely describing a component
 * through its runtime contract.
 *
 * This helper observes state only.
 * It does not start, stop, recover, or modify components.
 */
object AzimiComponentRuntimeInspector {

    /**
     * Creates a status snapshot from a runtime.
     */
    fun inspect(
        runtime: AzimiComponentRuntime
    ): AzimiComponentStatus {

        val componentId =
            runtime.componentId.trim()

        if (componentId.isEmpty()) {
            return AzimiComponentStatus(
                componentId = "",
                state = AzimiComponentState.UNAVAILABLE,
                healthy = false,
                message = "INVALID_COMPONENT_ID"
            )
        }

        return AzimiComponentStatus(
            componentId = componentId,
            state = runtime.state(),
            healthy = runtime.health().healthy,
            message = runtime.health().message
        )
    }
}
