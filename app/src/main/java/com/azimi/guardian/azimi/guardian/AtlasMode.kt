package com.azimi.guardian

/**
 * AZIMI Atlas Operating Modes
 *
 * Describes how Atlas can currently operate.
 *
 * Authentication controls access to the external AI path.
 * It must not unnecessarily disable local Atlas capabilities.
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
 * Determines Atlas operating mode from detected capabilities.
 *
 * This factory performs no Android or network operations.
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

        val localAvailable =
            localKnowledgeAvailable || localEngineAvailable

        /*
         * Authentication is required only for the external AI path.
         *
         * Local Atlas remains usable without authentication.
         */
        return when {
            internetAvailable && localAvailable ->
                AtlasMode.HYBRID

            internetAvailable && networkWeak ->
                AtlasMode.HYBRID

            internetAvailable && authenticated ->
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

        val localAvailable =
            localKnowledgeAvailable || localEngineAvailable

        return when {
            internetAvailable && networkWeak ->
                AtlasModeReason.WEAK_NETWORK_DETECTED

            internetAvailable && localAvailable ->
                AtlasModeReason.ONLINE_AND_LOCAL_AVAILABLE

            internetAvailable && authenticated ->
                AtlasModeReason.ONLINE_CONNECTION_AVAILABLE

            !internetAvailable && localAvailable ->
                AtlasModeReason.OFFLINE_OPERATION

            !authenticated && !localAvailable ->
                AtlasModeReason.AUTHENTICATION_REQUIRED

            else ->
                AtlasModeReason.LOCAL_ENGINE_UNAVAILABLE
        }
    }
}
