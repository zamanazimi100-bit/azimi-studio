package com.azimi.guardian

/**
 * Registry for AZIMI component providers.
 *
 * Providers are kept separate from component instances.
 *
 * This registry does not:
 *
 * - create components automatically
 * - register provided components
 * - start or stop components
 * - execute recovery
 * - execute consequential actions
 *
 * It only stores and retrieves provider contracts.
 */
object AzimiComponentProviderRegistry {

    private val providers =
        LinkedHashMap<String, AzimiComponentProvider>()

    /**
     * Registers or replaces a provider using its component ID.
     */
    @Synchronized
    fun register(
        provider: AzimiComponentProvider
    ): Boolean {

        val id =
            provider.componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        providers[id] =
            provider

        return true
    }

    /**
     * Removes a provider by component ID.
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

        return providers.remove(id) != null
    }

    /**
     * Returns a provider by component ID.
     */
    @Synchronized
    fun get(
        componentId: String
    ): AzimiComponentProvider? {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return null
        }

        return providers[id]
    }

    /**
     * Returns a snapshot of all registered providers.
     */
    @Synchronized
    fun all(): List<AzimiComponentProvider> {

        return providers.values.toList()
    }

    /**
     * Checks whether a provider exists for the component ID.
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

        return providers.containsKey(id)
    }

    /**
     * Returns the number of registered providers.
     */
    @Synchronized
    fun size(): Int {

        return providers.size
    }

    /**
     * Removes all providers.
     *
     * This only clears provider registrations.
     * It does not affect existing component instances.
     */
    @Synchronized
    fun clear() {

        providers.clear()
    }
}
