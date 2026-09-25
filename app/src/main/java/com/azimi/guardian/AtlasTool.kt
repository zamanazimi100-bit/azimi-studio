package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Tool Contract
 *
 * Every modular Atlas capability implements this contract.
 *
 * Examples:
 * - Z Vault Tool
 * - Z Recovery Tool
 * - Z Cloud Tool
 * - Diagnostics Tool
 * - Project Tool
 * - Memory Tool
 *
 * Atlas Core decides WHAT capability is needed.
 * Guardian decides WHAT the capability is allowed to access.
 * The individual tool decides HOW the capability is performed.
 *
 * This keeps Atlas modular and prevents future features
 * from spreading across many unrelated files.
 */
interface AtlasTool {

    /**
     * Stable machine-readable identifier.
     *
     * Example:
     * "z_vault"
     * "z_recovery"
     * "diagnostics"
     */
    val id: String

    /**
     * Human-readable tool name.
     */
    val name: String

    /**
     * Short description of the capability.
     */
    val description: String

    /**
     * Required Guardian permission boundary.
     */
    val permission: AtlasPermission

    /**
     * Whether this tool can operate without Internet access.
     */
    val supportsOffline: Boolean

    /**
     * Whether the tool can access protected Z Vault data.
     *
     * True does NOT automatically grant access.
     * Guardian permission checks must still pass.
     */
    val requiresVaultAccess: Boolean

    /**
     * Whether the tool performs a consequential operation.
     *
     * Consequential tools require an explicit permission
     * decision before execution.
     */
    val consequential: Boolean

    /**
     * Determine whether this tool can handle a request.
     *
     * This should remain lightweight and must not perform
     * the actual operation.
     */
    fun canHandle(
        request: String
    ): Boolean

    /**
     * Execute the capability.
     *
     * The tool must:
     *
     * - respect Guardian security policy
     * - respect owner authority
     * - respect Z Vault boundaries
     * - never bypass Android security
     * - never expose protected credentials
     * - return a structured AtlasToolResult
     */
    fun execute(
        context: Context,
        request: String
    ): AtlasToolResult
}
