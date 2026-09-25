package com.azimi.guardian

import android.content.Context

object AtlasGuardianBridge {

    data class AtlasBridgeResult(
        val success: Boolean,
        val message: String,
        val plan: AtlasCore.AtlasPlan? = null,
        val status: String = "UNKNOWN"
    )

    /**
     * Commands that are explicitly recognized as safe Vault-control
     * requests.
     *
     * These commands do not request secrets or protected credential
     * material. They are routed to AtlasVaultTool before the general
     * AI credential classifier.
     *
     * This does NOT bypass Vault permissions.
     * AtlasVaultTool -> ZVaultService still performs the actual
     * authorization decision.
     */
    private fun isSafeVaultCommand(
        message: String
    ): Boolean {

        val text =
            message
                .trim()
                .lowercase()

        if (text.isBlank()) {
            return false
        }

        val safeCommands =
            setOf(
                "vault status",
                "vault diagnostics",
                "lock vault",
                "unlock vault",
                "z vault status",
                "z vault diagnostics",
                "lock z vault",
                "unlock z vault",

                "lock z memory",
                "unlock z memory",

                "lock z project",
                "unlock z project",

                "lock z recovery",
                "unlock z recovery",

                "lock z origin",
                "unlock z origin",

                "lock z sovereign",
                "unlock z sovereign"
            )

        return text in safeCommands
    }

    /**
     * Commands that are explicitly recognized as safe Atlas Memory
     * requests.
     *
     * These requests are routed directly to AtlasMemoryTool before
     * the general protected-credential classifier.
     *
     * AtlasMemoryTool still performs the real permission check:
     *
     * AtlasMemoryTool
     *      ↓
     * AtlasPermissionChecker
     *      ↓
     * Z Vault authorization
     *
     * "memory state" is treated as a safe alias for "memory status".
     */
    private fun isSafeMemoryCommand(
        message: String
    ): Boolean {

        val text =
            message
                .trim()
                .lowercase()

        if (text.isBlank()) {
            return false
        }

        if (
            text == "memory status" ||
            text == "memory state" ||
            text == "atlas memory status" ||
            text == "atlas memory state" ||
            text == "show memory" ||
            text == "view memory" ||
            text == "memory list" ||
            text == "atlas memory" ||
            text == "clear memory" ||
            text == "clear atlas memory" ||
            text == "delete memory"
        ) {
            return true
        }

        if (
            text.startsWith("remember ") &&
            text.length > "remember ".length
        ) {
            return true
        }

        if (
            text.startsWith("forget ") &&
            text.length > "forget ".length
        ) {
            return true
        }

        if (
            text.startsWith("save to memory ") &&
            text.length > "save to memory ".length
        ) {
            return true
        }

        if (
            text.startsWith("save this to memory ") &&
            text.length > "save this to memory ".length
        ) {
            return true
        }

        return false
    }

    /**
     * Normalize safe Memory aliases before sending them to
     * AtlasMemoryTool.
     *
     * This allows:
     *
     * "memory state"
     * "atlas memory state"
     *
     * to behave exactly like:
     *
     * "memory status"
     * "atlas memory status"
     */
    private fun normalizeMemoryCommand(
        message: String
    ): String {

        return when (
            message
                .trim()
                .lowercase()
        ) {

            "memory state" ->
                "memory status"

            "atlas memory state" ->
                "atlas memory status"

            else ->
                message
        }
    }

