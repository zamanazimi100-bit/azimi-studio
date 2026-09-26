package com.azimi.guardian

import android.content.Context

/**
 * AZIMI — Z Shield
 *
 * High-level security policy gate for Atlas capabilities.
 *
 * Z Shield does NOT:
 *
 * - authenticate the owner
 * - unlock Z Vault
 * - elevate permissions
 * - execute tools
 * - bypass Android security
 * - grant Atlas additional authority
 * - expose Vault secrets
 *
 * Z Shield evaluates whether a registered Atlas capability
 * may proceed under the security state that already exists.
 *
 * Security model:
 *
 *     ATLAS REQUEST
 *          ↓
 *       Z SHIELD
 *          ↓
 *     PERMISSION POLICY
 *          ↓
 *     TOOL AVAILABILITY
 *          ↓
 *     ATLAS TOOL
 *          ↓
 *     EXISTING SECURITY BOUNDARIES
 *
 * Important principle:
 *
 *     CAPABILITY ≠ AUTHORITY
 *
 * A tool existing in AtlasToolRegistry does not mean that
 * the tool is authorized to perform an operation.
 *
 * Z Shield therefore fails closed whenever the required
 * security conditions cannot be established.
 */
object ZShield {

    /**
     * Result of the Z Shield policy evaluation.
     */
    enum class Decision {
        ALLOWED,
        DENIED,
        REQUIRES_CONFIRMATION
    }

    /**
     * Safe reason codes.
     *
     * These contain no secrets or protected data.
     */
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

    /**
     * Immutable result returned by the Shield.
     *
     * This is metadata only.
     * It must never contain protected Vault contents,
     * credentials, tokens, cryptographic keys, or raw
     * authentication information.
     */
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
     * Evaluate an Atlas tool before execution.
     *
     * This function does NOT execute the tool.
     *
     * The caller remains responsible for invoking
     * AtlasTool.execute() only after the policy result
     * permits it.
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

        /*
         * -----------------------------------------------------
         * REQUEST VALIDATION
         * -----------------------------------------------------
         */

        if (cleanRequest.isBlank()) {

            return denied(
                reasonCode =
                    ReasonCode.INVALID_REQUEST,

                message =
                    "Z Shield denied the request because it is empty.",

                tool =
                    tool
            )
        }

        /*
         * -----------------------------------------------------
         * TOOL ID VALIDATION
         * -----------------------------------------------------
         *
         * A registered capability must have a stable ID.
         */

        val toolId =
            tool.id.trim()

        if (toolId.isBlank()) {

            return denied(
                reasonCode =
                    ReasonCode.TOOL_NOT_FOUND,

                message =
                    "Z Shield denied the capability because its tool identity is invalid.",

                tool =
                    tool
            )
        }

        /*
         * -----------------------------------------------------
         * PERMISSION EVALUATION
         * -----------------------------------------------------
         *
         * Z Shield does not grant permissions.
         *
         * It asks the existing Guardian permission system
         * whether the declared permission is currently allowed.
         */

        val permissionDecision =
            runCatching {

                AtlasPermissionChecker.evaluate(
                    appContext,
                    tool.permission
                )

            }.getOrElse {

                return denied(
                    reasonCode =
                        ReasonCode.SECURITY_STATE_UNAVAILABLE,

                    message =
                        "Z Shield denied the capability because the security state could not be evaluated.",

                    tool =
                        tool
                )
            }

        if (!permissionDecision.allowed) {

            return denied(
                reasonCode =
                    permissionReason(
                        tool.permission
                    ),

                message =
                    permissionDecision.message,

                tool =
                    tool
            )
        }

        /*
         * -----------------------------------------------------
         * VAULT BOUNDARY
         * -----------------------------------------------------
         *
         * A tool declaring Vault access must satisfy BOTH:
         *
         * 1. Its declared Atlas permission.
         * 2. The actual Z Vault boundary.
         *
         * Z Shield does not unlock the Vault.
         */

