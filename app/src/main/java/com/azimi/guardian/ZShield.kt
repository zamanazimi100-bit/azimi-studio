package com.azimi.guardian

import android.content.Context

/**
 * ZShield
 *
 * Central execution-policy gate for Atlas tools.
 *
 * Security model:
 *
 *     Atlas request
 *          ↓
 *       Z Shield
 *          ↓
 *   Permission / Vault / Operation policy
 *          ↓
 *     Atlas tool
 *
 * ZShield does NOT:
 *
 * - authenticate the owner
 * - unlock Z Vault
 * - elevate privileges
 * - execute tools
 * - contact AI providers
 * - expose credentials
 *
 * ZShield only decides whether the requested capability may
 * proceed to the tool execution boundary.
 *
 * Consequential operations require explicit confirmation.
 *
 * Read-only operations may be allowed when their required
 * security level is satisfied.
 */
object ZShield {

    enum class Decision {
        ALLOWED,
        DENIED,
        REQUIRES_CONFIRMATION
    }

    enum class ReasonCode {
        AUTHORIZED,
        INVALID_REQUEST,
        TOOL_NOT_FOUND,
        PERMISSION_DENIED,
        AUTHENTICATION_REQUIRED,
        VAULT_ACCESS_REQUIRED,
        VAULT_ACCESS_DENIED,
        OWNER_AUTHORIZATION_REQUIRED,
        SOVEREIGN_AUTHORIZATION_REQUIRED,
        CONSEQUENT_OPERATION_REQUIRES_CONFIRMATION,
        SECURITY_STATE_UNAVAILABLE,
        POLICY_ERROR
    }

    data class DecisionResult(
        val decision: Decision,
        val reasonCode: ReasonCode,
        val message: String,
        val toolId: String? = null,
        val permission: AtlasPermission? = null,
        val requiresVaultAccess: Boolean = false,
        val consequential: Boolean = false
    )

