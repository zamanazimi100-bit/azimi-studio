package com.azimi.guardian

import android.content.Context

object GuardianStorage {

    private const val PREFS = "azimi_guardian_state"

    private const val VAULT_INITIALIZED =
        "vault_initialized"

    private const val LAST_ERROR =
        "last_storage_error"

    fun isVaultInitialized(
        context: Context
    ): Boolean {
        return runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .getBoolean(
                    VAULT_INITIALIZED,
                    false
                )
        }.getOrDefault(false)
    }

    fun initializeVault(
        context: Context
    ): Boolean {
        return runCatching {
            if (isVaultInitialized(context)) {
                return true
            }

            val statusSaved = VaultCrypto.put(
                context,
                "vault_status",
                "LOCKED"
            )

            val policySaved = VaultCrypto.put(
                context,
                "ai_memory_policy",
                "SAFE_CONTEXT_ONLY"
            )

            if (!statusSaved || !policySaved) {
                recordError(
                    context,
                    "Vault values could not be saved."
                )

                return false
            }

            val initialized = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .putBoolean(
                    VAULT_INITIALIZED,
                    true
                )
                .commit()

            if (!initialized) {
                recordError(
                    context,
                    "Vault initialization flag could not be saved."
                )
            }

            initialized
        }.getOrElse {
            recordError(
                context,
                it.message ?: "Unknown Vault initialization error"
            )

            false
        }
    }

    fun getVaultStatus(
        context: Context
    ): String {
        return VaultCrypto.get(
            context,
            "vault_status"
        ) ?: "LOCKED"
    }

    fun getAIMemoryPolicy(
        context: Context
    ): String {
        return VaultCrypto.get(
            context,
            "ai_memory_policy"
        ) ?: "SAFE_CONTEXT_ONLY"
    }

    fun lockVault(
        context: Context
    ): Boolean {
        return runCatching {
            if (!isVaultInitialized(context)) {
                if (!initializeVault(context)) {
                    return false
                }
            }

            val locked = VaultCrypto.put(
                context,
                "vault_status",
                "LOCKED"
            )

            if (!locked) {
                recordError(
                    context,
                    "Vault could not be locked."
                )
            }

            locked
        }.getOrElse {
            recordError(
                context,
                it.message ?: "Unknown Vault lock error"
            )

            false
        }
    }

    fun unlockVault(
        context: Context
    ): Boolean {
        return runCatching {
            if (!isVaultInitialized(context)) {
                if (!initializeVault(context)) {
                    return false
                }
            }

            val unlocked = VaultCrypto.put(
                context,
                "vault_status",
                "UNLOCKED"
            )

            if (!unlocked) {
                recordError(
                    context,
                    "Vault could not be unlocked."
                )
            }

            unlocked
        }.getOrElse {
            recordError(
                context,
                it.message ?: "Unknown Vault unlock error"
            )

            false
        }
    }

    fun clearVault(
        context: Context
    ): Boolean {
        return runCatching {
            val first = VaultCrypto.delete(
                context,
                "vault_status"
            )

            val second = VaultCrypto.delete(
                context,
                "ai_memory_policy"
            )

            val third = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .clear()
                .commit()

            first && second && third
        }.getOrDefault(false)
    }

    fun recordError(
        context: Context,
        message: String
    ) {
        runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .putString(
                    LAST_ERROR,
                    message.take(500)
                )
                .commit()
        }
    }

    fun getLastError(
        context: Context
    ): String {
        return runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .getString(
                    LAST_ERROR,
                    "NONE"
                ) ?: "NONE"
        }.getOrDefault("UNAVAILABLE")
    }
}
