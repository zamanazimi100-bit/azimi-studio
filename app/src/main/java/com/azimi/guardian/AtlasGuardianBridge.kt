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
 * AtlasMemoryTool still performs the real permission check.
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
 * SECURITY ORDER:
 *
 * 1. Identify the registered tool.
 * 2. Evaluate the request through Z Shield.
 * 3. DENIED -> stop.
 * 4. REQUIRES_CONFIRMATION -> stop.
 * 5. ALLOWED -> execute the existing tool.
 *
 * Z Shield is therefore placed at the actual execution boundary,
 * not merely at capability discovery.
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
        onResult(
            AtlasBridgeResult(
                success = false,
                message =
                    "Atlas capability is not currently registered.",
                status = "TOOL_NOT_FOUND"
            )
        )

        return true
    }

    /*
     * ------------------------------------------------------------
     * Z SHIELD — EXECUTION GATE
     * ------------------------------------------------------------
     *
     * This is the final policy gate immediately before actual
     * AtlasTool execution.
     *
     * Z Shield checks:
     *
     * - Atlas permission
     * - authentication state
     * - Vault access
     * - owner authorization
     * - sovereign authorization
     * - consequential-operation confirmation
     * - fail-closed security behavior
     *
     * Z Shield does not grant permissions or unlock anything.
     */
    val shieldDecision =
        runCatching {

            ZShield.evaluate(
                context = appContext,
                tool = tool,
                request = toolRequest
            )

        }.getOrElse { error ->

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        "Z Shield could not evaluate this Atlas capability. Execution was blocked.",
                    status =
                        "SHIELD_SECURITY_FAILURE:${error::class.simpleName}"
                )
            )

            return true
        }

    when (shieldDecision.decision) {

        ZShield.Decision.DENIED -> {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        shieldDecision.message,
                    status =
                        "SHIELD_DENIED:${shieldDecision.reasonCode.name}"
                )
            )

            return true
        }

        ZShield.Decision.REQUIRES_CONFIRMATION -> {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        shieldDecision.message,
                    status =
                        "SHIELD_CONFIRMATION_REQUIRED"
                )
            )

            return true
        }

        ZShield.Decision.ALLOWED -> {
            // Continue to the existing tool execution boundary.
        }
    }

    /*
     * ------------------------------------------------------------
     * ACTUAL TOOL EXECUTION
     * ------------------------------------------------------------
     *
     * Reaching this point means Z Shield explicitly allowed the
     * capability under the current security state.
     */
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
     * Z Shield is now the final execution gate for these tools.
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
