package com.azimi.guardian

/**
 * Canonical component identities for the AZIMI architecture.
 *
 * These identifiers are stable machine-readable names.
 * They are used to describe components and relationships
 * without coupling components to each other's implementation.
 *
 * Changing an existing identifier can break stored or declared
 * relationships, so identifiers should be treated as architectural
 * contracts.
 */
object AzimiComponentIds {

    /**
     * Central Atlas intelligence component.
     */
    const val ATLAS =
        "ATLAS"

    /**
     * Protected AZIMI Vault component.
     */
    const val Z_VAULT =
        "Z_VAULT"

    /**
     * Security and protection boundary.
     */
    const val Z_SHIELD =
        "Z_SHIELD"

    /**
     * Recovery and continuity component.
     */
    const val Z_RECOVERY =
        "Z_RECOVERY"

    /**
     * Owner authority and origin component.
     */
    const val Z_ORIGIN =
        "Z_ORIGIN"

    /**
     * Network and connectivity component.
     */
    const val Z_CONNECT =
        "Z_CONNECT"

    /**
     * Cloud infrastructure component.
     */
    const val Z_CLOUD =
        "Z_CLOUD"

    /**
     * User control and system command component.
     */
    const val Z_CONTROL =
        "Z_CONTROL"

    /**
     * Launcher and AZIMI entry-point component.
     */
    const val Z_LAUNCHER =
        "Z_LAUNCHER"

    /**
     * Personal AI component.
     */
    const val Z_AI =
        "Z_AI"

    /**
     * Main AZIMI Guardian application boundary.
     */
    const val GUARDIAN =
        "GUARDIAN"

    /**
     * Core AZIMI coordination layer.
     */
    const val AZIMI_CORE =
        "AZIMI_CORE"

    /**
     * Returns every canonical component ID.
     *
     * The returned list is a new immutable snapshot.
     */
    fun all(): List<String> {

        return listOf(
            ATLAS,
            Z_VAULT,
            Z_SHIELD,
            Z_RECOVERY,
            Z_ORIGIN,
            Z_CONNECT,
            Z_CLOUD,
            Z_CONTROL,
            Z_LAUNCHER,
            Z_AI,
            GUARDIAN,
            AZIMI_CORE
        )
    }

    /**
     * Checks whether an identifier belongs to the
     * canonical AZIMI component vocabulary.
     */
    fun isKnown(
        componentId: String
    ): Boolean {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        return all().contains(id)
    }
}
