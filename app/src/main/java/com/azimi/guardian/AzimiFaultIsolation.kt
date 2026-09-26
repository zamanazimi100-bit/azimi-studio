package com.azimi.guardian

/**
 * Describes the isolation boundary of an AZIMI component.
 *
 * Fault isolation defines what should happen when a component
 * experiences a failure. It does not perform recovery, stop
 * components, or execute consequential actions.
 */
data class AzimiFaultIsolation(

    /**
     * Component whose failure is being isolated.
     */
    val componentId: String,

    /**
     * Components that must remain isolated from the failure.
     *
     * These identifiers describe protected boundaries only.
     */
    val isolatedComponentIds: List<String> = emptyList(),

    /**
     * Whether the component is currently allowed
     * to operate independently of the isolated components.
     */
    val independentOperation: Boolean = true,

    /**
     * Short diagnostic description of the isolation boundary.
     *
     * Secrets and protected credentials must never
     * be placed in this field.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the minimum information required
     * for a usable isolation boundary.
     */
    fun isValid(): Boolean {

        return componentId
            .trim()
            .isNotEmpty()
    }
}

/**
 * Central registry for AZIMI fault-isolation boundaries.
 *
 * The registry describes isolation relationships only.
 * It does not execute recovery, terminate components,
 * grant authority, or bypass security.
 */
object AzimiFaultIsolationRegistry {

    private val boundaries =
        LinkedHashMap<String, AzimiFaultIsolation>()

    /**
     * Registers or replaces the isolation boundary
     * for a component.
     */
    @Synchronized
    fun register(
        isolation: AzimiFaultIsolation
    ): Boolean {

        if (!isolation.isValid()) {
            return false
        }

        val componentId =
            isolation.componentId.trim()

        val isolatedIds =
            isolation.isolatedComponentIds
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }
                .distinct()

        val message =
            isolation.message.trim()

        boundaries[componentId] =
            isolation.copy(
                componentId = componentId,
                isolatedComponentIds = isolatedIds,
                message =
                    if (message.isEmpty()) {
                        "NONE"
                    } else {
                        message
                    }
            )

        return true
    }

    /**
     * Removes the isolation boundary for a component.
     *
     * This changes only registry state.
     * It does not change the component itself.
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

        return boundaries.remove(id) != null
    }

    /**
     * Returns the isolation boundary for a component.
     */
    @Synchronized
    fun get(
        componentId: String
    ): AzimiFaultIsolation? {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return null
        }

        return boundaries[id]
    }

    /**
     * Returns all registered isolation boundaries
     * as a stable snapshot.
     */
    @Synchronized
    fun all(): List<AzimiFaultIsolation> {
        return boundaries.values.toList()
    }

    /**
     * Returns all components that are explicitly
     * configured for independent operation.
     */
    @Synchronized
    fun independentlyOperating():
        List<AzimiFaultIsolation> {

        return boundaries.values
            .filter {
                it.independentOperation
            }
    }

    /**
     * Checks whether one component has an
     * isolation boundary registered.
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

        return boundaries.containsKey(id)
    }

    /**
     * Checks whether a component is listed inside
     * another component's isolation boundary.
     */
    @Synchronized
    fun isIsolatedFrom(
        componentId: String,
        isolatedComponentId: String
    ): Boolean {

        val component =
            componentId.trim()

        val isolated =
            isolatedComponentId.trim()

        if (
            component.isEmpty() ||
            isolated.isEmpty()
        ) {
            return false
        }

        return boundaries[component]
            ?.isolatedComponentIds
            ?.contains(isolated)
            ?: false
    }

    /**
     * Returns the number of registered
     * isolation boundaries.
     */
    @Synchronized
    fun size(): Int {
        return boundaries.size
    }

    /**
     * Clears only the in-memory isolation registry.
     *
     * No component is stopped, modified, or recovered.
     */
    @Synchronized
    fun clear() {
        boundaries.clear()
    }
}
