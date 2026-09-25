package com.azimi.guardian

import android.content.Context

/**
 * Compatibility bridge for older Guardian callers.
 *
 * The active Atlas architecture is:
 *
 * Guardian
 *     ↓
 * GuardianAiBridge
 *     ↓
 * AtlasGuardianBridge
 *     ↓
 * AtlasCore
 *     ↓
 * AtlasRouter / Local Engine / Provider Adapter
 *
 * GuardianAiBridge does NOT:
 *
 * - require email authentication
 * - require Supabase authentication
 * - require an access token
 * - expose provider credentials
 * - unlock the Vault
 * - bypass Guardian owner authorization
 * - directly call the network layer
 *
 * The protected Guardian Atlas session is controlled locally.
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
        history: List<AzimiAiClient.ChatMessage>,
        onResult: (BridgeResult) -> Unit
    ) {

        AtlasGuardianBridge.process(
            context = context.applicationContext,
            message = message,
            history = history,
            approvedMemory = emptyList()
        ) { result ->

            onResult(
                BridgeResult(
                    success = result.success,
                    message = result.message,
                    engine =
                        result.plan
                            ?.capability
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: result.status
                )
            )
        }
    }
}
