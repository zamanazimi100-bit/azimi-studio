package com.azimi.guardian

import android.content.Context

/**
 * Selects the safest usable intelligence path for Atlas.
 *
 * AtlasRouter does not execute consequential actions.
 */
object AtlasRouter {

    enum class Route {
        LOCAL,
        ONLINE,
        HYBRID,
        RESTRICTED,
        UNAVAILABLE
    }

    enum class RouteReason {
        LOCAL_CAPABILITY_AVAILABLE,
        ONLINE_AI_AVAILABLE,
        LOCAL_AND_ONLINE_AVAILABLE,
        GUARDIAN_RESTRICTION,
        AUTHENTICATION_REQUIRED,
        INTERNET_UNAVAILABLE,
        LOCAL_FALLBACK,
        CAPABILITY_NOT_IMPLEMENTED,
        NO_INTELLIGENCE_PATH,
        UNKNOWN
    }

    data class RoutingDecision(
        val route: Route,
        val reason: RouteReason,
        val availability:
            AtlasAvailability.Availability,
        val analysis:
            AtlasRequirementEngine.RequirementAnalysis,
        val explanation: String,
        val fallbackAllowed: Boolean,
        val requiresAuthentication: Boolean
    )

    fun route(
        context: Context,
        request: String
    ): RoutingDecision {

        val appContext =
            context.applicationContext

        val analysis =
            AtlasRequirementEngine.analyze(
                request
            )

        val availability =
            AtlasAvailability.detect(
                appContext
            )

        val localAvailable =
            hasLocalCapability(
                availability
            )

        /*
         * Protected credential material always wins.
         */
        if (
            analysis.securityLevel ==
                AtlasRequirementEngine.SecurityLevel.PROTECTED
        ) {
            return RoutingDecision(
                route = Route.RESTRICTED,
                reason =
                    RouteReason.GUARDIAN_RESTRICTION,
                availability =
                    availability,
                analysis =
                    analysis,
                explanation =
                    "Atlas blocked the request because protected credential material was detected.",
                fallbackAllowed = false,
                requiresAuthentication = false
            )
        }

        /*
         * Guardian policy restriction is authoritative.
         */
        if (
            availability.restrictedByGuardian ||
            availability.mode == AtlasMode.RESTRICTED
        ) {
            return RoutingDecision(
                route = Route.RESTRICTED,
                reason =
                    RouteReason.GUARDIAN_RESTRICTION,
                availability =
                    availability,
                analysis =
                    analysis,
                explanation =
                    "Guardian security policy has restricted Atlas.",
                fallbackAllowed =
                    localAvailable,
                requiresAuthentication = false
            )
        }

        /*
         * Prefer local capability for security, recovery,
         * preservation and other locally suitable work.
         */
        if (
            shouldPreferLocal(analysis) &&
            localAvailable
        ) {

            val hybrid =
                availability.canUseExternalAI() &&
                    availability.networkQuality !=
                        AtlasAvailability.NetworkQuality.OFFLINE

            return RoutingDecision(
                route =
                    if (hybrid) {
                        Route.HYBRID
                    } else {
                        Route.LOCAL
                    },
                reason =
                    if (hybrid) {
                        RouteReason.LOCAL_AND_ONLINE_AVAILABLE
                    } else {
                        RouteReason.LOCAL_CAPABILITY_AVAILABLE
                    },
                availability =
                    availability,
                analysis =
                    analysis,
                explanation =
                    if (hybrid) {
                        "Atlas can begin locally while an authenticated online AI path is available."
                    } else {
                        "Atlas can operate using local AZIMI capabilities."
                    },
                fallbackAllowed = true,
                requiresAuthentication = false
            )
        }

        /*
         * External AI is available only when authentication,
         * network and Guardian policy permit it.
         */
        if (
            analysis.externalAIHelpful &&
            availability.canUseExternalAI()
        ) {
            return RoutingDecision(
                route = Route.ONLINE,
                reason =
                    RouteReason.ONLINE_AI_AVAILABLE,
                availability =
                    availability,
                analysis =
                    analysis,
                explanation =
                    "Atlas can use the authenticated online AI adapter for deeper reasoning.",
                fallbackAllowed =
                    localAvailable,
                requiresAuthentication = false
            )
        }

        /*
         * Local fallback remains available when the online
         * path is unavailable.
         */
        if (localAvailable) {

            val reason =
                when {
                    !availability.internetAvailable ->
                        RouteReason.INTERNET_UNAVAILABLE

                    !availability.authenticated &&
                        analysis.externalAIHelpful ->
                        RouteReason.AUTHENTICATION_REQUIRED

                    else ->
                        RouteReason.LOCAL_FALLBACK
                }

            return RoutingDecision(
                route = Route.LOCAL,
                reason = reason,
                availability =
                    availability,
                analysis =
                    analysis,
                explanation =
                    when {
                        !availability.internetAvailable ->
                            "Internet is unavailable, so Atlas will use local capabilities."

                        !availability.authenticated &&
                            analysis.externalAIHelpful ->
                            "Online AI requires authentication, so Atlas will use available local capabilities."

                        availability.networkQuality ==
                            AtlasAvailability.NetworkQuality.WEAK ->
                            "Network quality is weak, so Atlas will prioritize local capabilities."

                        else ->
                            "Online intelligence is unavailable, so Atlas will use local capabilities."
                    },
                fallbackAllowed = true,
                requiresAuthentication =
                    analysis.externalAIHelpful &&
                        !availability.authenticated
            )
        }

        /*
         * No local capability exists and online AI needs auth.
         */
        if (
            !availability.authenticated &&
            analysis.externalAIHelpful
        ) {
            return RoutingDecision(
                route = Route.UNAVAILABLE,
                reason =
                    RouteReason.AUTHENTICATION_REQUIRED,
                availability =
                    availability,
                analysis =
                    analysis,
                explanation =
                    "This request may require online AI, but Atlas is not currently authenticated.",
                fallbackAllowed = false,
                requiresAuthentication = true
            )
        }

        return RoutingDecision(
            route = Route.UNAVAILABLE,
            reason =
                RouteReason.NO_INTELLIGENCE_PATH,
            availability =
                availability,
            analysis =
                analysis,
            explanation =
                "Atlas currently has no usable intelligence capability for this request.",
            fallbackAllowed = false,
            requiresAuthentication = false
        )
    }

