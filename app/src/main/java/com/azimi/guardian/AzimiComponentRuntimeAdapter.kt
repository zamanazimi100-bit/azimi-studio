package com.azimi.guardian

/**
 * Adapts an existing AZIMI component to the
 * component runtime contract.
 *
 * This adapter does not replace the component's implementation.
 * It provides a controlled lifecycle boundary around a component
 * that already exposes state and health information.
 *
 * No security authority, recovery authority, or consequential
 * action permission is created by this adapter.
 */
class AzimiComponentRuntimeAdapter(
    private val component: AzimiComponent
) : AzimiComponentRuntime {

    override val componentId: String
        get() = component.componentId.trim()

    /**
     * Starts the component through the adapter boundary.
     *
     * The base AzimiComponent contract does not expose a
     * start operation, so this adapter records the lifecycle
     * boundary without pretending to start an operation that
     * the component does not provide.
     *
     * Concrete components that require real startup behavior
     * should provide their own AzimiComponentRuntime
     * implementation.
     */
    override fun start(): Boolean {

        return runCatching {

            if (componentId.isEmpty()) {
                return false
            }

            when (component.state()) {

                AzimiComponentState.CREATED,
                AzimiComponentState.STOPPED -> {
                    false
                }

                AzimiComponentState.STARTING,
                AzimiComponentState.READY,
                AzimiComponentState.DEGRADED,
                AzimiComponentState.RECOVERING -> {
                    true
                }

                AzimiComponentState.UNAVAILABLE -> {
                    false
                }
            }
        }.getOrDefault(false)
    }

    /**
     * Stops the component through the adapter boundary.
     *
     * The base AzimiComponent contract does not expose a
     * stop operation. Therefore this adapter does not falsely
     * claim that a component was stopped.
     *
     * Concrete components that require real shutdown behavior
     * should provide their own AzimiComponentRuntime
     * implementation.
     */
    override fun stop(): Boolean {

        return runCatching {

            if (componentId.isEmpty()) {
                return false
            }

            when (component.state()) {

                AzimiComponentState.STOPPED -> {
                    true
                }

                AzimiComponentState.UNAVAILABLE -> {
                    false
                }

                AzimiComponentState.CREATED,
                AzimiComponentState.STARTING,
                AzimiComponentState.READY,
                AzimiComponentState.DEGRADED,
                AzimiComponentState.RECOVERING -> {
                    false
                }
            }
        }.getOrDefault(false)
    }

    /**
     * Returns the component's current lifecycle state.
     *
     * Observation only.
     */
    override fun state(): AzimiComponentState {

        return runCatching {
            component.state()
        }.getOrDefault(
            AzimiComponentState.UNAVAILABLE
        )
    }

    /**
     * Returns the component's current health.
     *
     * Observation only.
     */
    override fun health(): AzimiComponentHealth {

        return runCatching {
            component.health()
        }.getOrElse {
            AzimiComponentHealth(
                healthy = false,
                state = AzimiComponentState.UNAVAILABLE,
                message = "RUNTIME_INSPECTION_FAILED"
            )
        }
    }
}
