package com.azimi.guardian

/**
 * Central registry for AZIMI recovery states.
 *
 * The registry stores recovery status only.
 * It does not execute recovery operations,
 * modify components, grant authority, or bypass security.
 *
 * Recovery remains isolated from the components it describes.
 */
object AzimiRecoveryRegistry {

    private val states =
        LinkedHashMap<String, AzimiRecoveryStatus>()

    /**
     * Registers or replaces the recovery status
     * for a component.
     */
    @Synchronized
    fun register(
        status: AzimiRecoveryStatus
    ): Boolean {

        if (!status.isValid()) {
            return false
        }

        val componentId =
            status.componentId.trim()

        states[componentId] =
            status.copy(
                componentId = componentId
            )

        return true
    }

    /**
     * Removes the recovery status for a component.
     *
     * This changes only registry state.
     * It does not stop, recover, or modify the component.
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

        return states.remove(id) != null
    }

    /**
     * Returns the current recovery status
     * for a component.
     */
    @Synchronized
    fun get(
        componentId: String
    ): AzimiRecoveryStatus? {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return null
        }

        return states[id]
    }

    /**
     * Returns a stable snapshot of all
     * registered recovery states.
     */
    @Synchronized
    fun all(): List<AzimiRecoveryStatus> {
        return states.values.toList()
    }

    /**
     * Returns all components currently
     * in the requested recovery state.
     */
    @Synchronized
    fun forState(
        state: AzimiRecoveryState
    ): List<AzimiRecoveryStatus> {

        return states.values
            .filter {
                it.state == state
            }
    }

    /**
     * Checks whether recovery information
     * exists for a component.
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

        return states.containsKey(id)
    }

    /**
     * Returns the number of components
     * with registered recovery state.
     */
    @Synchronized
    fun size(): Int {
        return states.size
    }

    /**
     * Clears all registered recovery states.
     *
     * This affects only registry state.
     * It does not perform recovery and does not
     * modify the components themselves.
     */
    @Synchronized
    fun clear() {
        states.clear()
    }
}
