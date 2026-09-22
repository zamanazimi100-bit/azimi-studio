package com.azimi.guardian

/**
 * AZIMI Atlas Operating Modes
 *
 * Defines how Atlas is currently operating.
 *
 * Atlas must always know whether it is:
 * - Using online intelligence
 * - Using local/offline intelligence
 * - Using both
 * - Restricted by security policy
 * - Temporarily unavailable
 *
 * This file contains only safe state definitions.
 * It does not connect to the internet or execute actions.
 */
enum class AtlasMode {

    /**
     * Atlas is using an online AI provider.
     *
     * Internet connectivity and a valid authenticated
     * session may be required.
     */
    ONLINE,

    /**
     * Atlas is operating with local capabilities only.
     *
     * No internet connection is required.
     */
    OFFLINE,

    /**
     * Atlas can use both local capabilities and online
     * AI providers depending on the request.
     */
    HYBRID,

    /**
     * Atlas is available only within restricted
     * Guardian security boundaries.
     *
     * External access and consequential actions
     * must remain blocked.
     */
    RESTRICTED,

    /**
     * Atlas cannot currently provide intelligence.
     *
     * This should be used only when no valid local,
     * online, or restricted capability is available.
     */
    UNAVAILABLE
}

/**
 * Describes the reason behind Atlas's current mode.
 */
enum class AtlasModeReason {

    ONLINE_CONNECTION_AVAILABLE,

    OFFLINE_OPERATION,

    ONLINE_AND_LOCAL_AVAILABLE,

    GUARDIAN_SECURITY_POLICY,

    AUTHENTICATION_REQUIRED,

    INTERNET_UNAVAILABLE,
    
    WEAK_NETWORK_DETECTED,

    LOCAL_ENGINE_UNAVAILABLE,

    CAPABILITY_NOT_IMPLEMENTED,

    SYSTEM_ERROR,

    OWNER_RESTRICTION,

    UNKNOWN
}

/**
 * Snapshot of Atlas availability.
 *
 * This is a read-only description of the current
 * operating environment.
 */
data class AtlasAvailability(

    val mode: AtlasMode,

    val reason: AtlasModeReason,

    val internetAvailable: Boolean,

    val authenticated: Boolean,

    val localKnowledgeAvailable: Boolean,

    val localEngineAvailable: Boolean,

    val voiceInputAvailable: Boolean,

    val voiceOutputAvailable: Boolean,

    val externalAIAvailable: Boolean,

    val restrictedByGuardian: Boolean,

    val message: String
) {

    /**
     * Returns true when Atlas can provide at least
     * some useful local response.
     */
    fun canRespondLocally(): Boolean {
        return localKnowledgeAvailable ||
            localEngineAvailable ||
            mode == AtlasMode.RESTRICTED
    }

    /**
     * Returns true when Atlas can contact an
     * external AI engine.
     */
    fun canUseExternalAI(): Boolean {
        return externalAIAvailable &&
            authenticated &&
            internetAvailable &&
            !restrictedByGuardian
    }

    /**
     * Returns true when Atlas can receive speech input.
     */
    fun canUseVoiceInput(): Boolean {
        return voiceInputAvailable
    }

    /**
     * Returns true when Atlas can speak responses aloud.
     */
    fun canUseVoiceOutput(): Boolean {
        return voiceOutputAvailable
    }

    /**
     * Returns a safe human-readable status.
     *
     * This method never exposes tokens, credentials,
     * private keys, or authentication secrets.
     */
    fun toSafeStatusMessage(): String {

        val modeText =
            when (mode) {
                AtlasMode.ONLINE ->
                    "Atlas is operating online."

                AtlasMode.OFFLINE ->
                    "Atlas is operating offline."

                AtlasMode.HYBRID ->
                    "Atlas is operating in hybrid mode."

                AtlasMode.RESTRICTED ->
                    "Atlas is operating under Guardian restrictions."

                AtlasMode.UNAVAILABLE ->
                    "Atlas is currently unavailable."
            }

        val voiceText =
            when {
                voiceInputAvailable && voiceOutputAvailable ->
                    "Voice input and voice output are available."

                voiceInputAvailable ->
                    "Voice input is available. Voice output is unavailable."

                voiceOutputAvailable ->
                    "Voice output is available. Voice input is unavailable."

                else ->
                    "Voice features are currently unavailable."
            }

        return buildString {
            appendLine("ATLAS AVAILABILITY")
            appendLine()
            appendLine(modeText)
            appendLine("REASON: $reason")
            appendLine()
            appendLine(message)
            appendLine()
            appendLine(voiceText)
        }
    }
}

