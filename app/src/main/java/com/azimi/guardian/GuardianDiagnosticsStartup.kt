package com.azimi.guardian

import android.content.Context

object GuardianDiagnosticsStartup {

    private const val PREFS =
        "azimi_guardian_startup"

    private const val LAST_CHECKPOINT =
        "last_checkpoint"

    private const val STARTUP_ERROR =
        "startup_error"

    fun start(
        context: Context
    ) {
        checkpoint(
            context,
            "STARTUP_BEGIN"
        )

        AtlasOwnerAuthority.initializeOwnerIdentity(
            context
        )

        checkpoint(
            context,
            "INITIALIZE_VAULT_BEGIN"
        )

        val vaultReady = GuardianStorage.lockVault(
            context
        )

        if (!vaultReady) {
            saveError(
                context,
                GuardianStorage.getLastError(context)
            )

            checkpoint(
                context,
                "VAULT_INITIALIZATION_FAILED"
            )

            // Do not crash the app.
            // Guardian remains open with Vault locked.
            return
        }

        checkpoint(
            context,
            "INITIALIZE_VAULT_COMPLETE"
        )

        checkpoint(
            context,
            "STARTUP_COMPLETE"
        )

        clearError(context)
    }

    private fun checkpoint(
        context: Context,
        value: String
    ) {
        runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .putString(
                    LAST_CHECKPOINT,
                    value
                )
                .commit()
        }
    }

    private fun saveError(
        context: Context,
        value: String
    ) {
        runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .putString(
                    STARTUP_ERROR,
                    value.take(500)
                )
                .commit()
        }
    }

    private fun clearError(
        context: Context
    ) {
        runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .remove(STARTUP_ERROR)
                .commit()
        }
    }

    fun getLastCheckpoint(
        context: Context
    ): String {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .getString(
                LAST_CHECKPOINT,
                "NOT_AVAILABLE"
            ) ?: "NOT_AVAILABLE"
    }

    fun getStartupError(
        context: Context
    ): String {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .getString(
                STARTUP_ERROR,
                "NONE"
            ) ?: "NONE"
    }
}