        if (tool.requiresVaultAccess) {

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
                            "Z Shield denied the capability because Z Vault access could not be verified.",

                        tool =
                            tool
                    )
                }

            if (!vaultAccessAllowed) {

                return denied(
                    reasonCode =
                        ReasonCode.VAULT_ACCESS_REQUIRED,

                    message =
                        "Z Shield denied the capability because the protected Z Vault boundary is not currently accessible.",

                    tool =
                        tool
                )
            }
        }

        /*
         * -----------------------------------------------------
         * CONSEQUENTIAL OPERATION
         * -----------------------------------------------------
         *
         * A consequential capability must not silently become
         * authorized merely because the user is authenticated.
         *
         * Z Shield therefore separates:
         *
         *     SECURITY AUTHORIZATION
         *
         * from
         *
         *     EXECUTION CONFIRMATION
         *
         * The current v1 layer does not perform confirmation.
         * It reports that explicit confirmation is required.
         */

        if (tool.consequential) {

            return DecisionResult(
                decision =
                    Decision.REQUIRES_CONFIRMATION,

                reasonCode =
                    ReasonCode.CONSEQUENT_OPERATION_REQUIRES_CONFIRMATION,

                message =
                    "Z Shield requires explicit confirmation before this consequential capability can execute.",

                toolId =
                    toolId,

                permission =
                    tool.permission,

                requiresVaultAccess =
                    tool.requiresVaultAccess,

                consequential =
                    true
            )
        }

        /*
         * -----------------------------------------------------
         * AUTHORIZED
         * -----------------------------------------------------
         */

        return DecisionResult(
            decision =
                Decision.ALLOWED,

            reasonCode =
                ReasonCode.AUTHORIZED,

            message =
                "Z Shield authorized this Atlas capability under the current security policy.",

            toolId =
                toolId,

            permission =
                tool.permission,

            requiresVaultAccess =
                tool.requiresVaultAccess,

            consequential =
                false
        )
    }

    /**
     * Evaluate a registered tool by stable ID.
     *
     * The registry is responsible for discovering the tool.
     * Z Shield remains responsible for policy evaluation.
     */
    fun evaluate(
        context: Context,
        toolId: String,
        request: String
    ): DecisionResult {

        val normalizedId =
            toolId.trim().lowercase()

        if (normalizedId.isBlank()) {

            return DecisionResult(
                decision =
                    Decision.DENIED,

                reasonCode =
                    ReasonCode.INVALID_REQUEST,

                message =
                    "Z Shield denied the request because the tool identity is empty.",

                toolId =
                    null
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
                decision =
                    Decision.DENIED,

                reasonCode =
                    ReasonCode.TOOL_NOT_FOUND,

                message =
                    "Z Shield denied the request because the requested Atlas capability is not registered.",

                toolId =
                    normalizedId
            )
        }

        return evaluate(
            context =
                context,

            tool =
                tool,

            request =
                request
        )
    }

    /**
     * Determine whether a previously evaluated result permits
     * immediate execution.
     *
     * REQUIRES_CONFIRMATION is intentionally NOT considered
     * executable.
     */
    fun mayExecute(
        result: DecisionResult
    ): Boolean {

        return result.decision ==
            Decision.ALLOWED
    }

    /**
     * Determine whether explicit confirmation is required.
     */
    fun requiresConfirmation(
        result: DecisionResult
    ): Boolean {

        return result.decision ==
            Decision.REQUIRES_CONFIRMATION
    }

    /**
     * Determine whether the capability was denied.
     */
    fun isDenied(
        result: DecisionResult
    ): Boolean {

        return result.decision ==
            Decision.DENIED
    }

    /**
     * Safe permission-specific reason mapping.
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
     * Construct a safe denied result.
     */
    private fun denied(
        reasonCode: ReasonCode,
        message: String,
        tool: AtlasTool? = null
    ): DecisionResult {

        return DecisionResult(
            decision =
                Decision.DENIED,

            reasonCode =
                reasonCode,

            message =
                message,

            toolId =
                tool?.id?.trim()?.ifBlank { null },

            permission =
                tool?.permission,

            requiresVaultAccess =
                tool?.requiresVaultAccess
                    ?: false,

            consequential =
                tool?.consequential
                    ?: false
        )
    }

    /**
     * Safe policy summary.
     *
     * This contains policy metadata only.
     */
    fun policy(): Map<String, String> {

        return mapOf(

            "shield" to
                "ACTIVE",

            "default_decision" to
                "DENY_ON_FAILURE",

            "permission_model" to
                "ATLAS_PERMISSION_CHECKED",

            "vault_boundary" to
                "Z_VAULT_SERVICE",

            "owner_boundary" to
                "OWNER_AUTHORIZATION_REQUIRED",

            "sovereign_boundary" to
                "SOVEREIGN_AUTHORIZATION_REQUIRED",

            "consequential_operations" to
                "EXPLICIT_CONFIRMATION_REQUIRED",

            "automatic_privilege_elevation" to
                "DENIED",

            "automatic_vault_unlock" to
                "DENIED",

            "atlas_direct_unlock" to
                "DENIED",

            "security_failure_behavior" to
                "FAIL_CLOSED"
        )
    }

    /**
     * Safe human-readable diagnostics.
     *
     * No protected data is returned.
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

        val vaultRoot =
            runCatching {

                ZVaultService.canAccessRoot(
                    appContext
                )

            }.getOrDefault(false)

        val atlasSleeping =
            runCatching {

                ZSecuritySession.isAtlasSleeping(
                    appContext
                )

            }.getOrDefault(true)

        val registeredTools =
            runCatching {

                AtlasToolRegistry.count()

            }.getOrDefault(0)

        return buildString {

            appendLine(
                "Z SHIELD"
            )

            appendLine(
                "STATUS: ACTIVE"
            )

            appendLine(
                "FAILURE POLICY: DENY"
            )

            appendLine(
                "AUTHENTICATED: $authenticated"
            )

            appendLine(
                "VAULT ROOT ACCESS: $vaultRoot"
            )

            appendLine(
                "ATLAS SLEEPING: $atlasSleeping"
            )

            appendLine(
                "REGISTERED TOOLS: $registeredTools"
            )

            appendLine()

            appendLine(
                "AUTOMATIC PRIVILEGE ELEVATION: DENIED"
            )

            appendLine(
                "AUTOMATIC VAULT UNLOCK: DENIED"
            )

            appendLine(
                "ATLAS DIRECT UNLOCK: DENIED"
            )

            appendLine(
                "CONSEQUENTIAL ACTIONS: CONFIRMATION REQUIRED"
            )

            appendLine(
                "SECURITY FAILURE: FAIL CLOSED"
            )
        }
    }
}
