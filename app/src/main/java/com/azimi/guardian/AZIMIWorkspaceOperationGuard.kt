package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Workspace Operation Guard
 *
 * Enforcement layer for AZIMI Workspace access policy.
 *
 * Responsibilities:
 * - Check whether an operation is allowed by the workspace policy.
 * - Distinguish read, safe-write, owner-authorized, and restricted operations.
 * - Require explicit owner-authorization proof when the selected policy requires it.
 * - Provide a trusted Context-based path to the existing AtlasOwnerAuthority.
 * - Never perform authentication itself.
 * - Never bypass Guardian security.
 * - Never perform destructive actions.
 *
 * Authentication and owner verification remain the responsibility
 * of the existing Guardian / Z Origin security architecture.
 *
 * Security principle:
 *
 * Policy says WHAT a level permits.
 * Owner authorization proves WHETHER the owner has authorized it.
 * This guard combines those facts before returning an allowed decision.
 *
 * Build #96 security integration:
 *
 * Context-based checks obtain owner authorization from the existing
 * AtlasOwnerAuthority.hasOwnerAuthorization(context) source.
 *
 * The Guard does NOT trigger biometric authentication, Voice Lock,
 * Z Origin authentication, or any other authentication flow.
 */
object AZIMIWorkspaceOperationGuard {

    data class Decision(
        val allowed: Boolean,
        val level: String,
        val operation: String,
        val requiresOwnerAuthorization: Boolean,
        val message: String
    )

