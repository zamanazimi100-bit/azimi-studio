package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Router
 *
 * Selects the safest available intelligence path for Atlas.
 *
 * Routing priority:
 *
 * 1. Guardian security restrictions
 * 2. Local capability when it is sufficient
 * 3. Online AI when authentication + network are available
 * 4. Offline fallback when online intelligence is unavailable
 * 5. Safe capability-gap response when nothing can answer
 *
 * IMPORTANT:
 * This router does NOT execute actions.
 * It only decides which intelligence path should be used.
 *
 * Atlas remains provider-independent:
 *
 *      AtlasCore
 *          |
 *      AtlasRouter
 *          |
 *    +-----+----------------+
 *    |     |                |
 * Local  Online          Restricted
 * Engine  AI              Response
 *
 * External AI providers are adapters, not Atlas itself.
 */
object AtlasRouter {

    /**
     * Intelligence paths available to Atlas.
     */
    enum class Route {

        /**
         * Use local Atlas capabilities.
         */
        LOCAL,

        /**
         * Use an authenticated external AI adapter.
         */
        ONLINE,

        /**
         * Use local capabilities first and allow
         * online intelligence when appropriate.
         */
        HYBRID,

        /**
         * Guardian has restricted Atlas.
         */
        RESTRICTED,

        /**
         * No usable intelligence path currently exists.
         */
        UNAVAILABLE
    }

    /**
     * Reason for selecting a route.
     */
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

    /**
     * Describes the routing decision.
     */
    data class RoutingDecision(
        val route: Route,
        val reason: RouteReason,
        val availability: AtlasAvailability,
        val analysis: AtlasRequirementEngine.RequirementAnalysis,
        val explanation: String,
        val fallbackAllowed: Boolean,
        val requiresAuthentication: Boolean
    )

    /**
     * Main routing entry point.
     *
     * AtlasCore or AtlasGuardianBridge can use this to
     * determine the safest intelligence path.
     */
    fun route(
        context: Context,
        request: String
    ): RoutingDecision {

        val appContext =
            context.applicationContext

        /*
         * First understand what the request requires.
         *
         * Requirement analysis remains local and does not
         * contact an external provider.
         */
        val analysis =
            AtlasRequirementEngine.analyze(
                request
            )

        /*
         * Detect the actual environment.
         */
        val availability =
            AtlasAvailability.detect(
                appContext
            )

        /*
         * Protected information must never be routed
         * to an external AI provider.
         */
        if (
            analysis.securityLevel ==
            AtlasRequirementEngine.SecurityLevel.PROTECTED
        ) {

            return RoutingDecision(
                route = Route.RESTRICTED,
                reason = RouteReason.GUARDIAN_RESTRICTION,
                availability = availability,
                analysis = analysis,
                explanation =
                    "Atlas blocked the request because protected credential material was detected.",
                fallbackAllowed = false,
                requiresAuthentication = false
            )
        }

        /*
         * Guardian restriction always has priority.
         */
        if (
            availability.restrictedByGuardian ||
            availability.mode == AtlasMode.RESTRICTED
        ) {

            return RoutingDecision(
                route = Route.RESTRICTED,
                reason = RouteReason.GUARDIAN_RESTRICTION,
                availability = availability,
                analysis = analysis,
                explanation =
                    "Guardian security policy has restricted Atlas.",
                fallbackAllowed =
                    availability.localKnowledgeAvailable,
                requiresAuthentication = false
            )
        }

        /*
         * Some requests are naturally local.
         *
         * Atlas Knowledge + Requirement Engine can already
         * answer certain architecture, capability, and
         * security questions without external AI.
         */
        if (
            shouldPreferLocal(
                analysis
            ) &&
            availability.localKnowledgeAvailable
        ) {

            return RoutingDecision(
                route =
                    if (availability.externalAIAvailable) {
                        Route.HYBRID
                    } else {
                        Route.LOCAL
                    },
                reason =
                    if (availability.externalAIAvailable) {
                        RouteReason.LOCAL_AND_ONLINE_AVAILABLE
                    } else {
                        RouteReason.LOCAL_CAPABILITY_AVAILABLE
                    },
                availability = availability,
                analysis = analysis,
                explanation =
                    "Atlas can begin with local AZIMI knowledge and reasoning.",
                fallbackAllowed = true,
                requiresAuthentication = false
            )
        }

        /*
         * If the request benefits from deeper language
         * reasoning and the online path is available,
         * select the online adapter.
         */
        if (
            analysis.externalAIHelpful &&
            availability.canUseExternalAI()
        ) {

            return RoutingDecision(
                route = Route.ONLINE,
                reason = RouteReason.ONLINE_AI_AVAILABLE,
                availability = availability,
                analysis = analysis,
                explanation =
                    "Atlas can use the authenticated online AI adapter for deeper reasoning.",
                fallbackAllowed =
                    availability.localKnowledgeAvailable,
                requiresAuthentication = false
            )
        }

        /*
         * Local knowledge fallback.
         *
         * This is what allows Atlas to remain useful when
         * the internet is unavailable or authentication
         * has expired.
         */
        if (
            availability.localKnowledgeAvailable
        ) {

            return RoutingDecision(
                route = Route.LOCAL,
                reason =
                    if (!availability.internetAvailable) {
                        RouteReason.INTERNET_UNAVAILABLE
                    } else {
                        RouteReason.LOCAL_FALLBACK
                    },
                availability = availability,
                analysis = analysis,
                explanation =
                    "Online intelligence is unavailable, so Atlas will use local capabilities.",
                fallbackAllowed = true,
                requiresAuthentication =
                    analysis.externalAIHelpful &&
                        !availability.authenticated
            )
        }

        /*
         * If the request needs online AI but the user is
         * not authenticated, explicitly report that state.
         */
        if (
            !availability.authenticated &&
            analysis.externalAIHelpful
        ) {

            return RoutingDecision(
                route = Route.UNAVAILABLE,
                reason = RouteReason.AUTHENTICATION_REQUIRED,
                availability = availability,
                analysis = analysis,
                explanation =
                    "This request may require online AI, but Atlas is not currently authenticated.",
                fallbackAllowed = false,
                requiresAuthentication = true
            )
        }

        /*
         * No usable route remains.
         */
        return RoutingDecision(
            route = Route.UNAVAILABLE,
            reason = RouteReason.NO_INTELLIGENCE_PATH,
            availability = availability,
            analysis = analysis,
            explanation =
                "Atlas currently has no usable intelligence capability for this request.",
            fallbackAllowed = false,
            requiresAuthentication = false
        )
    }

