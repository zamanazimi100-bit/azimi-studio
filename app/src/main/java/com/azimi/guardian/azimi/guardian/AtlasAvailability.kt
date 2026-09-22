package com.azimi.guardian

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * AZIMI Atlas Availability
 *
 * Detects the capabilities currently available to Atlas
 * on this Android device.
 *
 * Responsibilities:
 * - Detect internet connectivity
 * - Detect weak network conditions
 * - Detect Guardian authentication state
 * - Detect local Atlas Knowledge availability
 * - Detect local engine availability
 * - Detect Android speech recognition support
 * - Detect Android Text-to-Speech support
 * - Determine the appropriate Atlas operating mode
 *
 * This component is READ-ONLY.
 *
 * It does not:
 * - Execute project actions
 * - Change security settings
 * - Store credentials
 * - Send network requests
 * - Contact an external AI provider
 * - Bypass Guardian
 *
 * Important:
 * External AI availability means that the required path appears
 * available. It does NOT itself make an AI request.
 *
 * Weak-network detection is local Android capability detection.
 * Atlas does not perform an external speed test.
 */
object AtlasAvailability {

    /**
     * Describes the locally detected network condition.
     *
     * This is intentionally conservative.
     */
    enum class NetworkQuality {

        OFFLINE,

        WEAK,

        NORMAL
    }

    /**
     * Checks the complete current Atlas environment.
     *
     * This is the main entry point for Atlas availability detection.
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

        /*
         * Guardian remains the security authority.
         *
         * If the AI memory policy is not SAFE_CONTEXT_ONLY,
         * Atlas must not use the normal intelligence path.
         */
        val guardianRestricted =
            isRestrictedByGuardian(appContext)

        if (guardianRestricted) {

            return AtlasModeFactory.restricted(
                reason =
                    AtlasModeReason.GUARDIAN_SECURITY_POLICY,
                message =
                    "Atlas is restricted because Guardian AI policy is not SAFE_CONTEXT_ONLY."
            )
        }

        /*
         * External AI can only be considered available when:
         *
         * 1. Internet exists
         * 2. A valid Guardian session exists
         *
         * Weak network does NOT mean the internet is absent.
         *
         * The future AtlasRouter can use networkQuality to
         * decide whether an online request should be attempted,
         * delayed, reduced, or replaced by a local path.
         */
        val externalAIAvailable =
            internetAvailable &&
                authenticated

        /*
         * Offline/local capability.
         */
        val localAvailable =
            localKnowledgeAvailable ||
                localEngineAvailable

        /*
         * Weak network + local capability.
         *
         * Atlas remains HYBRID because both paths technically
         * exist, but the message tells the router/UI that the
         * online path should be treated carefully.
         */
        if (
            localAvailable &&
            externalAIAvailable
        ) {

            val weakNetwork =
                networkQuality == NetworkQuality.WEAK

            return AtlasAvailability(
                mode = AtlasMode.HYBRID,
                reason =
                    if (weakNetwork) {
                        AtlasModeReason.WEAK_NETWORK_DETECTED
                    } else {
                        AtlasModeReason.ONLINE_AND_LOCAL_AVAILABLE
                    },
                internetAvailable = true,
                authenticated = true,
                localKnowledgeAvailable =
                    localKnowledgeAvailable,
                localEngineAvailable =
                    localEngineAvailable,
                voiceInputAvailable =
                    voiceInputAvailable,
                voiceOutputAvailable =
                    voiceOutputAvailable,
                externalAIAvailable = true,
                restrictedByGuardian = false,
                message =
                    if (weakNetwork) {
                        "Internet is available but weak. Atlas can use local capabilities while conserving network usage."
                    } else {
                        "Atlas can use local capabilities and the authenticated online AI path."
                    }
            )
        }

        /*
         * Online only.
         */
        if (externalAIAvailable) {

            val weakNetwork =
                networkQuality == NetworkQuality.WEAK

            return AtlasAvailability(
                mode = AtlasMode.ONLINE,
                reason =
                    if (weakNetwork) {
                        AtlasModeReason.WEAK_NETWORK_DETECTED
                    } else {
                        AtlasModeReason.ONLINE_CONNECTION_AVAILABLE
                    },
                internetAvailable = true,
                authenticated = true,
                localKnowledgeAvailable =
                    localKnowledgeAvailable,
                localEngineAvailable =
                    localEngineAvailable,
                voiceInputAvailable =
                    voiceInputAvailable,
                voiceOutputAvailable =
                    voiceOutputAvailable,
                externalAIAvailable = true,
                restrictedByGuardian = false,
                message =
                    if (weakNetwork) {
                        "Online AI is authenticated, but the network appears weak. Atlas should use conservative network behavior."
                    } else {
                        "Atlas can use the authenticated online AI path."
                    }
            )
        }

