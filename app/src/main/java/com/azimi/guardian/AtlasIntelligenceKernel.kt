package com.azimi.guardian

/**
 * Atlas Intelligence Kernel
 *
 * The first intelligence-layer foundation for AZIMI Atlas.
 *
 * Atlas is the system.
 * AI providers are replaceable intelligence engines used by the system.
 *
 * This kernel is intentionally deterministic and provider-independent.
 * It does not:
 *
 * - call external AI providers
 * - access the network
 * - access credentials
 * - unlock Guardian
 * - bypass Android permissions
 * - modify Vault authorization
 *
 * Its job is to describe HOW Atlas should think about a request
 * before an execution engine is selected.
 */
object AtlasIntelligenceKernel {

    enum class KnowledgeState {
        KNOWN,
        VERIFIED,
        INFERRED,
        UNKNOWN
    }

    enum class IntentType {
        EXPLAIN,
        ANALYZE,
        BUILD,
        DEBUG,
        DESIGN,
        RESEARCH,
        PLAN,
        RECOVER,
        SECURE,
        AUTOMATE,
        LEARN,
        WRITE,
        TRANSLATE,
        PROJECT_WORK,
        GENERAL
    }

    enum class IntelligenceRoute {
        LOCAL,
        KNOWLEDGE,
        REASONING,
        EXTERNAL_PROVIDER,
        TOOL_REQUIRED,
        UNKNOWN
    }

    data class IntelligenceState(
        val intent: IntentType,
        val knowledgeState: KnowledgeState,
        val route: IntelligenceRoute,
        val confidence: Int,
        val requiresVerification: Boolean,
        val requiresOwnerPermission: Boolean,
        val requiresExternalProvider: Boolean,
        val reasoningSteps: List<String>,
        val knowledgeAreas: List<String>,
        val warnings: List<String>
    )

    fun analyze(
        context: android.content.Context,
        message: String
    ): IntelligenceState {

        val cleanMessage =
            message.trim()

        if (cleanMessage.isBlank()) {
            return IntelligenceState(
                intent = IntentType.GENERAL,
                knowledgeState = KnowledgeState.UNKNOWN,
                route = IntelligenceRoute.UNKNOWN,
                confidence = 0,
                requiresVerification = false,
                requiresOwnerPermission = false,
                requiresExternalProvider = false,
                reasoningSteps =
                    listOf(
                        "Reject empty request."
                    ),
                knowledgeAreas = emptyList(),
                warnings =
                    listOf(
                        "REQUEST_EMPTY"
                    )
            )
        }

        if (AzimiAuth.isProtectedCredential(cleanMessage)) {
            return IntelligenceState(
                intent = IntentType.SECURE,
                knowledgeState = KnowledgeState.UNKNOWN,
                route = IntelligenceRoute.UNKNOWN,
                confidence = 100,
                requiresVerification = false,
                requiresOwnerPermission = true,
                requiresExternalProvider = false,
                reasoningSteps =
                    listOf(
                        "Detect protected credential material.",
                        "Stop intelligence processing.",
                        "Keep protected material outside Atlas intelligence."
                    ),
                knowledgeAreas =
                    listOf(
                        "SECURITY"
                    ),
                warnings =
                    listOf(
                        "PROTECTED_CREDENTIAL_BLOCKED"
                    )
            )
        }

        val requirement =
            AtlasRequirementEngine.analyze(
                request = cleanMessage
            )

        val intent =
            detectIntent(cleanMessage)

        val areas =
            detectKnowledgeAreas(cleanMessage)

        val requiresOwnerPermission =
            requirement.securityLevel ==
                AtlasRequirementEngine.SecurityLevel.PROTECTED ||
                requiresOwnerAction(cleanMessage)

        val requiresExternalProvider =
            requiresDeepReasoning(intent) ||
                requiresResearch(intent) ||
                requiresGeneralGeneration(intent)

        val route =
            when {
                requiresOwnerPermission ->
                    IntelligenceRoute.TOOL_REQUIRED

                requiresExternalProvider ->
                    IntelligenceRoute.EXTERNAL_PROVIDER

                areas.isNotEmpty() ->
                    IntelligenceRoute.KNOWLEDGE

                else ->
                    IntelligenceRoute.LOCAL
            }

        val knowledgeState =
            when {
                intent == IntentType.RESEARCH ->
                    KnowledgeState.UNKNOWN

                areas.isNotEmpty() ->
                    KnowledgeState.KNOWN

                else ->
                    KnowledgeState.INFERRED
            }

        val requiresVerification =
            intent == IntentType.RESEARCH ||
                intent == IntentType.SECURE ||
                intent == IntentType.RECOVER ||
                intent == IntentType.DEBUG

        val confidence =
            calculateConfidence(
                intent = intent,
                areas = areas,
                knowledgeState = knowledgeState
            )

        return IntelligenceState(
            intent = intent,
            knowledgeState = knowledgeState,
            route = route,
            confidence = confidence,
            requiresVerification = requiresVerification,
            requiresOwnerPermission = requiresOwnerPermission,
            requiresExternalProvider = requiresExternalProvider,
            reasoningSteps =
                buildReasoningSteps(
                    intent = intent,
                    knowledgeState = knowledgeState,
                    route = route,
                    requiresVerification = requiresVerification
                ),
            knowledgeAreas = areas,
            warnings =
                buildWarnings(
                    knowledgeState = knowledgeState,
                    requiresVerification = requiresVerification
                )
        )
    }