    /**
     * Main Shield evaluation entry point.
     *
     * Operation-aware behavior is intentionally applied here
     * rather than weakening the tool's security metadata.
     */
    fun evaluate(
        context: Context,
        tool: AtlasTool,
        request: String
    ): DecisionResult {

        val appContext =
            context.applicationContext

        val cleanRequest =
            request.trim()

        if (cleanRequest.isBlank()) {

            return denied(
                reasonCode = ReasonCode.INVALID_REQUEST,
                message = "Z Shield denied an empty Atlas request.",
                tool = tool
            )
        }

        val toolId =
            tool.id.trim().lowercase()

        if (toolId.isBlank()) {

            return denied(
                reasonCode = ReasonCode.TOOL_NOT_FOUND,
                message = "Z Shield denied a tool with an invalid identifier.",
                tool = tool
            )
        }

        /*
         * Determine whether this particular request is a
         * read-only Vault operation.
         *
         * This is intentionally operation-aware.
         *
         * The tool remains declared as consequential because
         * some operations it handles are consequential.
         */
        val vaultOperation =
            if (toolId == "z_vault") {
                classifyVaultOperation(cleanRequest)
            } else {
                VaultOperation.OTHER
            }

        /*
         * For ordinary tools, use their declared permission.
         *
         * For Z Vault status/diagnostics, the operation itself
         * is read-only. We therefore do not apply the tool's
         * static requiresVaultAccess pre-check here.
         *
         * AtlasVaultTool remains responsible for deciding what
         * Vault information can actually be returned.
         */
        val permissionToCheck =
            if (
                toolId == "z_vault" &&
                vaultOperation == VaultOperation.READ_ONLY_STATUS
            ) {
                AtlasPermission.AUTHENTICATED
            } else {
                tool.permission
            }

        val permissionDecision =
            runCatching {
                AtlasPermissionChecker.evaluate(
                    appContext,
                    permissionToCheck
                )
            }.getOrElse {

                return denied(
                    reasonCode = ReasonCode.SECURITY_STATE_UNAVAILABLE,
                    message = "Z Shield could not safely evaluate the current security state.",
                    tool = tool
                )
            }

        if (!permissionDecision.allowed) {

            return denied(
                reasonCode =
                    permissionReason(permissionToCheck),
                message =
                    permissionDecision.message,
                tool = tool,
                permissionOverride = permissionToCheck
            )
        }

        /*
         * Read-only Vault status/diagnostics:
         *
         * No confirmation is required.
         *
         * No Vault unlock is performed.
         *
         * No privilege is elevated.
         *
         * AtlasVaultTool remains the authority over what
         * diagnostic information is actually exposed.
         */
        if (
            toolId == "z_vault" &&
            vaultOperation == VaultOperation.READ_ONLY_STATUS
        ) {

            return DecisionResult(
                decision = Decision.ALLOWED,
                reasonCode = ReasonCode.AUTHORIZED,
                message =
                    "Z Shield authorized the read-only Z Vault status operation under the current authenticated session.",
                toolId = toolId,
                permission = permissionToCheck,
                requiresVaultAccess = false,
                consequential = false
            )
        }

        /*
         * Explicit Vault lock/unlock operations are
         * consequential and require confirmation.
         *
         * Z Shield NEVER performs the confirmation itself.
         * It only stops execution until the caller provides
         * the required explicit confirmation path.
         */
        val operationIsConsequential =
            if (toolId == "z_vault") {

                when (vaultOperation) {

                    VaultOperation.UNLOCK,
                    VaultOperation.LOCK ->
                        true

                    VaultOperation.COMPARTMENT_UNLOCK,
                    VaultOperation.COMPARTMENT_LOCK ->
                        true

                    VaultOperation.READ_ONLY_STATUS ->
                        false

                    VaultOperation.OTHER ->
                        true
                }

            } else {
                tool.consequential
            }

        /*
         * Vault root access is still required for operations
         * that actually operate on protected Vault state.
         *
         * Read-only status is intentionally excluded from this
         * pre-execution root-access gate.
         */
        val operationRequiresVaultAccess =
            if (
                toolId == "z_vault" &&
                vaultOperation == VaultOperation.READ_ONLY_STATUS
            ) {
                false
            } else {
                tool.requiresVaultAccess
            }

        if (operationRequiresVaultAccess) {

            val vaultAccessAllowed =
                runCatching {
                    ZVaultService.canAccessRoot(
                        appContext
                    )
                }.getOrElse {

                    return denied(
                        reasonCode =
                            ReasonCode.VAULT_ACCESS_DENIED,
                        message =
                            "Z Shield could not safely verify Z Vault root access.",
                        tool = tool,
                        permissionOverride = permissionToCheck
                    )
                }

            if (!vaultAccessAllowed) {

                return denied(
                    reasonCode =
                        ReasonCode.VAULT_ACCESS_REQUIRED,
                    message =
                        "Z Vault access is required for this operation.",
                    tool = tool,
                    permissionOverride = permissionToCheck
                )
            }
        }

        /*
         * Consequential operations stop here.
         *
         * They must never reach AtlasTool.execute()
         * without an explicit confirmation mechanism.
         */
        if (operationIsConsequential) {

            return DecisionResult(
                decision =
                    Decision.REQUIRES_CONFIRMATION,
                reasonCode =
                    ReasonCode.CONSEQUENT_OPERATION_REQUIRES_CONFIRMATION,
                message =
                    "Z Shield requires explicit confirmation before this consequential capability can execute.",
                toolId = toolId,
                permission = permissionToCheck,
                requiresVaultAccess =
                    operationRequiresVaultAccess,
                consequential = true
            )
        }

        /*
         * Safe, non-consequential operation.
         */
        return DecisionResult(
            decision = Decision.ALLOWED,
            reasonCode = ReasonCode.AUTHORIZED,
            message =
                "Z Shield authorized this Atlas capability under the current security policy.",
            toolId = toolId,
            permission = permissionToCheck,
            requiresVaultAccess =
                operationRequiresVaultAccess,
            consequential = false
        )
    }