    /**
     * Checks whether a read operation is allowed.
     *
     * If the selected policy requires owner authorization,
     * the caller must provide verified owner authorization.
     *
     * The default is false so callers cannot accidentally
     * bypass the owner-authorized boundary.
     */
    fun checkRead(
        level: String = AZIMIWorkspaceAccessPolicy.READ_ONLY,
        ownerAuthorizationVerified: Boolean = false
    ): Decision {

        val safeLevel =
            normalizeLevel(level)

        val policy =
            AZIMIWorkspaceAccessPolicy.policy(
                safeLevel
            )

        if (
            !AZIMIWorkspaceAccessPolicy.isKnown(
                safeLevel
            )
        ) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "READ",
                requiresOwnerAuthorization = true,
                message =
                    "Workspace read operation is blocked because the access policy is unknown."
            )
        }

        if (!policy.allowsRead) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "READ",
                requiresOwnerAuthorization =
                    policy.requiresOwnerAuthorization,
                message =
                    "Workspace read operation is not allowed by the current policy."
            )
        }

        if (
            policy.requiresOwnerAuthorization &&
            !ownerAuthorizationVerified
        ) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "READ",
                requiresOwnerAuthorization = true,
                message =
                    "Workspace read operation requires verified owner authorization."
            )
        }

        return Decision(
            allowed = true,
            level = safeLevel,
            operation = "READ",
            requiresOwnerAuthorization =
                policy.requiresOwnerAuthorization,
            message =
                "Workspace read operation allowed."
        )
    }

    /**
     * Trusted Context-based read check.
     *
     * Owner authorization is obtained from the existing
     * AtlasOwnerAuthority state.
     *
     * This method does not authenticate the owner.
     */
    fun checkRead(
        context: Context,
        level: String = AZIMIWorkspaceAccessPolicy.READ_ONLY
    ): Decision {

        return checkRead(
            level = level,
            ownerAuthorizationVerified =
                hasVerifiedOwnerAuthorization(context)
        )
    }

    /**
     * Checks whether a non-destructive write operation is allowed.
     *
     * WRITE_SAFE does not require owner authorization.
     *
     * OWNER_AUTHORIZED requires explicit verified owner authorization.
     */
    fun checkWrite(
        level: String = AZIMIWorkspaceAccessPolicy.WRITE_SAFE,
        ownerAuthorizationVerified: Boolean = false
    ): Decision {

        val safeLevel =
            normalizeLevel(level)

        val policy =
            AZIMIWorkspaceAccessPolicy.policy(
                safeLevel
            )

        if (
            !AZIMIWorkspaceAccessPolicy.isKnown(
                safeLevel
            )
        ) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "WRITE",
                requiresOwnerAuthorization = true,
                message =
                    "Workspace write operation is blocked because the access policy is unknown."
            )
        }

        if (!policy.allowsWrite) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "WRITE",
                requiresOwnerAuthorization =
                    policy.requiresOwnerAuthorization,
                message =
                    "Workspace write operation is not allowed by the current policy."
            )
        }

        if (
            policy.requiresOwnerAuthorization &&
            !ownerAuthorizationVerified
        ) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "WRITE",
                requiresOwnerAuthorization = true,
                message =
                    "Workspace write operation requires verified owner authorization."
            )
        }

        return Decision(
            allowed = true,
            level = safeLevel,
            operation = "WRITE",
            requiresOwnerAuthorization =
                policy.requiresOwnerAuthorization,
            message =
                "Safe workspace write operation allowed."
        )
    }

    /**
     * Trusted Context-based write check.
     *
     * Owner authorization is obtained from the existing
     * AtlasOwnerAuthority state.
     *
     * This method does not authenticate the owner.
     */
    fun checkWrite(
        context: Context,
        level: String = AZIMIWorkspaceAccessPolicy.WRITE_SAFE
    ): Decision {

        return checkWrite(
            level = level,
            ownerAuthorizationVerified =
                hasVerifiedOwnerAuthorization(context)
        )
    }

    /**
     * Checks whether an owner-authorized operation may proceed.
     *
     * This method does NOT authenticate the owner.
     *
     * It requires the caller to provide a positive authorization proof
     * obtained from the existing Guardian / Z Origin authority layer.
     *
     * Without that proof, the operation is always blocked.
     */
    fun checkOwnerAuthorized(
        level: String = AZIMIWorkspaceAccessPolicy.OWNER_AUTHORIZED,
        ownerAuthorizationVerified: Boolean = false
    ): Decision {

        val safeLevel =
            normalizeLevel(level)

        val policy =
            AZIMIWorkspaceAccessPolicy.policy(
                safeLevel
            )

        if (
            !AZIMIWorkspaceAccessPolicy.isKnown(
                safeLevel
            )
        ) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "OWNER_AUTHORIZED",
                requiresOwnerAuthorization = true,
                message =
                    "Invalid owner-authorization policy."
            )
        }

        if (
            safeLevel !=
                AZIMIWorkspaceAccessPolicy.OWNER_AUTHORIZED
        ) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "OWNER_AUTHORIZED",
                requiresOwnerAuthorization =
                    policy.requiresOwnerAuthorization,
                message =
                    "Owner-authorized operation requires the OWNER_AUTHORIZED policy."
            )
        }

        if (!policy.requiresOwnerAuthorization) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "OWNER_AUTHORIZED",
                requiresOwnerAuthorization = true,
                message =
                    "Owner-authorized operation is not permitted by the selected policy."
            )
        }

        if (!ownerAuthorizationVerified) {
            return Decision(
                allowed = false,
                level = safeLevel,
                operation = "OWNER_AUTHORIZED",
                requiresOwnerAuthorization = true,
                message =
                    "Verified owner authorization is required before the operation may proceed."
            )
        }

        return Decision(
            allowed = true,
            level = safeLevel,
            operation = "OWNER_AUTHORIZED",
            requiresOwnerAuthorization = true,
            message =
                "Owner authorization verified. Workspace operation may proceed."
        )
    }

    /**
     * Trusted Context-based owner-authorized check.
     *
     * The authorization decision comes directly from
     * AtlasOwnerAuthority.hasOwnerAuthorization(context).
     *
     * No authentication is started here.
     */
    fun checkOwnerAuthorized(
        context: Context,
        level: String = AZIMIWorkspaceAccessPolicy.OWNER_AUTHORIZED
    ): Decision {

        return checkOwnerAuthorized(
            level = level,
            ownerAuthorizationVerified =
                hasVerifiedOwnerAuthorization(context)
        )
    }

    /**
     * Checks whether a destructive operation is permitted.
     *
     * Destructive workspace operations are currently denied.
     *
     * Even verified owner authorization does not enable destructive
     * workspace operations through this guard.
     */
    fun checkDestructive(
        level: String = AZIMIWorkspaceAccessPolicy.OWNER_AUTHORIZED,
        ownerAuthorizationVerified: Boolean = false
    ): Decision {

        val safeLevel =
            normalizeLevel(level)

        return Decision(
            allowed = false,
            level = safeLevel,
            operation = "DESTRUCTIVE",
            requiresOwnerAuthorization = true,
            message =
                "Destructive workspace operation is blocked by policy."
        )
    }

    /**
     * Trusted Context-based destructive check.
     *
     * Destructive operations remain blocked regardless of
     * current owner authorization state.
     */
    fun checkDestructive(
        context: Context,
        level: String = AZIMIWorkspaceAccessPolicy.OWNER_AUTHORIZED
    ): Decision {

        return checkDestructive(
            level = level,
            ownerAuthorizationVerified =
                hasVerifiedOwnerAuthorization(context)
        )
    }

    /**
     * Restricted operations are always blocked.
     *
     * This guard must never silently bypass restricted operations.
     */
    fun checkRestricted(): Decision {

        val level =
            AZIMIWorkspaceAccessPolicy.RESTRICTED

        return Decision(
            allowed = false,
            level = level,
            operation = "RESTRICTED",
            requiresOwnerAuthorization = true,
            message =
                "Restricted workspace operation is blocked."
        )
    }

    /**
     * General policy check.
     *
     * ownerAuthorizationVerified must come from an existing
     * trusted Guardian / Z Origin authority decision.
     *
     * This method never performs authentication itself.
     *
     * Workspace initialization is classified as a safe-write
     * operation because initialization creates/updates the
     * controlled non-destructive workspace foundation.
     */
    fun check(
        operation: String,
        level: String,
        ownerAuthorizationVerified: Boolean = false
    ): Decision {

        val safeOperation =
            operation.trim().uppercase()

        return when (safeOperation) {

            "READ" ->
                checkRead(
                    level = level,
                    ownerAuthorizationVerified =
                        ownerAuthorizationVerified
                )

            "WRITE",
            "INITIALIZE" ->
                checkWrite(
                    level = level,
                    ownerAuthorizationVerified =
                        ownerAuthorizationVerified
                )

            "OWNER_AUTHORIZED",
            "OWNER" ->
                checkOwnerAuthorized(
                    level = level,
                    ownerAuthorizationVerified =
                        ownerAuthorizationVerified
                )

            "DESTRUCTIVE",
            "DELETE",
            "RESET",
            "CLEAR" ->
                checkDestructive(
                    level = level,
                    ownerAuthorizationVerified =
                        ownerAuthorizationVerified
                )

            "RESTRICTED" ->
                checkRestricted()

            else ->
                Decision(
                    allowed = false,
                    level = normalizeLevel(level),
                    operation = safeOperation,
                    requiresOwnerAuthorization = true,
                    message =
                        "Unknown workspace operation is blocked."
                )
        }
    }

    /**
     * Trusted Context-based general policy check.
     *
     * The owner-authorization state is obtained directly from
     * AtlasOwnerAuthority.
     *
     * This method does not trigger authentication.
     */
    fun check(
        context: Context,
        operation: String,
        level: String
    ): Decision {

        return check(
            operation = operation,
            level = level,
            ownerAuthorizationVerified =
                hasVerifiedOwnerAuthorization(context)
        )
    }

    /**
     * Returns whether an operation can proceed under policy
     * and, when required, verified owner authorization.
     */
    fun isAllowed(
        operation: String,
        level: String,
        ownerAuthorizationVerified: Boolean = false
    ): Boolean {

        return check(
            operation = operation,
            level = level,
            ownerAuthorizationVerified =
                ownerAuthorizationVerified
        ).allowed
    }

    /**
     * Trusted Context-based permission check.
     *
     * Owner authorization is derived from the existing
     * AtlasOwnerAuthority state.
     */
    fun isAllowed(
        context: Context,
        operation: String,
        level: String
    ): Boolean {

        return check(
            context = context,
            operation = operation,
            level = level
        ).allowed
    }

    /**
     * Returns a human-readable explanation of the policy decision.
     */
    fun explain(
        operation: String,
        level: String,
        ownerAuthorizationVerified: Boolean = false
    ): String {

        return check(
            operation = operation,
            level = level,
            ownerAuthorizationVerified =
                ownerAuthorizationVerified
        ).message
    }

    /**
     * Trusted Context-based explanation.
     *
     * The explanation reflects the actual current owner-authority
     * state reported by AtlasOwnerAuthority.
     */
    fun explain(
        context: Context,
        operation: String,
        level: String
    ): String {

        return check(
            context = context,
            operation = operation,
            level = level
        ).message
    }

    /**
     * Obtains the current verified owner-authority state from
     * the existing AtlasOwnerAuthority security layer.
     *
     * This is deliberately failure-closed:
     *
     * - If authority is verified -> true.
     * - If authority is not verified -> false.
     * - If authority lookup fails -> false.
     *
     * No authentication is started here.
     */
    private fun hasVerifiedOwnerAuthorization(
        context: Context
    ): Boolean {

        return runCatching {
            AtlasOwnerAuthority.hasOwnerAuthorization(
                context
            )
        }.getOrDefault(false)
    }

    /**
     * Normalizes policy input without changing its meaning.
     */
    private fun normalizeLevel(
        level: String
    ): String {

        return level.trim().uppercase()
    }
}
