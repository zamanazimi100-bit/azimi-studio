package com.azimi.guardian

/**
 * Provider-independent intelligence contract for Atlas.
 *
 * Atlas owns this interface.
 * Individual AI providers implement it.
 *
 * Security boundary:
 * - Providers receive only the permitted request.
 * - Z Vault memory is NOT part of this contract.
 * - Protected credentials are never passed to providers.
 */
interface AtlasProvider {

    val id: String

    val displayName: String

    fun isAvailable(
        context: android.content.Context
    ): Boolean

    fun execute(
        context: android.content.Context,
        message: String,
        onResult: (ProviderResult) -> Unit
    )

    data class ProviderResult(
        val success: Boolean,
        val reply: String = "",
        val engine: String = "",
        val model: String = "",
        val fallback: Boolean = false,
        val error: String = ""
    )
}