    /**
     * Determines whether local Atlas capabilities should
     * be preferred before using external AI.
     *
     * This keeps AZIMI provider-independent and reduces
     * unnecessary external requests.
     */
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

    /**
     * Returns the best available route for the current
     * environment without analyzing a specific request.
     *
     * Useful for Guardian status screens.
     */
    fun currentRoute(
        context: Context
    ): Route {

        val availability =
            AtlasAvailability.detect(
                context.applicationContext
            )

        return when {

            availability.restrictedByGuardian ->
                Route.RESTRICTED

            availability.localKnowledgeAvailable &&
                availability.externalAIAvailable ->
                Route.HYBRID

            availability.externalAIAvailable ->
                Route.ONLINE

            availability.localKnowledgeAvailable ->
                Route.LOCAL

            else ->
                Route.UNAVAILABLE
        }
    }

    /**
     * Returns a safe explanation of the current route.
     *
     * No tokens or credentials are included.
     */
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

            appendLine(
                "ATLAS ROUTER"
            )

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
                "LOCAL KNOWLEDGE: ${availability.localKnowledgeAvailable}"
            )

            appendLine(
                "LOCAL ENGINE: ${availability.localEngineAvailable}"
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

            appendLine(
                "STATUS:"
            )

            appendLine(
                availability.message
            )
        }
    }

    /**
     * Returns a safe route explanation for a specific request.
     */
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
                "ATLAS ROUTING DECISION"
            )

            appendLine()

            appendLine(
                "ROUTE: ${decision.route}"
            )

            appendLine(
                "REASON: ${decision.reason}"
            )

            appendLine(
                "STATUS: ${decision.analysis.status}"
            )

            appendLine(
                "CATEGORY: ${decision.analysis.category}"
            )

            appendLine(
                "INTENT: ${decision.analysis.intent}"
            )

            appendLine()

            appendLine(
                "EXPLANATION:"
            )

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

    /**
     * Determines whether a request can safely remain
     * completely local.
     */
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
            decision.availability.localKnowledgeAvailable
    }

    /**
     * Determines whether a request may use the
     * authenticated external AI adapter.
     *
     * This method only checks routing eligibility.
     * It does not contact the provider.
     */
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

    /**
     * Determines whether the request requires
     * a stronger owner/security boundary.
     */
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

    /**
     * Determines whether a backup checkpoint should
     * be considered before the request proceeds.
     */
    fun requiresBackupCheckpoint(
        context: Context,
        request: String
    ): Boolean {

        val decision =
            route(
                context.applicationContext,
                request
            )

        return decision.analysis.backupRecommended
    }

    /**
     * Returns a safe machine-readable summary.
     *
     * Useful later for:
     * - AtlasCore
     * - Guardian diagnostics
     * - Z Control
     * - Audit Trail
     * - Atlas UI
     */
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

    /**
     * Creates the current route state.
     */
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
                decision.availability.localKnowledgeAvailable ||
                    decision.availability.localEngineAvailable,
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
