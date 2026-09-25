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

    private const val COMPONENT =
        "GUARDIAN STARTUP"

    private const val FILE =
        "GuardianDiagnosticsStartup.kt"

    fun start(
        context: Context
    ) {

        var degraded =
            false

        safeCheckpoint(
            context,
            "STARTUP_BEGIN",
            "start()"
        )

        /*
         * ------------------------------------------------------------
         * OWNER IDENTITY
         * ------------------------------------------------------------
         *
         * Owner identity is important, but an unexpected failure here
         * must not automatically terminate Guardian startup.
         */
        val ownerIdentityReady =
            runStartupStep(
                context = context,
                stage = "INITIALIZE_OWNER_IDENTITY"
            ) {
                AtlasOwnerAuthority.initializeOwnerIdentity(
                    context
                )
            }

        if (!ownerIdentityReady) {
            degraded = true
        }

        /*
         * ------------------------------------------------------------
         * VAULT
         * ------------------------------------------------------------
         */
        safeCheckpoint(
            context,
            "INITIALIZE_VAULT_BEGIN",
            "start()"
        )

        val vaultReady =
            runStartupStep(
                context = context,
                stage = "INITIALIZE_VAULT"
            ) {
                GuardianStorage.lockVault(
                    context
                )
            }

        if (!vaultReady) {

            degraded = true

            val storageError =
                runCatching {
                    GuardianStorage.getLastError(
                        context
                    )
                }.getOrDefault(
                    "Vault initialization failed."
                )

            safeSaveError(
                context,
                storageError
            )

            safeCheckpoint(
                context,
                "VAULT_INITIALIZATION_FAILED",
                "start()"
            )

            safeSaveEvidenceResult(
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

            safeContinuityFailure(
                context = context,
                stage = "INITIALIZE_VAULT",
                message = storageError
            )

            /*
             * Vault is a protected subsystem.
             *
             * We do not force unrelated Guardian systems to crash.
             * Guardian may continue with the Vault remaining locked.
             */
        } else {

            safeCheckpoint(
                context,
                "INITIALIZE_VAULT_COMPLETE",
                "start()"
            )
        }

        /*
         * ------------------------------------------------------------
         * EVIDENCE VERIFICATION
         * ------------------------------------------------------------
         *
         * Evidence failure is isolated from the rest of startup.
         */
        if (vaultReady) {

            safeCheckpoint(
                context,
                "EVIDENCE_VERIFICATION_BEGIN",
                "start()"
            )

            val evidenceResult =
                runEvidenceVerification(
                    context
                )

            safeSaveEvidenceResult(
                context,
                evidenceResult
            )

            when (
                evidenceResult.status
            ) {

                GuardianEvidenceVerifier.STATUS_VALID -> {
                    safeCheckpoint(
                        context,
                        "EVIDENCE_VERIFICATION_VALID",
                        "start()"
                    )
                }

                GuardianEvidenceVerifier.STATUS_INCOMPLETE -> {
                    degraded = true

                    safeCheckpoint(
                        context,
                        "EVIDENCE_VERIFICATION_INCOMPLETE",
                        "start()"
                    )

                    safeContinuityFailure(
                        context = context,
                        stage = "EVIDENCE_VERIFICATION",
                        message =
                            evidenceResult.message
                    )
                }

                GuardianEvidenceVerifier.STATUS_INVALID -> {
                    degraded = true

                    safeCheckpoint(
                        context,
                        "EVIDENCE_VERIFICATION_INVALID",
                        "start()"
                    )

                    safeContinuityFailure(
                        context = context,
                        stage = "EVIDENCE_VERIFICATION",
                        message =
                            evidenceResult.message
                    )
                }

                else -> {
                    degraded = true

                    safeCheckpoint(
                        context,
                        "EVIDENCE_VERIFICATION_UNKNOWN",
                        "start()"
                    )

                    safeContinuityFailure(
                        context = context,
                        stage = "EVIDENCE_VERIFICATION",
                        message =
                            "Unknown evidence verification status."
                    )
                }
            }
        }

        /*
         * ------------------------------------------------------------
         * FINAL STARTUP STATE
         * ------------------------------------------------------------
         */
        if (degraded) {

            safeCheckpoint(
                context,
                "STARTUP_DEGRADED",
                "start()"
            )

            safeContinuityDiagnostic(
                context,
                "Guardian startup completed in degraded mode.",
                "One or more isolated startup subsystems reported a failure."
            )

        } else {

            safeCheckpoint(
                context,
                "STARTUP_COMPLETE",
                "start()"
            )

            safeContinuitySuccess(
                context,
                "Guardian startup completed successfully.",
                "All required startup steps completed without a reported failure."
            )

            safeClearError(
                context
            )
        }
    }

    private fun runStartupStep(
        context: Context,
        stage: String,
        operation: () -> Boolean
    ): Boolean {

        return try {

            val result =
                operation()

            if (result) {

                safeCheckpoint(
                    context,
                    "${stage}_SUCCESS",
                    "runStartupStep()"
                )

                safeContinuitySuccess(
                    context,
                    "$stage succeeded.",
                    "Startup subsystem completed successfully."
                )

            } else {

                safeCheckpoint(
                    context,
                    "${stage}_FAILED",
                    "runStartupStep()"
                )

                safeContinuityFailure(
                    context,
                    stage,
                    "$stage returned false."
                )
            }

            result

        } catch (exception: Throwable) {

            safeRecordFailure(
                context = context,
                stage = stage,
                throwable = exception
            )

            safeCheckpoint(
                context,
                "${stage}_EXCEPTION",
                "runStartupStep()"
            )

            false
        }
    }

    private fun runEvidenceVerification(
        context: Context
    ): GuardianEvidenceVerifier.VerificationResult {

        return try {

            verifyPackagedEvidence(
                context
            )

        } catch (exception: Throwable) {

            safeRecordFailure(
                context = context,
                stage = "EVIDENCE_VERIFICATION",
                throwable = exception
            )

            GuardianEvidenceVerifier.VerificationResult(
                status =
                    GuardianEvidenceVerifier.STATUS_INCOMPLETE,
                message =
                    "Evidence verification failed safely: " +
                        (
                            exception.message
                                ?: "Unknown error"
                            ),
                recordId = "",
                integrityAlgorithm = "",
                integrityVersion = ""
            )
        }
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

                safeRecordFailure(
                    context = context,
                    stage = "READ_PACKAGED_EVIDENCE",
                    throwable = exception
                )

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

        return try {

            GuardianEvidenceVerifier.verify(
                context = context,
                recordJson = json
            )

        } catch (exception: Throwable) {

            safeRecordFailure(
                context = context,
                stage = "VERIFY_PACKAGED_EVIDENCE",
                throwable = exception
            )

            GuardianEvidenceVerifier.VerificationResult(
                status =
                    GuardianEvidenceVerifier.STATUS_INCOMPLETE,
                message =
                    "Packaged evidence verification failed safely.",
                recordId = "",
                integrityAlgorithm = "",
                integrityVersion = ""
            )
        }
    }

    private fun safeRecordFailure(
        context: Context,
        stage: String,
        throwable: Throwable
    ) {

        runCatching {

            ZFailureLocator.recordFailure(
                context = context,
                component = COMPONENT,
                file = FILE,
                function = "startup",
                stage = stage,
                throwable = throwable
            )

        }.onFailure {

            /*
             * Failure reporting must never become the cause
             * of a second failure.
             *
             * The original failure has already been contained.
             */
        }

        safeSaveError(
            context,
            ZFailureLocator.describe(
                throwable
            )
        )
    }

    private fun safeContinuityFailure(
        context: Context,
        stage: String,
        message: String
    ) {

        runCatching {

            ZContinuityStorage.recordFailure(
                context = context,
                title =
                    "Guardian startup: $stage",
                content =
                    "Startup subsystem reported a contained failure.\n" +
                        "Stage: $stage\n" +
                        "Message: ${message.take(1000)}"
            )

        }
    }

    private fun safeContinuitySuccess(
        context: Context,
        title: String,
        content: String
    ) {

        runCatching {

            ZContinuityStorage.recordDiagnostic(
                context = context,
                title = title,
                content = content
            )

        }
    }

    private fun safeContinuityDiagnostic(
        context: Context,
        title: String,
        content: String
    ) {

        runCatching {

            ZContinuityStorage.recordDiagnostic(
                context = context,
                title = title,
                content = content
            )

        }
    }

    private fun safeCheckpoint(
        context: Context,
        value: String,
        function: String
    ) {

        runCatching {

            checkpoint(
                context,
                value
            )

        }

        runCatching {

            ZFailureLocator.recordCheckpoint(
                context = context,
                component = COMPONENT,
                file = FILE,
                function = function,
                stage = value
            )

        }
    }

    private fun safeSaveEvidenceResult(
        context: Context,
        result:
            GuardianEvidenceVerifier.VerificationResult
    ) {

        runCatching {

            saveEvidenceResult(
                context,
                result
            )
        }
    }

    private fun safeSaveError(
        context: Context,
        value: String
    ) {

        runCatching {

            saveError(
                context,
                value
            )
        }
    }

    private fun safeClearError(
        context: Context
    ) {

        runCatching {

            clearError(
                context
            )
        }
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

        return runCatching {

            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .getString(
                    LAST_CHECKPOINT,
                    "NOT_AVAILABLE"
                )
                ?: "NOT_AVAILABLE"

        }.getOrDefault(
            "UNAVAILABLE"
        )
    }

    fun getStartupError(
        context: Context
    ): String {

        return runCatching {

            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .getString(
                    STARTUP_ERROR,
                    "NONE"
                )
                ?: "NONE"

        }.getOrDefault(
            "UNAVAILABLE"
        )
    }

    fun getEvidenceStatus(
        context: Context
    ): String {

        return runCatching {

            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .getString(
                    EVIDENCE_STATUS,
                    GuardianEvidenceVerifier.STATUS_INCOMPLETE
                )
                ?: GuardianEvidenceVerifier.STATUS_INCOMPLETE

        }.getOrDefault(
            GuardianEvidenceVerifier.STATUS_INCOMPLETE
        )
    }

    fun getEvidenceMessage(
        context: Context
    ): String {

        return runCatching {

            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .getString(
                    EVIDENCE_MESSAGE,
                    "Evidence verification has not completed."
                )
                ?: "Evidence verification has not completed."

        }.getOrDefault(
            "Evidence status unavailable."
        )
    }
}
