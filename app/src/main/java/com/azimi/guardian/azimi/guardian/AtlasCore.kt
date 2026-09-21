package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Core
 *
 * The central orchestration foundation for Atlas.
 *
 * This first version does NOT execute external project actions.
 * It:
 * - understands the requested task at a basic level
 * - identifies the current capability state
 * - determines whether permission is required
 * - creates a safe execution plan
 * - preserves provider independence
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
        val history: List<AzimiAiClient.ChatMessage> = emptyList()
    )

    data class AtlasPlan(
        val task: String,
        val capability: String,
        val permissionRequired: Boolean,
        val externalProviderRequired: Boolean,
        val steps: List<String>
    )

    data class AtlasResult(
        val success: Boolean,
        val message: String,
        val plan: AtlasPlan? = null,
        val status: String = "ATLAS_CORE"
    )

    /**
     * Entry point for Atlas reasoning/orchestration.
     *
     * This does not directly perform consequential actions.
     */
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
                message = "Atlas received an empty request.",
                status = "INVALID_REQUEST"
            )
        }

        /*
         * Guardian security boundary.
         *
         * Protected credentials must never become
         * an Atlas project task or ordinary memory.
         */
        if (
            AzimiAuth.isProtectedCredential(message)
        ) {
            return AtlasResult(
                success = false,
                message =
                    "Atlas blocked protected credential material.",
                status = "SECURITY_BLOCK"
            )
        }

        /*
         * Check the current Guardian AI policy.
         */
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
                status = "POLICY_BLOCK"
            )
        }

        val plan =
            createPlan(message)

        return AtlasResult(
            success = true,
            message =
                buildPlanMessage(plan),
            plan = plan,
            status = "PLAN_READY"
        )
    }

    /**
     * Creates an initial capability plan.
     *
     * This is intentionally conservative.
     * Actual capabilities will be registered later.
     */
    private fun createPlan(
        message: String
    ): AtlasPlan {

        val normalized =
            message.lowercase()

        return when {

            containsAny(
                normalized,
                "github",
                "repository",
                "repo",
                "source code"
            ) -> {

                AtlasPlan(
                    task =
                        "PROJECT_PRESERVATION",
                    capability =
                        "PROJECT_STORAGE",
                    permissionRequired =
                        true,
                    externalProviderRequired =
                        true,
                    steps =
                        listOf(
                            "Inspect the requested project.",
                            "Determine what data can be exported.",
                            "Request the required owner permission.",
                            "Create an integrity-checked project snapshot.",
                            "Encrypt and store the snapshot in Z Vault.",
                            "Report exactly what was preserved."
                        )
                )
            }

            containsAny(
                normalized,
                "build",
                "compile",
                "apk",
                "fix",
                "debug",
                "error"
            ) -> {

                AtlasPlan(
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
                            "Identify the exact failure or requested change.",
                            "Determine available capabilities.",
                            "Create a proposed repair or implementation plan.",
                            "Request permission before consequential changes.",
                            "Test the result.",
                            "Report the exact outcome."
                        )
                )
            }

            containsAny(
                normalized,
                "backup",
                "preserve",
                "archive",
                "save project"
            ) -> {

                AtlasPlan(
                    task =
                        "AZIMI_BACKUP",
                    capability =
                        "Z_VAULT_BACKUP",
                    permissionRequired =
                        true,
                    externalProviderRequired =
                        false,
                    steps =
                        listOf(
                            "Identify the requested AZIMI data.",
                            "Check whether each item is eligible for backup.",
                            "Create an integrity manifest.",
                            "Encrypt eligible data.",
                            "Store the backup in Z Vault.",
                            "Verify the backup.",
                            "Report the backup location and result."
                        )
                )
            }

            else -> {

                AtlasPlan(
                    task =
                        "GENERAL_ATLAS_TASK",
                    capability =
                        "ATLAS_REASONING",
                    permissionRequired =
                        false,
                    externalProviderRequired =
                        true,
                    steps =
                        listOf(
                            "Understand the request.",
                            "Identify the required capability.",
                            "Check whether Atlas can perform it locally.",
                            "Use a replaceable AI adapter only when needed.",
                            "Return the result or identify the capability gap."
                        )
                )
            }
        }
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
        plan: AtlasPlan
    ): String {

        val providerState =
            if (plan.externalProviderRequired) {
                "EXTERNAL ADAPTER MAY BE REQUIRED"
            } else {
                "LOCAL AZIMI CAPABILITY TARGETED"
            }

        val permissionState =
            if (plan.permissionRequired) {
                "OWNER PERMISSION REQUIRED"
            } else {
                "NO CONSEQUENTAL ACTION IDENTIFIED"
            }

        return buildString {

            appendLine(
                "ATLAS PLAN READY"
            )

            appendLine()

            appendLine(
                "TASK: ${plan.task}"
            )

            appendLine(
                "CAPABILITY: ${plan.capability}"
            )

            appendLine(
                "PERMISSION: $permissionState"
            )

            appendLine(
                "PROVIDER: $providerState"
            )

            appendLine()

            appendLine(
                "EXECUTION PLAN:"
            )

            plan.steps.forEachIndexed { index, step ->

                appendLine(
                    "${index + 1}. $step"
                )
            }
        }
    }
}