        /*
         * Offline/local operation.
         *
         * Authentication is not required for the local
         * knowledge layer.
         */
        if (localAvailable) {

            return AtlasAvailability(
                mode = AtlasMode.OFFLINE,
                reason =
                    when {
                        !internetAvailable ->
                            AtlasModeReason.INTERNET_UNAVAILABLE

                        networkQuality == NetworkQuality.WEAK ->
                            AtlasModeReason.WEAK_NETWORK_DETECTED

                        else ->
                            AtlasModeReason.AUTHENTICATION_REQUIRED
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
                externalAIAvailable = false,
                restrictedByGuardian = false,
                message =
                    when {
                        !internetAvailable -> {
                            "Internet is unavailable. Atlas can continue using local capabilities."
                        }

                        networkQuality == NetworkQuality.WEAK -> {
                            "The network is weak. Atlas can continue locally while conserving network usage."
                        }

                        else -> {
                            "Online AI requires authentication. Atlas can continue using local capabilities."
                        }
                    }
            )
        }

        /*
         * No local path and no online path.
         */
        return AtlasModeFactory.unavailable(
            reason =
                when {
                    !internetAvailable ->
                        AtlasModeReason.INTERNET_UNAVAILABLE

                    networkQuality == NetworkQuality.WEAK ->
                        AtlasModeReason.WEAK_NETWORK_DETECTED

                    !authenticated ->
                        AtlasModeReason.AUTHENTICATION_REQUIRED

                    else ->
                        AtlasModeReason.LOCAL_ENGINE_UNAVAILABLE
                },
            message =
                when {
                    !internetAvailable ->
                        "Atlas has no internet connection and no local intelligence path."

                    networkQuality == NetworkQuality.WEAK ->
                        "The network is too weak for reliable online intelligence and no local intelligence path is currently available."

                    !authenticated ->
                        "Online AI requires authentication and no local intelligence path is currently available."

                    else ->
                        "Atlas has no currently available intelligence path."
                }
        )
    }

    /**
     * Determines whether Android currently has an
     * active network with internet capability.
     *
     * This does not contact an external server.
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
     * Determines the current network quality using only
     * information supplied by Android.
     *
     * Atlas deliberately does NOT perform a speed test.
     *
     * Android exposes estimated downstream bandwidth and
     * signal strength for some network types. These values
     * are not guaranteed to exist on every device, so the
     * detection remains conservative.
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

        /*
         * Android's estimated downstream bandwidth is
         * expressed in Kbps.
         *
         * A very low estimate is treated as weak.
         */
        val downstreamKbps =
            capabilities.linkDownstreamBandwidthKbps

        /*
         * Signal strength is optional and may be unavailable.
         *
         * Integer.MIN_VALUE means that Android does not
         * provide a usable signal-strength estimate.
         */
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
     * Returns true when Android indicates that the current
     * validated connection is weak.
     *
     * This does not perform any network request.
     */
    fun isNetworkWeak(
        context: Context
    ): Boolean {

        return getNetworkQuality(
            context.applicationContext
        ) == NetworkQuality.WEAK
    }

    /**
     * Returns a safe human-readable network status.
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
     * Checks whether Guardian currently has an
     * authenticated AZIMI session.
     *
     * No token value is returned or exposed.
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
     * Atlas Knowledge is a local Kotlin object and therefore
     * does not require internet connectivity.
     *
     * We verify that the knowledge layer can be accessed
     * without exposing its internal data.
     */
    fun isLocalKnowledgeAvailable(): Boolean {

        return runCatching {
            AtlasKnowledge.KNOWLEDGE_VERSION.isNotBlank()
        }.getOrDefault(false)
    }

    /**
     * Determines whether a dedicated local language model
     * engine has been implemented.
     *
     * This currently returns false until AtlasLocalEngine
     * is implemented.
     *
     * Keeping this explicit prevents Atlas from claiming
     * local AI generation that does not yet exist.
     */
    fun isLocalEngineAvailable(): Boolean {

        return false
    }

