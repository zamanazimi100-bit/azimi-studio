package com.azimi.guardian

import android.content.Context

/**
 * Controlled boundary between Guardian and Atlas Core.
 *
 * Guardian remains responsible for authentication,
 * security policy, protected-data boundaries, and
 * authorization.
 *
 * Atlas Core understands and plans.
 * The replaceable AI engine provides the conversational
 * response when external intelligence is required.
 *
 * Consequential capabilities are NOT executed here.
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
                        "Guardian authentication is required before Atlas can respond.",
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
         * 4. Sanitize conversation history.
         */
        val safeHistory =
            history
                .takeLast(12)
                .filter { item ->

                    item.content.isNotBlank() &&
                        (
                            item.role == "user" ||
                                item.role == "assistant"
                            ) &&
                        !AzimiAuth.isProtectedCredential(
                            item.content
                        )
                }

        /*
         * 5. Atlas Core understands the request.
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
         * 6. Connect Atlas to the real AI engine.
         */
        AzimiNetwork.askAI(
            accessToken = session.accessToken,
            message = message,
            history = safeHistory
        ) { aiResult ->

            if (!aiResult.success) {

                onResult(
                    AtlasBridgeResult(
                        success = false,
                        message =
                            aiResult.error
                                ?: "Atlas could not generate a response.",
                        plan = atlasResult.plan,
                        status = "AI_ENGINE_ERROR"
                    )
                )

                return@askAI
            }

            /*
             * 7. Return the real Atlas answer to Guardian.
             */
            onResult(
                AtlasBridgeResult(
                    success = true,
                    message = aiResult.reply,
                    plan = atlasResult.plan,
                    status = "AI_RESPONSE_READY"
                )
            )
        }
    }
}
