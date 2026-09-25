package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Z Continuity Events
 *
 * Central event gateway for automatic project continuity.
 *
 * Architecture:
 *
 *     Guardian subsystem
 *            │
 *            ▼
 *     ZContinuityEvents
 *            │
 *            ▼
 *     ZContinuityStorage
 *            │
 *       ┌────┴────┐
 *       ▼         ▼
 *   ZContinuity  ZHistory
 *      Index
 *
 * Rules:
 * - Event recording must never crash the originating subsystem.
 * - A failed continuity write must never become a larger failure.
 * - Secrets must never be intentionally recorded.
 * - This layer does not own the underlying data.
 * - ZContinuityStorage remains the source of truth.
 */
object ZContinuityEvents {

    private const val COMPONENT =
        "Z CONTINUITY EVENTS"

    private const val MAX_TEXT =
        100_000

    /**
     * Generic project event.
     */
    fun project(
        context: Context,
        title: String,
        content: String,
        category: String = "PROJECT"
    ): String? {

        return safeRecord(
            context = context,
            operation = "project event"
        ) {

            ZContinuityStorage.recordProjectEvent(
                context = context,
                category = cleanCategory(category),
                title = cleanTitle(title),
                content = cleanContent(content)
            )
        }
    }

    /**
     * Record a build event.
     *
     * ZContinuityStorage specialized event methods use
     * the common (context, title, content) contract.
     */
    fun build(
        context: Context,
        buildNumber: String,
        status: String,
        details: String = ""
    ): String? {

        val safeBuildNumber =
            cleanText(buildNumber)

        val safeStatus =
            cleanText(status)

        val title =
            "Guardian Build $safeBuildNumber"

        val content =
            buildString {
                append("Build: ")
                append(safeBuildNumber)
                append("\nStatus: ")
                append(safeStatus)

                if (details.isNotBlank()) {
                    append("\nDetails: ")
                    append(details)
                }
            }

        return safeRecord(
            context = context,
            operation = "build event"
        ) {

            ZContinuityStorage.recordBuild(
                context = context,
                title = cleanTitle(title),
                content = cleanContent(content)
            )
        }
    }

    /**
     * Record a failure.
     *
     * The failure itself remains isolated from the
     * continuity system.
     *
     * The complete failure information is placed into
     * the content field because ZContinuityStorage uses
     * the common (context, title, content) contract.
     */
    fun failure(
        context: Context,
        component: String,
        file: String,
        function: String,
        stage: String,
        message: String
    ): String? {

        val safeComponent =
            cleanText(component)

        val safeFile =
            cleanText(file)

        val safeFunction =
            cleanText(function)

        val safeStage =
            cleanText(stage)

        val safeMessage =
            cleanContent(message)

        val title =
            "$safeComponent failure at $safeStage"

        val content =
            buildString {
                append("Component: ")
                append(safeComponent)

                append("\nFile: ")
                append(safeFile)

                append("\nFunction: ")
                append(safeFunction)

                append("\nStage: ")
                append(safeStage)

                append("\nMessage: ")
                append(safeMessage)
            }

        return safeRecord(
            context = context,
            operation = "failure event"
        ) {

            ZContinuityStorage.recordFailure(
                context = context,
                title = cleanTitle(title),
                content = cleanContent(content)
            )
        }
    }

    /**
     * Record an engineering decision.
     */
    fun decision(
        context: Context,
        title: String,
        decision: String,
        reason: String = ""
    ): String? {

        val content =
            buildString {
                append("Decision: ")
                append(decision)

                if (reason.isNotBlank()) {
                    append("\nReason: ")
                    append(reason)
                }
            }

        return safeRecord(
            context = context,
            operation = "decision event"
        ) {

            ZContinuityStorage.recordDecision(
                context = context,
                title = cleanTitle(title),
                content = cleanContent(content)
            )
        }
    }

    /**
     * Record a diagnostic event.
     */
    fun diagnostic(
        context: Context,
        title: String,
        details: String
    ): String? {

        return safeRecord(
            context = context,
            operation = "diagnostic event"
        ) {

            ZContinuityStorage.recordDiagnostic(
                context = context,
                title = cleanTitle(title),
                content = cleanContent(details)
            )
        }
    }

