package com.azimi.guardian

/**
 * Registry for declared AZIMI component dependencies.
 *
 * This registry stores architectural dependency metadata only.
 *
 * It does not:
 *
 * - resolve dependencies
 * - start components
 * - stop components
 * - enforce dependency rules
 * - execute recovery
 * - grant security authority
 */
object AzimiComponentDependencyRegistry {

    private val dependencies =
        LinkedHashMap<String, AzimiComponentDependency>()

    private fun key(
        componentId: String,
        dependencyComponentId: String
    ): String {

        return componentId.trim() +
            "->" +
            dependencyComponentId.trim()
    }

    /**
     * Registers a dependency declaration.
     */
    @Synchronized
    fun register(
        dependency: AzimiComponentDependency
    ): Boolean {

        val normalized =
            dependency.normalized()

        if (!normalized.isValid()) {
            return false
        }

        dependencies[
            key(
                normalized.componentId,
                normalized.dependencyComponentId
            )
        ] = normalized

        return true
    }

    /**
     * Removes a dependency declaration.
     */
    @Synchronized
    fun unregister(
        componentId: String,
        dependencyComponentId: String
    ): Boolean {

        val source =
            componentId.trim()

        val dependency =
            dependencyComponentId.trim()

        if (source.isEmpty() ||
            dependency.isEmpty()
        ) {
            return false
        }

        return dependencies.remove(
            key(
                source,
                dependency
            )
        ) != null
    }

    /**
     * Returns one declared dependency.
     */
    @Synchronized
    fun get(
        componentId: String,
        dependencyComponentId: String
    ): AzimiComponentDependency? {

        val source =
            componentId.trim()

        val dependency =
            dependencyComponentId.trim()

        if (source.isEmpty() ||
            dependency.isEmpty()
        ) {
            return null
        }

        return dependencies[
            key(
                source,
                dependency
            )
        ]
    }

    /**
     * Returns a snapshot of all declarations.
     */
    @Synchronized
    fun all(): List<AzimiComponentDependency> {

        return dependencies.values.toList()
    }

    /**
     * Returns dependencies declared by one component.
     */
    @Synchronized
    fun forComponent(
        componentId: String
    ): List<AzimiComponentDependency> {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return emptyList()
        }

        return dependencies.values
            .filter {
                it.componentId == id
            }
            .toList()
    }

    /**
     * Returns declarations that identify the supplied
     * component as a dependency.
     */
    @Synchronized
    fun forDependency(
        dependencyComponentId: String
    ): List<AzimiComponentDependency> {

        val id =
            dependencyComponentId.trim()

        if (id.isEmpty()) {
            return emptyList()
        }

        return dependencies.values
            .filter {
                it.dependencyComponentId == id
            }
            .toList()
    }

    /**
     * Returns only required dependencies declared by
     * the supplied component.
     */
    @Synchronized
    fun requiredForComponent(
        componentId: String
    ): List<AzimiComponentDependency> {

        return forComponent(componentId)
            .filter {
                it.required
            }
    }

    /**
     * Checks whether a dependency declaration exists.
     */
    @Synchronized
    fun contains(
        componentId: String,
        dependencyComponentId: String
    ): Boolean {

        return get(
            componentId,
            dependencyComponentId
        ) != null
    }

    /**
     * Returns the number of declarations.
     */
    @Synchronized
    fun size(): Int {

        return dependencies.size
    }

    /**
     * Clears all dependency declarations.
     *
     * This only clears architectural metadata.
     */
    @Synchronized
    fun clear() {

        dependencies.clear()
    }
}
