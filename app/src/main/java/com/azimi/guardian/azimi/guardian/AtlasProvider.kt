package com.azimi.guardian

import android.content.Context

/**
 * Provider-independent intelligence interface.
 *
 * Atlas owns routing and security.
 * Providers only receive an already-sanitized request.
 */
interface AtlasProvider {

    val id: String

    val displayName: String

    fun isAvailable(
        context: Context
    ): Boolean

    fun execute(
        context: Context,
        request: AtlasProviderRequest,
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
