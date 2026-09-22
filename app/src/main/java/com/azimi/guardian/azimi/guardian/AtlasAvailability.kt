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
 * AZIMI Atlas Availability
 *
 * Detects which Atlas intelligence capabilities are currently
 * available without executing consequential actions.
 *
 * This object is deliberately read-only.
 *
 * Responsibilities:
 * - Internet availability
 * - Guardian authentication state
 * - Guardian AI policy restriction
 * - Local AZIMI knowledge availability
 * - Local deterministic engine availability
 * - External AI path availability
 * - Voice input availability
 * - Voice output availability
 * - Atlas mode and reason
 * - Safe human-readable status
 *
 * It does NOT:
 * - bypass Android security
 * - authenticate the user
 * - unlock the Vault
 * - execute commands
 * - contact an external AI provider
 * - expose credentials
 */
object AtlasAvailability {

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
         * Compatibility aliases used by older Atlas code.
         */
        val restricted: Boolean = restrictedByGuardian,

        val unavailable: Boolean = false,

        val canRespondLocally: Boolean =
            localKnowledgeAvailable || localEngineAvailable,

        val message: String = ""
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
     *
     * This method performs detection only.
     */
    fun detect(
        context: Context
    ): Availability {

        val appContext =
            context.applicationContext

        val internetAvailable =
            detectInternet(appContext)

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
         * External AI is considered available only when:
         *
         * 1. Guardian authentication exists
         * 2. Internet exists
         * 3. Guardian AI policy permits operation
         *
         * This does not contact the provider.
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
                    restrictedByGuardian
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
                    restrictedByGuardian
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
                    restrictedByGuardian
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
                message
        )
    }

    /**
     * Safe convenience method.
     */
    fun isAvailable(
        context: Context
    ): Boolean {

        val availability =
            detect(context)

        return availability.canRespondLocally ||
            availability.externalAIAvailable
    }

    /**
     * Returns whether Atlas has a local path.
     */
    fun canRespondLocally(
        context: Context
    ): Boolean {

        return detect(
            context.applicationContext
        ).canRespondLocally
    }

    /**
     * Returns whether authenticated external AI
     * can currently be used.
     *
     * This does not make a network request.
     */
    fun canUseExternalAI(
        context: Context
    ): Boolean {

        return detect(
            context.applicationContext
        ).canUseExternalAI()
    }

    /**
     * Returns the current Atlas mode.
     */
    fun getMode(
        context: Context
    ): AtlasMode {

        return detect(
            context.applicationContext
        ).mode
    }

    /**
     * Returns the current Atlas mode reason.
     */
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
     * Detects usable network connectivity.
     *
     * This does not perform an internet request.
     */
    private fun detectInternet(
        context: Context
    ): Boolean {

        return runCatching {

            val manager =
                context.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as? ConnectivityManager
                    ?: return@runCatching false

            val network =
                manager.activeNetwork
                    ?: return@runCatching false

            val capabilities =
                manager.getNetworkCapabilities(
                    network
                )
                    ?: return@runCatching false

            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) &&
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

        }.getOrDefault(false)
    }

    /**
     * Detects whether Android has a speech-recognition
     * capability and the application has microphone access.
     */
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

    /**
     * Detects whether Android exposes a text-to-speech
     * service.
     */
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

    /**
     * Creates a safe status message.
     *
     * No tokens, credentials, emails, provider identifiers,
     * or private authentication information are included.
     */
    private fun buildSafeMessage(
        mode: AtlasMode,
        reason: AtlasModeReason,
        internetAvailable: Boolean,
        authenticated: Boolean,
        localKnowledgeAvailable: Boolean,
        localEngineAvailable: Boolean,
        externalAIAvailable: Boolean,
        restrictedByGuardian: Boolean
    ): String {

        return when {

            restrictedByGuardian ->
                "Guardian AI policy currently restricts Atlas intelligence."

            !authenticated && localKnowledgeAvailable ->
                "Atlas can use local AZIMI knowledge. Authentication is required for the external AI path."

            authenticated &&
                externalAIAvailable &&
                localKnowledgeAvailable ->
                "Atlas has both local AZIMI knowledge and an authenticated online AI path."

            authenticated &&
                externalAIAvailable ->
                "Atlas has an authenticated online AI path."

            !internetAvailable &&
                localEngineAvailable ->
                "Internet is unavailable. Atlas can continue with local intelligence."

            !internetAvailable &&
                localKnowledgeAvailable ->
                "Internet is unavailable. Atlas can continue using local AZIMI knowledge."

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

            appendLine(
                "ATLAS AVAILABILITY"
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
                "AUTHENTICATED: ${availability.authenticated}"
            )

            appendLine(
                "INTERNET: ${availability.internetAvailable}"
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

            appendLine(
                "STATUS:"
            )

            appendLine(
                availability.message
            )
        }
    }
}
