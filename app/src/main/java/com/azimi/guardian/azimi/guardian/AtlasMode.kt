package com.azimi.guardian

/**
 * AZIMI Atlas Operating Modes
 *
 * Defines how Atlas is currently operating.
 *
 * This file contains only state definitions.
 *
 * It does not:
 * - connect to the internet
 * - execute actions
 * - store credentials
 * - access private data
 * - bypass Guardian
 */
enum class AtlasMode {

    /**
     * Atlas is using an authenticated online AI path.
     */
    ONLINE,

    /**
     * Atlas is operating using local capabilities only.
     */
    OFFLINE,

    /**
     * Atlas can use both local and online capabilities.
     */
    HYBRID,

    /**
     * Atlas is operating under Guardian restrictions.
     *
     * External access and consequential actions remain blocked.
     */
    RESTRICTED,

    /**
     * Atlas currently has no usable intelligence path.
     */
    UNAVAILABLE
}

/**
 * Describes why Atlas currently has a particular mode.
 */
enum class AtlasModeReason {

    /**
     * A usable internet connection exists.
     */
    ONLINE_CONNECTION_AVAILABLE,

    /**
     * Atlas is intentionally operating locally.
     */
    OFFLINE_OPERATION,

    /**
     * Local and online intelligence paths are available.
     */
    ONLINE_AND_LOCAL_AVAILABLE,

    /**
     * Android reports that the network is weak.
     */
    WEAK_NETWORK_DETECTED,

    /**
     * Guardian security policy has restricted Atlas.
     */
    GUARDIAN_SECURITY_POLICY,

    /**
     * Online AI requires authentication.
     */
    AUTHENTICATION_REQUIRED,

    /**
     * No usable internet connection exists.
     */
    INTERNET_UNAVAILABLE,

    /**
     * A local intelligence engine is unavailable.
     */
    LOCAL_ENGINE_UNAVAILABLE,

    /**
     * A requested capability has not yet been implemented.
     */
    CAPABILITY_NOT_IMPLEMENTED,

    /**
     * An unexpected system error occurred.
     */
    SYSTEM_ERROR,

    /**
     * Atlas has been restricted by the owner/security policy.
     */
    OWNER_RESTRICTION,

    /**
     * The reason could not be determined.
     */
    UNKNOWN
}
