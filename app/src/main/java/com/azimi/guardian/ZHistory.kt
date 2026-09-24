package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Z History
 *
 * Read/query layer above ZContinuityIndex.
 *
 * Architecture:
 *
 *     ZContinuityStorage
 *              │
 *              ▼
 *     ZContinuityIndex
 *              │
 *              ▼
 *           ZHistory
 *
 * ZHistory does not own the underlying records.
 * It only provides a safe, simple way for other AZIMI
 * components to inspect continuity information.
 *
 * Failure isolation:
 * - A history query must never crash Guardian.
 * - Index failures are contained.
 * - Empty results are returned when history is unavailable.
 * - No history operation modifies authoritative records.
 */
object ZHistory {

    private const val MAX_RESULTS = 500

    data class HistoryItem(
        val recordId: String,
        val timestamp: String,
        val category: String,
        val title: String,
        val checksum: String
    )

    data class HistoryResult(
        val success: Boolean,
        val items: List<HistoryItem>,
        val message: String
    )

    /**
     * Get the most recent history entries.
     */
    fun recent(
        context: Context,
        limit: Int = 20
    ): HistoryResult {

        return safeQuery(
            context = context,
            operation = "recent history"
        ) {

            val safeLimit =
                limit.coerceIn(
                    1,
                    MAX_RESULTS
                )

            val result =
                ZContinuityIndex.load(
                    context
                )

            if (!result.success) {
                return@safeQuery HistoryResult(
                    success = false,
                    items = emptyList(),
                    message = result.message
                )
            }

            HistoryResult(
                success = true,
                items = result.entries
                    .take(safeLimit)
                    .map(::toHistoryItem),
                message = "Recent history loaded."
            )
        }
    }

    /**
     * Get build-related history.
     */
    fun builds(
        context: Context,
        limit: Int = 100
    ): HistoryResult {

        return byCategoryAliases(
            context = context,
            operation = "build history",
            categories = listOf(
                "BUILD",
                "BUILDS"
            ),
            limit = limit
        )
    }

    /**
     * Get failure-related history.
     */
    fun failures(
        context: Context,
        limit: Int = 100
    ): HistoryResult {

        return byCategoryAliases(
            context = context,
            operation = "failure history",
            categories = listOf(
                "FAILURE",
                "FAILURES"
            ),
            limit = limit
        )
    }

    /**
     * Get diagnostic history.
     */
    fun diagnostics(
        context: Context,
        limit: Int = 100
    ): HistoryResult {

        return byCategoryAliases(
            context = context,
            operation = "diagnostic history",
            categories = listOf(
                "DIAGNOSTIC",
                "DIAGNOSTICS"
            ),
            limit = limit
        )
    }

    /**
     * Get decision history.
     */
    fun decisions(
        context: Context,
        limit: Int = 100
    ): HistoryResult {

        return byCategoryAliases(
            context = context,
            operation = "decision history",
            categories = listOf(
                "DECISION",
                "DECISIONS"
            ),
            limit = limit
        )
    }

    /**
     * Get recovery-related history.
     */
    fun recovery(
        context: Context,
        limit: Int = 100
    ): HistoryResult {

        return byCategoryAliases(
            context = context,
            operation = "recovery history",
            categories = listOf(
                "RECOVERY",
                "RECOVERY_POINT",
                "RECOVERY_POINTS"
            ),
            limit = limit
        )
    }

    /**
     * Get configuration-related history.
     */
    fun configuration(
        context: Context,
        limit: Int = 100
    ): HistoryResult {

        return byCategoryAliases(
            context = context,
            operation = "configuration history",
            categories = listOf(
                "CONFIGURATION",
                "CONFIG"
            ),
            limit = limit
        )
    }

    /**
     * Get tool-related history.
     */
    fun tools(
        context: Context,
        limit: Int = 100
    ): HistoryResult {

        return byCategoryAliases(
            context = context,
            operation = "tool history",
            categories = listOf(
                "TOOL",
                "TOOLS",
                "TOOL_EVENT"
            ),
            limit = limit
        )
    }

