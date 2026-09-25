package com.azimi.guardian

/**
 * AZIMI Workspace Access Policy
 *
 * Declarative access classification for workspace operations.
 *
 * This module defines access levels only.
 * It does not perform authentication or authorization.
 *
 * Future authorization layers may use these classifications
 * when deciding whether an operation is permitted.
 *
 * Design rules:
 *
 * - Declarative only.
 * - No biometric prompt.
 * - No authentication changes.
 * - No Android permission bypass.
 * - No secret storage.
 * - No data deletion.
 * - No automatic repair.
 * - No migration.
 * - No modification of Z Continuity.
 * - No external provider dependency.
 */
object AZIMIWorkspaceAccessPolicy {

    const val READ_ONLY =
        "READ_ONLY"

    const val WRITE_SAFE =
        "WRITE_SAFE"

    const val OWNER_AUTHORIZED =
        "OWNER_AUTHORIZED"

    const val RESTRICTED =
        "RESTRICTED"

    /**
     * Describes the meaning of an access level.
     */
    data class Policy(
        val level: String,
        val title: String,
        val description: String,
        val requiresOwnerAuthorization: Boolean,
        val allowsRead: Boolean,
        val allowsWrite: Boolean,
        val allowsDestructiveActions: Boolean
    )

    /**
     * Returns the policy definition for a given access level.
     */
    fun policy(
        level: String
    ): Policy {

        return when (level.trim().uppercase()) {

            READ_ONLY ->
                Policy(
                    level =
                        READ_ONLY,
                    title =
                        "Read Only",
                    description =
                        "May inspect approved workspace metadata without modifying data.",
                    requiresOwnerAuthorization =
                        false,
                    allowsRead =
                        true,
                    allowsWrite =
                        false,
                    allowsDestructiveActions =
                        false
                )

            WRITE_SAFE ->
                Policy(
                    level =
                        WRITE_SAFE,
                    title =
                        "Safe Write",
                    description =
                        "May perform controlled non-destructive workspace updates.",
                    requiresOwnerAuthorization =
                        false,
                    allowsRead =
                        true,
                    allowsWrite =
                        true,
                    allowsDestructiveActions =
                        false
                )

            OWNER_AUTHORIZED ->
                Policy(
                    level =
                        OWNER_AUTHORIZED,
                    title =
                        "Owner Authorized",
                    description =
                        "Requires explicit owner authorization before the operation may proceed.",
                    requiresOwnerAuthorization =
                        true,
                    allowsRead =
                        true,
                    allowsWrite =
                        true,
                    allowsDestructiveActions =
                        false
                )

            RESTRICTED ->
                Policy(
                    level =
                        RESTRICTED,
                    title =
                        "Restricted",
                    description =
                        "Unavailable through ordinary workspace operations.",
                    requiresOwnerAuthorization =
                        true,
                    allowsRead =
                        false,
                    allowsWrite =
                        false,
                    allowsDestructiveActions =
                        false
                )

            else ->
                Policy(
                    level =
                        RESTRICTED,
                    title =
                        "Unknown Access",
                    description =
                        "Unknown access classifications are denied by default.",
                    requiresOwnerAuthorization =
                        true,
                    allowsRead =
                        false,
                    allowsWrite =
                        false,
                    allowsDestructiveActions =
                        false
                )
        }
    }

    /**
     * Returns true when the access level is recognized.
     */
    fun isKnown(
        level: String
    ): Boolean {

        return when (level.trim().uppercase()) {

            READ_ONLY,
            WRITE_SAFE,
            OWNER_AUTHORIZED,
            RESTRICTED ->
                true

            else ->
                false
        }
    }

    /**
     * Returns true when the policy permits reading.
     */
    fun allowsRead(
        level: String
    ): Boolean {

        return policy(level).allowsRead
    }

    /**
     * Returns true when the policy permits safe writing.
     */
    fun allowsWrite(
        level: String
    ): Boolean {

        return policy(level).allowsWrite
    }

    /**
     * Returns true when explicit owner authorization is required.
     */
    fun requiresOwnerAuthorization(
        level: String
    ): Boolean {

        return policy(level).requiresOwnerAuthorization
    }

    /**
     * Destructive actions are denied by this policy layer.
     *
     * A future dedicated security design would be required
     * before any destructive capability could exist.
     */
    fun allowsDestructiveActions(
        level: String
    ): Boolean {

        return policy(level).allowsDestructiveActions
    }

    /**
     * Returns a safe policy summary.
     */
    fun summary(
        level: String
    ): String {

        val result =
            policy(level)

        return buildString {

            append(
                "AZIMI Workspace Access Policy"
            )

            append("\nLevel: ")
            append(result.level)

            append("\nTitle: ")
            append(result.title)

            append("\nDescription: ")
            append(result.description)

            append("\nRequires Owner Authorization: ")
            append(result.requiresOwnerAuthorization)

            append("\nAllows Read: ")
            append(result.allowsRead)

            append("\nAllows Write: ")
            append(result.allowsWrite)

            append("\nAllows Destructive Actions: ")
            append(result.allowsDestructiveActions)
        }
    }
}