    /**
     * Execute a modular Atlas tool through the central registry.
     *
     * Tool execution remains behind the tool's own permission
     * boundary.
     *
     * This function currently handles the explicitly safe Vault
     * and Atlas Memory command families.
     */
    private fun executeRegisteredTool(
        context: Context,
        message: String,
        onResult: (AtlasBridgeResult) -> Unit
    ): Boolean {

        val appContext =
            context.applicationContext

        val vaultCommand =
            isSafeVaultCommand(message)

        val memoryCommand =
            isSafeMemoryCommand(message)

        if (
            !vaultCommand &&
            !memoryCommand
        ) {
            return false
        }

        val toolRequest =
            if (memoryCommand) {
                normalizeMemoryCommand(message)
            } else {
                message
            }

        val requiredToolId =
            if (memoryCommand) {
                "atlas_memory"
            } else {
                "z_vault"
            }

        val tool =
            AtlasToolRegistry
                .findTools(toolRequest)
                .firstOrNull {
                    it.id == requiredToolId
                }

        if (tool == null) {
            return false
        }

        val result =
            runCatching {

                tool.execute(
                    context = appContext,
                    request = toolRequest
                )

            }.getOrElse { error ->

                AtlasToolResult.error(
                    toolId = tool.id,
                    message =
                        if (memoryCommand) {
                            "Atlas Memory tool execution failed."
                        } else {
                            "Vault tool execution failed."
                        },
                    diagnostics =
                        "TOOL_EXCEPTION:${error::class.simpleName}"
                )
            }

        onResult(
            AtlasBridgeResult(
                success = result.success,
                message = result.message,
                status = result.status,
                plan = null
            )
        )

        return true
    }

    fun process(
        context: Context,
        message: String,
        history: List<AzimiAiClient.ChatMessage> = emptyList(),
        approvedMemory: List<AzimiAiClient.ChatMessage> = emptyList(),
        onResult: (AtlasBridgeResult) -> Unit
    ) {

        val appContext =
            context.applicationContext

        val cleanMessage =
            message.trim()

        if (!AtlasSession.isActive(appContext)) {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Atlas session is locked. Activate Atlas through Guardian before using Atlas.",
                    status = "LOCKED"
                )
            )

