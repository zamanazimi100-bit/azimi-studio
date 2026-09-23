package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Cloudflare Atlas Provider.
 *
 * Cloudflare is only an intelligence provider.
 *
 * It does not own:
 * - Atlas identity
 * - Guardian authority
 * - owner authentication
 * - memory
 * - Vault access
 *
 * Guardian establishes owner authority locally and the
 * online gateway verifies Guardian cryptographically.
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

        if (!isAvailable(appContext)) {

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

        /*
         * GuardianAtlasIdentity performs the actual
         * cryptographic authorization of the online request.
         *
         * No email session is required.
         * No Supabase session is required.
         */
        AzimiNetwork.askAI(
            message = message,
            history = history,
            memory = memory
        ) { result ->

            val engineName =
                result.engine ?: id

            val errorMessage =
                result.error ?: ""

            if (result.success) {

                onResult(
                    AtlasProvider.ProviderResult(
                        success = true,
                        reply = result.reply,
                        engine = engineName,
                        model = result.model,
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
