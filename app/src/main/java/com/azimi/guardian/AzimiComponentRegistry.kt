package com.azimi.guardian

/**
 * Central registry for AZIMI building blocks.
 *
 * The registry knows which components exist.
 * It does not own their internal behavior.
 *
 * Components remain independently implemented,
 * testable, and replaceable.
 */
object AzimiComponentRegistry {

    private val components =
        LinkedHashMap<String, AzimiComponent>()

    /**
     * Registers or replaces a component using
     * its stable component ID.
     */
    @Synchronized
    fun register(
        component: AzimiComponent
    ): Boolean {

        val id =
            component.componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        components[id] = component

        return true
    }

    /**
     * Removes a component from the registry.
     */
    @Synchronized
    fun unregister(
        componentId: String
    ): Boolean {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        return components.remove(id) != null
    }

    /**
     * Finds a registered component by ID.
     */
    @Synchronized
    fun get(
        componentId: String
    ): AzimiComponent? {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return null
        }

        return components[id]
    }

    /**
     * Returns a stable snapshot of all registered components.
     *
     * The returned list cannot modify the registry.
     */
    @Synchronized
    fun all(): List<AzimiComponent> {
        return components.values.toList()
    }

    /**
     * Checks whether a component is registered.
     */
    @Synchronized
    fun contains(
        componentId: String
    ): Boolean {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        return components.containsKey(id)
    }

    /**
     * Returns the number of registered components.
     */
    @Synchronized
    fun size(): Int {
        return components.size
    }

    /**
     * Clears the registry.
     *
     * This affects only registration state.
     * It does not stop, delete, or modify components.
     */
    @Synchronized
    fun clear() {
        components.clear()
    }
}
