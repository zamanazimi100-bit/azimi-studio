package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Guardian Bridge.
 *
 * Guardian is the security boundary between the user and Atlas intelligence.
 *
 * Responsibilities:
 * - validate Guardian authentication/policy boundaries
 * - reject protected credential material
 * - sanitize conversation history
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
                    message = "Atlas request cannot be empty.",
                    status = "INVALID_REQUEST"
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
                    status = "SECURITY_BLOCK"
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

        if (policy != "SAFE_CONTEXT_ONLY") {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Atlas request blocked by Guardian AI memory policy.",
                    status = "POLICY_BLOCK"
                )
            )
            return
        }

        /*
         * Keep only safe conversation history.
         */
        val safeHistory =
            history
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
                    message = cleanMessage,
                    history = safeHistory
                )
            )

        if (!atlasResult.success) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message = atlasResult.message,
                    plan = atlasResult.plan,
                    status = atlasResult.status
                )
            )
            return
        }

        /*
         * The router is now authoritative for intelligence-path selection.
         */
        val decision =
            AtlasRouter.route(
                appContext,
                cleanMessage
            )

        when (decision.route) {

            /*
             * Guardian has explicitly restricted this request/path.
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
             *
             * No external authentication is required here.
             */
            AtlasRouter.Route.LOCAL -> {

                executeLocal(
                    context = appContext,
                    message = cleanMessage,
                    plan = atlasResult.plan,
                    onResult = onResult
                )
            }

            /*
             * Online intelligence.
             *
             * Authentication is checked only now,
             * instead of blocking local Atlas earlier.
             */
            AtlasRouter.Route.ONLINE -> {

                executeOnline(
                    context = appContext,
                    message = cleanMessage,
                    history = safeHistory,
                    plan = atlasResult.plan,
                    fallbackAllowed =
                        decision.fallbackAllowed,
                    onResult = onResult
                )
            }

            /*
             * Hybrid means:
             *
             * 1. use local Atlas capability first when useful
             * 2. use authenticated online AI when deeper external
             *    reasoning is appropriate
             * 3. preserve the local response if the online path fails
             */
            AtlasRouter.Route.HYBRID -> {

                executeHybrid(
                    context = appContext,
                    message = cleanMessage,
                    history = safeHistory,
                    plan = atlasResult.plan,
                    fallbackAllowed =
                        decision.fallbackAllowed,
                    onResult = onResult
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
                        message = messageText,
                        plan = atlasResult.plan,
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
     * Execute the local Atlas capability.
     */
    private fun executeLocal(
        context: Context,
        message: String,
        plan: AtlasCore.AtlasPlan?,
        onResult: (AtlasBridgeResult) -> Unit
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
                    plan = plan,
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
                message =
                    localResult.reply.ifBlank {
                        "Atlas local engine could not generate a response."
                    },
                plan = plan,
                status =
                    "LOCAL_RESPONSE_ERROR"
            )
        )
    }

    /**
     * Execute the authenticated online AI path.
     *
     * Guardian never sends provider credentials.
     * The existing AzimiNetwork/AzimiAiClient path remains responsible
     * for communicating with the AZIMI gateway.
     */
    private fun executeOnline(
        context: Context,
        message: String,
        history: List<AzimiAiClient.ChatMessage>,
        plan: AtlasCore.AtlasPlan?,
        fallbackAllowed: Boolean,
        onResult: (AtlasBridgeResult) -> Unit
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
                    context = context,
                    message = message,
                    plan = plan,
                    onResult = onResult
                )
            } else {
                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            "Authentication is required before Atlas can use the online AI path.",
                        plan = plan,
                        status =
                            "AUTHENTICATION_REQUIRED"
                    )
                )
            }

            return
        }

        AzimiNetwork.askAI(
            accessToken = session.accessToken,
            message = message,
            history = history
        ) { aiResult ->

            if (aiResult.success) {

                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            aiResult.reply,
                        plan = plan,
                        status =
                            "AI_RESPONSE_READY"
                    )
                )

                return@askAI
            }

            /*
             * Online failure may fall back to local intelligence.
             */
            if (fallbackAllowed) {

                executeLocal(
                    context = context,
                    message = message,
                    plan = plan
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
                    plan = plan,
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
     * If the local engine determines that external AI is unnecessary,
     * its safe local response is returned without a network request.
     *
     * If deeper external reasoning is appropriate, the authenticated
     * online path is attempted.
     *
     * If online AI fails, the local response remains available.
     */
    private fun executeHybrid(
        context: Context,
        message: String,
        history: List<AzimiAiClient.ChatMessage>,
        plan: AtlasCore.AtlasPlan?,
        fallbackAllowed: Boolean,
        onResult: (AtlasBridgeResult) -> Unit
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
         * If local execution succeeds and does not request
         * external intelligence, remain completely local.
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
                    plan = plan,
                    status =
                        "HYBRID_LOCAL_RESPONSE_READY"
                )
            )

            return
        }

        /*
         * Hybrid needs the authenticated online path when the local
         * capability says deeper external AI is useful.
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
                        plan = plan,
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
                    plan = plan,
                    status =
                        "AUTHENTICATION_REQUIRED"
                )
            )

            return
        }

        AzimiNetwork.askAI(
            accessToken = session.accessToken,
            message = message,
            history = history
        ) { aiResult ->

            if (aiResult.success) {

                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            aiResult.reply,
                        plan = plan,
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
                        plan = plan,
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
                    plan = plan,
                    status =
                        "HYBRID_ENGINE_ERROR"
                )
            )
        }
    }
}