    private fun detectIntent(
        message: String
    ): IntentType {

        val text =
            message.lowercase()

        return when {

            containsAny(
                text,
                "debug",
                "error",
                "bug",
                "crash",
                "failed build",
                "compiler"
            ) ->
                IntentType.DEBUG

            containsAny(
                text,
                "build",
                "create",
                "implement",
                "code",
                "develop"
            ) ->
                IntentType.BUILD

            containsAny(
                text,
                "design",
                "architecture",
                "architect"
            ) ->
                IntentType.DESIGN

            containsAny(
                text,
                "research",
                "investigate",
                "find information",
                "look up"
            ) ->
                IntentType.RESEARCH

            containsAny(
                text,
                "plan",
                "roadmap",
                "strategy",
                "steps"
            ) ->
                IntentType.PLAN

            containsAny(
                text,
                "security",
                "secure",
                "protect",
                "permission",
                "authentication"
            ) ->
                IntentType.SECURE

            containsAny(
                text,
                "recover",
                "recovery",
                "restore",
                "backup"
            ) ->
                IntentType.RECOVER

            containsAny(
                text,
                "automate",
                "automation",
                "workflow"
            ) ->
                IntentType.AUTOMATE

            containsAny(
                text,
                "learn",
                "teach",
                "study",
                "lesson"
            ) ->
                IntentType.LEARN

            containsAny(
                text,
                "translate",
                "translation"
            ) ->
                IntentType.TRANSLATE

            containsAny(
                text,
                "write",
                "rewrite",
                "draft",
                "essay",
                "email"
            ) ->
                IntentType.WRITE

            containsAny(
                text,
                "analyze",
                "analysis",
                "compare",
                "evaluate"
            ) ->
                IntentType.ANALYZE

            containsAny(
                text,
                "explain",
                "what is",
                "how does",
                "why"
            ) ->
                IntentType.EXPLAIN

            else ->
                IntentType.GENERAL
        }
    }

