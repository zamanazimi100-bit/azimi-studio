package com.azimi.guardian

/**
 * Registry for declared AZIMI component connections.
 *
 * The registry stores relationships only.
 * It does not execute connected components,
 * grant permissions, bypass security, or perform actions.
 */
object AzimiConnectionRegistry {

    private val connections =
        LinkedHashMap<String, AzimiComponentConnection>()

    /**
     * Registers a valid component connection.
     *
     * The connection ID is generated from the source,
     * target, and purpose so the same relationship can
     * be safely registered more than once.
     */
    @Synchronized
    fun register(
        connection: AzimiComponentConnection
    ): Boolean {

        if (!connection.isValid()) {
            return false
        }

        val source =
            connection.sourceComponentId.trim()

        val target =
            connection.targetComponentId.trim()

        val purpose =
            connection.purpose.trim()

        val id =
            buildConnectionId(
                source,
                target,
                purpose
            )

        connections[id] =
            connection.copy(
                sourceComponentId = source,
                targetComponentId = target,
                purpose =
                    if (purpose.isEmpty()) {
                        "NONE"
                    } else {
                        purpose
                    }
            )

        return true
    }

    /**
     * Removes a declared connection.
     */
    @Synchronized
    fun unregister(
        sourceComponentId: String,
        targetComponentId: String,
        purpose: String = "NONE"
    ): Boolean {

        val id =
            buildConnectionId(
                sourceComponentId.trim(),
                targetComponentId.trim(),
                purpose.trim()
            )

        return connections.remove(id) != null
    }

    /**
     * Finds one declared connection.
     */
    @Synchronized
    fun get(
        sourceComponentId: String,
        targetComponentId: String,
        purpose: String = "NONE"
    ): AzimiComponentConnection? {

        val id =
            buildConnectionId(
                sourceComponentId.trim(),
                targetComponentId.trim(),
                purpose.trim()
            )

        return connections[id]
    }

    /**
     * Returns all declared connections as a snapshot.
     */
    @Synchronized
    fun all(): List<AzimiComponentConnection> {
        return connections.values.toList()
    }

    /**
     * Returns all connections originating from a component.
     */
    @Synchronized
    fun from(
        sourceComponentId: String
    ): List<AzimiComponentConnection> {

        val source =
            sourceComponentId.trim()

        if (source.isEmpty()) {
            return emptyList()
        }

        return connections.values
            .filter {
                it.sourceComponentId == source
            }
    }

    /**
     * Returns all connections targeting a component.
     */
    @Synchronized
    fun to(
        targetComponentId: String
    ): List<AzimiComponentConnection> {

        val target =
            targetComponentId.trim()

        if (target.isEmpty()) {
            return emptyList()
        }

        return connections.values
            .filter {
                it.targetComponentId == target
            }
    }

    /**
     * Checks whether a declared connection exists.
     */
    @Synchronized
    fun contains(
        sourceComponentId: String,
        targetComponentId: String,
        purpose: String = "NONE"
    ): Boolean {

        return get(
            sourceComponentId,
            targetComponentId,
            purpose
        ) != null
    }

    /**
     * Returns the number of declared connections.
     */
    @Synchronized
    fun size(): Int {
        return connections.size
    }

    /**
     * Removes every declared connection.
     *
     * This affects only registry state.
     * It does not stop or modify components.
     */
    @Synchronized
    fun clear() {
        connections.clear()
    }

    /**
     * Creates a stable registry key for a connection.
     */
    private fun buildConnectionId(
        sourceComponentId: String,
        targetComponentId: String,
        purpose: String
    ): String {

        return "$sourceComponentId->$targetComponentId:$purpose"
    }
}
