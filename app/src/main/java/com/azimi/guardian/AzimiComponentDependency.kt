package com.azimi.guardian

/**
 * Declares a relationship in which one AZIMI component
 * identifies another component as a dependency.
 *
 * This is architectural metadata only.
 *
 * Declaring a dependency does not:
 *
 * - resolve the dependency
 * - register the dependency
 * - start the dependency
 * - stop the dependency
 * - enforce the dependency
 * - execute recovery
 * - grant security authority
 */
data class AzimiComponentDependency(

    /**
     * Component declaring the dependency.
     */
    val componentId: String,

    /**
     * Component identified as the dependency.
     */
    val dependencyComponentId: String,

    /**
     * Human-readable reason for the relationship.
     */
    val purpose: String = "NONE",

    /**
     * Whether the dependency is required for the
     * declaring component's full operation.
     */
    val required: Boolean = false
) {

    /**
     * Validates the architectural identities.
     */
    fun isValid(): Boolean {

        val source =
            componentId.trim()

        val dependency =
            dependencyComponentId.trim()

        return source.isNotEmpty() &&
            dependency.isNotEmpty() &&
            source != dependency
    }

    /**
     * Returns a normalized copy of this declaration.
     */
    fun normalized(): AzimiComponentDependency {

        return copy(
            componentId =
                componentId.trim(),

            dependencyComponentId =
                dependencyComponentId.trim(),

            purpose =
                purpose.trim().ifEmpty {
                    "NONE"
                }
        )
    }
}