    private fun detectKnowledgeAreas(
        message: String
    ): List<String> {

        val text =
            message.lowercase()

        val areas =
            mutableListOf<String>()

        fun addIfMatched(
            area: String,
            vararg keywords: String
        ) {
            if (containsAny(text, *keywords)) {
                areas.add(area)
            }
        }

        addIfMatched(
            "ANDROID",
            "android",
            "kotlin",
            "apk",
            "guardian"
        )

        addIfMatched(
            "WINDOWS",
            "windows",
            ".net",
            "c#",
            "powershell"
        )

        addIfMatched(
            "WEB",
            "web",
            "website",
            "next.js",
            "react",
            "javascript",
            "typescript"
        )

        addIfMatched(
            "CLOUD",
            "cloud",
            "azure",
            "vercel",
            "cloudflare",
            "supabase"
        )

        addIfMatched(
            "SECURITY",
            "security",
            "authentication",
            "authorization",
            "encryption",
            "vault"
        )

        addIfMatched(
            "AI",
            "ai",
            "artificial intelligence",
            "machine learning",
            "model",
            "llm"
        )

        addIfMatched(
            "AUTOMATION",
            "automation",
            "workflow",
            "automate"
        )

        addIfMatched(
            "PROGRAMMING",
            "code",
            "programming",
            "developer",
            "software"
        )

        addIfMatched(
            "RECOVERY",
            "backup",
            "recovery",
            "restore"
        )

        addIfMatched(
            "ARCHITECTURE",
            "architecture",
            "system design",
            "design"
        )

        addIfMatched(
            "RESEARCH",
            "research",
            "investigate",
            "evidence",
            "source"
        )

        addIfMatched(
            "LANGUAGES",
            "language",
            "english",
            "dari",
            "translation"
        )

        return areas.distinct()
    }

    private fun requiresOwnerAction(
        message: String
    ): Boolean {

        val text =
            message.lowercase()

        return containsAny(
            text,
            "unlock",
            "lock",
            "vault",
            "z origin",
            "owner",
            "permission",
            "authorize",
            "device setting"
        )
    }

    private fun requiresDeepReasoning(
        intent: IntentType
    ): Boolean {

        return intent == IntentType.ANALYZE ||
            intent == IntentType.DESIGN ||
            intent == IntentType.BUILD ||
            intent == IntentType.DEBUG ||
            intent == IntentType.PLAN
    }

    private fun requiresResearch(
        intent: IntentType
    ): Boolean {

        return intent == IntentType.RESEARCH
    }

    private fun requiresGeneralGeneration(
        intent: IntentType
    ): Boolean {

        return intent == IntentType.GENERAL ||
            intent == IntentType.WRITE ||
            intent == IntentType.EXPLAIN ||
            intent == IntentType.LEARN ||
            intent == IntentType.TRANSLATE
    }

    private fun calculateConfidence(
        intent: IntentType,
        areas: List<String>,
        knowledgeState: KnowledgeState
    ): Int {

        var confidence = 50

        if (intent != IntentType.GENERAL) {
            confidence += 15
        }

        if (areas.isNotEmpty()) {
            confidence += 15
        }

        when (knowledgeState) {
            KnowledgeState.VERIFIED ->
                confidence += 20

            KnowledgeState.KNOWN ->
                confidence += 10

            KnowledgeState.INFERRED ->
                confidence -= 5

            KnowledgeState.UNKNOWN ->
                confidence -= 20
        }

        return confidence.coerceIn(0, 100)
    }

    private fun buildReasoningSteps(
        intent: IntentType,
        knowledgeState: KnowledgeState,
        route: IntelligenceRoute,
        requiresVerification: Boolean
    ): List<String> {

        val steps =
            mutableListOf<String>()

        steps.add("Identify user intent: $intent.")
        steps.add("Determine available knowledge state: $knowledgeState.")
        steps.add("Select intelligence route: $route.")

        if (requiresVerification) {
            steps.add(
                "Verify important information before presenting it as established fact."
            )
        }

        steps.add(
            "Keep protected credentials outside the intelligence pipeline."
        )

        steps.add(
            "Use external AI only as a replaceable execution engine when required."
        )

        return steps
    }

    private fun buildWarnings(
        knowledgeState: KnowledgeState,
        requiresVerification: Boolean
    ): List<String> {

        val warnings =
            mutableListOf<String>()

        if (knowledgeState == KnowledgeState.UNKNOWN) {
            warnings.add(
                "KNOWLEDGE_NOT_ESTABLISHED"
            )
        }

        if (knowledgeState == KnowledgeState.INFERRED) {
            warnings.add(
                "RESULT_MAY_REQUIRE_VERIFICATION"
            )
        }

        if (requiresVerification) {
            warnings.add(
                "VERIFICATION_RECOMMENDED"
            )
        }

        return warnings
    }

    private fun containsAny(
        text: String,
        vararg values: String
    ): Boolean {

        return values.any {
            text.contains(it)
        }
    }
}
