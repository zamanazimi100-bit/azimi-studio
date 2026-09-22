package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Core
 *
 * Central reasoning and orchestration foundation for Atlas.
 *
 * Atlas Core:
 * - validates the request
 * - respects Guardian security policy
 * - consults Atlas Knowledge
 * - understands the current actor/authority
 * - distinguishes owner-sensitive requests
 * - identifies relevant AZIMI components
 * - detects requirements and dependencies
 * - identifies security considerations
 * - determines whether permission is required
 * - creates a safe project-aware plan
 *
 * Atlas Core does NOT directly execute consequential actions.
 *
 * Future modules can plug into this core:
 * - Atlas Memory
 * - Z Vault
 * - Capability Engine
 * - Permission Engine
 * - Internet Gateway
 * - Project Tools
 * - Diagnostics
 * - Audit Trail
 * - Replaceable AI adapters
 */
object AtlasCore {

    data class AtlasRequest(
        val message: String,
        val history:
            List<AzimiAiClient.ChatMessage> =
            emptyList()
    )

    data class AtlasPlan(
        val task: String,
        val capability: String,
        val permissionRequired: Boolean,
        val externalProviderRequired: Boolean,
        val steps: List<String>,
        val knowledgeAreas:
            List<String> = emptyList(),
        val relevantComponents:
            List<String> = emptyList(),
        val dependencies:
            List<String> = emptyList(),
        val securityRequirements:
            List<String> = emptyList(),
        val warnings:
            List<String> = emptyList()
    )

    data class AtlasResult(
        val success: Boolean,
        val message: String,
        val plan: AtlasPlan? = null,
        val status: String = "ATLAS_CORE"
    )

    fun process(
        context: Context,
        request: AtlasRequest
    ): AtlasResult {

        val appContext =
            context.applicationContext

        val message =
            request.message.trim()

        if (message.isBlank()) {
            return AtlasResult(
                success = false,
                message =
                    "Atlas received an empty request.",
                status =
                    "INVALID_REQUEST"
            )
        }

        if (
            AzimiAuth.isProtectedCredential(
                message
            )
        ) {
            return AtlasResult(
                success = false,
                message =
                    "Atlas blocked protected credential material.",
                status =
                    "SECURITY_BLOCK"
            )
        }

        val policy =
            GuardianStorage.getAIMemoryPolicy(
                appContext
            )

        if (
            policy != "SAFE_CONTEXT_ONLY"
        ) {
            return AtlasResult(
                success = false,
                message =
                    "Atlas is blocked because Guardian AI memory policy is not SAFE_CONTEXT_ONLY.",
                status =
                    "POLICY_BLOCK"
            )
        }

        val authority =
            AtlasOwnerAuthority.getState(
                appContext
            )

        val knowledge =
            AtlasKnowledge.analyzeRequest(
                message
            )

        val requirements =
            AtlasRequirementEngine.analyze(
                request = message,
                ownerAuthority = authority
            )

        if (
            requirements.securityLevel ==
                AtlasRequirementEngine.SecurityLevel.PROTECTED
        ) {
            return AtlasResult(
                success = false,
                message =
                    "Atlas blocked protected credential material.",
                status =
                    "SECURITY_BLOCK"
            )
        }

        val plan =
            createPlan(
                message = message,
                knowledge = knowledge,
                requirements = requirements,
                authority = authority
            )

        return AtlasResult(
            success = true,
            message =
                buildPlanMessage(
                    plan = plan,
                    knowledge = knowledge,
                    requirements = requirements,
                    authority = authority
                ),
            plan = plan,
            status =
                "PLAN_READY"
        )
    }

