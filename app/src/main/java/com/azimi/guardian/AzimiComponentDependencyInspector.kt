package com.azimi.guardian

/**
 * Passively inspects declared AZIMI component dependencies.
 *
 * This inspector converts dependency declarations into
 * explicit dependency results.
 *
 * It does not:
 *
 * - start components
 * - stop components
 * - resolve providers
 * - enforce dependencies
 * - execute recovery
 * - change security authority
 */
object AzimiComponentDependencyInspector {

    /**
     * Inspects one declared dependency.
     *
     * A dependency must already be declared in the
     * dependency registry.
     */
    fun inspect(
        componentId: String,
        dependencyComponentId: String
    ): AzimiComponentDependencyResult {

        val source =
            componentId.trim()

        val dependencyId =
            dependencyComponentId.trim()

        if (source.isEmpty() ||
            dependencyId.isEmpty() ||
            source == dependencyId
        ) {

            return AzimiComponentDependencyResult(
                componentId = componentId,
                dependencyComponentId =
                    dependencyComponentId,
                state =
                    AzimiComponentDependencyState
                        .INVALID_COMPONENT_ID,
                message =
                    "INVALID_COMPONENT_ID"
            )
        }

        if (!AzimiComponentRegistry.contains(source)) {

            return AzimiComponentDependencyResult(
                componentId = source,
                dependencyComponentId =
                    dependencyId,
                state =
                    AzimiComponentDependencyState
                        .COMPONENT_NOT_REGISTERED,
                message =
                    "COMPONENT_NOT_REGISTERED"
            )
        }

        val declaration =
            AzimiComponentDependencyRegistry.get(
                source,
                dependencyId
            )

        if (declaration == null) {

            return AzimiComponentDependencyResult(
                componentId = source,
                dependencyComponentId =
                    dependencyId,
                state =
                    AzimiComponentDependencyState
                        .NOT_DECLARED,
                message =
                    "DEPENDENCY_NOT_DECLARED"
            )
        }

        val dependency =
            AzimiComponentRegistry.get(
                dependencyId
            )

        if (dependency == null) {

            return AzimiComponentDependencyResult(
                componentId = source,
                dependencyComponentId =
                    dependencyId,
                required =
                    declaration.required,
                state =
                    AzimiComponentDependencyState
                        .UNAVAILABLE,
                message =
                    "DEPENDENCY_UNAVAILABLE"
            )
        }

        return AzimiComponentDependencyResult(
            componentId = source,
            dependencyComponentId =
                dependencyId,
            required =
                declaration.required,
            state =
                AzimiComponentDependencyState
                    .AVAILABLE,
            dependency = dependency,
            message =
                "DEPENDENCY_AVAILABLE"
        )
    }

    /**
     * Inspects every dependency declared by one component.
     */
    fun inspectForComponent(
        componentId: String
    ): List<AzimiComponentDependencyResult> {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return emptyList()
        }

        return AzimiComponentDependencyRegistry
            .forComponent(id)
            .map { dependency ->

                inspect(
                    dependency.componentId,
                    dependency.dependencyComponentId
                )
            }
    }

    /**
     * Inspects all declared component dependencies.
     */
    fun inspectAll():
        List<AzimiComponentDependencyResult> {

        return AzimiComponentDependencyRegistry
            .all()
            .map { dependency ->

                inspect(
                    dependency.componentId,
                    dependency.dependencyComponentId
                )
            }
    }

    /**
     * Returns only required dependencies that are
     * currently unavailable.
     */
    fun unavailableRequiredForComponent(
        componentId: String
    ): List<AzimiComponentDependencyResult> {

        return inspectForComponent(componentId)
            .filter {
                it.required &&
                    it.state ==
                    AzimiComponentDependencyState
                        .UNAVAILABLE
            }
    }
}