            return
        }

        if (cleanMessage.isBlank()) {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Atlas message cannot be empty.",
                    status = "INVALID"
                )
            )

            return
        }

        /*
         * ------------------------------------------------------------
         * MODULAR TOOL ROUTING
         * ------------------------------------------------------------
         *
         * Safe Vault and Atlas Memory commands are routed directly
         * to their registered tools.
         *
         * This happens before the general credential classifier so
         * harmless commands such as:
         *
         * "Vault status"
         * "memory status"
         * "memory state"
         * "show memory"
         *
         * are handled by the correct modular capability.
         *
         * IMPORTANT:
         *
         * Direct routing does NOT bypass security.
         *
         * Vault:
         *
         * AtlasVaultTool
         *      ↓
         * ZVaultService
         *      ↓
         * AtlasPermissionChecker
         *
         * Memory:
         *
         * AtlasMemoryTool
         *      ↓
         * AtlasPermissionChecker
         *      ↓
         * Z Vault authorization
         */
        if (
            executeRegisteredTool(
                context = appContext,
                message = cleanMessage,
                onResult = onResult
            )
        ) {
            return
        }

        /*
         * ------------------------------------------------------------
         * GENERAL PROTECTED-CREDENTIAL BOUNDARY
         * ------------------------------------------------------------
         *
         * All non-tool requests continue through the existing
         * protected credential check.
         */
        if (
            AzimiAuth.isProtectedCredential(
                cleanMessage
            )
        ) {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "This message contains protected credential material and was blocked by Guardian.",
                    status = "BLOCKED"
                )
            )

            return
        }

        val policy =
            GuardianStorage.getAIMemoryPolicy(
                appContext
            )

        if (
            policy != "SAFE_CONTEXT_ONLY"
        ) {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Atlas request blocked. Guardian AI memory policy is not SAFE_CONTEXT_ONLY.",
                    status = "POLICY_BLOCKED"
                )
            )

            return
        }

        val safeHistory =
            history
                .takeLast(12)
                .filter { item ->

                    val content =
                        item.content.trim()

                    val role =
                        item.role.trim()

                    (
                        role == "user" ||
                            role == "assistant"
                        ) &&
                        content.isNotBlank() &&
                        content.length <= 4_000 &&
                        !AzimiAuth.isProtectedCredential(
                            content
                        )
                }

        val safeMemory =
            approvedMemory
                .takeLast(50)
                .filter { item ->

                    val content =
                        item.content.trim()

                    val role =
                        item.role.trim()

                    (
                        role == "user" ||
                            role == "assistant" ||
                            role == "system"
                        ) &&
                        content.isNotBlank() &&
                        content.length <= 4_000 &&
                        !AzimiAuth.isProtectedCredential(
                            content
                        )
                }

        val coreResult =
            AtlasCore.process(
                context = appContext,
                request =
                    AtlasCore.AtlasRequest(
                        message = cleanMessage,
                        history = safeHistory
                    )
            )

        if (!coreResult.success) {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message = coreResult.message,
                    status = coreResult.status
                )
            )

            return
        }

        val plan =
            coreResult.plan

        if (plan == null) {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Atlas Core completed without producing an execution plan.",
                    status = "NO_PLAN"
                )
            )

            return
        }

        if (!plan.externalProviderRequired) {

            val localResponse =
                AtlasLocalEngine.process(
                    context = appContext,
                    request = cleanMessage
                )

            onResult(
                AtlasBridgeResult(
                    success = localResponse.success,
                    message = localResponse.reply,
                    plan = plan,
                    status =
                        if (localResponse.success) {
                            "LOCAL"
                        } else {
                            localResponse.status
                        }
                )
            )

            return
        }

        val provider =
            AtlasProviderRegistry
                .getPrimaryOnlineProvider(
                    appContext
                )

        if (provider == null) {

            val localResponse =
                AtlasLocalEngine.process(
                    context = appContext,
                    request = cleanMessage
                )

            onResult(
                AtlasBridgeResult(
                    success = localResponse.success,
                    message =
                        if (localResponse.success) {

                            buildString {

                                appendLine(
                                    localResponse.reply
                                )

                                appendLine()

                                appendLine(
                                    "ATLAS STATUS: External AI provider unavailable."
                                )

                                appendLine(
                                    "FALLBACK: Local Guardian intelligence."
                                )
                            }

                        } else {
                            localResponse.reply
                        },
                    plan = plan,
                    status =
                        if (localResponse.success) {
                            "ONLINE_UNAVAILABLE_LOCAL_FALLBACK"
                        } else {
                            "LOCAL_FALLBACK_FAILED"
                        }
                )
            )

            return
        }

        provider.execute(
            context = appContext,
            message = cleanMessage,
            history = safeHistory,
            memory = safeMemory
        ) { response ->

            if (response.success) {

                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message = response.reply,
                        plan = plan,
                        status = "ONLINE"
                    )
                )

            } else {

                val localResponse =
                    AtlasLocalEngine.process(
                        context = appContext,
                        request = cleanMessage
                    )

                onResult(
                    AtlasBridgeResult(
                        success = localResponse.success,
                        message =
                            if (localResponse.success) {

                                buildString {

                                    appendLine(
                                        localResponse.reply
                                    )

                                    appendLine()

                                    appendLine(
                                        "ATLAS STATUS: External AI provider failed."
                                    )

                                    appendLine(
                                        "FALLBACK: Local Guardian intelligence."
                                    )

                                    if (
                                        response.error.isNotBlank()
                                    ) {

                                        appendLine(
                                            "PROVIDER ERROR: ${response.error}"
                                        )
                                    }
                                }

                            } else {
                                localResponse.reply
                            },
                        plan = plan,
                        status =
                            if (localResponse.success) {
                                "ONLINE_FAILED_LOCAL_FALLBACK"
                            } else {
                                "ONLINE_FAILED_LOCAL_FALLBACK_ERROR"
                            }
                    )
                )
            }
        }
    }
}
