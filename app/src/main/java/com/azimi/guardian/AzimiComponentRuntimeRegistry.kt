package com.azimi.guardian

/**
 * Central registry for AZIMI component runtimes.
 *
 * The registry knows which runtime boundaries exist.
 * It does not own component implementation, grant authority,
 * bypass security, or execute consequential actions.
 */
object AzimiComponentRuntimeRegistry {

    private val runtimes =
        LinkedHashMap<String, AzimiComponentRuntime>()

    /**
     * Registers or replaces a component runtime
     * using its stable component ID.
     */
    @Synchronized
    fun register(
        runtime: AzimiComponentRuntime
    ): Boolean {

        val id =
            runtime.componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        runtimes[id] = runtime

        return true
    }

    /**
     * Removes a component runtime from the registry.
     *
     * This changes only registry state.
     * It does not stop or modify the component.
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

        return runtimes.remove(id) != null
    }

    /**
     * Finds a registered runtime by component ID.
     */
    @Synchronized
    fun get(
        componentId: String
    ): AzimiComponentRuntime? {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return null
        }

        return runtimes[id]
    }

    /**
     * Returns a stable snapshot of all registered runtimes.
     *
     * The returned list cannot modify the registry.
     */
    @Synchronized
    fun all(): List<AzimiComponentRuntime> {
        return runtimes.values.toList()
    }

    /**
     * Checks whether a runtime is registered.
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

        return runtimes.containsKey(id)
    }

    /**
     * Returns the current status of a registered component.
     *
     * This performs observation only.
     */
    @Synchronized
    fun inspect(
        componentId: String
    ): AzimiComponentStatus? {

        val runtime =
            get(componentId)
                ?: return null

        return AzimiComponentRuntimeInspector.inspect(
            runtime
        )
    }

    /**
     * Returns the current status of every registered
     * component runtime.
     *
     * This performs observation only.
     */
    @Synchronized
    fun inspectAll(): List<AzimiComponentStatus> {

        return runtimes.values.map {
            AzimiComponentRuntimeInspector.inspect(it)
        }
    }

    /**
     * Returns the number of registered runtimes.
     */
    @Synchronized
    fun size(): Int {
        return runtimes.size
    }

    /**
     * Clears the runtime registry.
     *
     * This affects only registry state.
     * It does not stop, recover, or modify components.
     */
    @Synchronized
    fun clear() {
        runtimes.clear()
    }
}