    /**
     * Convenience evaluation by tool ID.
     */
    fun evaluate(
        context: Context,
        toolId: String,
        request: String
    ): DecisionResult {

        val normalizedId =
            toolId.trim().lowercase()

        if (normalizedId.isBlank()) {

            return denied(
                reasonCode = ReasonCode.INVALID_REQUEST,
                message = "Z Shield denied an empty tool identifier.",
                toolId = normalizedId
            )
        }

        val tool =
            runCatching {
                AtlasToolRegistry.getTool(
                    normalizedId
                )
            }.getOrNull()

        if (tool == null) {

            return DecisionResult(
                decision = Decision.DENIED,
                reasonCode = ReasonCode.TOOL_NOT_FOUND,
                message =
                    "Atlas tool '$normalizedId' is not registered.",
                toolId = normalizedId
            )
        }

        return evaluate(
            context = context,
            tool = tool,
            request = request
        )
    }

    /**
     * Returns true only when the capability may proceed
     * directly to execution.
     */
    fun mayExecute(
        result: DecisionResult
    ): Boolean {

        return result.decision ==
            Decision.ALLOWED
    }

    /**
     * Returns true when an explicit confirmation is required.
     */
    fun requiresConfirmation(
        result: DecisionResult
    ): Boolean {

        return result.decision ==
            Decision.REQUIRES_CONFIRMATION
    }

    /**
     * Returns true when Shield has denied the request.
     */
    fun isDenied(
        result: DecisionResult
    ): Boolean {

        return result.decision ==
            Decision.DENIED
    }

    /**
     * Classifies operations handled by Z Vault.
     *
     * IMPORTANT:
     *
     * Classification does not grant access.
     * It only determines the Shield policy applicable to
     * the requested operation.
     */
    private enum class VaultOperation {

        READ_ONLY_STATUS,

        UNLOCK,

        LOCK,

        COMPARTMENT_UNLOCK,

        COMPARTMENT_LOCK,

        OTHER
    }

    /**
     * Operation-aware classification for the Z Vault tool.
     */
    private fun classifyVaultOperation(
        request: String
    ): VaultOperation {

        val text =
            request.trim().lowercase()

        if (
            text.contains("status") ||
            text.contains("diagnostic")
        ) {

            return VaultOperation.READ_ONLY_STATUS
        }

        /*
         * Root unlock must be checked before the generic
         * unlock classification.
         */
        if (
            text.contains("unlock vault")
        ) {

            return VaultOperation.UNLOCK
        }

        if (
            text.contains("lock vault")
        ) {

            return VaultOperation.LOCK
        }

        /*
         * A compartment operation such as:
         *
         * "unlock z memory"
         * "lock z project"
         */
        if (
            text.contains("unlock") &&
            (
                text.contains("z memory") ||
                text.contains("memory") ||
                text.contains("z project") ||
                text.contains("project") ||
                text.contains("z recovery") ||
                text.contains("recovery") ||
                text.contains("z origin") ||
                text.contains("origin") ||
                text.contains("z sovereign") ||
                text.contains("sovereign")
            )
        ) {

            return VaultOperation.COMPARTMENT_UNLOCK
        }

        if (
            text.contains("lock") &&
            (
                text.contains("z memory") ||
                text.contains("memory") ||
                text.contains("z project") ||
                text.contains("project") ||
                text.contains("z recovery") ||
                text.contains("recovery") ||
                text.contains("z origin") ||
                text.contains("origin") ||
                text.contains("z sovereign") ||
                text.contains("sovereign")
            )
        ) {

            return VaultOperation.COMPARTMENT_LOCK
        }

        /*
         * Any other Z Vault request remains conservative.
         */
        return VaultOperation.OTHER
    }

    /**
     * Maps a permission failure to a precise Shield reason.
     */
    private fun permissionReason(
        permission: AtlasPermission
    ): ReasonCode {

        return when (permission) {

            AtlasPermission.PUBLIC ->
                ReasonCode.PERMISSION_DENIED

            AtlasPermission.AUTHENTICATED ->
                ReasonCode.AUTHENTICATION_REQUIRED

            AtlasPermission.VAULT ->
                ReasonCode.VAULT_ACCESS_REQUIRED

            AtlasPermission.OWNER ->
                ReasonCode.OWNER_AUTHORIZATION_REQUIRED

            AtlasPermission.SOVEREIGN ->
                ReasonCode.SOVEREIGN_AUTHORIZATION_REQUIRED
        }
    }

