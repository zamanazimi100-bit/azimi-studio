package com.azimi.guardian

/**
 * Resolves declared AZIMI capabilities to currently
 * registered component instances.
 *
 * Resolution is observational only.
 *
 * It does not:
 *
 * - execute capabilities
 * - grant authorization
 * - start components
 * - stop components
 * - resolve external providers
 * - execute recovery
 * - change security authority
 */
object AzimiCapabilityResolver {

    /**
     * Resolves a capability without requiring a specific
     * provider component.
     *
     * The first currently registered component that declares
     * the capability and is available in the component registry
     * is returned.
     */
    fun resolve(
        capabilityId: String
    ): AzimiCapabilityResolutionResult {

        val capability =
            capabilityId.trim()

        if (capability.isEmpty()) {
            return AzimiCapabilityResolutionResult(
                capabilityId = capabilityId,
                state =
                    AzimiCapabilityResolutionState
                        .INVALID_CAPABILITY_ID,
                message =
                    "INVALID_CAPABILITY_ID"
            )
        }

        val declarations =
            AzimiComponentCapabilityRegistry
                .providersOf(capability)

        if (declarations.isEmpty()) {
            return AzimiCapabilityResolutionResult(
                capabilityId = capability,
                state =
                    AzimiCapabilityResolutionState
                        .NOT_DECLARED,
                message =
                    "CAPABILITY_NOT_DECLARED"
            )
        }

        for (declaration in declarations) {

            val component =
                AzimiComponentRegistry.get(
                    declaration.componentId
                )

            if (component != null) {

                return AzimiCapabilityResolutionResult(
                    capabilityId = capability,
                    requestedComponentId =
                        declaration.componentId,
                    state =
                        AzimiCapabilityResolutionState
                            .AVAILABLE,
                    providerComponent =
                        component,
                    message =
                        "CAPABILITY_AVAILABLE"
                )
            }
        }

        return AzimiCapabilityResolutionResult(
            capabilityId = capability,
            state =
                AzimiCapabilityResolutionState
                    .PROVIDER_COMPONENT_UNAVAILABLE,
            message =
                "PROVIDER_COMPONENT_UNAVAILABLE"
        )
    }

    /**
     * Resolves a capability from a specific component.
     */
    fun resolveFromComponent(
        capabilityId: String,
        componentId: String
    ): AzimiCapabilityResolutionResult {

        val capability =
            capabilityId.trim()

        val component =
            componentId.trim()

        if (capability.isEmpty()) {
            return AzimiCapabilityResolutionResult(
                capabilityId = capabilityId,
                requestedComponentId = componentId,
                state =
                    AzimiCapabilityResolutionState
                        .INVALID_CAPABILITY_ID,
                message =
                    "INVALID_CAPABILITY_ID"
            )
        }

        if (component.isEmpty()) {
            return AzimiCapabilityResolutionResult(
                capabilityId = capability,
                requestedComponentId = componentId,
                state =
                    AzimiCapabilityResolutionState
                        .INVALID_COMPONENT_ID,
                message =
                    "INVALID_COMPONENT_ID"
            )
        }

        if (
            !AzimiComponentCapabilityRegistry.contains(
                component,
                capability
            )
        ) {
            return AzimiCapabilityResolutionResult(
                capabilityId = capability,
                requestedComponentId = component,
                state =
                    AzimiCapabilityResolutionState
                        .NOT_DECLARED,
                message =
                    "CAPABILITY_NOT_DECLARED"
            )
        }

        val provider =
            AzimiComponentRegistry.get(
                component
            )

        if (provider == null) {
            return AzimiCapabilityResolutionResult(
                capabilityId = capability,
                requestedComponentId = component,
                state =
                    AzimiCapabilityResolutionState
                        .PROVIDER_COMPONENT_UNAVAILABLE,
                message =
                    "PROVIDER_COMPONENT_UNAVAILABLE"
            )
        }

        return AzimiCapabilityResolutionResult(
            capabilityId = capability,
            requestedComponentId = component,
            state =
                AzimiCapabilityResolutionState
                    .AVAILABLE,
            providerComponent = provider,
            message =
                "CAPABILITY_AVAILABLE"
        )
    }

    /**
     * Returns every currently available provider for
     * the requested capability.
     */
    fun resolveAll(
        capabilityId: String
    ): List<AzimiCapabilityResolutionResult> {

        val capability =
            capabilityId.trim()

        if (capability.isEmpty()) {
            return emptyList()
        }

        return AzimiComponentCapabilityRegistry
            .providersOf(capability)
            .map { declaration ->

                resolveFromComponent(
                    capability,
                    declaration.componentId
                )
            }
            .filter {
                it.isAvailable()
            }
    }

    /**
     * Checks whether a usable registered provider currently
     * exists for the requested capability.
     */
    fun canResolve(
        capabilityId: String
    ): Boolean {

        return resolve(
            capabilityId
        ).isAvailable()
    }
}