    private fun hasLocalCapability(
        availability:
            AtlasAvailability.Availability
    ): Boolean {

        return availability.localKnowledgeAvailable ||
            availability.localEngineAvailable
    }

    private fun shouldPreferLocal(
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): Boolean {

        return when (analysis.category) {

            AtlasRequirementEngine.TaskCategory.SECURITY,
            AtlasRequirementEngine.TaskCategory.BACKUP_RECOVERY,
            AtlasRequirementEngine.TaskCategory.PROJECT_PRESERVATION,
            AtlasRequirementEngine.TaskCategory.AUTHENTICATION ->
                true

            AtlasRequirementEngine.TaskCategory.ANDROID_DEVELOPMENT ->
                !analysis.externalAIHelpful

            AtlasRequirementEngine.TaskCategory.WEB_DEVELOPMENT ->
                !analysis.externalAIHelpful

            AtlasRequirementEngine.TaskCategory.AI_INTELLIGENCE ->
                false

            AtlasRequirementEngine.TaskCategory.LEARNING ->
                false

            AtlasRequirementEngine.TaskCategory.LANGUAGE ->
                false

            AtlasRequirementEngine.TaskCategory.GENERAL ->
                !analysis.externalAIHelpful

            else ->
                false
        }
    }

    fun currentRoute(
        context: Context
    ): Route {

        val availability =
            AtlasAvailability.detect(
                context.applicationContext
            )

        val localAvailable =
            hasLocalCapability(
                availability
            )

        return when {

            availability.restrictedByGuardian ->
                Route.RESTRICTED

            localAvailable &&
                availability.canUseExternalAI() ->
                Route.HYBRID

            availability.canUseExternalAI() ->
                Route.ONLINE

            localAvailable ->
                Route.LOCAL

            else ->
                Route.UNAVAILABLE
        }
    }

