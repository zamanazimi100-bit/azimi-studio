package com.azimi.guardian

/**
 * Describes a request to execute an AZIMI capability.
 *
 * This contract carries execution context and the authorization
 * decision associated with the request.
 *
 * It does not:
 * - execute a capability
 * - authenticate a requester
 * - create an authorized session
 * - grant permissions
 * - contain passwords, tokens, keys, biometric material,
 *   or other protected credentials
 */
data class AzimiCapabilityExecutionRequest(

    /**
     * Stable identifier of the requester.
     */
    val requesterId: String,

    /**
     * Identifier of the capability to execute.
     */
    val capabilityId: String,

    /**
     * Optional component requesting execution.
     */
    val requesterComponentId: String? = null,

    /**
     * Authorization decision associated with this execution
     * request.
     */
    val authorization:
        AzimiCapabilityAuthorizationResult,

    /**
     * Human-readable execution context.
     *
     * This must remain non-secret and descriptive.
     */
    val context: String = "NONE"
) {

    /**
     * Validates the non-secret execution request fields.
     */
    fun isValid(): Boolean {

        val requester =
            requesterId.trim()

        val capability =
            capabilityId.trim()

        val component =
            requesterComponentId?.trim()

        val authorizationValid =
            authorization.isValid()

        return requester.isNotEmpty() &&
            capability.isNotEmpty() &&
            (
                component == null ||
                    component.isNotEmpty()
            ) &&
            authorizationValid
    }

    /**
     * Verifies that the authorization result belongs to
     * this execution request.
     *
     * This prevents an authorization result for one requester
     * or capability from being silently reused for another.
     */
    fun authorizationMatchesRequest(): Boolean {

        val requester =
            requesterId.trim()

        val capability =
            capabilityId.trim()

        val component =
            requesterComponentId
                ?.trim()

        val authorizedRequester =
            authorization.requesterId.trim()

        val authorizedCapability =
            authorization.capabilityId.trim()

        val authorizedComponent =
            authorization.requesterComponentId
                ?.trim()

        return requester == authorizedRequester &&
            capability == authorizedCapability &&
            component == authorizedComponent
    }

    /**
     * Returns true only when the request is structurally valid,
     * the authorization result belongs to this request, and the
     * authorization decision explicitly permits execution.
     */
    fun isAuthorizedForExecution(): Boolean {

        return isValid() &&
            authorizationMatchesRequest() &&
            authorization.isAuthorized()
    }

    /**
     * Returns a normalized copy of this execution request.
     */
    fun normalized(): AzimiCapabilityExecutionRequest {

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
                authorization.normalized(),

            context =
                context.trim().ifEmpty {
                    "NONE"
                }
        )
    }
}
