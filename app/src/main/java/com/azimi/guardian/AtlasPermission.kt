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
 * AZIMI Atlas Permission Checker
 *
 * Centralizes permission evaluation so individual tools do
 * not need to implement their own security interpretation.
 *
 * Guardian remains the final security authority.
 */
object AtlasPermissionChecker {

    /**
     * Determine whether the current Guardian session satisfies
     * the requested Atlas permission.
     */
    fun isAllowed(
        context: android.content.Context,
        permission: AtlasPermission
    ): Boolean {

        val appContext =
            context.applicationContext

        return when (permission) {

            AtlasPermission.PUBLIC ->
                true

            AtlasPermission.AUTHENTICATED ->
                ZSecuritySession.isAuthenticated(
                    appContext
                )

            AtlasPermission.VAULT ->
                ZSecuritySession.isAuthenticated(
                    appContext
                ) &&
                    !ZSecuritySession.isVaultLocked(
                        appContext
                    )

            AtlasPermission.OWNER ->
                ZSecuritySession.isOwnerVerified(
                    appContext
                ) &&
                    AtlasOwnerAuthority.hasOwnerAuthorization(
                        appContext
                    )

            AtlasPermission.SOVEREIGN ->
                ZSecuritySession.isOwnerVerified(
                    appContext
                ) &&
                    AtlasOwnerAuthority.hasOwnerAuthorization(
                        appContext
                    )
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
}
