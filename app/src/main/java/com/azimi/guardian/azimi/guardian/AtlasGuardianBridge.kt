package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Guardian Bridge.
 *
 * This is the authoritative Android boundary between
 * Guardian and Atlas intelligence.
 *
 * Responsibilities:
 * - enforce active Atlas session
 * - enforce Guardian AI policy
 * - reject protected credentials
 * - sanitize conversation history
 * - sanitize approved memory
 * - obtain Atlas execution planning
 * - route through AtlasRouter
 * - provide safe project context
 * - execute local/online/hybrid intelligence
 * - provide safe fallback
 *
 * This bridge never:
 * - exposes credentials
 * - unlocks the Vault
 * - bypasses Guardian
 * - performs privileged Android actions
 * - sends raw Vault contents to providers
 */
object AtlasGuardianBridge {

    data class AtlasBridgeResult(
        val success: Boolean,
        val message: String,
        val plan: AtlasCore.AtlasPlan? = null,
        val status: String = "ATLAS"
    )

    fun process(
        context: Context,
        message: String,
        history:
            List<AzimiAiClient.ChatMessage> =
            emptyList(),
        approvedMemory:
            List<AzimiAiClient.ChatMessage> =
            emptyList(),
        onResult:
            (AtlasBridgeResult) -> Unit
    ) {

        val appContext =
            context.applicationContext

        val cleanMessage =
            message.trim()

        /*
         * ---------------------------------------------------
         * 1. ATLAS SESSION GATE
         * ---------------------------------------------------
         *
         * Lock 1 intentionally leaves Atlas active.
         * Full Lock deactivates AtlasSession.
         *
         * No intelligence processing is permitted when
         * Atlas itself is locked.
         */
        if (
            !AtlasSession.isActive(
                appContext
            )
        ) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "ATLAS LOCKED: Open Z Vault and activate Atlas before using intelligence.",
                    status = "ATLAS_SESSION_LOCKED"
                )
            )
            return
        }

        /*
         * ---------------------------------------------------
         * 2. REQUEST VALIDATION
         * ---------------------------------------------------
         */

        if (cleanMessage.isBlank()) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "ATLAS: Invalid request.",
                    status = "INVALID_REQUEST"
                )
            )
            return
        }

        if (
            AzimiAuth.isProtectedCredential(
                cleanMessage
            )
        ) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "ATLAS SECURITY: Protected information was blocked before intelligence processing.",
                    status = "SECURITY_BLOCK"
                )
            )
            return
        }

        /*
         * ---------------------------------------------------
         * 3. GUARDIAN AI POLICY
         * ---------------------------------------------------
         */

        val policy =
            GuardianStorage.getAIMemoryPolicy(
                appContext
            )

        if (
            policy != "SAFE_CONTEXT_ONLY"
        ) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "ATLAS SECURITY: Current Guardian AI policy does not permit this request.",
                    status = "POLICY_BLOCK"
                )
            )
            return
        }

        /*
         * ---------------------------------------------------
         * 4. SANITIZE CONTEXT
         * ---------------------------------------------------
         */

        val safeHistory =
            sanitizeConversation(
                history
            )

        val safeMemory =
            sanitizeMemory(
                approvedMemory
            )

        /*
         * ---------------------------------------------------
         * 5. ATLAS CORE PLANNING
         * ---------------------------------------------------
         */

        val atlasResult =
            AtlasCore.process(
                appContext,
                AtlasCore.AtlasRequest(
                    message = cleanMessage,
                    history = safeHistory
                )
            )

        if (
            !atlasResult.success
        ) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        atlasResult.message,
                    plan = atlasResult.plan,
                    status = "ATLAS_PLAN_ERROR"
                )
            )
            return
        }

        /*
         * ---------------------------------------------------
         * 6. SAFE ATLAS APPLICATION CONTEXT
         * ---------------------------------------------------
         */

        val safeAtlasContext =
            buildSafeAtlasContext(
                appContext,
                atlasResult.message
            )

        /*
         * ---------------------------------------------------
         * 7. ROUTING
         * ---------------------------------------------------
         */

        val decision =
            AtlasRouter.route(
                appContext,
                cleanMessage
            )

        when (
            decision.route
        ) {

            AtlasRouter.Route.RESTRICTED -> {

                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            "ATLAS SECURITY: Request restricted by Guardian.",
                        plan = atlasResult.plan,
                        status = "RESTRICTED"
                    )
                )
            }

            AtlasRouter.Route.LOCAL -> {

                executeLocal(
                    appContext,
                    cleanMessage,
                    atlasResult.plan,
                    onResult
                )
            }

            AtlasRouter.Route.ONLINE -> {

                executeOnline(
                    appContext,
                    cleanMessage,
                    safeHistory,
                    safeMemory,
                    safeAtlasContext,
                    atlasResult.plan,
                    onResult
                )
            }

            AtlasRouter.Route.HYBRID -> {

                executeHybrid(
                    appContext,
                    cleanMessage,
                    safeHistory,
                    safeMemory,
                    safeAtlasContext,
                    atlasResult.plan,
                    onResult
                )
            }

            AtlasRouter.Route.UNAVAILABLE -> {

                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            decision.explanation.ifBlank {
                                "ATLAS: Required intelligence capability is currently unavailable."
                            },
                        plan = atlasResult.plan,
                        status = "UNAVAILABLE"
                    )
                )
            }
        }
    }

    private fun sanitizeConversation(
        history:
            List<AzimiAiClient.ChatMessage>
    ):
        List<AzimiAiClient.ChatMessage> {

        return history
            .takeLast(12)
            .mapNotNull { item ->

                val role =
                    item.role
                        .trim()
                        .lowercase()

                if (
                    role != "user" &&
                    role != "assistant"
                ) {
                    return@mapNotNull null
                }

                val content =
                    item.content
                        .trim()
                        .take(4_000)

                if (
                    content.isBlank()
                ) {
                    return@mapNotNull null
                }

                if (
                    AzimiAuth.isProtectedCredential(
                        content
                    )
                ) {
                    return@mapNotNull null
                }

                AzimiAiClient.ChatMessage(
                    role = role,
                    content = content
                )
            }
    }

    private fun sanitizeMemory(
        memory:
            List<AzimiAiClient.ChatMessage>
    ):
        List<AzimiAiClient.ChatMessage> {

        return memory
            .takeLast(50)
            .mapNotNull { item ->

                val role =
                    item.role
                        .trim()
                        .lowercase()

                if (
                    role != "user" &&
                    role != "assistant" &&
                    role != "system"
                ) {
                    return@mapNotNull null
                }

                val content =
                    item.content
                        .trim()
                        .take(4_000)

                if (
                    content.isBlank()
                ) {
                    return@mapNotNull null
                }

                if (
                    AzimiAuth.isProtectedCredential(
                        content
                    )
                ) {
                    return@mapNotNull null
                }

                AzimiAiClient.ChatMessage(
                    role = role,
                    content = content
                )
            }
    }

    private fun buildSafeAtlasContext(
        context: Context,
        planMessage: String
    ): String {

        val sections =
            mutableListOf<String>()

        /*
         * Project knowledge is already designed as
         * structured, read-only safe context.
         */
        val knowledge =
            AtlasKnowledge.getSafeContext()

        knowledge
            .entries
            .sortedBy {
                it.key
            }
            .forEach { entry ->

                val key =
                    entry.key
                        .toString()
                        .trim()

                val value =
                    entry.value
                        ?.toString()
                        ?.trim()
                        ?: ""

                if (
                    key.isBlank() ||
                    value.isBlank()
                ) {
                    return@forEach
                }

                val block =
                    "$key:\n$value"

                if (
                    !AzimiAuth.isProtectedCredential(
                        block
                    )
                ) {
                    sections.add(
                        block.take(6_000)
                    )
                }
            }

        /*
         * AtlasCore's plan message is generated locally.
         * It gives the real intelligence engine the
         * coordination/requirement context without exposing
         * Guardian internals.
         */
        val cleanPlan =
            planMessage
                .trim()
                .take(12_000)

        if (
            cleanPlan.isNotBlank() &&
            !AzimiAuth.isProtectedCredential(
                cleanPlan
            )
        ) {
            sections.add(
                "CURRENT ATLAS CORE PLAN:\n$cleanPlan"
            )
        }

        val combined =
            sections.joinToString(
                separator = "\n\n"
            )

        return combined
            .take(24_000)
    }

    private fun executeLocal(
        context: Context,
        message: String,
        plan:
            AtlasCore.AtlasPlan?,
        onResult:
            (AtlasBridgeResult) -> Unit
    ) {

        val result =
            AtlasLocalEngine.process(
                context,
                message
            )

        if (
            result.success
        ) {
            onResult(
                AtlasBridgeResult(
                    success = true,
                    message = result.reply,
                    plan = plan,
                    status =
                        "LOCAL_RESPONSE_READY"
                )
            )
            return
        }

        onResult(
            AtlasBridgeResult(
                success = false,
                message = result.reply,
                plan = plan,
                status =
                    "LOCAL_ENGINE_ERROR"
            )
        )
    }

    private fun executeOnline(
        context: Context,
        message: String,
        history:
            List<AzimiAiClient.ChatMessage>,
        memory:
            List<AzimiAiClient.ChatMessage>,
        atlasContext: String,
        plan:
            AtlasCore.AtlasPlan?,
        onResult:
            (AtlasBridgeResult) -> Unit
    ) {

        val provider =
            AtlasProviderRegistry
                .getPrimaryOnlineProvider(
                    context
                )

        if (
            provider == null
        ) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "ATLAS: No permitted online intelligence provider is currently available.",
                    plan = plan,
                    status =
                        "PROVIDER_UNAVAILABLE"
                )
            )
            return
        }

        val request =
            AtlasProviderRequest(
                message = message,
                history = history,
                approvedMemory = memory,
                atlasContext = atlasContext
            )

        provider.execute(
            context = context,
            request = request
        ) { result ->

            if (
                result.success
            ) {
                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message = result.reply,
                        plan = plan,
                        status =
                            "ONLINE_${
                                result.engine
                                    .ifBlank {
                                        provider.id
                                    }
                            }"
                    )
                )
                return@execute
            }

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        result.error.ifBlank {
                            "ATLAS: Online intelligence provider failed."
                        },
                    plan = plan,
                    status =
                        "ONLINE_PROVIDER_ERROR"
                )
            )
        }
    }

    private fun executeHybrid(
        context: Context,
        message: String,
        history:
            List<AzimiAiClient.ChatMessage>,
        memory:
            List<AzimiAiClient.ChatMessage>,
        atlasContext: String,
        plan:
            AtlasCore.AtlasPlan?,
        onResult:
            (AtlasBridgeResult) -> Unit
    ) {

        val localResult =
            AtlasLocalEngine.process(
                context,
                message
            )

        /*
         * If the local engine can completely satisfy the
         * request, do not send anything online.
         */
        if (
            localResult.success &&
            !localResult.requiresOnlineAI
        ) {
            onResult(
                AtlasBridgeResult(
                    success = true,
                    message =
                        localResult.reply,
                    plan = plan,
                    status =
                        "HYBRID_LOCAL_RESPONSE"
                )
            )
            return
        }

        val provider =
            AtlasProviderRegistry
                .getPrimaryOnlineProvider(
                    context
                )

        if (
            provider == null
        ) {

            if (
                localResult.success
            ) {
                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            localResult.reply,
                        plan = plan,
                        status =
                            "HYBRID_LOCAL_FALLBACK"
                    )
                )
            } else {
                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            "ATLAS: Local and online intelligence capabilities are currently unavailable.",
                        plan = plan,
                        status =
                            "HYBRID_UNAVAILABLE"
                    )
                )
            }

            return
        }

        val request =
            AtlasProviderRequest(
                message = message,
                history = history,
                approvedMemory = memory,
                atlasContext = atlasContext
            )

        provider.execute(
            context = context,
            request = request
        ) { result ->

            if (
                result.success
            ) {
                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            result.reply,
                        plan = plan,
                        status =
                            "HYBRID_${
                                result.engine
                                    .ifBlank {
                                        provider.id
                                    }
                            }"
                    )
                )
                return@execute
            }

            if (
                localResult.success
            ) {
                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            localResult.reply,
                        plan = plan,
                        status =
                            "HYBRID_ONLINE_FAILED_LOCAL_FALLBACK"
                    )
                )
            } else {
                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            result.error.ifBlank {
                                "ATLAS: Intelligence provider failed and no local answer is available."
                            },
                        plan = plan,
                        status =
                            "HYBRID_PROVIDER_ERROR"
                    )
                )
            }
        }
    }
}
