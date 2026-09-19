package com.azimi.guardian

import android.content.Context

object GuardianStorage {

    private const val PREFS = "azimi_guardian_state"

    private const val VAULT_INITIALIZED = "vault_initialized"

    fun isVaultInitialized(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(VAULT_INITIALIZED, false)
    }

    fun initializeVault(context: Context) {

        if (isVaultInitialized(context)) {
            return
        }

        VaultCrypto.put(
            context,
            "vault_status",
            "LOCKED"
        )

        VaultCrypto.put(
            context,
            "ai_memory_policy",
            "SAFE_CONTEXT_ONLY"
        )

        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(VAULT_INITIALIZED, true)
            .apply()
    }

    fun getVaultStatus(context: Context): String {
        return VaultCrypto.get(
            context,
            "vault_status"
        ) ?: "LOCKED"
    }

    fun getAIMemoryPolicy(context: Context): String {
        return VaultCrypto.get(
            context,
            "ai_memory_policy"
        ) ?: "SAFE_CONTEXT_ONLY"
    }

    fun lockVault(context: Context) {

        if (!isVaultInitialized(context)) {
            initializeVault(context)
            return
        }

        VaultCrypto.put(
            context,
            "vault_status",
            "LOCKED"
        )
    }

    fun unlockVault(context: Context) {

        if (!isVaultInitialized(context)) {
            initializeVault(context)
        }

        VaultCrypto.put(
            context,
            "vault_status",
            "UNLOCKED"
        )
    }

    fun clearVault(context: Context) {

        VaultCrypto.delete(
            context,
            "vault_status"
        )

        VaultCrypto.delete(
            context,
            "ai_memory_policy"
        )

        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}