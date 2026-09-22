package com.azimi.guardian

import android.content.Context

object GuardianDiagnosticsStartup {

    private const val PREFS =
        "azimi_guardian_startup"

    private const val LAST_CHECKPOINT =
        "last_checkpoint"

    private const val STARTUP_ERROR =
        "startup_error"

    private const val EVIDENCE_STATUS =
        "evidence_status"

    private const val EVIDENCE_MESSAGE =
        "evidence_message"

    private const val EVIDENCE_RECORD_ASSET =
        "AZIMI-EVIDENCE-RECORD.json"

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

        val vaultReady =
            GuardianStorage.lockVault(
                context
            )

        if (!vaultReady) {
            saveError(
                context,
                GuardianStorage.getLastError(
                    context
                )
            )

            checkpoint(
                context,
                "VAULT_INITIALIZATION_FAILED"
            )

            saveEvidenceResult(
                context,
                GuardianEvidenceVerifier.VerificationResult(
                    status =
                        GuardianEvidenceVerifier.STATUS_INCOMPLETE,
                    message =
                        "Evidence verification skipped because Vault initialization failed.",
                    recordId = "",
                    integrityAlgorithm = "",
                    integrityVersion = ""
                )
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
            "EVIDENCE_VERIFICATION_BEGIN"
        )

        val evidenceResult =
            verifyPackagedEvidence(
                context
            )

        saveEvidenceResult(
            context,
            evidenceResult
        )

        when (
            evidenceResult.status
        ) {
            GuardianEvidenceVerifier.STATUS_VALID ->
                checkpoint(
                    context,
                    "EVIDENCE_VERIFICATION_VALID"
                )

            GuardianEvidenceVerifier.STATUS_INCOMPLETE ->
                checkpoint(
                    context,
                    "EVIDENCE_VERIFICATION_INCOMPLETE"
                )

            GuardianEvidenceVerifier.STATUS_INVALID ->
                checkpoint(
                    context,
                    "EVIDENCE_VERIFICATION_INVALID"
                )

            else ->
                checkpoint(
                    context,
                    "EVIDENCE_VERIFICATION_UNKNOWN"
                )
        }

        checkpoint(
            context,
            "STARTUP_COMPLETE"
        )

        clearError(
            context
        )
    }

    private fun verifyPackagedEvidence(
        context: Context
    ): GuardianEvidenceVerifier.VerificationResult {

        val json =
            try {
                context.assets
                    .open(
                        EVIDENCE_RECORD_ASSET
                    )
                    .bufferedReader()
                    .use { reader ->
                        reader.readText()
                    }
            } catch (exception: Exception) {

                return GuardianEvidenceVerifier.VerificationResult(
                    status =
                        GuardianEvidenceVerifier.STATUS_INCOMPLETE,
                    message =
                        "Packaged evidence record could not be read.",
                    recordId = "",
                    integrityAlgorithm = "",
                    integrityVersion = ""
                )
            }

        return GuardianEvidenceVerifier.verify(
            context = context,
            recordJson = json
        )
    }

    private fun saveEvidenceResult(
        context: Context,
        result:
            GuardianEvidenceVerifier.VerificationResult
    ) {
        runCatching {
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .edit()
                .putString(
                    EVIDENCE_STATUS,
                    result.status
                )
                .putString(
                    EVIDENCE_MESSAGE,
                    result.message.take(500)
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
                .remove(
                    STARTUP_ERROR
                )
                .commit()
        }
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

    fun getEvidenceStatus(
        context: Context
    ): String {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .getString(
                EVIDENCE_STATUS,
                GuardianEvidenceVerifier.STATUS_INCOMPLETE
            )
            ?: GuardianEvidenceVerifier.STATUS_INCOMPLETE
    }

    fun getEvidenceMessage(
        context: Context
    ): String {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .getString(
                EVIDENCE_MESSAGE,
                "Evidence verification has not completed."
            )
            ?: "Evidence verification has not completed."
    }
}
