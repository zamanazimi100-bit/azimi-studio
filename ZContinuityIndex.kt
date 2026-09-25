package com.azimi.guardian

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * AZIMI Z Continuity Index
 *
 * Purpose:
 * - Provides a searchable metadata index over ZContinuityStorage records.
 * - The index is NOT the source of truth.
 * - ZContinuityStorage remains authoritative.
 * - If the index is missing, damaged, unreadable, or outdated,
 *   it can be rebuilt from the authoritative records.
 *
 * Failure isolation:
 * - Index failure must never delete or modify continuity records.
 * - Index failure must never crash Guardian.
 * - Rebuild failure is contained and reported as a degraded result.
 *
 * Security:
 * - The index itself is encrypted using VaultCrypto.
 * - No passwords, API keys, tokens, recovery codes, or credentials
 *   are intentionally indexed.
 */
object ZContinuityIndex {

    private const val ROOT_DIR =
        "z_continuity"

    private const val INDEX_FILE =
        "index.zci"

    private const val SCHEMA_VERSION =
        1

    private const val MAX_INDEX_RECORDS =
        5000

    private const val MAX_ID_LENGTH =
        200

    private const val MAX_CATEGORY_LENGTH =
        100

    private const val MAX_TITLE_LENGTH =
        200

    private const val MAX_TIMESTAMP_LENGTH =
        50

    private const val MAX_CHECKSUM_LENGTH =
        128

    data class IndexEntry(
        val recordId: String,
        val timestamp: String,
        val category: String,
        val title: String,
        val checksum: String
    )

    data class IndexResult(
        val success: Boolean,
        val rebuilt: Boolean,
        val entries: List<IndexEntry>,
        val message: String
    )

    private fun rootDirectory(
        context: Context
    ): File {
        return File(
            context.filesDir,
            ROOT_DIR
        )
    }

    private fun indexFile(
        context: Context
    ): File {
        return File(
            rootDirectory(context),
            INDEX_FILE
        )
    }

    /**
     * Initialize the index directory.
     *
     * This operation is intentionally independent from the
     * authoritative continuity record store.
     */
    fun initialize(
        context: Context
    ): Boolean {
        return runCatching {
            val root = rootDirectory(context)

            if (root.exists()) {
                root.isDirectory
            } else {
                root.mkdirs()
            }
        }.getOrDefault(false)
    }

    /**
     * Build an index from authoritative ZContinuityStorage records.
     *
     * Expected ZContinuityStorage.readRecord() format:
     *
     * {
     *   "schemaVersion": 1,
     *   "id": "...",
     *   "timestamp": "...",
     *   "category": "...",
     *   "title": "...",
     *   "content": "...",
     *   "checksum": "..."
     * }
     *
     * Invalid individual records are skipped.
     * One bad record must not destroy the entire index.
     */
    fun rebuild(
        context: Context
    ): IndexResult {

        return runCatching {

            if (!initialize(context)) {
                return IndexResult(
                    success = false,
                    rebuilt = false,
                    entries = emptyList(),
                    message = "Index directory could not be initialized."
                )
            }

            val recordIds = runCatching {
                ZContinuityStorage.listRecordIds(
                    context
                )
            }.getOrElse {
                return IndexResult(
                    success = false,
                    rebuilt = false,
                    entries = emptyList(),
                    message = "Authoritative record list could not be read."
                )
            }

            val entries =
                ArrayList<IndexEntry>()

            for (recordId in recordIds) {

                if (entries.size >= MAX_INDEX_RECORDS) {
                    break
                }

                val entry =
                    parseRecordForIndex(
                        context,
                        recordId
                    )

                if (entry != null) {
                    entries.add(entry)
                }
            }

            entries.sortByDescending {
                it.timestamp
            }

            val saved =
                saveIndex(
                    context,
                    entries
                )

            if (!saved) {
                return IndexResult(
                    success = false,
                    rebuilt = true,
                    entries = emptyList(),
                    message = "Index could not be saved."
                )
            }

            IndexResult(
                success = true,
                rebuilt = true,
                entries = entries,
                message = "Index rebuilt successfully."
            )

        }.getOrElse {

            safeRecordFailure(
                context,
                "ZContinuityIndex rebuild failed: " +
                    (it.message ?: it.javaClass.simpleName)
            )

            IndexResult(
                success = false,
                rebuilt = true,
                entries = emptyList(),
                message =
                    it.message
                        ?: "Unknown index rebuild failure."
            )
        }
    }

