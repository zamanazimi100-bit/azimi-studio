package com.azimi.guardian

/**
 * Describes the authorization context for a capability request.
 *
 * This contract carries authorization information only.
 * It does not perform authentication and does not execute
 * the requested capability.
 *
 * Authentication remains the responsibility of the appropriate
 * owner/security systems.
 */
data class AzimiCapabilityAuthorization(

    /**
     * Stable identifier of the requester.
     *
     * This must not contain passwords, tokens, keys,
     * biometric material, or other protected credentials.
     */
    val requesterId: String,

    /**
     * Requested capability identifier.
     */
    val capabilityId: String,

    /**
     * Optional component requesting the capability.
     */
    val requesterComponentId: String? = null,

    /**
     * Whether the request is associated with an active
     * authorized session.
     */
    val authorizedSession: Boolean = false,

    /**
     * Human-readable authorization context.
     */
    val context: String = "NONE"
) {

    /**
     * Validates the non-secret authorization identity fields.
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
     * Returns a normalized copy of this authorization context.
     */
    fun normalized(): AzimiCapabilityAuthorization {

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

            context =
                context.trim().ifEmpty {
                    "NONE"
                }
        )
    }
}
