package com.azimi.guardian

/**
 * Registry of executable AZIMI capability operations.
 *
 * The registry provides lookup and lifecycle-independent
 * registration of capability operations.
 *
 * It does not:
 * - authorize requests
 * - authenticate requesters
 * - execute operations
 * - grant permissions
 * - elevate authority
 */
object AzimiCapabilityOperationRegistry {

    private val operations =
        LinkedHashMap<String, AzimiCapabilityOperation>()

    /**
     * Registers an operation.
     *
     * The operation identifier is normalized before storage.
     * An existing operation with the same capability ID is
     * replaced intentionally.
     */
    @Synchronized
    fun register(
        operation: AzimiCapabilityOperation
    ): Boolean {

        val capabilityId =
            operation.capabilityId.trim()

        if (capabilityId.isEmpty()) {
            return false
        }

        operations[capabilityId] =
            operation

        return true
    }

    /**
     * Removes an operation by capability ID.
     */
    @Synchronized
    fun unregister(
        capabilityId: String
    ): Boolean {

        val id =
            capabilityId.trim()

        if (id.isEmpty()) {
            return false
        }

        return operations.remove(id) != null
    }

    /**
     * Resolves an operation by capability ID.
     */
    @Synchronized
    fun get(
        capabilityId: String
    ): AzimiCapabilityOperation? {

        val id =
            capabilityId.trim()

        if (id.isEmpty()) {
            return null
        }

        return operations[id]
    }

    /**
     * Returns a snapshot of all registered operations.
     */
    @Synchronized
    fun all():
        List<AzimiCapabilityOperation> {

        return operations.values.toList()
    }

    /**
     * Returns whether an operation is registered.
     */
    @Synchronized
    fun contains(
        capabilityId: String
    ): Boolean {

        val id =
            capabilityId.trim()

        if (id.isEmpty()) {
            return false
        }

        return operations.containsKey(id)
    }

    /**
     * Returns the number of registered operations.
     */
    @Synchronized
    fun size(): Int {
        return operations.size
    }

    /**
     * Clears all registered operations.
     */
    @Synchronized
    fun clear() {
        operations.clear()
    }
}