    fun currentStatus(
        context: Context
    ): String {

        val availability =
            AtlasAvailability.detect(
                context.applicationContext
            )

        val route =
            currentRoute(
                context.applicationContext
            )

        return buildString {

            appendLine("ATLAS ROUTER")
            appendLine()

            appendLine(
                "ROUTE: $route"
            )

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
                "LOCAL PATH: ${hasLocalCapability(availability)}"
            )

            appendLine(
                "ONLINE AI PATH: ${availability.externalAIAvailable}"
            )

            appendLine(
                "VOICE INPUT: ${availability.voiceInputAvailable}"
            )

            appendLine(
                "VOICE OUTPUT: ${availability.voiceOutputAvailable}"
            )

            appendLine()

            appendLine("STATUS:")

            appendLine(
                availability.message
            )
        }
    }

    fun explainRoute(
        context: Context,
        request: String
    ): String {

        val decision =
            route(
                context.applicationContext,
                request
            )

        return buildString {

            appendLine(
                "ATLAS — ROUTING EXPLANATION"
            )

            appendLine()

            appendLine(
                "ROUTE: ${decision.route}"
            )

            appendLine(
                "REASON: ${decision.reason}"
            )

            appendLine()

            appendLine(
                "MODE: ${decision.availability.mode}"
            )

            appendLine(
                "CATEGORY: ${decision.analysis.category}"
            )

            appendLine(
                "INTENT: ${decision.analysis.intent}"
            )

            appendLine()

            appendLine("EXPLANATION:")

            appendLine(
                decision.explanation
            )

            appendLine()

            appendLine(
                "FALLBACK ALLOWED: ${decision.fallbackAllowed}"
            )

            appendLine(
                "AUTHENTICATION REQUIRED: ${decision.requiresAuthentication}"
            )

            appendLine()

            appendLine(
                "NEXT SAFE ACTION:"
            )

            appendLine(
                decision.analysis.nextSafeAction
            )
        }
    }

    fun canUseLocalPath(
        context: Context,
        request: String
    ): Boolean {

        val decision =
            route(
                context.applicationContext,
                request
            )

        return (
            decision.route == Route.LOCAL ||
                decision.route == Route.HYBRID
            ) &&
            hasLocalCapability(
                decision.availability
            )
    }

    fun canUseOnlinePath(
        context: Context,
        request: String
    ): Boolean {

        val decision =
            route(
                context.applicationContext,
                request
            )

        return (
            decision.route == Route.ONLINE ||
                decision.route == Route.HYBRID
            ) &&
            decision.availability.canUseExternalAI()
    }

    fun requiresOwnerPermission(
        context: Context,
        request: String
    ): Boolean {

        val decision =
            route(
                context.applicationContext,
                request
            )

        return decision.analysis.permissionRequired ||
            decision.analysis.securityLevel ==
                AtlasRequirementEngine.SecurityLevel.SENSITIVE
    }

    fun requiresBackupCheckpoint(
        context: Context,
        request: String
    ): Boolean {

        return route(
            context.applicationContext,
            request
        ).analysis.backupRecommended
    }

    data class RouteState(
        val route: Route,
        val reason: RouteReason,
        val mode: AtlasMode,
        val localAvailable: Boolean,
        val onlineAvailable: Boolean,
        val voiceInputAvailable: Boolean,
        val voiceOutputAvailable: Boolean,
        val fallbackAllowed: Boolean,
        val requiresAuthentication: Boolean
    )

    fun getRouteState(
        context: Context,
        request: String
    ): RouteState {

        val decision =
            route(
                context.applicationContext,
                request
            )

        return RouteState(
            route =
                decision.route,

            reason =
                decision.reason,

            mode =
                decision.availability.mode,

            localAvailable =
                hasLocalCapability(
                    decision.availability
                ),

            onlineAvailable =
                decision.availability.canUseExternalAI(),

            voiceInputAvailable =
                decision.availability.voiceInputAvailable,

            voiceOutputAvailable =
                decision.availability.voiceOutputAvailable,

            fallbackAllowed =
                decision.fallbackAllowed,

            requiresAuthentication =
                decision.requiresAuthentication
        )
    }
}
