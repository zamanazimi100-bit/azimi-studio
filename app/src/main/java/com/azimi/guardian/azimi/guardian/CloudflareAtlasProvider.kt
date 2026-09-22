package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Cloudflare Atlas Provider.
 *
 * This is an implementation of the provider-independent
 * AtlasProvider contract.
 *
 * Cloudflare is a replaceable intelligence provider.
 * It does not own Atlas identity, memory, security,
 * or Guardian authority.
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
        message: String,
        onResult:
            (AtlasProvider.ProviderResult) -> Unit
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
                    reply = "",
                    engine = id,
                    model = "",
                    fallback = true,
                    error =
                        "Atlas authentication is required for the online provider."
                )
            )
            return
        }

        val accessToken =
            session.accessToken

        if (accessToken.isBlank()) {
            onResult(
                AtlasProvider.ProviderResult(
                    success = false,
                    reply = "",
                    engine = id,
                    model = "",
                    fallback = true,
                    error =
                        "Atlas authentication token is unavailable."
                )
            )
            return
        }

        AzimiNetwork.askAI(
            context = appContext,
            accessToken = accessToken,
            message = message,
            history = emptyList(),
            memory = emptyList()
        ) { result ->

            if (result.success) {

                onResult(
                    AtlasProvider.ProviderResult(
                        success = true,
                        reply = result.reply,
                        engine =
                            result.engine
                                ?.ifBlank { id }
                                ?: id,
                        model =
                            result.model,
                        fallback = false,
                        error = ""
                    )
                )

            } else {

                onResult(
                    AtlasProvider.ProviderResult(
                        success = false,
                        reply = result.reply,
                        engine =
                            result.engine
                                ?.ifBlank { id }
                                ?: id,
                        model =
                            result.model,
                        fallback = true,
                        error =
                            result.error.ifBlank {
                                "Cloudflare provider failed."
                            }
                    )
                )
            }
        }
    }
}
