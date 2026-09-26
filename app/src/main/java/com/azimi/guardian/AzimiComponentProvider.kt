package com.azimi.guardian

/**
 * Provides an AZIMI component to the architecture.
 *
 * A provider describes where a component instance comes from.
 * It does not register, start, stop, recover, or execute the
 * component.
 *
 * This keeps component sourcing independent from:
 *
 * - component registration
 * - component lifecycle
 * - diagnostics
 * - recovery
 * - security authority
 *
 * Providers can therefore be replaced without changing the
 * component registry contract.
 */
interface AzimiComponentProvider {

    /**
     * Stable identifier of the component supplied by this provider.
     */
    val componentId: String

    /**
     * Creates or obtains the component instance.
     *
     * Returning null means that the component is currently
     * unavailable from this provider.
     */
    fun provide(): AzimiComponent?
}
