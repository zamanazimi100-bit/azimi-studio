package com.azimi.guardian

/**
 * Represents the result of an AZIMI capability execution attempt.
 *
 * This is a result contract only.
 * It does not authorize, authenticate, retry, or execute a
 * capability.
 *
 * Output information must remain safe and must not contain
 * passwords, tokens, private keys, biometric material,
 * session cookies, or other protected credentials.
 */
data class AzimiCapabilityExecutionResult(

    /**
     * Identifier of the requester.
     */
    val requesterId: String,

    /**
     * Identifier of the executed capability.
     */
    val capabilityId: String,

    /**
     * Optional component that requested execution.
     */
    val requesterComponentId: String? = null,

    /**
     * Final execution state.
     */
    val state: AzimiCapabilityExecutionState,

    /**
     * Safe execution message.
     */
    val message: String = "NONE",

    /**
     * Optional safe output metadata.
     *
     * This field is descriptive metadata only.
     * It must never be used to transport protected credentials.
     */
    val outputMetadata: String? = null
) {

    /**
     * Validates the non-secret result identity fields.
     */
    fun isValid(): Boolean {

        val requester =
            requesterId.trim()

        val capability =
            capabilityId.trim()

        val component =
            requesterComponentId?.trim()

        val metadata =
            outputMetadata?.trim()

        return requester.isNotEmpty() &&
            capability.isNotEmpty() &&
            (
                component == null ||
                    component.isNotEmpty()
            ) &&
            (
                metadata == null ||
                    metadata.isNotEmpty()
            )
    }

    /**
     * Returns a normalized copy of this result.
     */
    fun normalized(): AzimiCapabilityExecutionResult {

        return copy(
            requesterId =
                requesterId.trim(),

            capabilityId =
                capabilityId.trim(),

            requesterComponentId =
                requesterComponentId
                    ?.trim()
                    ?.ifEmpty {
                        null
                    },

            message =
                message.trim().ifEmpty {
                    "NONE"
                },

            outputMetadata =
                outputMetadata
                    ?.trim()
                    ?.ifEmpty {
                        null
                    }
        )
    }

    /**
     * Returns true only when execution completed successfully
     * and the result is structurally valid.
     */
    fun isCompleted(): Boolean {

        return isValid() &&
            state ==
                AzimiCapabilityExecutionState.COMPLETED
    }

    /**
     * Returns true when execution explicitly failed.
     */
    fun isFailed(): Boolean {

        return state ==
            AzimiCapabilityExecutionState.FAILED
    }

    /**
     * Returns true when execution was denied before or during
     * the execution boundary.
     */
    fun isDenied(): Boolean {

        return state ==
            AzimiCapabilityExecutionState.DENIED
    }
}
