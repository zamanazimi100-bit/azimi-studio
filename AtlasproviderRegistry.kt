package com.azimi.guardian

import android.content.Context

/**
 * Atlas provider registry.
 *
 * Atlas decides which provider is available.
 * Providers do not decide Atlas architecture.
 *
 * New providers can be added here later without replacing
 * Atlas Core or Guardian.
 */
object AtlasProviderRegistry {

    private val providers: List<AtlasProvider> =
        listOf(
            CloudflareAtlasProvider()
        )

    fun allProviders(): List<AtlasProvider> {
        return providers.toList()
    }

    fun getProvider(
        context: Context,
        providerId: String
    ): AtlasProvider? {

        val appContext =
            context.applicationContext

        val normalizedId =
            providerId.trim().lowercase()

        return providers.firstOrNull {
            it.id.trim().lowercase() == normalizedId &&
                it.isAvailable(appContext)
        }
    }

    fun getAvailableProviders(
        context: Context
    ): List<AtlasProvider> {

        val appContext =
            context.applicationContext

        return providers.filter {
            runCatching {
                it.isAvailable(appContext)
            }.getOrDefault(false)
        }
    }

    fun getPrimaryOnlineProvider(
        context: Context
    ): AtlasProvider? {

        return getAvailableProviders(
            context
        ).firstOrNull()
    }

    fun hasAvailableProvider(
        context: Context
    ): Boolean {

        return getPrimaryOnlineProvider(
            context
        ) != null
    }
}