    /**
     * Get Atlas context history.
     */
    fun atlas(
        context: Context,
        limit: Int = 100
    ): HistoryResult {

        return byCategoryAliases(
            context = context,
            operation = "Atlas history",
            categories = listOf(
                "ATLAS",
                "ATLAS_CONTEXT"
            ),
            limit = limit
        )
    }

    /**
     * Search history metadata.
     *
     * Search is intentionally delegated to the index.
     */
    fun search(
        context: Context,
        query: String,
        limit: Int = 100
    ): HistoryResult {

        return safeQuery(
            context = context,
            operation = "history search"
        ) {

            val cleanQuery =
                query.trim()

            if (cleanQuery.isEmpty()) {
                return@safeQuery HistoryResult(
                    success = true,
                    items = emptyList(),
                    message = "Search query was empty."
                )
            }

            val safeLimit =
                limit.coerceIn(
                    1,
                    MAX_RESULTS
                )

            val entries =
                ZContinuityIndex.search(
                    context,
                    cleanQuery,
                    safeLimit
                )

            HistoryResult(
                success = true,
                items = entries.map(::toHistoryItem),
                message = "History search completed."
            )
        }
    }

    /**
     * Find one record by its ID.
     *
     * The index is used first because it is the fastest
     * metadata lookup path.
     *
     * The authoritative record remains untouched.
     */
    fun find(
        context: Context,
        recordId: String
    ): HistoryResult {

        return safeQuery(
            context = context,
            operation = "find history record"
        ) {

            val wanted =
                recordId.trim()

            if (wanted.isEmpty()) {
                return@safeQuery HistoryResult(
                    success = false,
                    items = emptyList(),
                    message = "Record ID was empty."
                )
            }

            val result =
                ZContinuityIndex.load(
                    context
                )

            if (!result.success) {
                return@safeQuery HistoryResult(
                    success = false,
                    items = emptyList(),
                    message = result.message
                )
            }

            val entry =
                result.entries.firstOrNull {
                    it.recordId == wanted
                }

            if (entry == null) {
                HistoryResult(
                    success = true,
                    items = emptyList(),
                    message = "Record was not found."
                )
            } else {
                HistoryResult(
                    success = true,
                    items = listOf(
                        toHistoryItem(entry)
                    ),
                    message = "Record found."
                )
            }
        }
    }

    /**
     * Find failures whose timestamp is near a specified
     * history timestamp.
     *
     * This provides the foundation for future questions such as:
     *
     * "What happened around this failure?"
     *
     * Timestamp comparison is kept simple here because
     * the existing index stores timestamps as strings.
     */
    fun around(
        context: Context,
        timestamp: String,
        limit: Int = 20
    ): HistoryResult {

        return safeQuery(
            context = context,
            operation = "history around timestamp"
        ) {

            val wanted =
                timestamp.trim()

            if (wanted.isEmpty()) {
                return@safeQuery HistoryResult(
                    success = false,
                    items = emptyList(),
                    message = "Timestamp was empty."
                )
            }

            val safeLimit =
                limit.coerceIn(
                    1,
                    MAX_RESULTS
                )

            val result =
                ZContinuityIndex.load(
                    context
                )

            if (!result.success) {
                return@safeQuery HistoryResult(
                    success = false,
                    items = emptyList(),
                    message = result.message
                )
            }

            /*
             * ISO timestamps sort naturally when they use the
             * standard UTC/ISO representation used by AZIMI records.
             */
            val ordered =
                result.entries
                    .sortedBy {
                        distanceFrom(
                            it.timestamp,
                            wanted
                        )
                    }
                    .take(safeLimit)

            HistoryResult(
                success = true,
                items = ordered.map(::toHistoryItem),
                message = "Nearby history loaded."
            )
        }
    }

    /**
     * Check whether history is currently available.
     *
     * This is intentionally lightweight and has no side effects.
     */
    fun isAvailable(
        context: Context
    ): Boolean {

        return runCatching {

            ZContinuityIndex
                .load(context)
                .success

        }.getOrDefault(false)
    }