    /**
     * Detects whether the Android device has an application
     * capable of handling speech recognition requests.
     */
    fun isVoiceInputAvailable(
        context: Context
    ): Boolean {

        return runCatching {

            val intent =
                Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                )

            val packageManager =
                context.packageManager

            val activities =
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )

            activities.isNotEmpty()

        }.getOrDefault(false)
    }

    /**
     * Detects whether Android has a usable Text-to-Speech
     * engine installed.
     *
     * Text-to-Speech initialization is asynchronous, so this
     * method uses package-level detection rather than claiming
     * that speech synthesis is immediately initialized.
     */
    fun isVoiceOutputAvailable(
        context: Context
    ): Boolean {

        return runCatching {

            val packageManager =
                context.packageManager

            val intent =
                Intent(
                    TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE
                )

            val services =
                packageManager.queryIntentServices(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )

            services.isNotEmpty()

        }.getOrDefault(false)
    }

    /**
     * Checks the Guardian AI memory policy.
     *
     * SAFE_CONTEXT_ONLY is the required normal operating
     * policy for Atlas project context.
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
     * Returns the current Atlas mode only.
     *
     * Useful when the UI only needs the mode and not
     * the complete diagnostic snapshot.
     */
    fun getMode(
        context: Context
    ): AtlasMode {

        return detect(context).mode
    }

    /**
     * Returns a safe status message for the Guardian UI.
     *
     * No credentials, tokens, or private authentication
     * information are included.
     */
    fun getSafeStatus(
        context: Context
    ): String {

        return detect(context)
            .toSafeStatusMessage()
    }

    /**
     * Determines whether Atlas currently has at least
     * one usable response path.
     */
    fun canRespond(
        context: Context
    ): Boolean {

        val availability =
            detect(
                context.applicationContext
            )

        return availability.canRespondLocally() ||
            availability.canUseExternalAI()
    }

    /**
     * Determines whether Atlas can currently use
     * external AI.
     *
     * This does not send anything.
     */
    fun canUseExternalAI(
        context: Context
    ): Boolean {

        return detect(context)
            .canUseExternalAI()
    }

    /**
     * Determines whether Atlas has local capabilities.
     */
    fun canUseOfflineCapabilities(
        context: Context
    ): Boolean {

        val availability =
            detect(context)

        return availability.localKnowledgeAvailable ||
            availability.localEngineAvailable
    }

    /**
     * Determines whether voice input can currently
     * be offered by the Guardian UI.
     */
    fun canUseVoiceInput(
        context: Context
    ): Boolean {

        return detect(context)
            .canUseVoiceInput()
    }

    /**
     * Determines whether voice output can currently
     * be offered by the Guardian UI.
     */
    fun canUseVoiceOutput(
        context: Context
    ): Boolean {

        return detect(context)
            .canUseVoiceOutput()
    }

    /**
     * Returns a compact machine-readable state.
     *
     * This is useful later for:
     * - AtlasRouter
     * - diagnostics
     * - Z Control
     * - Guardian status panel
     * - weak-network adaptation
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

        val appContext =
            context.applicationContext

        val availability =
            detect(appContext)

        return State(
            mode =
                availability.mode,

            reason =
                availability.reason,

            internetAvailable =
                availability.internetAvailable,

            networkQuality =
                getNetworkQuality(appContext),

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
     * Returns a diagnostic report suitable for
     * Guardian diagnostics or Z Control.
     *
     * Sensitive values are intentionally excluded.
     */
    fun diagnostics(
        context: Context
    ): String {

        val appContext =
            context.applicationContext

        val availability =
            detect(appContext)

        val networkQuality =
            getNetworkQuality(appContext)

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
                "NETWORK QUALITY: $networkQuality"
            )

            appendLine(
                "NETWORK WEAK: ${networkQuality == NetworkQuality.WEAK}"
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
                    availability.canRespondLocally() ||
                        availability.canUseExternalAI()
                }"
            )
        }
    }

    /**
     * Conservative weak-network threshold.
     *
     * Android reports downstream bandwidth in Kbps.
     *
     * This is intentionally not treated as a guaranteed
     * real-world internet speed measurement.
     */
    private const val WEAK_BANDWIDTH_KBPS = 512

    /**
     * Conservative Android signal-strength threshold.
     *
     * Signal strength availability varies by transport
     * and Android implementation.
     */
    private const val WEAK_SIGNAL_LEVEL = -90
}
