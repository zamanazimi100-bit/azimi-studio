package com.azimi.guardian

/**
 * Central registry for AZIMI component health observations.
 *
 * The registry stores the latest known health status for each
 * component. It does not start, stop, recover, authorize, or
 * modify components.
 *
 * Health is observation data, not permission.
 */
object AzimiHealthRegistry {

    private val healthStates =
        LinkedHashMap<String, AzimiComponentHealth>()

    /**
     * Records or replaces the latest health observation
     * for a component.
     */
    @Synchronized
    fun record(
        componentId: String,
        health: AzimiComponentHealth
    ): Boolean {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        healthStates[id] = health

        return true
    }

    /**
     * Records the health reported by a component.
     *
     * Observation only.
     */
    @Synchronized
    fun record(
        component: AzimiComponent
    ): Boolean {

        val id =
            component.componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        return runCatching {

            healthStates[id] =
                component.health()

            true
        }.getOrDefault(false)
    }

    /**
     * Returns the latest known health observation
     * for a component.
     */
    @Synchronized
    fun get(
        componentId: String
    ): AzimiComponentHealth? {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return null
        }

        return healthStates[id]
    }

    /**
     * Returns a stable snapshot of all health observations.
     */
    @Synchronized
    fun all(): Map<String, AzimiComponentHealth> {
        return LinkedHashMap(
            healthStates
        )
    }

    /**
     * Returns all components currently reporting
     * a healthy condition.
     */
    @Synchronized
    fun healthy(): List<String> {

        return healthStates
            .filter {
                it.value.healthy
            }
            .keys
            .toList()
    }

    /**
     * Returns all components currently reporting
     * an unhealthy condition.
     */
    @Synchronized
    fun unhealthy(): List<String> {

        return healthStates
            .filter {
                !it.value.healthy
            }
            .keys
            .toList()
    }

    /**
     * Returns all components currently in a
     * particular lifecycle state.
     */
    @Synchronized
    fun forState(
        state: AzimiComponentState
    ): List<String> {

        return healthStates
            .filter {
                it.value.state == state
            }
            .keys
            .toList()
    }

    /**
     * Checks whether health information exists
     * for a component.
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

        return healthStates.containsKey(id)
    }

    /**
     * Removes the stored health observation
     * for a component.
     *
     * This does not modify the component itself.
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

        return healthStates.remove(id) != null
    }

    /**
     * Returns the number of stored health observations.
     */
    @Synchronized
    fun size(): Int {
        return healthStates.size
    }

    /**
     * Clears only the in-memory health registry.
     *
     * No component is stopped, recovered, or modified.
     */
    @Synchronized
    fun clear() {
        healthStates.clear()
    }
}
