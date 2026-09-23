package com.azimi.guardian

import android.content.Context

object GuardianAiBridge {

    data class BridgeResult(
        val success: Boolean,
        val message: String,
        val engine: String = "GUARDIAN"
    )

    fun ask(
        context: Context,
        message: String,
        history: List<AzimiAiClient.ChatMessage>,
        onResult: (BridgeResult) -> Unit
    ) {
        val appContext = context.applicationContext

        // 1. Guardian authentication boundary
        val session = AzimiAuth.getSession(appContext)

        if (session == null || session.accessToken.isBlank()) {
            onResult(
                BridgeResult(
                    success = false,
                    message = "Guardian authentication is required before AZIMI AI can be used."
                )
            )
            return
        }

        // 2. Protected credential boundary
        if (AzimiAuth.isProtectedCredential(message)) {
            onResult(
                BridgeResult(
                    success = false,
                    message = "This message appears to contain protected credential material and was blocked by Guardian."
                )
            )
            return
        }

        // 3. Guardian Vault policy boundary
        val policy = GuardianStorage.getAIMemoryPolicy(appContext)

        if (policy != "SAFE_CONTEXT_ONLY") {
            onResult(
                BridgeResult(
                    success = false,
                    message = "AZIMI AI request blocked. Guardian AI memory policy is not SAFE_CONTEXT_ONLY."
                )
            )
            return
        }

        // 4. Filter conversation history before it reaches AI.
        val safeHistory = history.filter { item ->
            !AzimiAuth.isProtectedCredential(item.content)
        }

        // 5. Send only through the existing authenticated network layer.
        AzimiNetwork.askAI(
            accessToken = session.accessToken,
            message = message,
            history = safeHistory
        ) { response ->

            if (response.success) {
                onResult(
                    BridgeResult(
                        success = true,
                        message = response.reply,
                        engine = response.engine
                    )
                )
            } else {
                onResult(
                    BridgeResult(
                        success = false,
                        message = response.reply,
                        engine = response.engine
                    )
                )
            }
        }
    }
}