    /**
     * Return the number of currently indexed history records.
     */
    fun count(
        context: Context
    ): Int {

        return runCatching {

            ZContinuityIndex.getCount(
                context
            )

        }.getOrDefault(0)
    }

    /**
     * Force a safe index rebuild.
     *
     * This is useful for diagnostics and recovery.
     */
    fun rebuild(
        context: Context
    ): HistoryResult {

        return safeQuery(
            context = context,
            operation = "history rebuild"
        ) {

            val result =
                ZContinuityIndex.rebuild(
                    context
                )

            HistoryResult(
                success = result.success,
                items = result.entries
                    .map(::toHistoryItem),
                message = result.message
            )
        }
    }

    /**
     * Convert index metadata into the public history model.
     */
    private fun toHistoryItem(
        entry: ZContinuityIndex.IndexEntry
    ): HistoryItem {

        return HistoryItem(
            recordId = entry.recordId,
            timestamp = entry.timestamp,
            category = entry.category,
            title = entry.title,
            checksum = entry.checksum
        )
    }

    /**
     * Query several possible category names.
     *
     * This makes the history API tolerant of small category
     * naming differences between existing and future records.
     */
    private fun byCategoryAliases(
        context: Context,
        operation: String,
        categories: List<String>,
        limit: Int
    ): HistoryResult {

        return safeQuery(
            context = context,
            operation = operation
        ) {

            val safeLimit =
                limit.coerceIn(
                    1,
                    MAX_RESULTS
                )

            val result =
                ZContinuityIndex.load(
                    context
                )

            if (!result.success) {
                return@safeQuery HistoryResult(
                    success = false,
                    items = emptyList(),
                    message = result.message
                )
            }

            val categorySet =
                categories
                    .map {
                        it.trim()
                            .uppercase()
                    }
                    .toSet()

            val items =
                result.entries
                    .filter {
                        it.category
                            .uppercase() in categorySet
                    }
                    .take(safeLimit)
                    .map(::toHistoryItem)

            HistoryResult(
                success = true,
                items = items,
                message = "$operation loaded."
            )
        }
    }

    /**
     * Safe query boundary.
     *
     * No exception from the history layer should escape into
     * unrelated Guardian systems.
     */
    private fun safeQuery(
        context: Context,
        operation: String,
        block: () -> HistoryResult
    ): HistoryResult {

        return runCatching {

            block()

        }.getOrElse {

            safeRecordFailure(
                context,
                operation,
                it
            )

            HistoryResult(
                success = false,
                items = emptyList(),
                message =
                    it.message
                        ?: "History operation failed."
            )
        }
    }

    /**
     * Calculate a simple sortable timestamp distance.
     *
     * This does not throw if timestamps are malformed.
     */
    private fun distanceFrom(
        first: String,
        second: String
    ): Long {

        return runCatching {

            val firstValue =
                parseTimestamp(first)

            val secondValue =
                parseTimestamp(second)

            kotlin.math.abs(
                firstValue - secondValue
            )

        }.getOrDefault(
            Long.MAX_VALUE
        )
    }

    /**
     * Parse ISO-8601 timestamps safely.
     *
     * Java's java.time API is available on modern Android
     * and is safe for our min SDK through desugaring in the
     * normal Android build configuration.
     *
     * If parsing fails, the timestamp receives a neutral
     * maximum distance rather than crashing history.
     */
    private fun parseTimestamp(
        value: String
    ): Long {

        return runCatching {

            java.time.Instant
                .parse(value)
                .toEpochMilli()

        }.getOrElse {

            /*
             * Some existing records may use a numeric timestamp.
             */
            runCatching {
                value.toLong()
            }.getOrDefault(
                Long.MAX_VALUE
            )
        }
    }

    /**
     * Diagnostics must never become a second failure.
     */
    private fun safeRecordFailure(
        context: Context,
        operation: String,
        throwable: Throwable
    ) {

        runCatching {

            GuardianStorage.recordError(
                context,
                "ZHistory $operation failed: " +
                    (
                        throwable.message
                            ?: throwable.javaClass.simpleName
                    ).take(450)
            )

        }
    }
}
