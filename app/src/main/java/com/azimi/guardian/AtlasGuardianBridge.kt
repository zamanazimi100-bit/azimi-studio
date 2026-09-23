package com.azimi.guardian

import android.content.Context

/**
 * Authoritative Guardian → Atlas bridge.
 *
 * Flow:
 *
 * Guardian
 *     ↓
 * AtlasGuardianBridge
 *     ↓
 * AtlasCore
 *     ↓
 * AtlasRouter
 *     ↓
 * AtlasProvider
 *
 * This bridge does not require Supabase authentication.
 * Cloud identity remains optional and separate from the
 * protected Guardian Atlas session.
 */
object AtlasGuardianBridge {

    data class AtlasBridgeResult(
        val success: Boolean,
        val message: String,
        val plan: AtlasMode? = null,
        val status: String = "UNKNOWN"
    )

    fun process(
        context: Context,
        message: String,
        history: List<AzimiAiClient.ChatMessage> = emptyList(),
        approvedMemory: List<AzimiAiClient.ChatMessage> = emptyList(),
        onResult: (AtlasBridgeResult) -> Unit
    ) {
        val appContext = context.applicationContext
        val cleanMessage = message.trim()

        // 1. Guardian Atlas session boundary.
        if (!AtlasSession.isActive(appContext)) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message = "Atlas session is locked. Activate Atlas through Guardian before using AI.",
                    status = "LOCKED"
                )
            )
            return
        }

        // 2. Protected credential boundary.
        if (cleanMessage.isBlank()) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message = "Atlas message cannot be empty.",
                    status = "INVALID"
                )
            )
            return
        }

        if (AzimiAuth.isProtectedCredential(cleanMessage)) {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message = "This message contains protected credential material and was blocked by Guardian.",
                    status = "BLOCKED"
                )
            )
            return
        }

        // 3. Guardian AI memory policy boundary.
        val policy = GuardianStorage.getAIMemoryPolicy(appContext)

        if (policy != "SAFE_CONTEXT_ONLY") {
            onResult(
                AtlasBridgeResult(
                    success = false,
                    message = "Atlas request blocked. Guardian AI memory policy is not SAFE_CONTEXT_ONLY.",
                    status = "POLICY_BLOCKED"
                )
            )
            return
        }

        // 4. Sanitize conversation history.
        val safeHistory = history
            .takeLast(12)
            .filter { item ->
                val content = item.content.trim()
                val role = item.role.trim()

                (role == "user" || role == "assistant") &&
                    content.isNotBlank() &&
                    content.length <= 4_000 &&
                    !AzimiAuth.isProtectedCredential(content)
            }

        // 5. Sanitize explicitly approved memory.
        val safeMemory = approvedMemory
            .takeLast(50)
            .filter { item ->
                val content = item.content.trim()
                val role = item.role.trim()

                (role == "user" ||
                    role == "assistant" ||
                    role == "system") &&
                    content.isNotBlank() &&
                    content.length <= 4_000 &&
                    !AzimiAuth.isProtectedCredential(content)
            }

        // 6. Ask Atlas Core for the execution plan.
        val plan = AtlasCore.process(
            context = appContext,
            message = cleanMessage,
            history = safeHistory,
            memory = safeMemory
        )

        when (plan.mode) {

            AtlasMode.LOCAL -> {
                val response = AtlasLocalEngine.process(
                    context = appContext,
                    message = cleanMessage,
                    history = safeHistory,
                    memory = safeMemory
                )

                onResult(
                    AtlasBridgeResult(
                        success = response.success,
                        message = response.message,
                        plan = AtlasMode.LOCAL,
                        status = if (response.success) {
                            "LOCAL"
                        } else {
                            "LOCAL_ERROR"
                        }
                    )
                )
            }

            AtlasMode.ONLINE,
            AtlasMode.HYBRID -> {

                val provider =
                    AtlasProviderRegistry.getPrimaryOnlineProvider()

                if (provider == null) {
                    val localResponse = AtlasLocalEngine.process(
                        context = appContext,
                        message = cleanMessage,
                        history = safeHistory,
                        memory = safeMemory
                    )

                    onResult(
                        AtlasBridgeResult(
                            success = localResponse.success,
                            message = localResponse.message,
                            plan = AtlasMode.LOCAL,
                            status = "ONLINE_UNAVAILABLE_LOCAL_FALLBACK"
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
                                message = response.message,
                                plan = plan.mode,
                                status = "ONLINE"
                            )
                        )
                    } else {

                        // Online provider failed → protected local fallback.
                        val localResponse = AtlasLocalEngine.process(
                            context = appContext,
                            message = cleanMessage,
                            history = safeHistory,
                            memory = safeMemory
                        )

                        onResult(
                            AtlasBridgeResult(
                                success = localResponse.success,
                                message = localResponse.message,
                                plan = AtlasMode.LOCAL,
                                status = "ONLINE_FAILED_LOCAL_FALLBACK"
                            )
                        )
                    }
                }
            }
        }
    }
}
