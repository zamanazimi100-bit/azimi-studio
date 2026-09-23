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
 * Local capability OR replaceable provider adapter
 *
 * Important:
 * - Guardian Atlas authentication is local.
 * - Email/Supabase/website identity is NOT required.
 * - Provider authentication is separate from Guardian owner authority.
 * - Protected credentials are blocked before processing.
 * - Z Vault memory is never automatically sent to an AI provider.
 * - This bridge does not bypass Android permissions or Guardian security.
 */
object AtlasGuardianBridge {

    data class AtlasBridgeResult(
        val success: Boolean,
        val message: String,
        val plan: AtlasCore.AtlasPlan? = null,
        val status: String = "UNKNOWN"
    )

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

        // ---------------------------------------------------------
        // 1. LOCAL GUARDIAN ATLAS SESSION BOUNDARY
        // ---------------------------------------------------------
        //
        // Atlas uses the local Guardian authorization/session.
        // No email, website login, Supabase session, or access
        // token is required here.
        //
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

        // ---------------------------------------------------------
        // 2. REQUEST VALIDATION
        // ---------------------------------------------------------

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

        // ---------------------------------------------------------
        // 3. PROTECTED CREDENTIAL BOUNDARY
        // ---------------------------------------------------------

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

        // ---------------------------------------------------------
        // 4. GUARDIAN AI MEMORY POLICY
        // ---------------------------------------------------------

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

        // ---------------------------------------------------------
        // 5. SANITIZE CONVERSATION HISTORY
        // ---------------------------------------------------------

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

        // ---------------------------------------------------------
        // 6. SANITIZE EXPLICITLY APPROVED MEMORY
        // ---------------------------------------------------------
        //
        // Only explicitly supplied approved context is considered.
        // Protected credentials are always excluded.
        //
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

        // ---------------------------------------------------------
        // 7. ASK ATLAS CORE FOR THE PLAN
        // ---------------------------------------------------------

        val coreResult =
            AtlasCore.process(
                context = appContext,
                request =
                    AtlasCore.AtlasRequest(
                        message =
                            cleanMessage,
                        history =
                            safeHistory
                    )
            )

        if (!coreResult.success) {

            onResult(
                AtlasBridgeResult(
                    success = false,
                    message =
                        coreResult.message,
                    status =
                        coreResult.status
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
                    status =
                        "NO_PLAN"
                )
            )

            return
        }

        // ---------------------------------------------------------
        // 8. DETERMINE THE INTELLIGENCE PATH
        // ---------------------------------------------------------
        //
        // AtlasPlan is the authoritative current API.
        //
        // There is no AtlasMode.LOCAL/ONLINE/HYBRID dependency here.
        //
        if (!plan.externalProviderRequired) {

            // -----------------------------------------------------
            // LOCAL CAPABILITY
            // -----------------------------------------------------

            val localResponse =
                AtlasLocalEngine.process(
                    context =
                        appContext,
                    request =
                        cleanMessage
                )

            onResult(
                AtlasBridgeResult(
                    success =
                        localResponse.success,
                    message =
                        localResponse.reply,
                    plan =
                        plan,
                    status =
                        if (
                            localResponse.success
                        ) {
                            "LOCAL"
                        } else {
                            localResponse.status
                        }
                )
            )

            return
        }

        // ---------------------------------------------------------
        // 9. EXTERNAL AI IS REQUIRED
        // ---------------------------------------------------------
        //
        // External AI is an adapter only.
        //
        // It does NOT become Guardian's identity authority.
        //
        val provider =
            AtlasProviderRegistry
                .getPrimaryOnlineProvider()

        if (provider == null) {

            // -----------------------------------------------------
            // 10. PROVIDER UNAVAILABLE → LOCAL FALLBACK
            // -----------------------------------------------------

            val localResponse =
                AtlasLocalEngine.process(
                    context =
                        appContext,
                    request =
                        cleanMessage
                )

            onResult(
                AtlasBridgeResult(
                    success =
                        localResponse.success,
                    message =
                        if (
                            localResponse.success
                        ) {
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
                    plan =
                        plan,
                    status =
                        if (
                            localResponse.success
                        ) {
                            "ONLINE_UNAVAILABLE_LOCAL_FALLBACK"
                        } else {
                            "LOCAL_FALLBACK_FAILED"
                        }
                )
            )

            return
        }

        // ---------------------------------------------------------
        // 11. EXECUTE THROUGH THE REPLACEABLE PROVIDER
        // ---------------------------------------------------------
        //
        // Only sanitized request/history/approved context is passed.
        //
        // The provider receives no Guardian authentication token.
        // The provider does not control Atlas authorization.
        //
        provider.execute(
            context =
                appContext,
            message =
                cleanMessage,
            history =
                safeHistory,
            memory =
                safeMemory
        ) { response ->

            if (response.success) {

                onResult(
                    AtlasBridgeResult(
                        success = true,
                        message =
                            response.reply,
                        plan =
                            plan,
                        status =
                            "ONLINE"
                    )
                )

            } else {

                // -------------------------------------------------
                // 12. ONLINE PROVIDER FAILED → LOCAL FALLBACK
                // -------------------------------------------------

                val localResponse =
                    AtlasLocalEngine.process(
                        context =
                            appContext,
                        request =
                            cleanMessage
                    )

                onResult(
                    AtlasBridgeResult(
                        success =
                            localResponse.success,
                        message =
                            if (
                                localResponse.success
                            ) {
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
                        plan =
                            plan,
                        status =
                            if (
                                localResponse.success
                            ) {
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