    /**
     * Read the existing index.
     *
     * If it cannot be read, this function automatically attempts
     * a rebuild from the authoritative storage.
     */
    fun load(
        context: Context
    ): IndexResult {

        return runCatching {

            val file =
                indexFile(context)

            if (!file.exists()) {
                return rebuild(context)
            }

            val encrypted =
                file.readText()

            if (encrypted.isBlank()) {
                return rebuild(context)
            }

            val decrypted =
                VaultCrypto.decrypt(
                    encrypted
                )

            val entries =
                parseIndex(
                    decrypted
                )

            if (entries == null) {
                return rebuild(context)
            }

            IndexResult(
                success = true,
                rebuilt = false,
                entries = entries,
                message = "Index loaded successfully."
            )

        }.getOrElse {

            safeRecordFailure(
                context,
                "ZContinuityIndex load failed: " +
                    (it.message ?: it.javaClass.simpleName)
            )

            rebuild(context)
        }
    }

    /**
     * Return recent indexed records.
     *
     * The index is treated as a cache. If it fails, an empty result
     * is returned rather than allowing the failure to propagate.
     */
    fun recent(
        context: Context,
        limit: Int = 20
    ): List<IndexEntry> {

        return runCatching {

            val safeLimit =
                limit.coerceIn(
                    1,
                    100
                )

            val result =
                load(context)

            if (!result.success) {
                emptyList()
            } else {
                result.entries
                    .take(safeLimit)
            }

        }.getOrDefault(emptyList())
    }

    /**
     * Find records by category.
     */
    fun byCategory(
        context: Context,
        category: String,
        limit: Int = 100
    ): List<IndexEntry> {

        return runCatching {

            val wanted =
                category.trim()

            if (wanted.isEmpty()) {
                return emptyList()
            }

            val safeLimit =
                limit.coerceIn(
                    1,
                    500
                )

            val result =
                load(context)

            if (!result.success) {
                emptyList()
            } else {
                result.entries
                    .filter {
                        it.category.equals(
                            wanted,
                            ignoreCase = true
                        )
                    }
                    .take(safeLimit)
            }

        }.getOrDefault(emptyList())
    }

    /**
     * Search titles and categories.
     *
     * This is intentionally metadata-only.
     * Full record content is not indexed.
     */
    fun search(
        context: Context,
        query: String,
        limit: Int = 100
    ): List<IndexEntry> {

        return runCatching {

            val wanted =
                query.trim()

            if (wanted.isEmpty()) {
                return emptyList()
            }

            val safeLimit =
                limit.coerceIn(
                    1,
                    500
                )

            val result =
                load(context)

            if (!result.success) {
                emptyList()
            } else {

                result.entries
                    .filter {

                        it.title.contains(
                            wanted,
                            ignoreCase = true
                        ) ||
                            it.category.contains(
                                wanted,
                                ignoreCase = true
                            ) ||
                            it.recordId.contains(
                                wanted,
                                ignoreCase = true
                            )
                    }
                    .take(safeLimit)
            }

        }.getOrDefault(emptyList())
    }

    /**
     * Remove the derived index only.
     *
     * IMPORTANT:
     * This does NOT delete ZContinuityStorage records.
     *
     * The index can always be rebuilt.
     */
    fun clearIndex(
        context: Context
    ): Boolean {

        return runCatching {

            val file =
                indexFile(context)

            if (!file.exists()) {
                true
            } else {
                file.delete()
            }

        }.getOrDefault(false)
    }

    /**
     * Number of entries currently indexed.
     */
    fun getCount(
        context: Context
    ): Int {

        return runCatching {

            load(context)
                .entries
                .size

        }.getOrDefault(0)
    }

    /**
     * Parse one authoritative continuity record.
     *
     * An invalid individual record is ignored.
     */
    private fun parseRecordForIndex(
        context: Context,
        recordId: String
    ): IndexEntry? {

        return runCatching {

            if (
                recordId.isBlank() ||
                recordId.length > MAX_ID_LENGTH
            ) {
                return null
            }

            val raw =
                ZContinuityStorage.readRecord(
                    context,
                    recordId
                )
                    ?: return null

            val json =
                JSONObject(raw)

            val id =
                json.optString(
                    "id",
                    recordId
                )
                    .trim()
                    .take(MAX_ID_LENGTH)

            val timestamp =
                json.optString(
                    "timestamp",
                    ""
                )
                    .trim()
                    .take(MAX_TIMESTAMP_LENGTH)

            val category =
                json.optString(
                    "category",
                    "UNKNOWN"
                )
                    .trim()
                    .take(MAX_CATEGORY_LENGTH)

            val title =
                json.optString(
                    "title",
                    ""
                )
                    .trim()
                    .take(MAX_TITLE_LENGTH)

            val checksum =
                json.optString(
                    "checksum",
                    ""
                )
                    .trim()
                    .take(MAX_CHECKSUM_LENGTH)

            if (id.isBlank()) {
                return null
            }

            IndexEntry(
                recordId = id,
                timestamp = timestamp,
                category = category,
                title = title,
                checksum = checksum
            )

        }.getOrNull()
    }