/**
 * Factory helpers for creating safe availability snapshots.
 */
object AtlasModeFactory {

    /**
     * Creates an online availability state.
     */
    fun online(
        authenticated: Boolean,
        localKnowledgeAvailable: Boolean = true,
        localEngineAvailable: Boolean = false,
        voiceInputAvailable: Boolean = false,
        voiceOutputAvailable: Boolean = false
    ): AtlasAvailability {

        return AtlasAvailability(
            mode =
                if (localKnowledgeAvailable || localEngineAvailable) {
                    AtlasMode.HYBRID
                } else {
                    AtlasMode.ONLINE
                },
            reason =
                if (localKnowledgeAvailable || localEngineAvailable) {
                    AtlasModeReason.ONLINE_AND_LOCAL_AVAILABLE
                } else {
                    AtlasModeReason.ONLINE_CONNECTION_AVAILABLE
                },
            internetAvailable = true,
            authenticated = authenticated,
            localKnowledgeAvailable = localKnowledgeAvailable,
            localEngineAvailable = localEngineAvailable,
            voiceInputAvailable = voiceInputAvailable,
            voiceOutputAvailable = voiceOutputAvailable,
            externalAIAvailable = authenticated,
            restrictedByGuardian = false,
            message =
                if (authenticated) {
                    "Online AI access is available through the configured adapter."
                } else {
                    "Online connectivity exists, but authentication is required."
                }
        )
    }

    /**
     * Creates an offline availability state.
     */
    fun offline(
        localKnowledgeAvailable: Boolean = true,
        localEngineAvailable: Boolean = false,
        voiceInputAvailable: Boolean = false,
        voiceOutputAvailable: Boolean = false
    ): AtlasAvailability {

        return AtlasAvailability(
            mode = AtlasMode.OFFLINE,
            reason =
                if (localKnowledgeAvailable || localEngineAvailable) {
                    AtlasModeReason.OFFLINE_OPERATION
                } else {
                    AtlasModeReason.LOCAL_ENGINE_UNAVAILABLE
                },
            internetAvailable = false,
            authenticated = false,
            localKnowledgeAvailable = localKnowledgeAvailable,
            localEngineAvailable = localEngineAvailable,
            voiceInputAvailable = voiceInputAvailable,
            voiceOutputAvailable = voiceOutputAvailable,
            externalAIAvailable = false,
            restrictedByGuardian = false,
            message =
                if (localKnowledgeAvailable || localEngineAvailable) {
                    "Atlas can provide local capabilities without internet access."
                } else {
                    "No offline intelligence capability is currently available."
                }
        )
    }

    /**
     * Creates a restricted availability state.
     */
    fun restricted(
        reason: AtlasModeReason = AtlasModeReason.GUARDIAN_SECURITY_POLICY,
        message: String =
            "Atlas is restricted by Guardian security policy."
    ): AtlasAvailability {

        return AtlasAvailability(
            mode = AtlasMode.RESTRICTED,
            reason = reason,
            internetAvailable = false,
            authenticated = false,
            localKnowledgeAvailable = true,
            localEngineAvailable = false,
            voiceInputAvailable = false,
            voiceOutputAvailable = false,
            externalAIAvailable = false,
            restrictedByGuardian = true,
            message = message
        )
    }

    /**
     * Creates an unavailable availability state.
     */
    fun unavailable(
        reason: AtlasModeReason = AtlasModeReason.SYSTEM_ERROR,
        message: String =
            "Atlas cannot currently provide an available intelligence path."
    ): AtlasAvailability {

        return AtlasAvailability(
            mode = AtlasMode.UNAVAILABLE,
            reason = reason,
            internetAvailable = false,
            authenticated = false,
            localKnowledgeAvailable = false,
            localEngineAvailable = false,
            voiceInputAvailable = false,
            voiceOutputAvailable = false,
            externalAIAvailable = false,
            restrictedByGuardian = false,
            message = message
        )
    }
}
