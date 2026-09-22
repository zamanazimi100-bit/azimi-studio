package com.azimi.guardian

import android.content.Context

/**
 * Cloudflare implementation of the AtlasProvider contract.
 *
 * Cloudflare is a provider.
 * It is NOT Atlas itself.
 *
 * IMPORTANT:
 * This provider deliberately receives only the current
 * permitted message.
 *
 * It does NOT receive:
 * - Z Vault contents
 * - AtlasMemoryStore contents
 * - protected memory
 * - authentication credentials
 * - biometric information
 * - Guardian security material
 * - conversation history
 */
object CloudflareAtlasProvider : AtlasProvider {

    override val id: String =
        "cloudflare"

    override val displayName: String =
        "AZIMI-CLOUDFLARE"

    override fun isAvailable(
        context: Context
    ): Boolean {

        return AtlasAvailability
            .get(context.applicationContext)
            .canUseExternalAI()
    }

    override fun execute(
        context: Context,
        message: String,
        onResult: (AtlasProvider.ProviderResult) -> Unit
    ) {

        val appContext =
            context.applicationContext

        val session =
            AzimiAuth.getSession(
                appContext
            )

        if (
            session == null ||
            session.accessToken.isBlank()
        ) {
            onResult(
                AtlasProvider.ProviderResult(
                    success = false,
                    error =
                        "Authenticated online session required"
                )
            )

            return
        }

        /*
         * ------------------------------------------------
         * PROVIDER PRIVACY BOUNDARY
         * ------------------------------------------------
         *
         * Only the current permitted message crosses
         * the online provider boundary.
         *
         * History and memory are deliberately empty.
         *
         * Z Vault remains local.
         */
        AzimiNetwork.askAI(
            accessToken =
                session.accessToken,

            message =
                message,

            history =
                emptyList(),

            memory =
                emptyList()
        ) { result ->

            if (!result.success) {

                onResult(
                    AtlasProvider.ProviderResult(
                        success = false,

                        error =
                            result.error.ifBlank {
                                "Cloudflare provider unavailable"
                            }
                    )
                )

                return@askAI
            }

            onResult(
                AtlasProvider.ProviderResult(
                    success = true,

                    reply =
                        result.reply,

                    engine =
                        result.engine.ifBlank {
                            displayName
                        },

                    model =
                        result.model.ifBlank {
                            "unknown"
                        },

                    fallback =
                        result.fallback
                )
            )
        }
    }
}
