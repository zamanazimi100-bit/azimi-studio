package com.azimi.guardian

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech

/**
 * AZIMI Atlas Availability.
 *
 * Detection only.
 *
 * This object does not:
 * - authenticate the user
 * - unlock the Vault
 * - execute commands
 * - contact an external AI provider
 * - expose credentials
 */
object AtlasAvailability {

    enum class NetworkQuality {
        OFFLINE,
        WEAK,
        NORMAL
    }

    data class Availability(
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

        /**
         * Compatibility alias used by older Atlas code.
         */
        val restricted: Boolean = restrictedByGuardian,

        val unavailable: Boolean = false,

        val canRespondLocally: Boolean =
            localKnowledgeAvailable || localEngineAvailable,

        val message: String = "",

        /**
         * Network quality is informational and defaults to NORMAL
         * for compatibility with older constructors.
         */
        val networkQuality: NetworkQuality = NetworkQuality.NORMAL
    ) {

        fun canUseExternalAI(): Boolean {
            return externalAIAvailable &&
                authenticated &&
                internetAvailable &&
                !restrictedByGuardian
        }

        fun toSafeStatusMessage(): String {
            return buildString {

                appendLine("ATLAS — AVAILABILITY")
                appendLine()

                appendLine("MODE: $mode")
                appendLine("REASON: $reason")
                appendLine()

                appendLine(
                    "AUTHENTICATED: $authenticated"
                )

                appendLine(
                    "INTERNET: $internetAvailable"
                )

                appendLine(
                    "NETWORK QUALITY: $networkQuality"
                )

                appendLine(
                    "LOCAL KNOWLEDGE: $localKnowledgeAvailable"
                )

                appendLine(
                    "LOCAL ENGINE: $localEngineAvailable"
                )

                appendLine(
                    "EXTERNAL AI: $externalAIAvailable"
                )

                appendLine(
                    "VOICE INPUT: $voiceInputAvailable"
                )

                appendLine(
                    "VOICE OUTPUT: $voiceOutputAvailable"
                )

                appendLine(
                    "GUARDIAN RESTRICTION: $restrictedByGuardian"
                )

                appendLine()

                appendLine(
                    "LOCAL RESPONSE AVAILABLE: $canRespondLocally"
                )

                appendLine(
                    "STATUS: $message"
                )
            }
        }
    }

    /**
     * Detects the current Atlas environment.
     */
    fun detect(
        context: Context
    ): Availability {

        val appContext =
            context.applicationContext

        val networkQuality =
            detectNetworkQuality(appContext)

        val internetAvailable =
            networkQuality != NetworkQuality.OFFLINE

        val authenticated =
            runCatching {
                AzimiAuth.hasSession(
                    appContext
                )
            }.getOrDefault(false)

        val policy =
            runCatching {
                GuardianStorage.getAIMemoryPolicy(
                    appContext
                )
            }.getOrDefault("")

        val restrictedByGuardian =
            policy != "SAFE_CONTEXT_ONLY"

        val localKnowledgeAvailable =
            detectLocalKnowledge()

        val localEngineAvailable =
            runCatching {
                AtlasLocalEngine.isAvailable()
            }.getOrDefault(false)

        val voiceInputAvailable =
            detectVoiceInput(appContext)

        val voiceOutputAvailable =
            detectVoiceOutput(appContext)

        /*
         * This means the external path is permitted and appears
         * reachable from the device.
         *
         * It does NOT contact the provider.
         */
        val externalAIAvailable =
            authenticated &&
                internetAvailable &&
                !restrictedByGuardian

        val mode =
            AtlasModeFactory.determine(
                internetAvailable =
                    internetAvailable,
                authenticated =
                    authenticated,
                localKnowledgeAvailable =
                    localKnowledgeAvailable,
                localEngineAvailable =
                    localEngineAvailable,
                restrictedByGuardian =
                    restrictedByGuardian,
                networkWeak =
                    networkQuality == NetworkQuality.WEAK
            )

        val reason =
            AtlasModeFactory.determineReason(
                internetAvailable =
                    internetAvailable,
                authenticated =
                    authenticated,
                localKnowledgeAvailable =
                    localKnowledgeAvailable,
                localEngineAvailable =
                    localEngineAvailable,
                restrictedByGuardian =
                    restrictedByGuardian,
                networkWeak =
                    networkQuality == NetworkQuality.WEAK
            )

        val canRespondLocally =
            localKnowledgeAvailable ||
                localEngineAvailable

        val unavailable =
            !canRespondLocally &&
                !externalAIAvailable

        val message =
            buildSafeMessage(
                mode = mode,
                reason = reason,
                internetAvailable =
                    internetAvailable,
                authenticated =
                    authenticated,
                localKnowledgeAvailable =
                    localKnowledgeAvailable,
                localEngineAvailable =
                    localEngineAvailable,
                externalAIAvailable =
                    externalAIAvailable,
                restrictedByGuardian =
                    restrictedByGuardian,
                networkQuality =
                    networkQuality
            )

        return Availability(
            mode = mode,
            reason = reason,

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
                externalAIAvailable,

            restrictedByGuardian =
                restrictedByGuardian,

            restricted =
                restrictedByGuardian,

            unavailable =
                unavailable,

            canRespondLocally =
                canRespondLocally,

            message =
                message,

            networkQuality =
                networkQuality
        )
    }

    fun isAvailable(
        context: Context
    ): Boolean {

        val availability =
            detect(context)

        return availability.canRespondLocally ||
            availability.externalAIAvailable
    }

    fun canRespondLocally(
        context: Context
    ): Boolean {

        return detect(
            context.applicationContext
        ).canRespondLocally
    }

