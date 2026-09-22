package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Local Engine
 *
 * First-generation local/offline intelligence engine for Atlas.
 *
 * Purpose:
 * - Keep Atlas useful without internet
 * - Use AZIMI's local knowledge
 * - Understand common AZIMI project requests
 * - Provide safe local explanations
 * - Provide local diagnostics and planning
 * - Work without an external AI provider
 *
 * IMPORTANT:
 * This is NOT a large language model.
 *
 * It is a deterministic local intelligence layer.
 *
 * A future local language model can be connected through
 * this engine without changing AtlasCore or AtlasRouter.
 *
 * Architecture:
 *
 * User
 *   ↓
 * AtlasCore
 *   ↓
 * AtlasRequirementEngine
 *   ↓
 * AtlasRouter
 *   ↓
 * AtlasLocalEngine
 *   ↓
 * AtlasKnowledge
 *
 * Security:
 * - Read-only
 * - No network access
 * - No credentials
 * - No external provider
 * - No consequential actions
 * - No permission bypass
 */
object AtlasLocalEngine {

    /**
     * Result returned by the local engine.
     */
    data class LocalResult(
        val success: Boolean,
        val reply: String,
        val status: String,
        val confidence: Int,
        val usedKnowledge: Boolean,
        val capability: String,
        val requiresOnlineAI: Boolean,
        val requiresOwnerPermission: Boolean
    )

    /**
     * Local capabilities currently supported.
     */
    enum class LocalCapability {

        PROJECT_KNOWLEDGE,

        ARCHITECTURE,

        SECURITY,

        RECOVERY,

        REQUIREMENT_ANALYSIS,

        AVAILABILITY,

        PROJECT_STATUS,

        GENERAL_EXPLANATION,

        LANGUAGE,

        OFFLINE_STATUS,

        UNKNOWN
    }

    /**
     * Main local processing entry point.
     *
     * This method never contacts the internet.
     */
    fun process(
        context: Context,
        request: String
    ): LocalResult {

        val appContext =
            context.applicationContext

        val cleanRequest =
            request.trim()

        if (cleanRequest.isBlank()) {
            return failure(
                message =
                    "Atlas received an empty request."
            )
        }

        /*
         * Protected information must never become
         * ordinary local context.
         */
        if (
            AzimiAuth.isProtectedCredential(
                cleanRequest
            )
        ) {
            return LocalResult(
                success = false,
                reply =
                    "Atlas blocked protected credential material.",
                status = "SECURITY_BLOCK",
                confidence = 100,
                usedKnowledge = false,
                capability =
                    LocalCapability.SECURITY.name,
                requiresOnlineAI = false,
                requiresOwnerPermission = false
            )
        }

        /*
         * Guardian AI policy remains a local security boundary.
         */
        val policy =
            GuardianStorage.getAIMemoryPolicy(
                appContext
            )

        if (
            policy != "SAFE_CONTEXT_ONLY"
        ) {
            return LocalResult(
                success = false,
                reply =
                    "Atlas local intelligence is restricted because Guardian AI policy is not SAFE_CONTEXT_ONLY.",
                status = "POLICY_BLOCK",
                confidence = 100,
                usedKnowledge = false,
                capability =
                    LocalCapability.SECURITY.name,
                requiresOnlineAI = false,
                requiresOwnerPermission = true
            )
        }

        /*
         * Requirement analysis is local.
         */
        val analysis =
            AtlasRequirementEngine.analyze(
                cleanRequest
            )

        /*
         * Select the local capability.
         */
        val capability =
            determineCapability(
                analysis,
                cleanRequest
            )

        /*
         * Generate the local response.
         */
        return when (capability) {

            LocalCapability.PROJECT_KNOWLEDGE ->
                projectKnowledgeResponse(
                    cleanRequest,
                    analysis
                )

            LocalCapability.ARCHITECTURE ->
                architectureResponse(
                    cleanRequest,
                    analysis
                )

            LocalCapability.SECURITY ->
                securityResponse(
                    cleanRequest,
                    analysis
                )

            LocalCapability.RECOVERY ->
                recoveryResponse(
                    cleanRequest,
                    analysis
                )

            LocalCapability.REQUIREMENT_ANALYSIS ->
                requirementResponse(
                    cleanRequest,
                    analysis
                )

            LocalCapability.AVAILABILITY ->
                availabilityResponse(
                    appContext
                )

            LocalCapability.PROJECT_STATUS ->
                projectStatusResponse(
                    cleanRequest
                )

            LocalCapability.LANGUAGE ->
                languageResponse(
                    cleanRequest
                )

            LocalCapability.OFFLINE_STATUS ->
                offlineStatusResponse(
                    appContext
                )

            LocalCapability.GENERAL_EXPLANATION ->
                generalExplanation(
                    cleanRequest,
                    analysis
                )

            LocalCapability.UNKNOWN ->
                unknownResponse(
                    cleanRequest,
                    analysis
                )
        }
    }

