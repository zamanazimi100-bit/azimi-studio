package com.azimi.guardian

import android.content.Context

/**
 * Compatibility bridge for older Guardian callers.
 *
 * The active Atlas architecture is:
 *
 * Guardian
 *     ↓
 * AtlasGuardianBridge
 *     ↓
 * AtlasCore
 *     ↓
 * AtlasRouter
 *     ↓
 * AtlasProvider
 *
 * This class intentionally does NOT:
 *
 * - require Supabase authentication
 * - require an access token
 * - expose provider credentials
 * - bypass Guardian owner authorization
 * - unlock the Vault
 * - execute privileged Android actions
 *
 * AtlasGuardianBridge is the authoritative implementation.
 */
object GuardianAiBridge {

    data class BridgeResult(
        val success: Boolean,
        val message: String,
        val engine: String = "GUARDIAN"
    )

    fun ask(
        context: Context,
        message: String,
        history:
            List<AzimiAiClient.ChatMessage>,
        onResult:
            (BridgeResult) -> Unit
    ) {

        AtlasGuardianBridge.process(
            context = context.applicationContext,
            message = message,
            history = history,
            approvedMemory = emptyList()
        ) { result ->

            onResult(
                BridgeResult(
                    success =
                        result.success,

                    message =
                        result.message,

                    engine =
                        result.plan
                            ?.toString()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "GUARDIAN"
                )
            )
        }
    }
}
