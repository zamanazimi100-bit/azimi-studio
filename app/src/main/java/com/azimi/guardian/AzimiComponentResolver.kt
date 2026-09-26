package com.azimi.guardian

/**
 * Resolves AZIMI components from registered providers.
 *
 * Resolution is intentionally separate from:
 *
 * - component registration
 * - component lifecycle
 * - recovery
 * - diagnostics
 * - security authority
 *
 * A resolver only obtains a component and verifies that
 * the returned component belongs to the requested provider
 * identity.
 */
object AzimiComponentResolver {

    /**
     * Resolves a component using a registered provider.
     *
     * Returns null when:
     *
     * - the component ID is empty
     * - no provider exists
     * - the provider fails
     * - the provider returns null
     * - the returned component has a different ID
     */
    fun resolve(
        componentId: String
    ): AzimiComponent? {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return null
        }

        val provider =
            AzimiComponentProviderRegistry.get(id)
                ?: return null

        return runCatching {

            val component =
                provider.provide()
                    ?: return null

            val providedId =
                component.componentId.trim()

            if (providedId != id) {
                return null
            }

            component

        }.getOrNull()
    }

    /**
     * Resolves all currently registered providers.
     *
     * Only successfully resolved components are returned.
     */
    fun resolveAll(): List<AzimiComponent> {

        return AzimiComponentProviderRegistry
            .all()
            .mapNotNull { provider ->

                resolve(
                    provider.componentId
                )
            }
    }

    /**
     * Checks whether a registered provider can currently
     * resolve a valid component.
     */
    fun canResolve(
        componentId: String
    ): Boolean {

        return resolve(componentId) != null
    }
}
