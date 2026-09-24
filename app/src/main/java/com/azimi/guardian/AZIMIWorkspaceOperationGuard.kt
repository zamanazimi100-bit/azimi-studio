package com.azimi.guardian

/**
 * AZIMI Workspace Operation Guard
 *
 * Enforcement layer for AZIMI Workspace access policy.
 *
 * Responsibilities:
 * - Check whether an operation is allowed by the workspace policy.
 * - Distinguish read, safe-write, owner-authorized, and restricted operations.
 * - Require explicit owner-authorization proof when the selected policy requires it.
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

            "WRITE" ->
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
     * Normalizes policy input without changing its meaning.
     */
    private fun normalizeLevel(
        level: String
    ): String {

        return level.trim().uppercase()
    }
}