    /**
     * Determines which local capability should handle
     * the request.
     */
    private fun determineCapability(
        analysis:
            AtlasRequirementEngine.RequirementAnalysis,
        request: String
    ): LocalCapability {

        val text =
            request.lowercase()

        if (
            containsAny(
                text,
                "offline",
                "without internet",
                "no internet",
                "internet unavailable",
                "can you work offline"
            )
        ) {
            return LocalCapability.OFFLINE_STATUS
        }

        if (
            containsAny(
                text,
                "availability",
                "available",
                "online or offline",
                "what mode",
                "current mode"
            )
        ) {
            return LocalCapability.AVAILABILITY
        }

        if (
            containsAny(
                text,
                "security",
                "secure",
                "protect",
                "guardian security",
                "vault",
                "shield",
                "permission"
            )
        ) {
            return LocalCapability.SECURITY
        }

        if (
            containsAny(
                text,
                "backup",
                "restore",
                "recovery",
                "rollback",
                "checkpoint",
                "preserve"
            )
        ) {
            return LocalCapability.RECOVERY
        }

        if (
            containsAny(
                text,
                "architecture",
                "structure",
                "how is azimi built",
                "how is atlas built",
                "components",
                "modules"
            )
        ) {
            return LocalCapability.ARCHITECTURE
        }

        if (
            containsAny(
                text,
                "status",
                "state",
                "what exists",
                "what do we have",
                "what is built",
                "current project"
            )
        ) {
            return LocalCapability.PROJECT_STATUS
        }

        if (
            containsAny(
                text,
                "requirement",
                "requirements",
                "what do we need",
                "dependencies",
                "what is needed"
            )
        ) {
            return LocalCapability.REQUIREMENT_ANALYSIS
        }

        if (
            containsAny(
                text,
                "dari",
                "english",
                "language",
                "languages",
                "multilingual",
                "translation"
            )
        ) {
            return LocalCapability.LANGUAGE
        }

        if (
            containsAny(
                text,
                "azimi",
                "atlas",
                "guardian",
                "z vault",
                "z origin",
                "z recovery",
                "z shield",
                "z cloud",
                "z launcher",
                "z lab"
            )
        ) {
            return LocalCapability.PROJECT_KNOWLEDGE
        }

        if (
            analysis.intent ==
                AtlasRequirementEngine.Intent.UNDERSTAND ||
            analysis.intent ==
                AtlasRequirementEngine.Intent.LEARN
        ) {
            return LocalCapability.GENERAL_EXPLANATION
        }

        return LocalCapability.UNKNOWN
    }

