package com.azimi.guardian

/**
 * Coordinates AZIMI component resolution.
 *
 * This coordinator connects the provider registry with the
 * component resolver and converts resolution into an explicit
 * architectural result.
 *
 * It does not:
 *
 * - register components
 * - start or stop components
 * - execute recovery
 * - change security authority
 * - execute consequential actions
 */
object AzimiComponentResolutionCoordinator {

    /**
     * Resolves one component and returns an explicit result.
     */
    fun resolve(
        componentId: String
    ): AzimiComponentResolutionResult {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return AzimiComponentResolutionResult(
                requestedComponentId = componentId,
                state =
                    AzimiComponentResolutionState
                        .INVALID_COMPONENT_ID,
                message = "INVALID_COMPONENT_ID"
            )
        }

        val provider =
            AzimiComponentProviderRegistry.get(id)

        if (provider == null) {
            return AzimiComponentResolutionResult(
                requestedComponentId = id,
                state =
                    AzimiComponentResolutionState
                        .PROVIDER_NOT_FOUND,
                message = "PROVIDER_NOT_FOUND"
            )
        }

        return try {

            val component =
                provider.provide()

            if (component == null) {
                AzimiComponentResolutionResult(
                    requestedComponentId = id,
                    state =
                        AzimiComponentResolutionState
                            .PROVIDER_UNAVAILABLE,
                    message = "PROVIDER_UNAVAILABLE"
                )

            } else {

                val providedId =
                    component.componentId.trim()

                if (providedId != id) {

                    AzimiComponentResolutionResult(
                        requestedComponentId = id,
                        state =
                            AzimiComponentResolutionState
                                .COMPONENT_ID_MISMATCH,
                        component = null,
                        message =
                            "COMPONENT_ID_MISMATCH"
                    )

                } else {

                    AzimiComponentResolutionResult(
                        requestedComponentId = id,
                        state =
                            AzimiComponentResolutionState
                                .RESOLVED,
                        component = component,
                        message =
                            "COMPONENT_RESOLVED"
                    )
                }
            }

        } catch (throwable: Throwable) {

            AzimiComponentResolutionResult(
                requestedComponentId = id,
                state =
                    AzimiComponentResolutionState
                        .PROVIDER_FAILURE,
                message =
                    throwable.message
                        ?.take(500)
                        ?: "PROVIDER_FAILURE"
            )
        }
    }

    /**
     * Resolves all currently registered providers.
     *
     * Every provider receives an explicit result.
     */
    fun resolveAll():
        List<AzimiComponentResolutionResult> {

        return AzimiComponentProviderRegistry
            .all()
            .map { provider ->

                resolve(
                    provider.componentId
                )
            }
    }

    /**
     * Checks whether a component can currently be resolved.
     */
    fun isResolvable(
        componentId: String
    ): Boolean {

        return resolve(componentId)
            .isResolved()
    }
}
