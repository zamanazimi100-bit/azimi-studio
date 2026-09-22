package com.azimi.guardian

/**
 * AZIMI Atlas Operating Modes
 *
 * Defines how Atlas is currently operating.
 *
 * This file contains state definitions and the mode factory.
 * It does not perform network operations, execute actions,
 * store credentials, or bypass Guardian security.
 */
enum class AtlasMode {
    ONLINE,
    OFFLINE,
    HYBRID,
    RESTRICTED,
    UNAVAILABLE
}

/**
 * Describes why Atlas currently has a particular mode.
 */
enum class AtlasModeReason {
    ONLINE_CONNECTION_AVAILABLE,
    OFFLINE_OPERATION,
    ONLINE_AND_LOCAL_AVAILABLE,
    WEAK_NETWORK_DETECTED,
    GUARDIAN_SECURITY_POLICY,
    AUTHENTICATION_REQUIRED,
    INTERNET_UNAVAILABLE,
    LOCAL_ENGINE_UNAVAILABLE,
    CAPABILITY_NOT_IMPLEMENTED,
    SYSTEM_ERROR,
    OWNER_RESTRICTION,
    UNKNOWN
}

/**
 * Determines Atlas operating mode from its currently detected capabilities.
 *
 * This factory contains no Android/network calls.
 * AtlasAvailability remains responsible for detection.
 */
object AtlasModeFactory {

    fun determine(
        internetAvailable: Boolean,
        authenticated: Boolean,
        localKnowledgeAvailable: Boolean,
        localEngineAvailable: Boolean,
        restrictedByGuardian: Boolean,
        networkWeak: Boolean = false
    ): AtlasMode {

        if (restrictedByGuardian) {
            return AtlasMode.RESTRICTED
        }

        if (!authenticated) {
            return AtlasMode.RESTRICTED
        }

        val localAvailable =
            localKnowledgeAvailable || localEngineAvailable

        return when {
            internetAvailable && localAvailable ->
                AtlasMode.HYBRID

            internetAvailable && networkWeak ->
                AtlasMode.HYBRID

            internetAvailable ->
                AtlasMode.ONLINE

            localAvailable ->
                AtlasMode.OFFLINE

            else ->
                AtlasMode.UNAVAILABLE
        }
    }

    fun determineReason(
        internetAvailable: Boolean,
        authenticated: Boolean,
        localKnowledgeAvailable: Boolean,
        localEngineAvailable: Boolean,
        restrictedByGuardian: Boolean,
        networkWeak: Boolean = false
    ): AtlasModeReason {

        if (restrictedByGuardian) {
            return AtlasModeReason.GUARDIAN_SECURITY_POLICY
        }

        if (!authenticated) {
            return AtlasModeReason.AUTHENTICATION_REQUIRED
        }

        val localAvailable =
            localKnowledgeAvailable || localEngineAvailable

        return when {
            internetAvailable && networkWeak ->
                AtlasModeReason.WEAK_NETWORK_DETECTED

            internetAvailable && localAvailable ->
                AtlasModeReason.ONLINE_AND_LOCAL_AVAILABLE

            internetAvailable ->
                AtlasModeReason.ONLINE_CONNECTION_AVAILABLE

            localAvailable ->
                AtlasModeReason.OFFLINE_OPERATION

            else ->
                AtlasModeReason.LOCAL_ENGINE_UNAVAILABLE
        }
    }
}
