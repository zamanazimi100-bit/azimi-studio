package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Cloudflare Atlas Provider.
 *
 * Cloudflare is an intelligence provider only.
 *
 * Guardian owns:
 * - owner authority
 * - Atlas session
 * - security policy
 * - memory boundary
 * - Vault boundary
 *
 * No Supabase/email authentication is required here.
 */
class CloudflareAtlasProvider : AtlasProvider {

    override val id: String =
        "cloudflare"

    override val displayName: String =
        "AZIMI-CLOUDFLARE"

    override fun isAvailable(
        context: Context
    ): Boolean {

        return AtlasAvailability.canUseExternalAI(
            context.applicationContext
        )
    }

    override fun execute(
        context: Context,
        message: String,
        history: List<AzimiAiClient.ChatMessage>,
        memory: List<AzimiAiClient.ChatMessage>,
        onResult: (AtlasProvider.ProviderResult) -> Unit
    ) {

        val appContext =
            context.applicationContext

        if (
            !isAvailable(appContext)
        ) {

            onResult(
                AtlasProvider.ProviderResult(
                    success = false,
                    reply = "",
                    engine = id,
                    model = "",
                    fallback = true,
                    error =
                        "Guardian-authorized online Atlas is currently unavailable."
                )
            )

            return
        }

        AzimiAiClient.ask(
            context = appContext,
            message = message,
            history = history,
            memory = memory
        ) { result ->

            val engineName =
                result.engine

                    .ifBlank {
                        id
                    }

            val errorMessage =
                result.error
                    ?: ""

            if (
                result.success
            ) {

                onResult(
                    AtlasProvider.ProviderResult(
                        success = true,
                        reply = result.reply,
                        engine = engineName,
                        model = result.model,
                        fallback =
                            result.fallback,
                        error = ""
                    )
                )

            } else {

                onResult(
                    AtlasProvider.ProviderResult(
                        success = false,
                        reply = result.reply,
                        engine = engineName,
                        model = result.model,
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
}
