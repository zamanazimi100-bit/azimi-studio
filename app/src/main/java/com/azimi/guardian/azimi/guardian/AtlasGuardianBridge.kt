package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Guardian Bridge.
 *
 * Guardian is the security boundary between the user
 * and Atlas intelligence.
 *
 * Responsibilities:
 * - validate Guardian authentication/policy boundaries
 * - reject protected credential material
 * - sanitize conversation history
 * - sanitize explicitly approved memory
 * - obtain the Atlas execution plan
 * - ask AtlasRouter which intelligence path is allowed
 * - execute LOCAL / ONLINE / HYBRID paths
 * - fall back safely when permitted
 *
 * This bridge does not:
 * - expose provider credentials
 * - unlock the Vault
 * - execute privileged Android actions
 * - bypass Guardian restrictions
 * - store secrets
 *
 * IMPORTANT:
 * Memory supplied to this bridge must already be
 * approved by Guardian. This bridge performs an
 * additional security sanitization pass before
 * anything can reach online Atlas.
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
        history: List<AzimiAiClient.ChatMessage> = emptyList(),
        approvedMemory: List<AzimiAiClient.ChatMessage> = emptyList(),
        onResult: (AtlasBridgeResult) -> Unit
    ) {

        val appContext =
            context.applicationContext

        val cleanMessage =
            message.trim()

        /*
         * Basic request validation.
         */
        if (cleanMessage.isBlank()) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Atlas request cannot be empty.",
                    status =
                        "INVALID_REQUEST"
                )
            )
            return
        }

        /*
         * Guardian security gate.
         *
         * Protected credentials must never reach:
         * - local Atlas
         * - online AI
         * - history
         * - memory
         * - external providers
         */
        if (
            AzimiAuth.isProtectedCredential(
                cleanMessage
            )
        ) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Protected credential material was blocked by Guardian before reaching Atlas.",
                    status =
                        "SECURITY_BLOCK"
                )
            )
            return
        }

        /*
         * AI memory policy remains authoritative.
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
                        "Atlas request blocked by Guardian AI memory policy.",
                    status =
                        "POLICY_BLOCK"
                )
            )
            return
        }

        /*
         * Sanitize conversation history.
         */
        val safeHistory =
            sanitizeConversation(
                history
            )

        /*
         * Sanitize explicitly approved memory.
         *
         * IMPORTANT:
         * This does not discover private device data.
         * It only accepts memory explicitly supplied
         * by the Guardian memory layer.
         */
        val safeMemory =
            sanitizeMemory(
                approvedMemory
            )

        /*
         * First create the normal Atlas plan.
         *
         * AtlasCore is planning only.
         * It does not execute consequential actions.
         */
        val atlasResult =
            AtlasCore.process(
                appContext,
                AtlasCore.AtlasRequest(
                    message =
                        cleanMessage,
                    history =
                        safeHistory
                )
            )

        if (!atlasResult.success) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        atlasResult.message,
                    plan =
                        atlasResult.plan,
                    status =
                        atlasResult.status
                )
            )
            return
        }

        /*
         * The router remains authoritative for
         * intelligence-path selection.
         */
        val decision =
            AtlasRouter.route(
                appContext,
                cleanMessage
            )

        when (decision.route) {

            /*
             * Guardian has explicitly restricted
             * this request/path.
             */
            AtlasRouter.Route.RESTRICTED -> {

                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            decision.explanation,
                        plan =
                            atlasResult.plan,
                        status =
                            "ROUTING_RESTRICTED"
                    )
                )
            }

            /*
             * Local intelligence only.
             */
            AtlasRouter.Route.LOCAL -> {

                executeLocal(
                    context =
                        appContext,
                    message =
                        cleanMessage,
                    plan =
                        atlasResult.plan,
                    onResult =
                        onResult
                )
            }

            /*
             * Online intelligence.
             */
            AtlasRouter.Route.ONLINE -> {

                executeOnline(
                    context =
                        appContext,
                    message =
                        cleanMessage,
                    history =
                        safeHistory,
                    memory =
                        safeMemory,
                    plan =
                        atlasResult.plan,
                    fallbackAllowed =
                        decision.fallbackAllowed,
                    onResult =
                        onResult
                )
            }

            /*
             * Hybrid intelligence.
             */
            AtlasRouter.Route.HYBRID -> {

                executeHybrid(
                    context =
                        appContext,
                    message =
                        cleanMessage,
                    history =
                        safeHistory,
                    memory =
                        safeMemory,
                    plan =
                        atlasResult.plan,
                    fallbackAllowed =
                        decision.fallbackAllowed,
                    onResult =
                        onResult
                )
            }

            /*
             * No usable intelligence path.
             */
            AtlasRouter.Route.UNAVAILABLE -> {

                val messageText =
                    if (
                        decision.requiresAuthentication
                    ) {
                        "Atlas needs authentication for the available online intelligence path."
                    } else {
                        decision.explanation
                    }

                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            messageText,
                        plan =
                            atlasResult.plan,
                        status =
                            if (
                                decision.requiresAuthentication
                            ) {
                                "AUTHENTICATION_REQUIRED"
                            } else {
                                "INTELLIGENCE_UNAVAILABLE"
                            }
                    )
                )
            }
        }
    }

    /**
     * Sanitizes normal conversation history.
     */
    private fun sanitizeConversation(
        history:
            List<AzimiAiClient.ChatMessage>
    ): List<AzimiAiClient.ChatMessage> {

        return history
            .takeLast(12)
            .mapNotNull { item ->

                val role =
                    item.role.trim()

                val content =
                    item.content.trim()

                if (
                    role != "user" &&
                    role != "assistant"
                ) {
                    return@mapNotNull null
                }

                if (content.isBlank()) {
                    return@mapNotNull null
                }

                if (
                    content.length > 4_000
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
                    role =
                        role,
                    content =
                        content
                )
            }
    }

    /**
     * Sanitizes explicitly approved memory.
     *
     * Memory is never allowed to become a
     * credential container.
     */
    private fun sanitizeMemory(
        memory:
            List<AzimiAiClient.ChatMessage>
    ): List<AzimiAiClient.ChatMessage> {

        return memory
            .takeLast(50)
            .mapNotNull { item ->

                val role =
                    item.role.trim()

                val content =
                    item.content.trim()

                if (
                    role != "user" &&
                    role != "assistant" &&
                    role != "system"
                ) {
                    return@mapNotNull null
                }

                if (content.isBlank()) {
                    return@mapNotNull null
                }

                if (
                    content.length > 4_000
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
                    role =
                        role,
                    content =
                        content
                )
            }
    }

    /**
     * Execute the local Atlas capability.
     */
    private fun executeLocal(
        context: Context,
        message: String,
        plan: AtlasCore.AtlasPlan?,
        onResult:
            (AtlasBridgeResult) -> Unit
    ) {

        val localResult =
            runCatching {
                AtlasLocalEngine.process(
                    context,
                    message
                )
            }.getOrElse {
                null
            }

        if (localResult == null) {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Atlas local engine could not be started.",
                    plan =
                        plan,
                    status =
                        "LOCAL_ENGINE_ERROR"
                )
            )

            return
        }

        if (localResult.success) {

            onResult(
                AtlasBridgeResult(
                    success = true,
                    message =
                        localResult.reply,
                    plan =
                        plan,
                    status =
                        "LOCAL_RESPONSE_READY"
                )
            )

            return
        }

        onResult(
            AtlasBridgeResult(
                success = false,
                message =
                    localResult.reply.ifBlank {
                        "Atlas local engine could not generate a response."
                    },
                plan =
                    plan,
                status =
                    "LOCAL_RESPONSE_ERROR"
            )
        )
    }

    /**
     * Execute the authenticated online AI path.
     *
     * Only sanitized history and sanitized,
     * explicitly approved memory are sent.
     */
    private fun executeOnline(
        context: Context,
        message: String,
        history:
            List<AzimiAiClient.ChatMessage>,
        memory:
            List<AzimiAiClient.ChatMessage>,
        plan: AtlasCore.AtlasPlan?,
        fallbackAllowed: Boolean,
        onResult:
            (AtlasBridgeResult) -> Unit
    ) {

        val session =
            AzimiAuth.getSession(
                context.applicationContext
            )

        if (
            session == null ||
            session.accessToken.isBlank()
        ) {

            if (fallbackAllowed) {

                executeLocal(
                    context =
                        context,
                    message =
                        message,
                    plan =
                        plan,
                    onResult =
                        onResult
                )

            } else {

                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            "Authentication is required before Atlas can use the online AI path.",
                        plan =
                            plan,
                        status =
                            "AUTHENTICATION_REQUIRED"
                    )
                )
            }

            return
        }

        AzimiNetwork.askAI(
            accessToken =
                session.accessToken,
            message =
                message,
            history =
                history,
            memory =
                memory
        ) { aiResult ->

            if (aiResult.success) {

                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            aiResult.reply,
                        plan =
                            plan,
                        status =
                            "AI_RESPONSE_READY"
                    )
                )

                return@askAI
            }

            /*
             * Online failure may fall back
             * to local intelligence.
             */
            if (fallbackAllowed) {

                executeLocal(
                    context =
                        context,
                    message =
                        message,
                    plan =
                        plan
                ) { localResult ->

                    if (localResult.success) {

                        onResult(
                            AtlasBridgeResult(
                                success = true,
                                message =
                                    localResult.message,
                                plan =
                                    plan,
                                status =
                                    "ONLINE_FAILED_LOCAL_FALLBACK"
                            )
                        )

                    } else {

                        onResult(
                            AtlasBridgeResult(
                                success = false,
                                message =
                                    aiResult.error
                                        ?: localResult.message
                                        ?: "Atlas could not generate a response.",
                                plan =
                                    plan,
                                status =
                                    "AI_ENGINE_ERROR"
                            )
                        )
                    }
                }

                return@askAI
            }

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        aiResult.error
                            ?: "Atlas could not generate a response.",
                    plan =
                        plan,
                    status =
                        "AI_ENGINE_ERROR"
                )
            )
        }
    }

    /**
     * Execute the hybrid path.
     *
     * Local intelligence is attempted first.
     *
     * If the local engine can answer safely without
     * external intelligence, the local answer is used.
     *
     * Otherwise authenticated online Atlas is used.
     *
     * If online Atlas fails, local output remains
     * available when permitted.
     */
    private fun executeHybrid(
        context: Context,
        message: String,
        history:
            List<AzimiAiClient.ChatMessage>,
        memory:
            List<AzimiAiClient.ChatMessage>,
        plan: AtlasCore.AtlasPlan?,
        fallbackAllowed: Boolean,
        onResult:
            (AtlasBridgeResult) -> Unit
    ) {

        val localResult =
            runCatching {
                AtlasLocalEngine.process(
                    context,
                    message
                )
            }.getOrElse {
                null
            }

        /*
         * If local execution succeeds and does not
         * require external intelligence, remain local.
         */
        if (
            localResult != null &&
            localResult.success &&
            !localResult.requiresOnlineAI
        ) {

            onResult(
                AtlasBridgeResult(
                    success = true,
                    message =
                        localResult.reply,
                    plan =
                        plan,
                    status =
                        "HYBRID_LOCAL_RESPONSE_READY"
                )
            )

            return
        }

        /*
         * Hybrid needs authenticated online Atlas
         * when deeper external reasoning is useful.
         */
        val session =
            AzimiAuth.getSession(
                context.applicationContext
            )

        if (
            session == null ||
            session.accessToken.isBlank()
        ) {

            /*
             * Preserve any successful local response.
             */
            if (
                localResult != null &&
                localResult.success
            ) {

                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            localResult.reply,
                        plan =
                            plan,
                        status =
                            "HYBRID_LOCAL_FALLBACK"
                    )
                )

                return
            }

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Authentication is required for the online portion of this Atlas request.",
                    plan =
                        plan,
                    status =
                        "AUTHENTICATION_REQUIRED"
                )
            )

            return
        }

        AzimiNetwork.askAI(
            accessToken =
                session.accessToken,
            message =
                message,
            history =
                history,
            memory =
                memory
        ) { aiResult ->

            if (aiResult.success) {

                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            aiResult.reply,
                        plan =
                            plan,
                        status =
                            "HYBRID_AI_RESPONSE_READY"
                    )
                )

                return@askAI
            }

            /*
             * Local response is the safe hybrid fallback.
             */
            if (
                fallbackAllowed &&
                localResult != null &&
                localResult.success
            ) {

                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            localResult.reply,
                        plan =
                            plan,
                        status =
                            "HYBRID_LOCAL_FALLBACK"
                    )
                )

                return@askAI
            }

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        aiResult.error
                            ?: "Atlas hybrid intelligence could not generate a response.",
                    plan =
                        plan,
                    status =
                        "HYBRID_ENGINE_ERROR"
                )
            )
        }
    }
}