    /**
     * Save index atomically.
     *
     * The temporary encrypted file is written first.
     * Only after successful writing is it renamed to the live index.
     */
    private fun saveIndex(
        context: Context,
        entries: List<IndexEntry>
    ): Boolean {

        return runCatching {

            val root =
                rootDirectory(context)

            if (!root.exists() && !root.mkdirs()) {
                return false
            }

            val json =
                JSONObject()

            json.put(
                "schemaVersion",
                SCHEMA_VERSION
            )

            json.put(
                "generatedAt",
                System.currentTimeMillis()
            )

            val array =
                JSONArray()

            entries
                .take(MAX_INDEX_RECORDS)
                .forEach { entry ->

                    val item =
                        JSONObject()

                    item.put(
                        "recordId",
                        entry.recordId
                    )

                    item.put(
                        "timestamp",
                        entry.timestamp
                    )

                    item.put(
                        "category",
                        entry.category
                    )

                    item.put(
                        "title",
                        entry.title
                    )

                    item.put(
                        "checksum",
                        entry.checksum
                    )

                    array.put(item)
                }

            json.put(
                "entries",
                array
            )

            val plaintext =
                json.toString()

            val encrypted =
                VaultCrypto.encrypt(
                    plaintext
                )

            val target =
                indexFile(context)

            val temporary =
                File(
                    root,
                    "$INDEX_FILE.tmp"
                )

            temporary.writeText(
                encrypted
            )

            if (!temporary.exists()) {
                return false
            }

            if (temporary.length() <= 0L) {
                temporary.delete()
                return false
            }

            if (target.exists()) {
                target.delete()
            }

            val renamed =
                temporary.renameTo(
                    target
                )

            if (!renamed) {
                temporary.delete()
            }

            renamed

        }.getOrDefault(false)
    }

    /**
     * Parse the encrypted index after decryption.
     */
    private fun parseIndex(
        plaintext: String
    ): List<IndexEntry>? {

        return runCatching {

            val json =
                JSONObject(plaintext)

            val schemaVersion =
                json.optInt(
                    "schemaVersion",
                    -1
                )

            if (schemaVersion != SCHEMA_VERSION) {
                return null
            }

            val array =
                json.optJSONArray(
                    "entries"
                )
                    ?: return emptyList()

            val entries =
                ArrayList<IndexEntry>()

            val count =
                array.length()
                    .coerceAtMost(
                        MAX_INDEX_RECORDS
                    )

            for (index in 0 until count) {

                val item =
                    array.optJSONObject(index)
                        ?: continue

                val recordId =
                    item.optString(
                        "recordId",
                        ""
                    )
                        .trim()
                        .take(MAX_ID_LENGTH)

                if (recordId.isBlank()) {
                    continue
                }

                val timestamp =
                    item.optString(
                        "timestamp",
                        ""
                    )
                        .trim()
                        .take(MAX_TIMESTAMP_LENGTH)

                val category =
                    item.optString(
                        "category",
                        "UNKNOWN"
                    )
                        .trim()
                        .take(MAX_CATEGORY_LENGTH)

                val title =
                    item.optString(
                        "title",
                        ""
                    )
                        .trim()
                        .take(MAX_TITLE_LENGTH)

                val checksum =
                    item.optString(
                        "checksum",
                        ""
                    )
                        .trim()
                        .take(MAX_CHECKSUM_LENGTH)

                entries.add(
                    IndexEntry(
                        recordId = recordId,
                        timestamp = timestamp,
                        category = category,
                        title = title,
                        checksum = checksum
                    )
                )
            }

            entries.sortByDescending {
                it.timestamp
            }

            entries

        }.getOrNull()
    }

    /**
     * Diagnostics must never become another failure.
     */
    private fun safeRecordFailure(
        context: Context,
        message: String
    ) {
        runCatching {

            GuardianStorage.recordError(
                context,
                message.take(500)
            )

        }
    }

    /**
     * Generate a stable checksum for future index validation.
     */
    fun calculateIndexFingerprint(
        entries: List<IndexEntry>
    ): String? {

        return runCatching {

            val canonical =
                entries
                    .sortedBy {
                        it.recordId
                    }
                    .joinToString("\n") {
                        buildString {
                            append(it.recordId)
                            append("|")
                            append(it.timestamp)
                            append("|")
                            append(it.category)
                            append("|")
                            append(it.title)
                            append("|")
                            append(it.checksum)
                        }
                    }

            val digest =
                MessageDigest.getInstance(
                    "SHA-256"
                )

            digest
                .digest(
                    canonical.toByteArray(
                        Charsets.UTF_8
                    )
                )
                .joinToString("") {
                    "%02x".format(it)
                }

        }.getOrNull()
    }
}
