package com.azimi.guardian

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech

/**
 * AZIMI Atlas Availability
 *
 * Immutable snapshot of the capabilities currently available
 * to Atlas.
 *
 * The companion object performs READ-ONLY detection.
 *
 * This design intentionally supports both:
 *
 *     AtlasAvailability.detect(context)
 *
 * and:
 *
 *     availability.mode
 *     availability.internetAvailable
 *     availability.localKnowledgeAvailable
 *
 * Atlas does not:
 * - execute actions
 * - change security settings
 * - store credentials
 * - send network requests
 * - contact external AI providers
 * - bypass Guardian
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
    val networkQuality: NetworkQuality,
    val message: String
) {

    /**
     * Describes the locally detected network condition.
     */
    enum class NetworkQuality {
        OFFLINE,
        WEAK,
        NORMAL
    }

    /**
     * Determines whether Atlas has a local response path.
     */
    fun canRespondLocally(): Boolean {
        return localKnowledgeAvailable ||
            localEngineAvailable
    }

    /**
     * Determines whether the authenticated external
     * intelligence path is currently available.
     *
     * This does NOT contact an external provider.
     */
    fun canUseExternalAI(): Boolean {
        return externalAIAvailable &&
            internetAvailable &&
            authenticated &&
            !restrictedByGuardian
    }

    /**
     * Determines whether voice input can be offered.
     */
    fun canUseVoiceInput(): Boolean {
        return voiceInputAvailable
    }

    /**
     * Determines whether voice output can be offered.
     */
    fun canUseVoiceOutput(): Boolean {
        return voiceOutputAvailable
    }

    /**
     * Determines whether Atlas currently has at least
     * one usable response path.
     */
    fun canRespond(): Boolean {
        return canRespondLocally() ||
            canUseExternalAI()
    }

    /**
     * Safe human-readable status.
     */
    fun toSafeStatusMessage(): String {
        return message
    }

    companion object {

        /**
         * Main Atlas availability detector.
         */
        fun detect(
            context: Context
        ): AtlasAvailability {

            val appContext =
                context.applicationContext

            val internetAvailable =
                isInternetAvailable(appContext)

            val networkQuality =
                getNetworkQuality(appContext)

            val authenticated =
                isAuthenticated(appContext)

            val localKnowledgeAvailable =
                isLocalKnowledgeAvailable()

            val localEngineAvailable =
                isLocalEngineAvailable()

            val voiceInputAvailable =
                isVoiceInputAvailable(appContext)

            val voiceOutputAvailable =
                isVoiceOutputAvailable(appContext)

            val restrictedByGuardian =
                isRestrictedByGuardian(appContext)

            /*
             * Guardian security policy has absolute priority.
             */
            if (restrictedByGuardian) {

                return AtlasAvailability(
                    mode =
                        AtlasMode.RESTRICTED,

                    reason =
                        AtlasModeReason.GUARDIAN_SECURITY_POLICY,

                    internetAvailable =
                        internetAvailable,

                    authenticated =
                        authenticated,

                    localKnowledgeAvailable =
                        localKnowledgeAvailable,

                    localEngineAvailable =
                        localEngineAvailable,

                    voiceInputAvailable =
                        voiceInputAvailable,

                    voiceOutputAvailable =
                        voiceOutputAvailable,

                    externalAIAvailable =
                        false,

                    restrictedByGuardian =
                        true,

                    networkQuality =
                        networkQuality,

                    message =
                        "Atlas is restricted because Guardian AI policy is not SAFE_CONTEXT_ONLY."
                )
            }

            /*
             * External AI is only considered available when
             * both internet and an authenticated session exist.
             */
            val externalAIAvailable =
                internetAvailable &&
                    authenticated

            val localAvailable =
                localKnowledgeAvailable ||
                    localEngineAvailable

            /*
             * Local + online capability.
             */
            if (localAvailable && externalAIAvailable) {

                val weakNetwork =
                    networkQuality ==
                        NetworkQuality.WEAK

                return AtlasAvailability(
                    mode =
                        AtlasMode.HYBRID,

                    reason =
                        if (weakNetwork) {
                            AtlasModeReason.WEAK_NETWORK_DETECTED
                        } else {
                            AtlasModeReason.ONLINE_AND_LOCAL_AVAILABLE
                        },

                    internetAvailable =
                        internetAvailable,

                    authenticated =
                        authenticated,

                    localKnowledgeAvailable =
                        localKnowledgeAvailable,

                    localEngineAvailable =
                        localEngineAvailable,

                    voiceInputAvailable =
                        voiceInputAvailable,

                    voiceOutputAvailable =
                        voiceOutputAvailable,

                    externalAIAvailable =
                        true,

                    restrictedByGuardian =
                        false,

                    networkQuality =
                        networkQuality,

                    message =
                        if (weakNetwork) {
                            "Internet is available but weak. Atlas can use local capabilities while conserving network usage."
                        } else {
                            "Atlas can use local capabilities and the authenticated online AI path."
                        }
                )
            }

            /*
             * Online-only capability.
             */
            if (externalAIAvailable) {

                val weakNetwork =
                    networkQuality ==
                        NetworkQuality.WEAK

                return AtlasAvailability(
                    mode =
                        AtlasMode.ONLINE,

                    reason =
                        if (weakNetwork) {
                            AtlasModeReason.WEAK_NETWORK_DETECTED
                        } else {
                            AtlasModeReason.ONLINE_CONNECTION_AVAILABLE
                        },

                    internetAvailable =
                        internetAvailable,

                    authenticated =
                        authenticated,

                    localKnowledgeAvailable =
                        localKnowledgeAvailable,

                    localEngineAvailable =
                        localEngineAvailable,

                    voiceInputAvailable =
                        voiceInputAvailable,

                    voiceOutputAvailable =
                        voiceOutputAvailable,

                    externalAIAvailable =
                        true,

                    restrictedByGuardian =
                        false,

                    networkQuality =
                        networkQuality,

                    message =
                        if (weakNetwork) {
                            "Online AI is authenticated, but the network appears weak. Atlas should use conservative network behavior."
                        } else {
                            "Atlas can use the authenticated online AI path."
                        }
                )
            }

            /*
             * Local/offline capability.
             */
            if (localAvailable) {

                val reason =
                    when {
                        !internetAvailable ->
                            AtlasModeReason.INTERNET_UNAVAILABLE

                        networkQuality ==
                            NetworkQuality.WEAK ->
                            AtlasModeReason.WEAK_NETWORK_DETECTED

                        !authenticated ->
                            AtlasModeReason.AUTHENTICATION_REQUIRED

                        else ->
                            AtlasModeReason.OFFLINE_OPERATION
                    }

                val status =
                    when {
                        !internetAvailable ->
                            "Internet is unavailable. Atlas can continue using local capabilities."

                        networkQuality ==
                            NetworkQuality.WEAK ->
                            "The network is weak. Atlas can continue locally while conserving network usage."

                        !authenticated ->
                            "Online AI requires authentication. Atlas can continue using local capabilities."

                        else ->
                            "Atlas can continue using local capabilities."
                    }

                return AtlasAvailability(
                    mode =
                        AtlasMode.OFFLINE,

                    reason =
                        reason,

                    internetAvailable =
                        internetAvailable,

                    authenticated =
                        authenticated,

                    localKnowledgeAvailable =
                        localKnowledgeAvailable,

                    localEngineAvailable =
                        localEngineAvailable,

                    voiceInputAvailable =
                        voiceInputAvailable,

                    voiceOutputAvailable =
                        voiceOutputAvailable,

                    externalAIAvailable =
                        false,

                    restrictedByGuardian =
                        false,

                    networkQuality =
                        networkQuality,

                    message =
                        status
                )
            }

            /*
             * Nothing is currently available.
             */
            val unavailableReason =
                when {
                    !internetAvailable ->
                        AtlasModeReason.INTERNET_UNAVAILABLE

                    networkQuality ==
                        NetworkQuality.WEAK ->
                        AtlasModeReason.WEAK_NETWORK_DETECTED

                    !authenticated ->
                        AtlasModeReason.AUTHENTICATION_REQUIRED

                    else ->
                        AtlasModeReason.LOCAL_ENGINE_UNAVAILABLE
                }

            val unavailableMessage =
                when {
                    !internetAvailable ->
                        "Atlas has no internet connection and no local intelligence path."

                    networkQuality ==
                        NetworkQuality.WEAK ->
                        "The network is weak and no local intelligence path is currently available."

                    !authenticated ->
                        "Online AI requires authentication and no local intelligence path is currently available."

                    else ->
                        "Atlas has no currently available intelligence path."
                }

            return AtlasAvailability(
                mode =
                    AtlasMode.UNAVAILABLE,

                reason =
                    unavailableReason,

                internetAvailable =
                    internetAvailable,

                authenticated =
                    authenticated,

                localKnowledgeAvailable =
                    localKnowledgeAvailable,

                localEngineAvailable =
                    localEngineAvailable,

                voiceInputAvailable =
                    voiceInputAvailable,

                voiceOutputAvailable =
                    voiceOutputAvailable,

                externalAIAvailable =
                    false,

                restrictedByGuardian =
                    false,

                networkQuality =
                    networkQuality,

                message =
                    unavailableMessage
            )
        }

        /**
         * Checks whether Android currently has a
         * validated internet connection.
         *
         * No external request is made.
         */
        fun isInternetAvailable(
            context: Context
        ): Boolean {

            val connectivityManager =
                context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as? ConnectivityManager
                    ?: return false

            val network =
                connectivityManager.activeNetwork
                    ?: return false

            val capabilities =
                connectivityManager.getNetworkCapabilities(
                    network
                )
                    ?: return false

            return capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) &&
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
        }

        /**
         * Determines current network quality using only
         * Android-provided capability information.
         */
        fun getNetworkQuality(
            context: Context
        ): NetworkQuality {

            val connectivityManager =
                context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as? ConnectivityManager
                    ?: return NetworkQuality.OFFLINE

            val network =
                connectivityManager.activeNetwork
                    ?: return NetworkQuality.OFFLINE

            val capabilities =
                connectivityManager.getNetworkCapabilities(
                    network
                )
                    ?: return NetworkQuality.OFFLINE

            val internet =
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )

            val validated =
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

            if (!internet || !validated) {
                return NetworkQuality.OFFLINE
            }

            val downstreamKbps =
                capabilities.linkDownstreamBandwidthKbps

            val signalStrength =
                capabilities.signalStrength

            val veryLowBandwidth =
                downstreamKbps > 0 &&
                    downstreamKbps < WEAK_BANDWIDTH_KBPS

            val weakSignal =
                signalStrength != Int.MIN_VALUE &&
                    signalStrength < WEAK_SIGNAL_LEVEL

            return if (
                veryLowBandwidth ||
                weakSignal
            ) {
                NetworkQuality.WEAK
            } else {
                NetworkQuality.NORMAL
            }
        }

        /**
         * Returns whether the current network is weak.
         */
        fun isNetworkWeak(
            context: Context
        ): Boolean {

            return getNetworkQuality(
                context.applicationContext
            ) == NetworkQuality.WEAK
        }

        /**
         * Safe network status.
         */
        fun getNetworkQualityStatus(
            context: Context
        ): String {

            return when (
                getNetworkQuality(
                    context.applicationContext
                )
            ) {

                NetworkQuality.OFFLINE ->
                    "OFFLINE"

                NetworkQuality.WEAK ->
                    "WEAK NETWORK"

                NetworkQuality.NORMAL ->
                    "NORMAL NETWORK"
            }
        }

        /**
         * Checks whether Guardian has an authenticated
         * AZIMI session.
         */
        fun isAuthenticated(
            context: Context
        ): Boolean {

            return runCatching {
                AzimiAuth.hasSession(
                    context.applicationContext
                )
            }.getOrDefault(false)
        }

        /**
         * Checks local AZIMI Knowledge availability.
         */
        fun isLocalKnowledgeAvailable(): Boolean {

            return runCatching {
                AtlasKnowledge.KNOWLEDGE_VERSION.isNotBlank()
            }.getOrDefault(false)
        }

        /**
         * Dedicated local language-model engine.
         *
         * Explicitly false until a real local engine exists.
         */
        fun isLocalEngineAvailable(): Boolean {
            return false
        }

        /**
         * Detect Android speech-recognition support.
         */
        fun isVoiceInputAvailable(
            context: Context
        ): Boolean {

            return runCatching {

                val intent =
                    Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    )

                val activities =
                    context.packageManager.queryIntentActivities(
                        intent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )

                activities.isNotEmpty()

            }.getOrDefault(false)
        }

        /**
         * Detect Android Text-to-Speech support.
         */
        fun isVoiceOutputAvailable(
            context: Context
        ): Boolean {

            return runCatching {

                val intent =
                    Intent(
                        TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE
                    )

                val services =
                    context.packageManager.queryIntentServices(
                        intent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )

                services.isNotEmpty()

            }.getOrDefault(false)
        }

        /**
         * Guardian AI memory policy check.
         *
         * Any failure is treated as restricted.
         */
        fun isRestrictedByGuardian(
            context: Context
        ): Boolean {

            return runCatching {

                val policy =
                    GuardianStorage.getAIMemoryPolicy(
                        context.applicationContext
                    )

                policy != "SAFE_CONTEXT_ONLY"

            }.getOrDefault(true)
        }

        /**
         * Returns current Atlas mode.
         */
        fun getMode(
            context: Context
        ): AtlasMode {

            return detect(context).mode
        }

        /**
         * Returns safe Atlas status.
         */
        fun getSafeStatus(
            context: Context
        ): String {

            return detect(context)
                .toSafeStatusMessage()
        }

        /**
         * Determines whether Atlas has any usable response path.
         */
        fun canRespond(
            context: Context
        ): Boolean {

            return detect(
                context.applicationContext
            ).canRespond()
        }

        /**
         * Determines whether external AI is eligible.
         *
         * No provider request is made here.
         */
        fun canUseExternalAI(
            context: Context
        ): Boolean {

            return detect(
                context.applicationContext
            ).canUseExternalAI()
        }

        /**
         * Determines whether offline capabilities exist.
         */
        fun canUseOfflineCapabilities(
            context: Context
        ): Boolean {

            val availability =
                detect(
                    context.applicationContext
                )

            return availability.canRespondLocally()
        }

        /**
         * Determines whether voice input is available.
         */
        fun canUseVoiceInput(
            context: Context
        ): Boolean {

            return detect(
                context.applicationContext
            ).canUseVoiceInput()
        }

        /**
         * Determines whether voice output is available.
         */
        fun canUseVoiceOutput(
            context: Context
        ): Boolean {

            return detect(
                context.applicationContext
            ).canUseVoiceOutput()
        }

        /**
         * Compact state for Guardian diagnostics and Z Control.
         */
        data class State(
            val mode: AtlasMode,
            val reason: AtlasModeReason,
            val internetAvailable: Boolean,
            val networkQuality: NetworkQuality,
            val authenticated: Boolean,
            val localKnowledgeAvailable: Boolean,
            val localEngineAvailable: Boolean,
            val voiceInputAvailable: Boolean,
            val voiceOutputAvailable: Boolean,
            val externalAIAvailable: Boolean,
            val restrictedByGuardian: Boolean
        )

        /**
         * Creates a compact state snapshot.
         */
        fun getState(
            context: Context
        ): State {

            val availability =
                detect(
                    context.applicationContext
                )

            return State(
                mode =
                    availability.mode,

                reason =
                    availability.reason,

                internetAvailable =
                    availability.internetAvailable,

                networkQuality =
                    availability.networkQuality,

                authenticated =
                    availability.authenticated,

                localKnowledgeAvailable =
                    availability.localKnowledgeAvailable,

                localEngineAvailable =
                    availability.localEngineAvailable,

                voiceInputAvailable =
                    availability.voiceInputAvailable,

                voiceOutputAvailable =
                    availability.voiceOutputAvailable,

                externalAIAvailable =
                    availability.externalAIAvailable,

                restrictedByGuardian =
                    availability.restrictedByGuardian
            )
        }

        /**
         * Safe diagnostic report.
         */
        fun diagnostics(
            context: Context
        ): String {

            val availability =
                detect(
                    context.applicationContext
                )

            return buildString {

                appendLine(
                    "ATLAS AVAILABILITY DIAGNOSTICS"
                )

                appendLine()

                appendLine(
                    "MODE: ${availability.mode}"
                )

                appendLine(
                    "REASON: ${availability.reason}"
                )

                appendLine()

                appendLine(
                    "INTERNET: ${availability.internetAvailable}"
                )

                appendLine(
                    "NETWORK QUALITY: ${availability.networkQuality}"
                )

                appendLine(
                    "NETWORK WEAK: ${
                        availability.networkQuality ==
                            NetworkQuality.WEAK
                    }"
                )

                appendLine(
                    "AUTHENTICATED: ${availability.authenticated}"
                )

                appendLine(
                    "LOCAL KNOWLEDGE: ${availability.localKnowledgeAvailable}"
                )

                appendLine(
                    "LOCAL ENGINE: ${availability.localEngineAvailable}"
                )

                appendLine(
                    "VOICE INPUT: ${availability.voiceInputAvailable}"
                )

                appendLine(
                    "VOICE OUTPUT: ${availability.voiceOutputAvailable}"
                )

                appendLine(
                    "EXTERNAL AI PATH: ${availability.externalAIAvailable}"
                )

                appendLine(
                    "GUARDIAN RESTRICTED: ${availability.restrictedByGuardian}"
                )

                appendLine()

                appendLine(
                    "STATUS:"
                )

                appendLine(
                    availability.message
                )

                appendLine()

                appendLine(
                    "SAFE RESPONSE PATH: ${
                        availability.canRespond()
                    }"
                )
            }
        }

        private const val WEAK_BANDWIDTH_KBPS = 512

        private const val WEAK_SIGNAL_LEVEL = -90
    }
}