    private fun createPlan(
        message: String,
        knowledge:
            AtlasKnowledge.RequirementProfile,
        requirements:
            AtlasRequirementEngine.RequirementAnalysis,
        authority:
            AtlasOwnerAuthority.AuthorityState
    ): AtlasPlan {

        val normalized =
            message.lowercase()

        val knowledgeAreas =
            knowledge.areas.map {
                it.name
            }

        val relevantComponents =
            knowledge.relevantComponents.map {
                "${it.name} [${it.status.name}]"
            }

        val dependencies =
            (
                knowledge.likelyDependencies +
                    requirements.dependencies
                ).distinct()

        val securityRequirements =
            (
                knowledge.securityRequirements +
                    requirements.warnings
                ).distinct()

        val warnings =
            (
                knowledge.warnings +
                    requirements.warnings
                ).distinct()
                    .toMutableList()

        if (
            requirements.ownershipSensitive
        ) {
            warnings +=
                "Ownership/provenance-sensitive request detected."
        }

        if (
            requirements.ownerAuthorizationRequired &&
            !requirements.ownerAuthorized
        ) {
            warnings +=
                "Owner authorization is not currently active."
        }

        if (
            authority.authorityLevel ==
                AtlasOwnerAuthority.AuthorityLevel.OWNER
        ) {
            warnings +=
                "Current actor has owner authority. Individual consequential operations still require their specific permission boundary."
        }

        if (
            containsAny(
                normalized,
                "github",
                "repository",
                "repo",
                "source code",
                "source",
                "ownership",
                "provenance",
                "creator",
                "founder"
            )
        ) {

            return AtlasPlan(
                task =
                    "PROJECT_PRESERVATION_AND_PROVENANCE",
                capability =
                    "PROJECT_STORAGE_AND_OWNERSHIP",
                permissionRequired =
                    true,
                externalProviderRequired =
                    true,
                steps =
                    listOf(
                        "Inspect the requested project context.",
                        "Identify the current actor and authority level.",
                        "Use Atlas Knowledge to identify relevant AZIMI components.",
                        "Use the Requirement Engine to determine required artifacts and dependencies.",
                        "Determine what project data can be safely preserved.",
                        "Separate technical provenance records from legal ownership claims.",
                        "Request or verify the required owner authorization.",
                        "Create an integrity-checked project snapshot.",
                        "Create or update the appropriate ownership/provenance record.",
                        "Protect eligible sensitive project data through the appropriate security layer.",
                        "Verify the snapshot and generated records.",
                        "Report exactly what was created and what remains unverified."
                    ),
                knowledgeAreas =
                    knowledgeAreas,
                relevantComponents =
                    relevantComponents,
                dependencies =
                    dependencies,
                securityRequirements =
                    securityRequirements,
                warnings =
                    warnings
            )
        }

        if (
            containsAny(
                normalized,
                "build",
                "compile",
                "apk",
                "fix",
                "debug",
                "error",
                "crash",
                "broken",
                "test"
            )
        ) {

            return AtlasPlan(
                task =
                    "PROJECT_ENGINEERING",
                capability =
                    "PROJECT_ANALYSIS",
                permissionRequired =
                    true,
                externalProviderRequired =
                    false,
                steps =
                    listOf(
                        "Inspect the available project context.",
                        "Identify the current actor and authority level.",
                        "Use Atlas Knowledge to identify affected components.",
                        "Use the Requirement Engine to identify the exact requirements.",
                        "Identify the exact failure or requested change.",
                        "Determine available capabilities and dependencies.",
                        "Identify security and permission requirements.",
                        "Create a proposed repair or implementation plan.",
                        "Request permission before consequential changes.",
                        "Build and test the result.",
                        "Verify the result.",
                        "Report the exact outcome and remaining issues."
                    ),
                knowledgeAreas =
                    knowledgeAreas,
                relevantComponents =
                    relevantComponents,
                dependencies =
                    dependencies,
                securityRequirements =
                    securityRequirements,
                warnings =
                    warnings
            )
        }

        if (
            containsAny(
                normalized,
                "backup",
                "preserve",
                "archive",
                "save project",
                "recover",
                "recovery",
                "rollback",
                "restore",
                "checkpoint"
            )
        ) {

            return AtlasPlan(
                task =
                    "AZIMI_BACKUP_RECOVERY",
                capability =
                    "Z_RECOVERY",
                permissionRequired =
                    true,
                externalProviderRequired =
                    false,
                steps =
                    listOf(
                        "Identify the requested AZIMI data.",
                        "Identify the current actor and authority level.",
                        "Use Atlas Knowledge to identify related architecture and dependencies.",
                        "Use the Requirement Engine to identify backup and recovery requirements.",
                        "Check whether each item is eligible for backup.",
                        "Create an integrity manifest.",
                        "Protect eligible data.",
                        "Store the backup through the appropriate recovery/storage layer.",
                        "Verify the backup.",
                        "Record the checkpoint.",
                        "Report exactly what was preserved and verified."
                    ),
                knowledgeAreas =
                    knowledgeAreas,
                relevantComponents =
                    relevantComponents,
                dependencies =
                    dependencies,
                securityRequirements =
                    securityRequirements,
                warnings =
                    warnings
            )
        }

        if (
            containsAny(
                normalized,
                "security",
                "secure",
                "protect",
                "shield",
                "permission",
                "vault",
                "credential",
                "biometric",
                "authority"
            )
        ) {

            return AtlasPlan(
                task =
                    "AZIMI_SECURITY",
                capability =
                    "GUARDIAN_SECURITY",
                permissionRequired =
                    true,
                externalProviderRequired =
                    false,
                steps =
                    listOf(
                        "Identify the requested security capability.",
                        "Identify the current actor and authority level.",
                        "Consult Atlas Knowledge for relevant security architecture.",
                        "Use the Requirement Engine to identify affected components and dependencies.",
                        "Check Guardian policy and required permissions.",
                        "Identify potential security risks.",
                        "Create a non-destructive implementation plan.",
                        "Request owner authorization for protected consequential changes.",
                        "Test the security behavior.",
                        "Verify that protected information remains protected."
                    ),
                knowledgeAreas =
                    knowledgeAreas,
                relevantComponents =
                    relevantComponents,
                dependencies =
                    dependencies,
                securityRequirements =
                    securityRequirements,
                warnings =
                    warnings
            )
        }

        if (
            containsAny(
                normalized,
                "atlas",
                "ai",
                "assistant",
                "intelligence",
                "model",
                "reasoning",
                "chat",
                "smart",
                "knowledge"
            )
        ) {

            return AtlasPlan(
                task =
                    "ATLAS_INTELLIGENCE",
                capability =
                    "ATLAS_REASONING",
                permissionRequired =
                    requirements.permissionRequired,
                externalProviderRequired =
                    true,
                steps =
                    listOf(
                        "Understand the request.",
                        "Identify the current actor and authority level.",
                        "Consult Atlas Knowledge.",
                        "Run Requirement Analysis.",
                        "Identify the relevant AZIMI components.",
                        "Determine missing requirements and dependencies.",
                        "Check applicable security and memory rules.",
                        "Determine whether the capability can be handled locally.",
                        "Use a replaceable AI adapter only when external intelligence is required.",
                        "Require owner authorization for protected owner operations.",
                        "Return the result or identify the remaining capability gap."
                    ),
                knowledgeAreas =
                    knowledgeAreas,
                relevantComponents =
                    relevantComponents,
                dependencies =
                    dependencies,
                securityRequirements =
                    securityRequirements,
                warnings =
                    warnings
            )
        }

        if (
            containsAny(
                normalized,
                "cloud",
                "sync",
                "synchronization",
                "storage",
                "server"
            )
        ) {

            return AtlasPlan(
                task =
                    "AZIMI_CLOUD",
                capability =
                    "Z_CLOUD",
                permissionRequired =
                    true,
                externalProviderRequired =
                    true,
                steps =
                    listOf(
                        "Understand the requested cloud capability.",
                        "Identify the current actor and authority level.",
                        "Consult Atlas Knowledge for Z Cloud architecture.",
                        "Use the Requirement Engine to identify cloud requirements.",
                        "Identify storage, synchronization, identity, and recovery requirements.",
                        "Check Z Vault and Z Recovery dependencies.",
                        "Keep the design provider-independent.",
                        "Identify security and permission requirements.",
                        "Create a portable architecture plan.",
                        "Request owner authorization before consequential changes.",
                        "Test synchronization and recovery behavior."
                    ),
                knowledgeAreas =
                    knowledgeAreas,
                relevantComponents =
                    relevantComponents,
                dependencies =
                    dependencies,
                securityRequirements =
                    securityRequirements,
                warnings =
                    warnings
            )
        }

        if (
            containsAny(
                normalized,
                "dari",
                "english",
                "language",
                "multilingual",
                "translation"
            )
        ) {

            return AtlasPlan(
                task =
                    "AZIMI_LANGUAGE",
                capability =
                    "Z_LANGUAGE",
                permissionRequired =
                    false,
                externalProviderRequired =
                    false,
                steps =
                    listOf(
                        "Identify the requested language capability.",
                        "Consult Atlas Knowledge for supported languages.",
                        "Determine whether the requested language is already supported.",
                        "Preserve the existing language architecture.",
                        "Design the change so additional languages can be added later.",
                        "Test the language behavior."
                    ),
                knowledgeAreas =
                    knowledgeAreas,
                relevantComponents =
                    relevantComponents,
                dependencies =
                    dependencies,
                securityRequirements =
                    securityRequirements,
                warnings =
                    warnings
            )
        }

        return AtlasPlan(
            task =
                "GENERAL_ATLAS_TASK",
            capability =
                "ATLAS_REASONING",
            permissionRequired =
                requirements.permissionRequired,
            externalProviderRequired =
                true,
            steps =
                listOf(
                    "Understand the request.",
                    "Identify the current actor and authority level.",
                    "Consult Atlas Knowledge.",
                    "Run Requirement Analysis.",
                    "Identify the relevant AZIMI components.",
                    "Determine what already exists.",
                    "Identify missing requirements.",
                    "Identify dependencies.",
                    "Check applicable security rules.",
                    "Determine whether Atlas can handle the request locally.",
                    "Require owner authorization for protected owner operations.",
                    "Use a replaceable AI adapter only when needed.",
                    "Return the result or identify the capability gap."
                ),
            knowledgeAreas =
                knowledgeAreas,
            relevantComponents =
                relevantComponents,
            dependencies =
                dependencies,
            securityRequirements =
                securityRequirements,
            warnings =
                warnings
        )
    }

