package com.azimi.guardian

/**
 * Represents the result of an AZIMI capability authorization
 * decision.
 *
 * This is a result contract only.
 * It does not authenticate a requester, modify security state,
 * grant permissions, or execute a capability.
 */
data class AzimiCapabilityAuthorizationResult(

    /**
     * Identifier of the requester.
     *
     * This must not contain passwords, tokens, keys,
     * biometric material, or other protected credentials.
     */
    val requesterId: String,

    /**
     * Identifier of the requested capability.
     */
    val capabilityId: String,

    /**
     * Optional component requesting the capability.
     */
    val requesterComponentId: String? = null,

    /**
     * Explicit authorization decision.
     */
    val state: AzimiCapabilityAuthorizationState,

    /**
     * Authorization context associated with the request.
     */
    val authorization: AzimiCapabilityAuthorization? = null,

    /**
     * Safe human-readable explanation of the decision.
     *
     * This must not contain protected credentials or secrets.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the non-secret identity fields of the result.
     */
    fun isValid(): Boolean {

        val requester =
            requesterId.trim()

        val capability =
            capabilityId.trim()

        val component =
            requesterComponentId?.trim()

        return requester.isNotEmpty() &&
            capability.isNotEmpty() &&
            (
                component == null ||
                    component.isNotEmpty()
            )
    }

    /**
     * Returns a normalized copy of this result.
     */
    fun normalized(): AzimiCapabilityAuthorizationResult {

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

            authorization =
                authorization
                    ?.normalized(),

            message =
                message.trim().ifEmpty {
                    "NONE"
                }
        )
    }

    /**
     * Returns true only when the explicit authorization
     * decision is AUTHORIZED and the result is structurally valid.
     */
    fun isAuthorized(): Boolean {

        return isValid() &&
            state ==
                AzimiCapabilityAuthorizationState.AUTHORIZED
    }

    /**
     * Returns true when the authorization decision explicitly
     * denies the request.
     */
    fun isDenied(): Boolean {

        return state ==
            AzimiCapabilityAuthorizationState.DENIED
    }
}
