package com.azimi.guardian

/**
 * Coordinates passive AZIMI capability inspection.
 *
 * This coordinator provides a stable boundary above the
 * capability resolver.
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
object AzimiCapabilityCoordinator {

    /**
     * Resolves one capability.
     */
    fun resolve(
        capabilityId: String
    ): AzimiCapabilityResolutionResult {

        return AzimiCapabilityResolver.resolve(
            capabilityId
        )
    }

    /**
     * Resolves a capability from a specific component.
     */
    fun resolveFromComponent(
        capabilityId: String,
        componentId: String
    ): AzimiCapabilityResolutionResult {

        return AzimiCapabilityResolver
            .resolveFromComponent(
                capabilityId,
                componentId
            )
    }

    /**
     * Returns every currently available provider
     * for a capability.
     */
    fun resolveAll(
        capabilityId: String
    ): List<AzimiCapabilityResolutionResult> {

        return AzimiCapabilityResolver.resolveAll(
            capabilityId
        )
    }

    /**
     * Checks whether a usable provider currently exists
     * for the requested capability.
     */
    fun isAvailable(
        capabilityId: String
    ): Boolean {

        return AzimiCapabilityResolver.canResolve(
            capabilityId
        )
    }

    /**
     * Returns the number of currently registered
     * capability declarations.
     */
    fun declarationCount(): Int {

        return AzimiComponentCapabilityRegistry.size()
    }
}