    /**
     * Answers project knowledge questions locally.
     */
    private fun projectKnowledgeResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        val knowledge =
            AtlasKnowledge.getKnowledgeSummary()

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — LOCAL KNOWLEDGE"
                    )

                    appendLine()

                    appendLine(
                        "I am using the local AZIMI knowledge layer."
                    )

                    appendLine()

                    appendLine(
                        "SYSTEM: ${AtlasKnowledge.SYSTEM_NAME}"
                    )

                    appendLine(
                        "INTELLIGENCE: ${AtlasKnowledge.INTELLIGENCE_NAME}"
                    )

                    appendLine(
                        "OWNER: ${AtlasKnowledge.OWNER_NAME}"
                    )

                    appendLine(
                        "KNOWLEDGE VERSION: ${AtlasKnowledge.KNOWLEDGE_VERSION}"
                    )

                    appendLine()

                    appendLine(
                        knowledge
                    )

                    appendLine()

                    appendLine(
                        "REQUEST CLASSIFICATION:"
                    )

                    appendLine(
                        "INTENT: ${analysis.intent}"
                    )

                    appendLine(
                        "CATEGORY: ${analysis.category}"
                    )

                    appendLine(
                        "STATUS: ${analysis.status}"
                    )

                    appendLine()

                    appendLine(
                        "This response was generated locally without external AI."
                    )
                },
            capability =
                LocalCapability.PROJECT_KNOWLEDGE,
            confidence = 94
        )
    }

    /**
     * Answers architecture questions locally.
     */
    private fun architectureResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — AZIMI ARCHITECTURE"
                    )

                    appendLine()

                    appendLine(
                        "CORE INTELLIGENCE"
                    )

                    appendLine(
                        "• AtlasCore — central orchestration"
                    )

                    appendLine(
                        "• AtlasKnowledge — approved AZIMI knowledge"
                    )

                    appendLine(
                        "• AtlasRequirementEngine — requirement analysis"
                    )

                    appendLine(
                        "• AtlasAvailability — environment detection"
                    )

                    appendLine(
                        "• AtlasRouter — intelligence path selection"
                    )

                    appendLine(
                        "• AtlasLocalEngine — offline/local intelligence"
                    )

                    appendLine()

                    appendLine(
                        "GUARDIAN"
                    )

                    appendLine(
                        "• AtlasGuardianBridge"
                    )

                    appendLine(
                        "• Z Vault"
                    )

                    appendLine(
                        "• Z Shield"
                    )

                    appendLine(
                        "• Z Recovery"
                    )

                    appendLine(
                        "• Z Control"
                    )

                    appendLine(
                        "• Z Origin"
                    )

                    appendLine()

                    appendLine(
                        "CONNECTIVITY"
                    )

                    appendLine(
                        "• AzimiNetwork"
                    )

                    appendLine(
                        "• AzimiAiClient"
                    )

                    appendLine(
                        "• Replaceable AI provider adapters"
                    )

                    appendLine()

                    appendLine(
                        "DESIGN PRINCIPLE"
                    )

                    appendLine(
                        "External AI is an adapter. Atlas itself remains the orchestration and intelligence layer."
                    )

                    appendLine()

                    appendLine(
                        "REQUEST:"
                    )

                    appendLine(
                        request
                    )

                    appendLine()

                    appendLine(
                        "CLASSIFICATION: ${analysis.category}"
                    )
                },
            capability =
                LocalCapability.ARCHITECTURE,
            confidence = 96
        )
    }

    /**
     * Answers local security questions.
     */
    private fun securityResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — SECURITY"
                    )

                    appendLine()

                    appendLine(
                        "Guardian remains the security authority."
                    )

                    appendLine()

                    appendLine(
                        "CURRENT SECURITY PRINCIPLES:"
                    )

                    appendLine(
                        "• Protected credentials must not enter ordinary Atlas context."
                    )

                    appendLine(
                        "• Authentication tokens must not be exposed in responses."
                    )

                    appendLine(
                        "• Sensitive operations require appropriate authorization."
                    )

                    appendLine(
                        "• Atlas must not bypass Android or Guardian security boundaries."
                    )

                    appendLine(
                        "• External AI access must remain behind the approved adapter."
                    )

                    appendLine(
                        "• Approved memory must not contain passwords, API keys, recovery codes, or private credentials."
                    )

                    appendLine()

                    appendLine(
                        "REQUEST SECURITY LEVEL: ${analysis.securityLevel}"
                    )

                    appendLine(
                        "PERMISSION REQUIRED: ${analysis.permissionRequired}"
                    )

                    appendLine()

                    appendLine(
                        "LOCAL SECURITY RESPONSE COMPLETE."
                    )
                },
            capability =
                LocalCapability.SECURITY,
            confidence = 97
        )
    }

    /**
     * Answers backup and recovery questions locally.
     */
    private fun recoveryResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — RECOVERY"
                    )

                    appendLine()

                    appendLine(
                        "AZIMI recovery principle:"
                    )

                    appendLine(
                        "BACKUP → CHANGE → BUILD → TEST → VERIFY → CHECKPOINT"
                    )

                    appendLine()

                    appendLine(
                        "If a change fails:"
                    )

                    appendLine(
                        "ROLLBACK → LAST KNOWN GOOD → DIAGNOSE → FIX → TEST AGAIN"
                    )

                    appendLine()

                    appendLine(
                        "Important recovery rules:"
                    )

                    appendLine(
                        "• Preserve the existing known-good source."
                    )

                    appendLine(
                        "• Create checkpoints before consequential changes."
                    )

                    appendLine(
                        "• Keep architecture and decision records portable."
                    )

                    appendLine(
                        "• Never place secrets inside Atlas knowledge or recovery manifests."
                    )

                    appendLine(
                        "• Verify a checkpoint before treating it as a known-good recovery point."
                    )

                    appendLine()

                    appendLine(
                        "REQUEST CLASSIFICATION: ${analysis.category}"
                    )

                    appendLine(
                        "BACKUP RECOMMENDED: ${analysis.backupRecommended}"
                    )

                    appendLine(
                        "PERMISSION REQUIRED: ${analysis.permissionRequired}"
                    )
                },
            capability =
                LocalCapability.RECOVERY,
            confidence = 98
        )
    }

    /**
     * Answers requirement questions locally.
     */
    private fun requirementResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply =
                AtlasRequirementEngine.buildSummary(
                    analysis
                ),
            capability =
                LocalCapability.REQUIREMENT_ANALYSIS,
            confidence = 95
        )
    }

    /**
     * Returns live Atlas availability information.
     */
    private fun availabilityResponse(
        context: Context
    ): LocalResult {

        val availability =
            AtlasAvailability.detect(
                context.applicationContext
            )

        return success(
            reply =
                availability.toSafeStatusMessage(),
            capability =
                LocalCapability.AVAILABILITY,
            confidence = 99
        )
    }

    /**
     * Returns current AZIMI project status from local knowledge.
     */
    private fun projectStatusResponse(
        request: String
    ): LocalResult {

        val components =
            AtlasKnowledge.getComponentsByStatus(
                AtlasKnowledge.ComponentStatus.EXISTS
            )

        val developmentComponents =
            AtlasKnowledge.getComponentsByStatus(
                AtlasKnowledge.ComponentStatus.IN_DEVELOPMENT
            )

        val plannedComponents =
            AtlasKnowledge.getComponentsByStatus(
                AtlasKnowledge.ComponentStatus.PLANNED
            )

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — LOCAL PROJECT STATUS"
                    )

                    appendLine()

                    appendLine(
                        "KNOWLEDGE VERSION: ${AtlasKnowledge.KNOWLEDGE_VERSION}"
                    )

                    appendLine()

                    appendLine(
                        "EXISTING COMPONENTS:"
                    )

                    components.forEach {
                        appendLine(
                            "• ${it.name}"
                        )
                    }

                    appendLine()

                    appendLine(
                        "IN DEVELOPMENT:"
                    )

                    developmentComponents.forEach {
                        appendLine(
                            "• ${it.name}"
                        )
                    }

                    appendLine()

                    appendLine(
                        "PLANNED:"
                    )

                    plannedComponents.forEach {
                        appendLine(
                            "• ${it.name}"
                        )
                    }

                    appendLine()

                    appendLine(
                        "CURRENT RECOVERY POINT:"
                    )

                    appendLine(
                        AtlasKnowledge.CURRENT_RECOVERY_POINT
                    )

                    appendLine()

                    appendLine(
                        "NEXT TARGET CHECKPOINT:"
                    )

                    appendLine(
                        AtlasKnowledge.NEXT_TARGET_CHECKPOINT
                    )

                    appendLine()

                    appendLine(
                        "This status was read locally from Atlas Knowledge."
                    )
                },
            capability =
                LocalCapability.PROJECT_STATUS,
            confidence = 97
        )
    }

    /**
     * Answers language capability questions.
     */
    private fun languageResponse(
        request: String
    ): LocalResult {

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — LANGUAGE"
                    )

                    appendLine()

                    appendLine(
                        "Current primary languages:"
                    )

                    appendLine(
                        "• English"
                    )

                    appendLine(
                        "• Dari"
                    )

                    appendLine()

                    appendLine(
                        "AZIMI is designed so additional languages can be added through the localization layer."
                    )

                    appendLine()

                    appendLine(
                        "Voice input and voice output are separate capabilities and will be connected through the Android voice layer."
                    )

                    appendLine()

                    appendLine(
                        "LANGUAGE FOUNDATION: ZLanguage"
                    )
                },
            capability =
                LocalCapability.LANGUAGE,
            confidence = 96
        )
    }

    /**
     * Explains offline operation.
     */
    private fun offlineStatusResponse(
        context: Context
    ): LocalResult {

        val availability =
            AtlasAvailability.detect(
                context.applicationContext
            )

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — OFFLINE MODE"
                    )

                    appendLine()

                    appendLine(
                        "Yes. Atlas has a local intelligence foundation."
                    )

                    appendLine()

                    appendLine(
                        "CURRENT LOCAL CAPABILITIES:"
                    )

                    appendLine(
                        "• AZIMI project knowledge"
                    )

                    appendLine(
                        "• Requirement analysis"
                    )

                    appendLine(
                        "• Architecture explanations"
                    )

                    appendLine(
                        "• Security guidance"
                    )

                    appendLine(
                        "• Recovery guidance"
                    )

                    appendLine(
                        "• Project status"
                    )

                    appendLine(
                        "• Availability diagnostics"
                    )

                    appendLine()

                    appendLine(
                        "LOCAL LANGUAGE MODEL:"
                    )

                    appendLine(
                        if (availability.localEngineAvailable) {
                            "Available."
                        } else {
                            "Not implemented yet."
                        }
                    )

                    appendLine()

                    appendLine(
                        "IMPORTANT:"
                    )

                    appendLine(
                        "A full local language model will be added as a replaceable adapter later. Atlas will not claim that capability until it is actually installed and verified."
                    )

                    appendLine()

                    appendLine(
                        "INTERNET:"
                    )

                    appendLine(
                        if (availability.internetAvailable) {
                            "Currently available."
                        } else {
                            "Currently unavailable."
                        }
                    )
                },
            capability =
                LocalCapability.OFFLINE_STATUS,
            confidence = 99
        )
    }

    /**
     * Provides a safe general local explanation.
     */
    private fun generalExplanation(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — LOCAL RESPONSE"
                    )

                    appendLine()

                    appendLine(
                        "I can understand this request locally, but the current offline engine does not contain enough verified knowledge to provide a complete answer."
                    )

                    appendLine()

                    appendLine(
                        "REQUEST:"
                    )

                    appendLine(
                        request
                    )

                    appendLine()

                    appendLine(
                        "INTENT: ${analysis.intent}"
                    )

                    appendLine(
                        "CATEGORY: ${analysis.category}"
                    )

                    appendLine(
                        "SECURITY: ${analysis.securityLevel}"
                    )

                    appendLine()

                    appendLine(
                        "NEXT SAFE ACTION:"
                    )

                    appendLine(
                        analysis.nextSafeAction
                    )

                    appendLine()

                    appendLine(
                        "A future local language model can expand this capability without changing Atlas's core architecture."
                    )
                },
            capability =
                LocalCapability.GENERAL_EXPLANATION,
            confidence = 78,
            requiresOnlineAI = true
        )
    }

    /**
     * Handles requests that the current local engine
     * cannot confidently understand.
     */
    private fun unknownResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply =
                buildString {

                    appendLine(
                        "ATLAS — LOCAL CAPABILITY LIMIT"
                    )

                    appendLine()

                    appendLine(
                        "I received your request, but the current offline intelligence layer does not yet have a verified local capability for it."
                    )

                    appendLine()

                    appendLine(
                        "REQUEST:"
                    )

                    appendLine(
                        request
                    )

                    appendLine()

                    appendLine(
                        "DETECTED INTENT: ${analysis.intent}"
                    )

                    appendLine(
                        "DETECTED CATEGORY: ${analysis.category}"
                    )

                    appendLine(
                        "STATUS: ${analysis.status}"
                    )

                    appendLine()

                    appendLine(
                        "WHAT I CAN DO:"
                    )

                    appendLine(
                        "• Use local AZIMI knowledge"
                    )

                    appendLine(
                        "• Analyze requirements"
                    )

                    appendLine(
                        "• Explain AZIMI architecture"
                    )

                    appendLine(
                        "• Explain security and recovery principles"
                    )

                    appendLine(
                        "• Report local availability"
                    )

                    appendLine()

                    appendLine(
                        "For deeper reasoning, an authenticated online AI adapter may be required."
                    )
                },
            capability =
                LocalCapability.UNKNOWN,
            confidence = 65,
            requiresOnlineAI = true
        )
    }

    /**
     * Creates a successful local result.
     */
    private fun success(
        reply: String,
        capability: LocalCapability,
        confidence: Int,
        requiresOnlineAI: Boolean = false,
        requiresOwnerPermission: Boolean = false
    ): LocalResult {

        return LocalResult(
            success = true,
            reply = reply,
            status = "LOCAL_RESPONSE_READY",
            confidence =
                confidence.coerceIn(0, 100),
            usedKnowledge = true,
            capability = capability.name,
            requiresOnlineAI = requiresOnlineAI,
            requiresOwnerPermission =
                requiresOwnerPermission
        )
    }

    /**
     * Creates a safe failure result.
     */
    private fun failure(
        message: String
    ): LocalResult {

        return LocalResult(
            success = false,
            reply = message,
            status = "LOCAL_ENGINE_ERROR",
            confidence = 100,
            usedKnowledge = false,
            capability =
                LocalCapability.UNKNOWN.name,
            requiresOnlineAI = false,
            requiresOwnerPermission = false
        )
    }

    /**
     * Checks multiple keywords safely.
     */
    private fun containsAny(
        text: String,
        vararg values: String
    ): Boolean {

        return values.any { value ->
            text.contains(value)
        }
    }

    /**
     * Simple engine health check.
     *
     * Returns true because this local deterministic
     * engine is implemented in this application.
     */
    fun isAvailable(): Boolean {
        return true
    }

    /**
     * Returns the current local engine version.
     */
    fun version(): String {
        return "1.0.0"
    }

    /**
     * Returns a safe diagnostic report.
     */
    fun diagnostics(): String {

        return buildString {

            appendLine(
                "ATLAS LOCAL ENGINE"
            )

            appendLine()

            appendLine(
                "AVAILABLE: ${isAvailable()}"
            )

            appendLine(
                "VERSION: ${version()}"
            )

            appendLine(
                "NETWORK REQUIRED: false"
            )

            appendLine(
                "EXTERNAL AI REQUIRED: false"
            )

            appendLine()

            appendLine(
                "LOCAL CAPABILITIES:"
            )

            appendLine(
                "• Project knowledge"
            )

            appendLine(
                "• Architecture"
            )

            appendLine(
                "• Security"
            )

            appendLine(
                "• Recovery"
            )

            appendLine(
                "• Requirement analysis"
            )

            appendLine(
                "• Availability"
            )

            appendLine(
                "• Project status"
            )

            appendLine(
                "• Language information"
            )

            appendLine(
                "• Offline status"
            )

            appendLine()

            appendLine(
                "LOCAL LANGUAGE MODEL: NOT IMPLEMENTED"
            )

            appendLine(
                "FUTURE: REPLACEABLE LOCAL MODEL ADAPTER"
            )
        }
    }
}
