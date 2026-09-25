package com.azimi.guardian

/**
 * AZIMI Atlas Tool Result
 *
 * Standard result returned by every Atlas Tool.
 *
 * Keeping one result format means Atlas Core does not need
 * custom handling for every individual capability.
 */
data class AtlasToolResult(

    /**
     * Whether the requested operation completed successfully.
     */
    val success: Boolean,

    /**
     * Safe user-facing response.
     *
     * This must never contain passwords, API keys,
     * recovery codes, private credentials, or other
     * protected secret material.
     */
    val message: String,

    /**
     * Stable status describing the result.
     *
     * Examples:
     * SUCCESS
     * DENIED
     * SECURITY_BLOCK
     * PERMISSION_REQUIRED
     * VAULT_LOCKED
     * UNAVAILABLE
     * ERROR
     */
    val status: String = "UNKNOWN",

    /**
     * Tool that produced this result.
     */
    val toolId: String = "",

    /**
     * Optional diagnostic information.
     *
     * Diagnostics must remain safe and must never contain
     * protected credential material.
     */
    val diagnostics: String = "",

    /**
     * Whether the result came from a local/offline path.
     */
    val offline: Boolean = false,

    /**
     * Whether the operation was blocked by Guardian policy.
     */
    val blocked: Boolean = false,

    /**
     * Whether the caller must obtain additional permission
     * before the operation can continue.
     */
    val permissionRequired: Boolean = false
) {

    companion object {

        fun success(
            toolId: String,
            message: String,
            diagnostics: String = "",
            offline: Boolean = false
        ): AtlasToolResult {

            return AtlasToolResult(
                success = true,
                message = message,
                status = "SUCCESS",
                toolId = toolId,
                diagnostics = diagnostics,
                offline = offline
            )
        }

        fun blocked(
            toolId: String,
            message: String,
            status: String = "SECURITY_BLOCK",
            diagnostics: String = ""
        ): AtlasToolResult {

            return AtlasToolResult(
                success = false,
                message = message,
                status = status,
                toolId = toolId,
                diagnostics = diagnostics,
                blocked = true
            )
        }

        fun permissionRequired(
            toolId: String,
            message: String,
            diagnostics: String = ""
        ): AtlasToolResult {

            return AtlasToolResult(
                success = false,
                message = message,
                status = "PERMISSION_REQUIRED",
                toolId = toolId,
                diagnostics = diagnostics,
                permissionRequired = true
            )
        }

        fun error(
            toolId: String,
            message: String,
            diagnostics: String = ""
        ): AtlasToolResult {

            return AtlasToolResult(
                success = false,
                message = message,
                status = "ERROR",
                toolId = toolId,
                diagnostics = diagnostics
            )
        }
    }
}
