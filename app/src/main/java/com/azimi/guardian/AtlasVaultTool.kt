package com.azimi.guardian

import android.content.Context

/**
 * AtlasVaultTool
 *
 * Modular Atlas tool for controlled Z Vault operations.
 *
 * Security boundary:
 *
 *     Atlas
 *       ↓
 *   AtlasVaultTool
 *       ↓
 *   ZVaultService
 *       ↓
 *    VaultCrypto
 *       ↓
 * Android Keystore
 *
 * This tool never receives or exposes cryptographic keys.
 *
 * It also does not automatically unlock protected compartments.
 * Explicit authorization and compartment policy remain enforced
 * by ZVaultService.
 */
class AtlasVaultTool : AtlasTool {

    override val id: String =
        "z_vault"

    override val name: String =
        "Z Vault"

    override val description: String =
        "Controlled access to AZIMI Z Vault compartments and Vault status."

    override val permission: AtlasPermission =
        AtlasPermission.VAULT

    override val supportsOffline: Boolean =
        true

    override val requiresVaultAccess: Boolean =
        true

    override val consequential: Boolean =
        true

    override fun canHandle(
        request: String
    ): Boolean {

        val text =
            request.trim().lowercase()

        if (text.isBlank()) {
            return false
        }

        val keywords =
            listOf(
                "vault",
                "z vault",
                "z memory",
                "z project",
                "z recovery",
                "z origin",
                "z sovereign",
                "lock vault",
                "unlock vault",
                "vault status",
                "vault diagnostics"
            )

        return keywords.any { keyword ->
            text.contains(keyword)
        }
    }

    override fun execute(
        context: Context,
        request: String
    ): AtlasToolResult {

        val appContext =
            context.applicationContext

        val text =
            request.trim().lowercase()

        if (text.isBlank()) {
            return AtlasToolResult.error(
                toolId = id,
                message = "Vault request is empty.",
                diagnostics = "REQUEST_EMPTY"
            )
        }

        /*
         * Status / diagnostics are deliberately available
         * only after Vault-level authentication.
         */
        if (
            text.contains("status") ||
            text.contains("diagnostic")
        ) {

            if (!ZVaultService.canAccessRoot(appContext)) {

                return AtlasToolResult.blocked(
                    toolId = id,
                    message = "Z Vault is locked.",
                    status = "VAULT_LOCKED",
                    diagnostics = "ROOT_ACCESS_REQUIRED"
                )
            }

            return AtlasToolResult.success(
                toolId = id,
                message = ZVaultService.diagnostics(appContext),
                diagnostics = "VAULT_DIAGNOSTICS",
                offline = true
            )
        }

        /*
         * Explicit root unlock request.
         *
         * This does not grant access to protected compartments.
         */
        if (
            text.contains("unlock vault")
        ) {

            val unlocked =
                ZVaultService.unlockRoot(appContext)

            return if (unlocked) {

                AtlasToolResult.success(
                    toolId = id,
                    message = "Z Vault root unlocked. Protected compartments remain independently locked.",
                    diagnostics = "ROOT_UNLOCKED",
                    offline = true
                )

            } else {

                AtlasToolResult.blocked(
                    toolId = id,
                    message = "Z Vault root unlock was denied.",
                    status = "VAULT_UNLOCK_DENIED",
                    diagnostics = "AUTHENTICATION_REQUIRED"
                )
            }
        }

        /*
         * Explicit root lock.
         *
         * All compartments are closed by ZVaultService.
         * Atlas itself remains independent.
         */
        if (
            text.contains("lock vault")
        ) {

            ZVaultService.lockRoot(appContext)

            return AtlasToolResult.success(
                toolId = id,
                message = "Z Vault locked. All Vault compartments are now closed. Atlas session remains independent.",
                diagnostics = "ROOT_LOCKED",
                offline = true
            )
        }

        /*
         * Determine the requested compartment.
         */
        val compartment =
            when {

                text.contains("z sovereign") ||
                    text.contains("sovereign") ->
                    ZVaultService.Compartment.Z_SOVEREIGN

                text.contains("z origin") ||
                    text.contains("origin") ->
                    ZVaultService.Compartment.Z_ORIGIN

                text.contains("z recovery") ||
                    text.contains("recovery") ->
                    ZVaultService.Compartment.Z_RECOVERY

                text.contains("z project") ||
                    text.contains("project") ->
                    ZVaultService.Compartment.Z_PROJECT

                text.contains("z memory") ||
                    text.contains("memory") ->
                    ZVaultService.Compartment.Z_MEMORY

                else ->
                    null
            }

        if (compartment == null) {

            return AtlasToolResult.success(
                toolId = id,
                message = ZVaultService.diagnostics(appContext),
                diagnostics = "VAULT_STATUS",
                offline = true
            )
        }

        /*
         * Explicit compartment unlock.
         *
         * Permission is evaluated by ZVaultService.
         */
        if (
            text.contains("unlock")
        ) {

            val unlocked =
                ZVaultService.unlockCompartment(
                    context = appContext,
                    compartment = compartment
                )

            return if (unlocked) {

                AtlasToolResult.success(
                    toolId = id,
                    message = "${compartment.displayName} unlocked.",
                    diagnostics = "COMPARTMENT_UNLOCKED:${compartment.id}",
                    offline = true
                )

            } else {

                AtlasToolResult.blocked(
                    toolId = id,
                    message = "${compartment.displayName} unlock denied.",
                    status = "COMPARTMENT_ACCESS_DENIED",
                    diagnostics =
                        "REQUIRED_PERMISSION=${compartment.requiredPermission}"
                )
            }
        }

        /*
         * Explicit compartment lock.
         */
        if (
            text.contains("lock")
        ) {

            ZVaultService.lockCompartment(
                context = appContext,
                compartment = compartment
            )

            return AtlasToolResult.success(
                toolId = id,
                message = "${compartment.displayName} locked.",
                diagnostics = "COMPARTMENT_LOCKED:${compartment.id}",
                offline = true
            )
        }

        /*
         * A request mentioning a protected compartment without
         * an explicit lock/unlock command must not accidentally
         * expose data.
         */
        if (
            ZVaultService.canAccess(
                context = appContext,
                compartment = compartment
            )
        ) {

            return AtlasToolResult.success(
                toolId = id,
                message = "${compartment.displayName} is currently accessible to the authorized session.",
                diagnostics = "COMPARTMENT_ACCESS_GRANTED:${compartment.id}",
                offline = true
            )
        }

        return AtlasToolResult.blocked(
            toolId = id,
            message = "${compartment.displayName} is locked or not authorized.",
            status = "COMPARTMENT_LOCKED",
            diagnostics =
                "ACCESS_REQUIRED:${compartment.requiredPermission}"
        )
    }
}
