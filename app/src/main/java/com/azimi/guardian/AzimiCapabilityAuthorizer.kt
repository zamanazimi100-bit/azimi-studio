package com.azimi.guardian

/**
 * Performs policy-only authorization for AZIMI capabilities.
 *
 * AzimiCapabilityAuthorizer does not:
 * - authenticate a requester
 * - create or modify an authorized session
 * - grant persistent permissions
 * - execute capabilities
 * - perform consequential actions
 *
 * It only evaluates an authorization request against the
 * currently observable AZIMI component and capability state.
 */
object AzimiCapabilityAuthorizer {

    /**
     * Evaluates an authorization request.
     *
     * Policy order:
     *
     * 1. Validate the request.
     * 2. Require an active authorized session.
     * 3. Verify the requester component when supplied.
     * 4. Resolve the requested capability.
     * 5. Return an explicit authorization result.
     */
    fun authorize(
        request: AzimiCapabilityAuthorization
    ): AzimiCapabilityAuthorizationResult {

        val normalized =
            request.normalized()

        if (!normalized.isValid()) {
            return result(
                request = normalized,
                state =
                    AzimiCapabilityAuthorizationState
                        .INVALID_REQUEST,
                message = "INVALID_AUTHORIZATION_REQUEST"
            )
        }

        if (!normalized.authorizedSession) {
            return result(
                request = normalized,
                state =
                    AzimiCapabilityAuthorizationState
                        .NO_ACTIVE_SESSION,
                message = "NO_ACTIVE_AUTHORIZED_SESSION"
            )
        }

        val requesterComponentId =
            normalized.requesterComponentId

        if (requesterComponentId != null) {

            val registered =
                runCatching {
                    AzimiComponentRegistry.contains(
                        requesterComponentId
                    )
                }.getOrDefault(false)

            if (!registered) {
                return result(
                    request = normalized,
                    state =
                        AzimiCapabilityAuthorizationState
                            .REQUESTER_COMPONENT_UNAVAILABLE,
                    message =
                        "REQUESTER_COMPONENT_UNAVAILABLE"
                )
            }
        }

        val capabilityResult =
            runCatching {

                if (requesterComponentId != null) {

                    AzimiCapabilityResolver.resolveFromComponent(
                        normalized.capabilityId,
                        requesterComponentId
                    )

                } else {

                    AzimiCapabilityResolver.resolve(
                        normalized.capabilityId
                    )
                }

            }.getOrElse {

                return result(
                    request = normalized,
                    state =
                        AzimiCapabilityAuthorizationState
                            .CAPABILITY_UNAVAILABLE,
                    message =
                        "CAPABILITY_RESOLUTION_FAILED"
                )
            }

        return when (capabilityResult.state) {

            AzimiCapabilityResolutionState.AVAILABLE ->
                result(
                    request = normalized,
                    state =
                        AzimiCapabilityAuthorizationState
                            .AUTHORIZED,
                    message =
                        "CAPABILITY_AUTHORIZED"
                )

            AzimiCapabilityResolutionState.NOT_DECLARED ->
                result(
                    request = normalized,
                    state =
                        AzimiCapabilityAuthorizationState
                            .CAPABILITY_NOT_DECLARED,
                    message =
                        "CAPABILITY_NOT_DECLARED"
                )

            AzimiCapabilityResolutionState
                .PROVIDER_COMPONENT_UNAVAILABLE ->
                result(
                    request = normalized,
                    state =
                        AzimiCapabilityAuthorizationState
                            .CAPABILITY_UNAVAILABLE,
                    message =
                        "CAPABILITY_PROVIDER_UNAVAILABLE"
                )

            AzimiCapabilityResolutionState.INVALID_CAPABILITY_ID ->
                result(
                    request = normalized,
                    state =
                        AzimiCapabilityAuthorizationState
                            .INVALID_REQUEST,
                    message =
                        "INVALID_CAPABILITY_ID"
                )

            AzimiCapabilityResolutionState.INVALID_COMPONENT_ID ->
                result(
                    request = normalized,
                    state =
                        AzimiCapabilityAuthorizationState
                            .INVALID_REQUEST,
                    message =
                        "INVALID_COMPONENT_ID"
                )
        }
    }

    /**
     * Convenience method for checking whether a request is
     * currently authorized.
     */
    fun isAuthorized(
        request: AzimiCapabilityAuthorization
    ): Boolean {

        return authorize(request)
            .isAuthorized()
    }

    /**
     * Builds a normalized authorization result.
     */
    private fun result(
        request: AzimiCapabilityAuthorization,
        state: AzimiCapabilityAuthorizationState,
        message: String
    ): AzimiCapabilityAuthorizationResult {

        return AzimiCapabilityAuthorizationResult(
            requesterId =
                request.requesterId.trim(),

            capabilityId =
                request.capabilityId.trim(),

            requesterComponentId =
                request.requesterComponentId
                    ?.trim()
                    ?.ifEmpty {
                        null
                    },

            state = state,

            authorization =
                request.normalized(),

            message =
                message.trim().ifEmpty {
                    "NONE"
                }
        )
    }
}
