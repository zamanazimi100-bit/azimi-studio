package com.azimi.guardian

import android.content.Context

/**
 * Cloudflare-backed Atlas intelligence provider.
 *
 * Cloudflare is an intelligence engine, not the owner,
 * memory authority, security authority, or coordinator.
 */
class CloudflareAtlasProvider : AtlasProvider {

    override val id: String =
        "cloudflare"

    override val displayName: String =
        "AZIMI-CLOUDFLARE"

    override fun isAvailable(
        context: Context
    ): Boolean {
        return AtlasAvailability
            .canUseExternalAI(
                context.applicationContext
            )
    }

    override fun execute(
        context: Context,
        request: AtlasProviderRequest,
        onResult: (AtlasProvider.ProviderResult) -> Unit
    ) {

        val appContext =
            context.applicationContext

        if (!isAvailable(appContext)) {
            onResult(
                AtlasProvider.ProviderResult(
                    success = false,
                    engine = id,
                    error =
                        "Cloudflare provider is currently unavailable."
                )
            )
            return
        }

        val session =
            AzimiAuth.getSession(
                appContext
            )

        if (session == null) {
            onResult(
                AtlasProvider.ProviderResult(
                    success = false,
                    engine = id,
                    error =
                        "Atlas authentication is required for the online provider."
                )
            )
            return
        }

        val accessToken =
            session.accessToken.trim()

        if (accessToken.isBlank()) {
            onResult(
                AtlasProvider.ProviderResult(
                    success = false,
                    engine = id,
                    error =
                        "Atlas authentication token is unavailable."
                )
            )
            return
        }

        /*
         * The Atlas context is represented as a safe system
         * message inside the memory/context channel.
         *
         * This avoids changing the existing AzimiNetwork
         * transport contract while allowing Atlas to send
         * structured safe context to /api/chat.
         */
        val safeMemory =
            buildSafeProviderMemory(
                request
            )

        AzimiNetwork.askAI(
            accessToken = accessToken,
            message = request.message,
            history = request.history,
            memory = safeMemory
        ) { result ->

            val engineName =
                result.engine
                    ?: id

            val modelName =
                result.model
                    ?: "unknown"

            val errorMessage =
                result.error
                    ?: ""

            if (result.success) {

                onResult(
                    AtlasProvider.ProviderResult(
                        success = true,
                        reply = result.reply,
                        engine = engineName,
                        model = modelName,
                        fallback = false,
                        error = ""
                    )
                )

            } else {

                onResult(
                    AtlasProvider.ProviderResult(
                        success = false,
                        reply = result.reply,
                        engine = engineName,
                        model = modelName,
                        fallback = true,
                        error =
                            errorMessage.ifBlank {
                                "Cloudflare provider failed."
                            }
                    )
                )
            }
        }
    }

    private fun buildSafeProviderMemory(
        request: AtlasProviderRequest
    ): List<AzimiAiClient.ChatMessage> {

        val result =
            mutableListOf<AzimiAiClient.ChatMessage>()

        /*
         * Atlas application context is generated locally
         * and must never contain protected credentials.
         */
        val context =
            request.atlasContext
                .trim()
                .take(24_000)

        if (
            context.isNotBlank() &&
            !AzimiAuth.isProtectedCredential(
                context
            )
        ) {
            result.add(
                AzimiAiClient.ChatMessage(
                    role = "system",
                    content =
                        "ATLAS CORE SAFE APPLICATION CONTEXT:\n$context"
                )
            )
        }

        /*
         * Approved memory has already passed Guardian
         * filtering. We filter again at the provider
         * boundary as defense in depth.
         */
        request.approvedMemory
            .takeLast(50)
            .forEach { item ->

                val role =
                    item.role
                        .trim()
                        .lowercase()

                val content =
                    item.content
                        .trim()
                        .take(4_000)

                if (
                    content.isBlank()
                ) {
                    return@forEach
                }

                if (
                    role != "user" &&
                    role != "assistant" &&
                    role != "system"
                ) {
                    return@forEach
                }

                if (
                    AzimiAuth.isProtectedCredential(
                        content
                    )
                ) {
                    return@forEach
                }

                result.add(
                    AzimiAiClient.ChatMessage(
                        role = role,
                        content = content
                    )
                )
            }

        return result
            .takeLast(50)
    }
}
