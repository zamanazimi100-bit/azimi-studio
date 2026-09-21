package com.azimi.guardian

import android.content.Context

/**
 * Controlled boundary between Guardian and Atlas Core.
 *
 * Atlas may reason and create plans here, while Guardian
 * remains responsible for authentication, security policy,
 * and protected-data boundaries.
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

        /*
         * 1. Guardian authentication boundary.
         */
        val session =
            AzimiAuth.getSession(appContext)

        if (
            session == null ||
            session.accessToken.isBlank()
        ) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Guardian authentication is required before Atlas Core can process this request.",
                    status = "AUTHENTICATION_REQUIRED"
                )
            )
            return
        }

        /*
         * 2. Protected credential boundary.
         */
        if (
            AzimiAuth.isProtectedCredential(
                message
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
         * 3. Guardian AI policy boundary.
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
                    status = "POLICY_BLOCK"
                )
            )
            return
        }

        /*
         * 4. Sanitize history before Atlas receives it.
         */
        val safeHistory =
            history.filter { item ->

                item.content.isNotBlank() &&
                    !AzimiAuth.isProtectedCredential(
                        item.content
                    )
            }

        /*
         * 5. Send the request into Atlas Core.
         *
         * Atlas Core currently creates a plan only.
         * It does not perform external or consequential
         * actions at this stage.
         */
        val atlasResult =
            AtlasCore.process(
                appContext,
                AtlasCore.AtlasRequest(
                    message = message,
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
         * 6. Return the Atlas plan.
         */
        onResult(
            AtlasBridgeResult(
                success = true,
                message = atlasResult.message,
                plan = atlasResult.plan,
                status = atlasResult.status
            )
        )
    }
}
