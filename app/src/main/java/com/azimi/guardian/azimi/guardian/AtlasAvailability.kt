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
 */
object AtlasAvailability {

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
         * The actual provider request is still handled
         * separately by the replaceable AI adapter.
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
         * Hybrid:
         *
         * Both local and online paths exist.
         */
        if (
            localAvailable &&
            externalAIAvailable
        ) {

            return AtlasAvailability(
                mode = AtlasMode.HYBRID,
                reason =
                    AtlasModeReason.ONLINE_AND_LOCAL_AVAILABLE,
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
                    "Atlas can use local capabilities and the authenticated online AI path."
            )
        }

        /*
         * Online only.
         */
        if (externalAIAvailable) {

            return AtlasAvailability(
                mode = AtlasMode.ONLINE,
                reason =
                    AtlasModeReason.ONLINE_CONNECTION_AVAILABLE,
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
                    "Atlas can use the authenticated online AI path."
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
                    if (!internetAvailable) {
                        AtlasModeReason.INTERNET_UNAVAILABLE
                    } else {
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
                    if (!internetAvailable) {
                        "Internet is unavailable. Atlas can continue using local capabilities."
                    } else {
                        "Online AI requires authentication. Atlas can continue using local capabilities."
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

                    !authenticated ->
                        AtlasModeReason.AUTHENTICATION_REQUIRED

                    else ->
                        AtlasModeReason.LOCAL_ENGINE_UNAVAILABLE
                },
            message =
                "Atlas has no currently available intelligence path."
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

        return detect(context)
            .canRespondLocally() ||
            detect(context)
                .canUseExternalAI()
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
     * This is useful later for AtlasRouter, diagnostics,
     * Z Control, and the Guardian status panel.
     */
    data class State(
        val mode: AtlasMode,
        val reason: AtlasModeReason,
        val internetAvailable: Boolean,
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

        val availability =
            detect(
                context.applicationContext
            )

        return buildString {

            appendLine("ATLAS AVAILABILITY DIAGNOSTICS")
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
                "SAFE RESPONSE PATH: ${availability.canRespondLocally() || availability.canUseExternalAI()}"
            )
        }
    }
}
