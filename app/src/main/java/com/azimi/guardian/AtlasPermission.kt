package com.azimi.guardian

/**
 * AZIMI Atlas Permission Boundary
 *
 * Defines what level of Guardian authorization a modular
 * Atlas Tool requires.
 *
 * A permission declaration does NOT grant permission.
 * Guardian must still verify the current security session
 * before a tool is allowed to execute.
 */
enum class AtlasPermission {

    /**
     * Read-only information that does not require protected
     * data or consequential access.
     */
    PUBLIC,

    /**
     * Requires an authenticated Guardian session.
     */
    AUTHENTICATED,

    /**
     * Requires access to protected Z Vault functionality.
     */
    VAULT,

    /**
     * Requires verified owner authority.
     */
    OWNER,

    /**
     * Requires the strongest owner-controlled authorization
     * boundary.
     */
    SOVEREIGN
}

/**
 * AZIMI Atlas Permission Decision
 *
 * Represents the result of Guardian's permission evaluation.
 *
 * This is intentionally a small, immutable result object.
 *
 * Security principle:
 *
 *     Capability request != authorization
 *
 * Atlas or an Atlas Tool may request a capability, but only
 * Guardian can produce an allowed decision.
 */
data class AtlasPermissionDecision(

    /**
     * Permission that was evaluated.
     */
    val permission: AtlasPermission,

    /**
     * Whether Guardian authorized the request.
     */
    val allowed: Boolean,

    /**
     * Safe human-readable explanation.
     *
     * This must never contain credentials, tokens, private
     * security material, or sensitive implementation details.
     */
    val message: String
)

/**
 * AZIMI Atlas Permission Checker
 *
 * Centralizes permission evaluation so individual tools do
 * not need to implement their own security interpretation.
 *
 * Guardian remains the final security authority.
 *
 * IMPORTANT:
 *
 * This object evaluates authorization.
 * It does not grant authorization.
 *
 * It also does not:
 * - unlock Z Vault
 * - authenticate the owner
 * - modify security sessions
 * - elevate permissions
 * - execute tools
 * - contact external AI providers
 */
object AtlasPermissionChecker {

    /**
     * Determine whether the current Guardian session satisfies
     * the requested Atlas permission.
     *
     * This method is retained for compatibility with existing
     * Atlas tools and code.
     */
    fun isAllowed(
        context: android.content.Context,
        permission: AtlasPermission
    ): Boolean {

        return evaluate(
            context = context,
            permission = permission
        ).allowed
    }

    /**
     * Evaluate an Atlas permission request.
     *
     * The evaluation is deliberately fail-closed:
     *
     * - Unknown/invalid state must not become authorization.
     * - Protected permissions require their existing Guardian
     *   security conditions.
     * - This function never changes security state.
     */
    fun evaluate(
        context: android.content.Context,
        permission: AtlasPermission
    ): AtlasPermissionDecision {

        val appContext =
            context.applicationContext

        return when (permission) {

            AtlasPermission.PUBLIC -> {

                AtlasPermissionDecision(
                    permission = permission,
                    allowed = true,
                    message = "Atlas public capability is available."
                )
            }

            AtlasPermission.AUTHENTICATED -> {

                val authenticated =
                    ZSecuritySession.isAuthenticated(
                        appContext
                    )

                if (authenticated) {

                    AtlasPermissionDecision(
                        permission = permission,
                        allowed = true,
                        message = "Guardian authentication is available."
                    )

                } else {

                    deny(
                        permission = permission
                    )
                }
            }

            AtlasPermission.VAULT -> {

                val authenticated =
                    ZSecuritySession.isAuthenticated(
                        appContext
                    )

                if (!authenticated) {

                    deny(
                        permission = permission
                    )

                } else {

                    val vaultLocked =
                        ZSecuritySession.isVaultLocked(
                            appContext
                        )

                    if (vaultLocked) {

                        deny(
                            permission = permission
                        )

                    } else {

                        AtlasPermissionDecision(
                            permission = permission,
                            allowed = true,
                            message = "Z Vault access is authorized."
                        )
                    }
                }
            }

            AtlasPermission.OWNER -> {

                val ownerVerified =
                    ZSecuritySession.isOwnerVerified(
                        appContext
                    )

                if (!ownerVerified) {

                    deny(
                        permission = permission
                    )

                } else {

                    val ownerAuthorized =
                        AtlasOwnerAuthority.hasOwnerAuthorization(
                            appContext
                        )

                    if (ownerAuthorized) {

                        AtlasPermissionDecision(
                            permission = permission,
                            allowed = true,
                            message = "Owner authorization is available."
                        )

                    } else {

                        deny(
                            permission = permission
                        )
                    }
                }
            }

            AtlasPermission.SOVEREIGN -> {

                val ownerVerified =
                    ZSecuritySession.isOwnerVerified(
                        appContext
                    )

                if (!ownerVerified) {

                    deny(
                        permission = permission
                    )

                } else {

                    val ownerAuthorized =
                        AtlasOwnerAuthority.hasOwnerAuthorization(
                            appContext
                        )

                    if (ownerAuthorized) {

                        AtlasPermissionDecision(
                            permission = permission,
                            allowed = true,
                            message =
                                "Sovereign owner authorization is available."
                        )

                    } else {

                        deny(
                            permission = permission
                        )
                    }
                }
            }
        }
    }

    /**
     * Returns a safe explanation when permission is missing.
     *
     * This does not expose internal credentials or security
     * implementation details.
     */
    fun denialMessage(
        permission: AtlasPermission
    ): String {

        return when (permission) {

            AtlasPermission.PUBLIC ->
                "Atlas public capability is available."

            AtlasPermission.AUTHENTICATED ->
                "Guardian authentication is required."

            AtlasPermission.VAULT ->
                "Z Vault access is required."

            AtlasPermission.OWNER ->
                "Owner authorization is required."

            AtlasPermission.SOVEREIGN ->
                "Sovereign owner authorization is required."
        }
    }

    /**
     * Construct a safe denied decision.
     *
     * Keeping denial construction centralized prevents individual
     * permission branches from accidentally exposing internal
     * security information.
     */
    private fun deny(
        permission: AtlasPermission
    ): AtlasPermissionDecision {

        return AtlasPermissionDecision(
            permission = permission,
            allowed = false,
            message = denialMessage(permission)
        )
    }
}
