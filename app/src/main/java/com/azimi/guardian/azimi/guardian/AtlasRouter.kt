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

        if (
            availability.restrictedByGuardian ||
            availability.mode ==
                AtlasMode.RESTRICTED
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
                    availability.localKnowledgeAvailable,
                requiresAuthentication = false
            )
        }

        if (
            shouldPreferLocal(
                analysis
            ) &&
            availability.localKnowledgeAvailable
        ) {
            return RoutingDecision(
                route =
                    if (
                        availability.externalAIAvailable
                    ) {
                        Route.HYBRID
                    } else {
                        Route.LOCAL
                    },
                reason =
                    if (
                        availability.externalAIAvailable
                    ) {
                        RouteReason.LOCAL_AND_ONLINE_AVAILABLE
                    } else {
                        RouteReason.LOCAL_CAPABILITY_AVAILABLE
                    },
                availability =
                    availability,
                analysis =
                    analysis,
                explanation =
                    "Atlas can begin with local AZIMI knowledge and reasoning.",
                fallbackAllowed = true,
                requiresAuthentication = false
            )
        }

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
                    availability.localKnowledgeAvailable,
                requiresAuthentication = false
            )
        }

        if (
            availability.localKnowledgeAvailable
        ) {
            return RoutingDecision(
                route = Route.LOCAL,
                reason =
                    if (
                        !availability.internetAvailable
                    ) {
                        RouteReason.INTERNET_UNAVAILABLE
                    } else {
                        RouteReason.LOCAL_FALLBACK
                    },
                availability =
                    availability,
                analysis =
                    analysis,
                explanation =
                    "Online intelligence is unavailable, so Atlas will use local capabilities.",
                fallbackAllowed = true,
                requiresAuthentication =
                    analysis.externalAIHelpful &&
                        !availability.authenticated
            )
        }

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

        val decision =
            route(
                context.applicationContext,
                request
            )

        return decision.analysis.backupRecommended
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
