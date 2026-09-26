package com.azimi.guardian

/**
 * Describes a relationship between two AZIMI components.
 *
 * A connection is only a declared relationship.
 * It does not execute actions, grant authority,
 * bypass security, or create dependencies automatically.
 */
data class AzimiComponentConnection(

    /**
     * Stable ID of the component initiating the connection.
     */
    val sourceComponentId: String,

    /**
     * Stable ID of the component being connected to.
     */
    val targetComponentId: String,

    /**
     * Optional human-readable purpose of the connection.
     */
    val purpose: String = "NONE"
) {

    /**
     * Returns true when the connection contains
     * usable component identifiers.
     */
    fun isValid(): Boolean {

        return sourceComponentId
            .trim()
            .isNotEmpty() &&
            targetComponentId
                .trim()
                .isNotEmpty()
    }
}
