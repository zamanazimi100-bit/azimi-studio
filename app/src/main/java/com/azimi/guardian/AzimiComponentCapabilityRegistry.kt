package com.azimi.guardian

/**
 * Registry for declared AZIMI component capabilities.
 *
 * This registry stores capability metadata only.
 *
 * It does not:
 *
 * - execute capabilities
 * - grant authorization
 * - resolve providers
 * - start components
 * - stop components
 * - execute recovery
 * - change security authority
 */
object AzimiComponentCapabilityRegistry {

    private val capabilities =
        LinkedHashMap<String, AzimiComponentCapability>()

    private fun key(
        componentId: String,
        capabilityId: String
    ): String {

        return componentId.trim() +
            "->" +
            capabilityId.trim()
    }

    /**
     * Registers a capability declaration.
     */
    @Synchronized
    fun register(
        capability: AzimiComponentCapability
    ): Boolean {

        val normalized =
            capability.normalized()

        if (!normalized.isValid()) {
            return false
        }

        capabilities[
            key(
                normalized.componentId,
                normalized.capabilityId
            )
        ] = normalized

        return true
    }

    /**
     * Removes a capability declaration.
     */
    @Synchronized
    fun unregister(
        componentId: String,
        capabilityId: String
    ): Boolean {

        val component =
            componentId.trim()

        val capability =
            capabilityId.trim()

        if (component.isEmpty() ||
            capability.isEmpty()
        ) {
            return false
        }

        return capabilities.remove(
            key(
                component,
                capability
            )
        ) != null
    }

    /**
     * Returns one capability declaration.
     */
    @Synchronized
    fun get(
        componentId: String,
        capabilityId: String
    ): AzimiComponentCapability? {

        val component =
            componentId.trim()

        val capability =
            capabilityId.trim()

        if (component.isEmpty() ||
            capability.isEmpty()
        ) {
            return null
        }

        return capabilities[
            key(
                component,
                capability
            )
        ]
    }

    /**
     * Returns a snapshot of all capability declarations.
     */
    @Synchronized
    fun all(): List<AzimiComponentCapability> {

        return capabilities.values.toList()
    }

    /**
     * Returns capabilities declared by one component.
     */
    @Synchronized
    fun forComponent(
        componentId: String
    ): List<AzimiComponentCapability> {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return emptyList()
        }

        return capabilities.values
            .filter {
                it.componentId == id
            }
            .toList()
    }

    /**
     * Returns all components declaring the supplied
     * capability.
     */
    @Synchronized
    fun providersOf(
        capabilityId: String
    ): List<AzimiComponentCapability> {

        val id =
            capabilityId.trim()

        if (id.isEmpty()) {
            return emptyList()
        }

        return capabilities.values
            .filter {
                it.capabilityId == id
            }
            .toList()
    }

    /**
     * Checks whether a component declares a capability.
     */
    @Synchronized
    fun contains(
        componentId: String,
        capabilityId: String
    ): Boolean {

        return get(
            componentId,
            capabilityId
        ) != null
    }

    /**
     * Returns the number of capability declarations.
     */
    @Synchronized
    fun size(): Int {

        return capabilities.size
    }

    /**
     * Clears all capability declarations.
     *
     * This only clears architectural metadata.
     */
    @Synchronized
    fun clear() {

        capabilities.clear()
    }
}
