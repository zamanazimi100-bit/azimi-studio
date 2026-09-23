package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Local Engine
 *
 * Deterministic local/offline intelligence layer.
 *
 * This is NOT a large language model.
 *
 * It provides:
 * - AZIMI project knowledge
 * - architecture explanations
 * - security explanations
 * - recovery guidance
 * - requirement analysis
 * - availability diagnostics
 * - project status
 * - language information
 * - offline capability information
 *
 * Security:
 * - read-only
 * - no network access
 * - no credentials
 * - no provider calls
 * - no consequential actions
 * - no permission bypass
 */
object AtlasLocalEngine {

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
                "Atlas received an empty request."
            )
        }

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

        val analysis =
            AtlasRequirementEngine.analyze(
                cleanRequest
            )

        val capability =
            determineCapability(
                analysis,
                cleanRequest
            )

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
                    analysis
                )

            LocalCapability.AVAILABILITY ->
                availabilityResponse(
                    appContext
                )

            LocalCapability.PROJECT_STATUS ->
                projectStatusResponse()

            LocalCapability.LANGUAGE ->
                languageResponse()

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

    private fun projectKnowledgeResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply = buildString {

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
                    AtlasKnowledge.getKnowledgeSummary()
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

    private fun architectureResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply = buildString {

                appendLine(
                    "ATLAS — AZIMI ARCHITECTURE"
                )

                appendLine()

                appendLine("CORE INTELLIGENCE")

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

                appendLine("GUARDIAN")

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

                appendLine("CONNECTIVITY")

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

                appendLine("DESIGN PRINCIPLE")

                appendLine(
                    "External AI is an adapter. Atlas remains the orchestration and intelligence layer."
                )

                appendLine()

                appendLine(
                    "REQUEST: $request"
                )

                appendLine(
                    "CLASSIFICATION: ${analysis.category}"
                )
            },
            capability =
                LocalCapability.ARCHITECTURE,
            confidence = 96
        )
    }

    private fun securityResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply = buildString {

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
                    "• External AI access remains behind the approved adapter."
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

    private fun recoveryResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply = buildString {

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
                    "• Verify a checkpoint before treating it as known-good."
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

    private fun requirementResponse(
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

    private fun projectStatusResponse(): LocalResult {

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
            reply = buildString {

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

    private fun languageResponse(): LocalResult {

        return success(
            reply = buildString {

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
                    "Voice input and voice output remain separate Android capabilities."
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

    private fun offlineStatusResponse(
        context: Context
    ): LocalResult {

        val availability =
            AtlasAvailability.detect(
                context.applicationContext
            )

        return success(
            reply = buildString {

                appendLine(
                    "ATLAS — OFFLINE MODE"
                )

                appendLine()

                appendLine(
                    "Atlas has a deterministic local intelligence foundation."
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
                    "LOCAL ENGINE:"
                )

                appendLine(
                    if (availability.localEngineAvailable) {
                        "Available."
                    } else {
                        "Unavailable."
                    }
                )

                appendLine()

                appendLine(
                    "LOCAL LANGUAGE MODEL:"
                )

                appendLine(
                    "Not implemented yet."
                )

                appendLine()

                appendLine(
                    "A full local language model will be added as a replaceable adapter later."
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

    private fun generalExplanation(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply = buildString {

                appendLine(
                    "ATLAS — LOCAL RESPONSE"
                )

                appendLine()

                appendLine(
                    "The deterministic offline engine understands this request category, but it does not contain enough verified local knowledge to provide a complete answer."
                )

                appendLine()

                appendLine(
                    "REQUEST: $request"
                )

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
                    "A future local language model can expand this capability without changing AtlasCore or AtlasRouter."
                )
            },
            capability =
                LocalCapability.GENERAL_EXPLANATION,
            confidence = 78,
            requiresOnlineAI = true
        )
    }

    private fun unknownResponse(
        request: String,
        analysis:
            AtlasRequirementEngine.RequirementAnalysis
    ): LocalResult {

        return success(
            reply = buildString {

                appendLine(
                    "ATLAS — LOCAL CAPABILITY LIMIT"
                )

                appendLine()

                appendLine(
                    "The current offline intelligence layer does not yet have a verified local capability for this request."
                )

                appendLine()

                appendLine(
                    "REQUEST: $request"
                )

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
            requiresOnlineAI =
                requiresOnlineAI,
            requiresOwnerPermission =
                requiresOwnerPermission
        )
    }

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

    private fun containsAny(
        text: String,
        vararg values: String
    ): Boolean {

        return values.any {
            value ->
            text.contains(value)
        }
    }

    fun isAvailable(): Boolean {
        return true
    }

    fun version(): String {
        return "1.0.0"
    }

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
