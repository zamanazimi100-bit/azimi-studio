package com.azimi.guardian

/**
 * Coordinates passive AZIMI component dependency inspection.
 *
 * This coordinator provides a stable boundary above the
 * dependency inspector.
 *
 * It does not:
 *
 * - start components
 * - stop components
 * - resolve providers
 * - enforce dependencies
 * - execute recovery
 * - change security authority
 * - execute consequential actions
 */
object AzimiComponentDependencyCoordinator {

    /**
     * Inspects one dependency relationship.
     */
    fun inspect(
        componentId: String,
        dependencyComponentId: String
    ): AzimiComponentDependencyResult {

        return AzimiComponentDependencyInspector.inspect(
            componentId,
            dependencyComponentId
        )
    }

    /**
     * Inspects every dependency declared by one component.
     */
    fun inspectForComponent(
        componentId: String
    ): List<AzimiComponentDependencyResult> {

        return AzimiComponentDependencyInspector
            .inspectForComponent(
                componentId
            )
    }

    /**
     * Inspects all declared dependencies in the system.
     */
    fun inspectAll():
        List<AzimiComponentDependencyResult> {

        return AzimiComponentDependencyInspector
            .inspectAll()
    }

    /**
     * Returns required dependencies that are currently
     * unavailable for a component.
     */
    fun unavailableRequiredForComponent(
        componentId: String
    ): List<AzimiComponentDependencyResult> {

        return AzimiComponentDependencyInspector
            .unavailableRequiredForComponent(
                componentId
            )
    }

    /**
     * Returns true when every required dependency declared
     * by the component is currently available.
     *
     * A component with no required dependencies is considered
     * dependency-ready.
     */
    fun requiredDependenciesAvailable(
        componentId: String
    ): Boolean {

        val results =
            inspectForComponent(componentId)

        return results.none {
            it.required &&
                it.state !=
                AzimiComponentDependencyState
                    .AVAILABLE
        }
    }
}