    /**
     * Record a recovery point.
     */
    fun recoveryPoint(
        context: Context,
        title: String,
        details: String = ""
    ): String? {

        return safeRecord(
            context = context,
            operation = "recovery point"
        ) {

            ZContinuityStorage.recordRecoveryPoint(
                context = context,
                title = cleanTitle(title),
                content = cleanContent(details)
            )
        }
    }

    /**
     * Record a configuration change.
     */
    fun configuration(
        context: Context,
        title: String,
        details: String
    ): String? {

        return safeRecord(
            context = context,
            operation = "configuration event"
        ) {

            ZContinuityStorage.recordConfiguration(
                context = context,
                title = cleanTitle(title),
                content = cleanContent(details)
            )
        }
    }

    /**
     * Record a tool event.
     */
    fun tool(
        context: Context,
        toolName: String,
        action: String,
        details: String = ""
    ): String? {

        val title =
            "$toolName: $action"

        return safeRecord(
            context = context,
            operation = "tool event"
        ) {

            ZContinuityStorage.recordToolEvent(
                context = context,
                title = cleanTitle(title),
                content = cleanContent(details)
            )
        }
    }

    /**
     * Record approved Atlas project context.
     *
     * This is deliberately named "context" rather than
     * "memory" because this layer is for project continuity.
     */
    fun atlasContext(
        context: Context,
        title: String,
        approvedContext: String
    ): String? {

        return safeRecord(
            context = context,
            operation = "Atlas context event"
        ) {

            ZContinuityStorage.recordAtlasContext(
                context = context,
                title = cleanTitle(title),
                content = cleanContent(approvedContext)
            )
        }
    }

    /**
     * Safe wrapper around all continuity writes.
     *
     * If anything fails here:
     *
     *     originating feature → CONTINUES
     *     continuity write    → FAILED SAFELY
     *
     * This is a critical failure-isolation boundary.
     */
    private fun safeRecord(
        context: Context,
        operation: String,
        block: () -> String?
    ): String? {

        return runCatching {

            block()

        }.getOrElse { throwable ->

            safeDiagnostic(
                context = context,
                operation = operation,
                throwable = throwable
            )

            null
        }
    }

    /**
     * Failure handling must itself be failure-safe.
     *
     * We intentionally do not call ZFailureLocator here because
     * ZFailureLocator itself uses continuity events.
     *
     * Otherwise we could create:
     *
     *     failure
     *       ↓
     *     event failure
     *       ↓
     *     failure locator
     *       ↓
     *     event failure
     *       ↓
     *     ...
     *
     * Instead, this lowest-level fallback writes only to the
     * existing GuardianStorage error channel.
     */
    private fun safeDiagnostic(
        context: Context,
        operation: String,
        throwable: Throwable
    ) {

        runCatching {

            GuardianStorage.recordError(
                context,
                (
                    "$COMPONENT: $operation failed: " +
                        (
                            throwable.message
                                ?: throwable.javaClass.simpleName
                        )
                    ).take(500)
            )
        }
    }

    /**
     * Normalize categories.
     */
    private fun cleanCategory(
        value: String
    ): String {

        return value
            .trim()
            .uppercase()
            .take(100)
            .ifBlank {
                "PROJECT"
            }
    }

    /**
     * Normalize short titles.
     */
    private fun cleanTitle(
        value: String
    ): String {

        return cleanText(value)
            .take(200)
            .ifBlank {
                "Untitled Event"
            }
    }

    /**
     * Normalize general metadata.
     */
    private fun cleanText(
        value: String
    ): String {

        return value
            .trim()
            .take(MAX_TEXT)
    }

    /**
     * Normalize event content.
     *
     * The authoritative ZContinuityStorage credential gate
     * remains the final security boundary.
     */
    private fun cleanContent(
        value: String
    ): String {

        return value
            .trim()
            .take(MAX_TEXT)
    }
}