    fun canUseExternalAI(
        context: Context
    ): Boolean {

        return detect(
            context.applicationContext
        ).canUseExternalAI()
    }

    fun getMode(
        context: Context
    ): AtlasMode {

        return detect(
            context.applicationContext
        ).mode
    }

    fun getReason(
        context: Context
    ): AtlasModeReason {

        return detect(
            context.applicationContext
        ).reason
    }

    /**
     * Local AZIMI knowledge is compiled into the application.
     */
    private fun detectLocalKnowledge(): Boolean {

        return runCatching {
            AtlasKnowledge.KNOWLEDGE_VERSION
                .isNotBlank()
        }.getOrDefault(false)
    }

    /**
     * Detects network quality without performing a speed test.
     *
     * Android-reported bandwidth values are used only as a
     * routing hint. No external request is made.
     */
    private fun detectNetworkQuality(
        context: Context
    ): NetworkQuality {

        return runCatching {

            val manager =
                context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as? ConnectivityManager
                    ?: return@runCatching NetworkQuality.OFFLINE

            val network =
                manager.activeNetwork
                    ?: return@runCatching NetworkQuality.OFFLINE

            val capabilities =
                manager.getNetworkCapabilities(
                    network
                )
                    ?: return@runCatching NetworkQuality.OFFLINE

            val hasInternet =
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )

            val validated =
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

            if (!hasInternet || !validated) {
                return@runCatching NetworkQuality.OFFLINE
            }

            val downstreamKbps =
                capabilities.linkDownstreamBandwidthKbps

            val upstreamKbps =
                capabilities.linkUpstreamBandwidthKbps

            val weakBandwidth =
                downstreamKbps in 1..512 ||
                    upstreamKbps in 1..256

            when {
                weakBandwidth ->
                    NetworkQuality.WEAK

                else ->
                    NetworkQuality.NORMAL
            }

        }.getOrDefault(
            NetworkQuality.OFFLINE
        )
    }

    /**
     * Legacy/simple internet detection retained for compatibility.
     */
    private fun detectInternet(
        context: Context
    ): Boolean {

        return detectNetworkQuality(
            context
        ) != NetworkQuality.OFFLINE
    }

    private fun detectVoiceInput(
        context: Context
    ): Boolean {

        return runCatching {

            if (
                context.checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@runCatching false
            }

            val intent =
                Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                )

            intent.resolveActivity(
                context.packageManager
            ) != null

        }.getOrDefault(false)
    }

    private fun detectVoiceOutput(
        context: Context
    ): Boolean {

        return runCatching {

            val intent =
                Intent(
                    TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE
                )

            context.packageManager
                .queryIntentServices(
                    intent,
                    PackageManager.MATCH_ALL
                )
                .isNotEmpty()

        }.getOrDefault(false)
    }

    private fun buildSafeMessage(
        mode: AtlasMode,
        reason: AtlasModeReason,
        internetAvailable: Boolean,
        authenticated: Boolean,
        localKnowledgeAvailable: Boolean,
        localEngineAvailable: Boolean,
        externalAIAvailable: Boolean,
        restrictedByGuardian: Boolean,
        networkQuality: NetworkQuality
    ): String {

        return when {

            restrictedByGuardian ->
                "Guardian AI policy currently restricts Atlas intelligence."

            !authenticated &&
                (localKnowledgeAvailable ||
                    localEngineAvailable) ->
                "Atlas can continue locally. Authentication is required only for the external AI path."

            networkQuality ==
                NetworkQuality.WEAK &&
                (localKnowledgeAvailable ||
                    localEngineAvailable) ->
                "Network quality is weak. Atlas can prioritize local capabilities and reduce external dependency."

            authenticated &&
                externalAIAvailable &&
                (localKnowledgeAvailable ||
                    localEngineAvailable) ->
                "Atlas has local capabilities and an authenticated online AI path."

            authenticated &&
                externalAIAvailable ->
                "Atlas has an authenticated online AI path."

            !internetAvailable &&
                (localKnowledgeAvailable ||
                    localEngineAvailable) ->
                "Internet is unavailable. Atlas can continue with local intelligence."

            !localKnowledgeAvailable &&
                !localEngineAvailable &&
                !externalAIAvailable ->
                "Atlas currently has no usable intelligence path."

            else ->
                "Atlas is currently operating in $mode mode because of $reason."
        }
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

            appendLine("ATLAS AVAILABILITY")
            appendLine()

            appendLine(
                "MODE: ${availability.mode}"
            )

            appendLine(
                "REASON: ${availability.reason}"
            )

            appendLine()

            appendLine(
                "AUTHENTICATED: ${availability.authenticated}"
            )

            appendLine(
                "INTERNET: ${availability.internetAvailable}"
            )

            appendLine(
                "NETWORK QUALITY: ${availability.networkQuality}"
            )

            appendLine(
                "LOCAL KNOWLEDGE: ${availability.localKnowledgeAvailable}"
            )

            appendLine(
                "LOCAL ENGINE: ${availability.localEngineAvailable}"
            )

            appendLine(
                "EXTERNAL AI: ${availability.externalAIAvailable}"
            )

            appendLine(
                "VOICE INPUT: ${availability.voiceInputAvailable}"
            )

            appendLine(
                "VOICE OUTPUT: ${availability.voiceOutputAvailable}"
            )

            appendLine(
                "GUARDIAN RESTRICTED: ${availability.restrictedByGuardian}"
            )

            appendLine(
                "LOCAL RESPONSE: ${availability.canRespondLocally}"
            )

            appendLine(
                "UNAVAILABLE: ${availability.unavailable}"
            )

            appendLine()

            appendLine("STATUS:")

            appendLine(
                availability.message
            )
        }
    }
}