    /**
     * Creates a standard denied decision.
     */
    private fun denied(
        reasonCode: ReasonCode,
        message: String,
        tool: AtlasTool? = null,
        toolId: String? = null,
        permissionOverride: AtlasPermission? = null
    ): DecisionResult {

        return DecisionResult(
            decision = Decision.DENIED,
            reasonCode = reasonCode,
            message = message,
            toolId =
                toolId
                    ?: tool?.id?.trim()?.lowercase(),
            permission =
                permissionOverride
                    ?: tool?.permission,
            requiresVaultAccess =
                tool?.requiresVaultAccess
                    ?: false,
            consequential =
                tool?.consequential
                    ?: false
        )
    }

    /**
     * Public Shield policy description.
     *
     * This is descriptive only. It does not grant capability.
     */
    fun policy(): Map<String, String> {

        return mapOf(

            "shield" to
                "ACTIVE",

            "version" to
                "2.0",

            "default_decision" to
                "DENY_ON_FAILURE",

            "permission_model" to
                "ATLAS_PERMISSION_CHECKED",

            "operation_policy" to
                "OPERATION_AWARE",

            "vault_boundary" to
                "Z_VAULT_SERVICE",

            "owner_boundary" to
                "OWNER_AUTHORIZATION_REQUIRED",

            "sovereign_boundary" to
                "SOVEREIGN_AUTHORIZATION_REQUIRED",

            "read_only_status" to
                "AUTHENTICATED_SESSION_ALLOWED",

            "consequential_operations" to
                "EXPLICIT_CONFIRMATION_REQUIRED",

            "automatic_privilege_elevation" to
                "DENIED",

            "automatic_vault_unlock" to
                "DENIED",

            "atlas_direct_unlock" to
                "DENIED",

            "remote_unlock" to
                "DENIED",

            "security_failure_behavior" to
                "FAIL_CLOSED"
        )
    }

    /**
     * Human-readable Shield diagnostics.
     *
     * Diagnostics do not expose secrets or Vault contents.
     */
    fun diagnostics(
        context: Context
    ): String {

        val appContext =
            context.applicationContext

        val authenticated =
            runCatching {
                ZSecuritySession.isAuthenticated(
                    appContext
                )
            }.getOrDefault(false)

        val vaultRootAccess =
            runCatching {
                ZVaultService.canAccessRoot(
                    appContext
                )
            }.getOrDefault(false)

        val atlasSleeping =
            runCatching {
                AtlasSession.isSleeping(
                    appContext
                )
            }.getOrDefault(false)

        val registeredTools =
            runCatching {
                AtlasToolRegistry.count()
            }.getOrDefault(0)

        return buildString {

            appendLine("Z SHIELD")
            appendLine("STATUS=ACTIVE")
            appendLine("VERSION=2.0")
            appendLine(
                "AUTHENTICATED=$authenticated"
            )
            appendLine(
                "VAULT_ROOT_ACCESS=$vaultRootAccess"
            )
            appendLine(
                "ATLAS_SLEEPING=$atlasSleeping"
            )
            appendLine(
                "REGISTERED_TOOLS=$registeredTools"
            )
            appendLine(
                "OPERATION_POLICY=OPERATION_AWARE"
            )
            appendLine(
                "READ_ONLY_STATUS=ALLOWED_WHEN_AUTHENTICATED"
            )
            appendLine(
                "CONSEQUENTIAL_OPERATIONS=CONFIRMATION_REQUIRED"
            )
            appendLine(
                "AUTOMATIC_PRIVILEGE_ELEVATION=DENIED"
            )
            appendLine(
                "AUTOMATIC_VAULT_UNLOCK=DENIED"
            )
            appendLine(
                "FAILURE_BEHAVIOR=FAIL_CLOSED"
            )
        }
    }
}
