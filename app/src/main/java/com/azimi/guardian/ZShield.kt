package com.azimi.guardian

import android.content.Context

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

    data class ShieldDecision(
        val decision: Decision,
        val reasonCode: ReasonCode,
        val message: String,
        val requiresConfirmation: Boolean = false
    )

    private enum class VaultOperation {
        READ_ONLY_STATUS,
        UNLOCK,
        LOCK,
        COMPARTMENT_UNLOCK,
        COMPARTMENT_LOCK,
        OTHER
    }

    fun evaluate(
        context: Context,
        tool: AtlasTool,
        request: String
    ): ShieldDecision {

        val appContext = context.applicationContext
        val text = request.trim().lowercase()

        if (text.isBlank()) {
            return deny(
                ReasonCode.INVALID_REQUEST,
                "Atlas request is empty."
            )
        }

        if (tool.id.isBlank()) {
            return deny(
                ReasonCode.TOOL_NOT_FOUND,
                "Atlas tool identity is invalid."
            )
        }

        if (tool.id == "z_vault") {

            val operation = classifyVaultOperation(text)

            val permissionToCheck: AtlasPermission
            val requiresVaultRoot: Boolean
            val consequential: Boolean

            when (operation) {

                VaultOperation.READ_ONLY_STATUS -> {
                    permissionToCheck = AtlasPermission.AUTHENTICATED
                    requiresVaultRoot = false
                    consequential = false
                }

                VaultOperation.UNLOCK -> {
                    permissionToCheck = AtlasPermission.VAULT
                    requiresVaultRoot = true
                    consequential = true
                }

                VaultOperation.LOCK -> {
                    permissionToCheck = AtlasPermission.VAULT
                    requiresVaultRoot = true
                    consequential = true
                }

                VaultOperation.COMPARTMENT_UNLOCK -> {
                    permissionToCheck = AtlasPermission.VAULT
                    requiresVaultRoot = true
                    consequential = true
                }

                VaultOperation.COMPARTMENT_LOCK -> {
                    permissionToCheck = AtlasPermission.VAULT
                    requiresVaultRoot = true
                    consequential = true
                }

                VaultOperation.OTHER -> {
                    permissionToCheck = tool.permission
                    requiresVaultRoot = tool.requiresVaultAccess
                    consequential = tool.consequential
                }
            }

            val permissionDecision =
                AtlasPermissionChecker.evaluate(
                    appContext,
                    permissionToCheck
                )

            if (!permissionDecision.allowed) {
                return when (permissionToCheck) {

                    AtlasPermission.AUTHENTICATED ->
                        deny(
                            ReasonCode.AUTHENTICATION_REQUIRED,
                            permissionDecision.message
                        )

                    AtlasPermission.VAULT ->
                        deny(
                            ReasonCode.VAULT_ACCESS_REQUIRED,
                            permissionDecision.message
                        )

                    AtlasPermission.OWNER ->
                        deny(
                            ReasonCode.OWNER_AUTHORIZATION_REQUIRED,
                            permissionDecision.message
                        )

                    AtlasPermission.SOVEREIGN ->
                        deny(
                            ReasonCode.SOVEREIGN_AUTHORIZATION_REQUIRED,
                            permissionDecision.message
                        )

                    AtlasPermission.PUBLIC ->
                        deny(
                            ReasonCode.PERMISSION_DENIED,
                            permissionDecision.message
                        )
                }
            }

            if (requiresVaultRoot) {

                val rootAccess =
                    ZVaultService.canAccessRoot(appContext)

                if (!rootAccess) {
                    return deny(
                        ReasonCode.VAULT_ACCESS_DENIED,
                        "Z Vault root access is not currently available."
                    )
                }
            }

            if (consequential) {
                return ShieldDecision(
                    decision = Decision.REQUIRES_CONFIRMATION,
                    reasonCode =
                        ReasonCode.CONSEQUENT_OPERATION_REQUIRES_CONFIRMATION,
                    message =
                        "This Z Vault operation requires explicit owner confirmation.",
                    requiresConfirmation = true
                )
            }

            return allow(
                "Z Shield authorized this read-only Vault request."
            )
        }

        val permissionDecision =
            AtlasPermissionChecker.evaluate(
                appContext,
                tool.permission
            )

        if (!permissionDecision.allowed) {

            return when (tool.permission) {

                AtlasPermission.AUTHENTICATED ->
                    deny(
                        ReasonCode.AUTHENTICATION_REQUIRED,
                        permissionDecision.message
                    )

                AtlasPermission.VAULT ->
                    deny(
                        ReasonCode.VAULT_ACCESS_REQUIRED,
                        permissionDecision.message
                    )

                AtlasPermission.OWNER ->
                    deny(
                        ReasonCode.OWNER_AUTHORIZATION_REQUIRED,
                        permissionDecision.message
                    )

                AtlasPermission.SOVEREIGN ->
                    deny(
                        ReasonCode.SOVEREIGN_AUTHORIZATION_REQUIRED,
                        permissionDecision.message
                    )

                AtlasPermission.PUBLIC ->
                    deny(
                        ReasonCode.PERMISSION_DENIED,
                        permissionDecision.message
                    )
            }
        }

        if (tool.requiresVaultAccess) {

            val rootAccess =
                ZVaultService.canAccessRoot(appContext)

            if (!rootAccess) {
                return deny(
                    ReasonCode.VAULT_ACCESS_DENIED,
                    "Z Vault root access is not currently available."
                )
            }
        }

        if (tool.consequential) {
            return ShieldDecision(
                decision = Decision.REQUIRES_CONFIRMATION,
                reasonCode =
                    ReasonCode.CONSEQUENT_OPERATION_REQUIRES_CONFIRMATION,
                message =
                    "This operation requires explicit owner confirmation.",
                requiresConfirmation = true
            )
        }

        return allow(
            "Z Shield authorized this operation."
        )
    }

    private fun classifyVaultOperation(
        text: String
    ): VaultOperation {

        if (
            text.contains("status") ||
            text.contains("state") ||
            text.contains("diagnostic")
        ) {
            return VaultOperation.READ_ONLY_STATUS
        }

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
            !text.contains("unlock") &&
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

        if (
            text.contains("unlock vault") ||
            text == "unlock" ||
            text.contains("open vault")
        ) {
            return VaultOperation.UNLOCK
        }

        if (
            text.contains("lock vault") ||
            text == "lock" ||
            text.contains("close vault")
        ) {
            return VaultOperation.LOCK
        }

        return VaultOperation.OTHER
    }

    private fun allow(
        message: String
    ): ShieldDecision {
        return ShieldDecision(
            decision = Decision.ALLOWED,
            reasonCode = ReasonCode.AUTHORIZED,
            message = message,
            requiresConfirmation = false
        )
    }

    private fun deny(
        reasonCode: ReasonCode,
        message: String
    ): ShieldDecision {
        return ShieldDecision(
            decision = Decision.DENIED,
            reasonCode = reasonCode,
            message = message,
            requiresConfirmation = false
        )
    }

    fun policy(): String {
        return """
            Z SHIELD
            VERSION=2.1
            STATUS=ACTIVE
            CONTROL_LAYER=ENFORCED
            OPERATION_POLICY=OPERATION_AWARE
            READ_ONLY_STATUS=ALLOWED_WHEN_AUTHENTICATED
            VAULT_OPERATIONS=PROTECTED
            CONSEQUENT_OPERATIONS=CONFIRMATION_REQUIRED
            AUTOMATIC_PRIVILEGE_ELEVATION=DENIED
            AUTOMATIC_VAULT_UNLOCK=DENIED
            FAILURE_BEHAVIOR=FAIL_CLOSED
        """.trimIndent()
    }

    fun diagnostics(
        context: Context
    ): String {

        val appContext = context.applicationContext

        val authenticated =
            ZSecuritySession.isAuthenticated(appContext)

        val vaultRootAccess =
            ZVaultService.canAccessRoot(appContext)

        val atlasSleeping =
            ZSecuritySession.isAtlasSleeping(appContext)

        val registeredTools =
            AtlasToolRegistry.count()

        return """
            Z SHIELD
            STATUS=ACTIVE
            VERSION=2.1
            AUTHENTICATED=$authenticated
            VAULT_ROOT_ACCESS=$vaultRootAccess
            ATLAS_SLEEPING=$atlasSleeping
            REGISTERED_TOOLS=$registeredTools
            OPERATION_POLICY=OPERATION_AWARE
            READ_ONLY_STATUS=ALLOWED_WHEN_AUTHENTICATED
            CONSEQUENT_OPERATIONS=CONFIRMATION_REQUIRED
            AUTOMATIC_PRIVILEGE_ELEVATION=DENIED
            AUTOMATIC_VAULT_UNLOCK=DENIED
            FAILURE_BEHAVIOR=FAIL_CLOSED
        """.trimIndent()
    }
}
