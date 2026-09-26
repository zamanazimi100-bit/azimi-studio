package com.azimi.guardian

/**
 * Resolves an executable AZIMI capability operation.
 *
 * Resolution is intentionally separate from:
 * - authentication
 * - authorization
 * - execution
 *
 * The resolver only determines whether a registered operation
 * can safely be associated with the requested capability ID.
 */
object AzimiCapabilityOperationResolver {

    /**
     * Resolves the operation registered for a capability.
     *
     * Resolution succeeds only when:
     *
     * 1. The capability ID is non-empty.
     * 2. A registered operation exists.
     * 3. The operation's own capability ID matches the
     *    requested capability ID.
     */
    fun resolve(
        capabilityId: String
    ): AzimiCapabilityOperation? {

        val id =
            capabilityId.trim()

        if (id.isEmpty()) {
            return null
        }

        val operation =
            AzimiCapabilityOperationRegistry.get(
                id
            )
            ?: return null

        val operationId =
            operation.capabilityId.trim()

        if (operationId != id) {
            return null
        }

        return operation
    }

    /**
     * Returns whether an executable operation can be resolved
     * for the supplied capability ID.
     */
    fun canResolve(
        capabilityId: String
    ): Boolean {

        return resolve(
            capabilityId
        ) != null
    }

    /**
     * Resolves all currently registered operations.
     *
     * Only operations whose capability IDs are valid and
     * internally consistent are returned.
     */
    fun resolveAll():
        List<AzimiCapabilityOperation> {

        return AzimiCapabilityOperationRegistry
            .all()
            .filter { operation ->

                val id =
                    operation.capabilityId.trim()

                id.isNotEmpty() &&
                    AzimiCapabilityOperationRegistry
                        .get(id) === operation
            }
    }
}
