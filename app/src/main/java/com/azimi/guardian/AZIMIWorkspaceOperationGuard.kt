package com.azimi.guardian

/**
 * AZIMI Workspace Operation Guard
 *
 * Final enforcement layer for AZIMI Workspace access policy.
 *
 * Responsibilities:
 * - Check whether an operation is allowed by the workspace policy.
 * - Distinguish read, safe-write, owner-authorized, and restricted operations.
 * - Never perform authentication itself.
 * - Never bypass Guardian security.
 * - Never perform destructive actions.
 *
 * Authentication and authorization proofs remain the responsibility
 * of the existing Guardian / Z Origin security architecture.
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
     */
    fun checkRead(
        level: String = AZIMIWorkspaceAccessPolicy.READ_ONLY
    ): Decision {

        val safeLevel = normalizeLevel(level)

        return if (
            AZIMIWorkspaceAccessPolicy.isKnown(safeLevel) &&
            AZIMIWorkspaceAccessPolicy.allowsRead(safeLevel)
        ) {
            Decision(
                allowed = true,
                level = safeLevel,
                operation = "READ",
                requiresOwnerAuthorization =
                    AZIMIWorkspaceAccessPolicy.requiresOwnerAuthorization(safeLevel),
                message = "Workspace read operation allowed."
            )
        } else {
            Decision(
                allowed = false,
                level = safeLevel,
                operation = "READ",
                requiresOwnerAuthorization = true,
                message = "Workspace read operation is not allowed by the current policy."
            )
        }
    }

    /**
     * Checks whether a non-destructive write operation is allowed.
     */
    fun checkWrite(
        level: String = AZIMIWorkspaceAccessPolicy.WRITE_SAFE
    ): Decision {

        val safeLevel = normalizeLevel(level)

        return if (
            AZIMIWorkspaceAccessPolicy.isKnown(safeLevel) &&
            AZIMIWorkspaceAccessPolicy.allowsWrite(safeLevel)
        ) {
            Decision(
                allowed = true,
                level = safeLevel,
                operation = "WRITE",
                requiresOwnerAuthorization =
                    AZIMIWorkspaceAccessPolicy.requiresOwnerAuthorization(safeLevel),
                message = "Safe workspace write operation allowed."
            )
        } else {
            Decision(
                allowed = false,
                level = safeLevel,
                operation = "WRITE",
                requiresOwnerAuthorization = true,
                message = "Workspace write operation requires stronger authorization."
            )
        }
    }

    /**
     * Checks whether an owner-authorized operation may proceed.
     *
     * This method only verifies the policy requirement.
     * It does NOT authenticate the owner.
     */
    fun checkOwnerAuthorized(
        level: String = AZIMIWorkspaceAccessPolicy.OWNER_AUTHORIZED
    ): Decision {

        val safeLevel = normalizeLevel(level)

        return if (
            AZIMIWorkspaceAccessPolicy.isKnown(safeLevel) &&
            AZIMIWorkspaceAccessPolicy.requiresOwnerAuthorization(safeLevel)
        ) {
            Decision(
                allowed = true,
                level = safeLevel,
                operation = "OWNER_AUTHORIZED",
                requiresOwnerAuthorization = true,
                message = "Operation requires valid owner authorization before execution."
            )
        } else {
            Decision(
                allowed = false,
                level = safeLevel,
                operation = "OWNER_AUTHORIZED",
                requiresOwnerAuthorization = true,
                message = "Invalid owner-authorization policy."
            )
        }
    }

    /**
     * Checks whether a destructive operation is permitted.
     *
     * This does not execute the operation.
     */
    fun checkDestructive(
        level: String = AZIMIWorkspaceAccessPolicy.OWNER_AUTHORIZED
    ): Decision {

        val safeLevel = normalizeLevel(level)

        return if (
            AZIMIWorkspaceAccessPolicy.isKnown(safeLevel) &&
            AZIMIWorkspaceAccessPolicy.allowsDestructiveActions(safeLevel) &&
            AZIMIWorkspaceAccessPolicy.requiresOwnerAuthorization(safeLevel)
        ) {
            Decision(
                allowed = true,
                level = safeLevel,
                operation = "DESTRUCTIVE",
                requiresOwnerAuthorization = true,
                message = "Destructive operation requires verified owner authorization."
            )
        } else {
            Decision(
                allowed = false,
                level = safeLevel,
                operation = "DESTRUCTIVE",
                requiresOwnerAuthorization = true,
                message = "Destructive workspace operation is blocked by policy."
            )
        }
    }

    /**
     * Restricted operations are always blocked at this layer.
     *
     * A future dedicated security subsystem may explicitly handle
     * restricted operations. This guard must never silently bypass them.
     */
    fun checkRestricted(): Decision {

        val level = AZIMIWorkspaceAccessPolicy.RESTRICTED

        return Decision(
            allowed = false,
            level = level,
            operation = "RESTRICTED",
            requiresOwnerAuthorization = true,
            message = "Restricted workspace operation is blocked."
        )
    }

    /**
     * General policy check.
     *
     * This is useful for callers that already know the requested
     * operation type.
     */
    fun check(
        operation: String,
        level: String
    ): Decision {

        val safeOperation = operation.trim().uppercase()

        return when (safeOperation) {
            "READ" -> checkRead(level)
            "WRITE" -> checkWrite(level)
            "OWNER_AUTHORIZED",
            "OWNER" -> checkOwnerAuthorized(level)
            "DESTRUCTIVE",
            "DELETE",
            "RESET",
            "CLEAR" -> checkDestructive(level)
            "RESTRICTED" -> checkRestricted()
            else -> Decision(
                allowed = false,
                level = normalizeLevel(level),
                operation = safeOperation,
                requiresOwnerAuthorization = true,
                message = "Unknown workspace operation is blocked."
            )
        }
    }

    /**
     * Returns whether an operation can proceed under policy.
     */
    fun isAllowed(
        operation: String,
        level: String
    ): Boolean {
        return check(operation, level).allowed
    }

    /**
     * Returns a human-readable explanation of the policy decision.
     */
    fun explain(
        operation: String,
        level: String
    ): String {
        return check(operation, level).message
    }

    /**
     * Normalizes policy input without changing its meaning.
     */
    private fun normalizeLevel(level: String): String {
        return level.trim().uppercase()
    }
}