    private fun containsAny(
        text: String,
        vararg values: String
    ): Boolean {

        return values.any {
            text.contains(it)
        }
    }

    private fun buildPlanMessage(
        plan: AtlasPlan,
        knowledge:
            AtlasKnowledge.RequirementProfile,
        requirements:
            AtlasRequirementEngine.RequirementAnalysis,
        authority:
            AtlasOwnerAuthority.AuthorityState
    ): String {

        val providerState =
            if (
                plan.externalProviderRequired
            ) {
                "EXTERNAL ADAPTER MAY BE REQUIRED"
            } else {
                "LOCAL AZIMI CAPABILITY TARGETED"
            }

        val permissionState =
            if (
                plan.permissionRequired
            ) {
                "PERMISSION REQUIRED"
            } else {
                "NO CONSEQUENTIAL ACTION IDENTIFIED"
            }

        val ownerState =
            if (
                requirements.ownerAuthorized
            ) {
                "OWNER AUTHORITY ACTIVE"
            } else {
                "OWNER AUTHORITY NOT ACTIVE"
            }

        return buildString {

            appendLine(
                "ATLAS KNOWLEDGE + AUTHORITY PLAN"
            )

            appendLine()

            appendLine(
                "TASK: ${plan.task}"
            )

            appendLine(
                "CAPABILITY: ${plan.capability}"
            )

            appendLine(
                "ACTOR: ${authority.actorType}"
            )

            appendLine(
                "AUTHORITY: ${authority.authorityLevel}"
            )

            appendLine(
                "OWNER STATE: $ownerState"
            )

            appendLine(
                "OWNERSHIP SENSITIVE: ${requirements.ownershipSensitive}"
            )

            appendLine(
                "OWNER AUTHORIZATION REQUIRED: ${requirements.ownerAuthorizationRequired}"
            )

            appendLine(
                "PERMISSION: $permissionState"
            )

            appendLine(
                "PROVIDER: $providerState"
            )

            appendLine(
                "REQUIREMENT STATUS: ${requirements.status}"
            )

            if (
                plan.knowledgeAreas.isNotEmpty()
            ) {

                appendLine()
                appendLine(
                    "KNOWLEDGE AREAS:"
                )

                plan.knowledgeAreas.forEach {
                    appendLine("- $it")
                }
            }

            if (
                plan.relevantComponents.isNotEmpty()
            ) {

                appendLine()
                appendLine(
                    "RELEVANT AZIMI COMPONENTS:"
                )

                plan.relevantComponents.forEach {
                    appendLine("- $it")
                }
            }

            if (
                plan.dependencies.isNotEmpty()
            ) {

                appendLine()
                appendLine(
                    "LIKELY DEPENDENCIES:"
                )

                plan.dependencies.forEach {
                    appendLine("- $it")
                }
            }

            if (
                plan.securityRequirements.isNotEmpty()
            ) {

                appendLine()
                appendLine(
                    "SECURITY REQUIREMENTS:"
                )

                plan.securityRequirements.forEach {
                    appendLine("- $it")
                }
            }

            if (
                plan.warnings.isNotEmpty()
            ) {

                appendLine()
                appendLine(
                    "ATLAS WARNINGS:"
                )

                plan.warnings.forEach {
                    appendLine("- $it")
                }
            }

            appendLine()
            appendLine(
                "EXECUTION PLAN:"
            )

            plan.steps.forEachIndexed {
                    index,
                    step ->
                appendLine(
                    "${index + 1}. $step"
                )
            }

            appendLine()
            appendLine(
                "NEXT SAFE ACTION:"
            )

            appendLine(
                requirements.nextSafeAction
            )

            appendLine()

            appendLine(
                "KNOWLEDGE STATUS: ${AtlasKnowledge.KNOWLEDGE_VERSION}"
            )

            appendLine(
                "CURRENT RECOVERY POINT: ${AtlasKnowledge.CURRENT_RECOVERY_POINT}"
            )

            appendLine(
                "STATUS: PLANNING ONLY — NO ACTION EXECUTED"
            )
        }
    }
}
