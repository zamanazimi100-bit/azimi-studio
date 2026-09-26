package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Z ARCHIVE
 *
 * The historical layer of AZIMI.
 *
 * Z Archive preserves the journey of AZIMI:
 *
 * Day One
 * Origin
 * Journey
 * Milestones
 * Failures
 * Recoveries
 * Lessons
 * Decisions
 * Architecture evolution
 * Evidence / checkpoints
 * Vision
 * Legacy
 *
 * Z Archive intentionally does NOT create its own storage engine.
 * It uses ZContinuityStorage so archived records receive the
 * existing AZIMI encryption, integrity verification and
 * protected-credential filtering.
 *
 * SECURITY PRINCIPLE:
 *
 * Story      -> preservable
 * Evidence   -> verifiable
 * Secrets    -> never archived
 *
 * Z Archive is historical/project context.
 * It is NOT an authorization mechanism and MUST NOT be used
 * to unlock Vault, bypass Guardian policy, or grant Atlas
 * permissions.
 */
object ZArchive {

    private const val ARCHIVE_PREFIX =
        "Z ARCHIVE"

    private const val MAX_ARCHIVE_CONTENT_LENGTH =
        100_000

    /*
     * Archive categories.
     *
     * These remain intentionally separate from the generic
     * continuity categories so Z Archive can evolve without
     * changing ZContinuityStorage's underlying format.
     */
    private const val CATEGORY_ORIGIN =
        "ARCHIVE_ORIGIN"

    private const val CATEGORY_JOURNEY =
        "ARCHIVE_JOURNEY"

    private const val CATEGORY_MILESTONE =
        "ARCHIVE_MILESTONE"

    private const val CATEGORY_FAILURE =
        "ARCHIVE_FAILURE"

    private const val CATEGORY_RECOVERY =
        "ARCHIVE_RECOVERY"

    private const val CATEGORY_LESSON =
        "ARCHIVE_LESSON"

    private const val CATEGORY_DECISION =
        "ARCHIVE_DECISION"

    private const val CATEGORY_ARCHITECTURE =
        "ARCHIVE_ARCHITECTURE"

    private const val CATEGORY_EVIDENCE =
        "ARCHIVE_EVIDENCE"

    private const val CATEGORY_VISION =
        "ARCHIVE_VISION"

    private const val CATEGORY_LEGACY =
        "ARCHIVE_LEGACY"

    private const val CATEGORY_CHECKPOINT =
        "ARCHIVE_CHECKPOINT"

    /**
     * Initializes the archive storage.
     *
     * This only prepares the existing Z Continuity storage.
     * It does not create a historical entry automatically.
     */
    fun initialize(
        context: Context
    ): Boolean {
        return ZContinuityStorage.initialize(
            context.applicationContext
        )
    }

    /**
     * Records the original beginning of AZIMI.
     *
     * This is intended for the eventual official
     * "DAY ONE" historical record.
     */
    fun recordOrigin(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_ORIGIN,
            title = title,
            content = content
        )
    }

    /**
     * Records an important part of the journey.
     */
    fun recordJourney(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_JOURNEY,
            title = title,
            content = content
        )
    }

    /**
     * Records a completed milestone.
     */
    fun recordMilestone(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_MILESTONE,
            title = title,
            content = content
        )
    }

    /**
     * Records a failure as part of the historical journey.
     *
     * Failures are not treated as worthless data.
     * They can explain what happened and what was learned.
     */
    fun recordFailure(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_FAILURE,
            title = title,
            content = content
        )
    }

    /**
     * Records a recovery from a difficult event.
     */
    fun recordRecovery(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_RECOVERY,
            title = title,
            content = content
        )
    }

    /**
     * Records a lesson learned during development.
     */
    fun recordLesson(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_LESSON,
            title = title,
            content = content
        )
    }

    /**
     * Records an important engineering or project decision.
     */
    fun recordDecision(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_DECISION,
            title = title,
            content = content
        )
    }

    /**
     * Records evolution of AZIMI architecture.
     */
    fun recordArchitecture(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_ARCHITECTURE,
            title = title,
            content = content
        )
    }

    /**
     * Records a verifiable project checkpoint or evidence reference.
     *
     * Do not place secrets, private keys, tokens, passwords or
     * authentication material into evidence content.
     */
    fun recordEvidence(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_EVIDENCE,
            title = title,
            content = content
        )
    }

    /**
     * Records future direction and vision.
     */
    fun recordVision(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_VISION,
            title = title,
            content = content
        )
    }

    /**
     * Records the long-term legacy or meaning of the project.
     */
    fun recordLegacy(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_LEGACY,
            title = title,
            content = content
        )
    }

    /**
     * Records an official AZIMI checkpoint.
     */
    fun recordCheckpoint(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = CATEGORY_CHECKPOINT,
            title = title,
            content = content
        )
    }

    /**
     * Reads one archived record.
     *
     * ZContinuityStorage performs the encrypted read and
     * checksum/integrity verification.
     */
    fun read(
        context: Context,
        recordId: String
    ): String? {
        return ZContinuityStorage.readRecord(
            context.applicationContext,
            recordId
        )
    }

    /**
     * Lists archive/continuity record IDs.
     *
     * The underlying continuity store currently contains both
     * ordinary continuity records and Z Archive records.
     *
     * Filtering by archive category is intentionally deferred
     * until the dedicated archive index/UI is introduced.
     */
    fun listRecordIds(
        context: Context
    ): List<String> {
        return ZContinuityStorage.listRecordIds(
            context.applicationContext
        )
    }

    /**
     * Returns the number of records currently held by the
     * underlying continuity store.
     */
    fun getRecordCount(
        context: Context
    ): Int {
        return ZContinuityStorage.getRecordCount(
            context.applicationContext
        )
    }

    /**
     * Deletes one continuity/archive record.
     *
     * This is deliberately a direct pass-through to the
     * existing storage layer.
     */
    fun delete(
        context: Context,
        recordId: String
    ): Boolean {
        return ZContinuityStorage.deleteRecord(
            context.applicationContext,
            recordId
        )
    }

    /**
     * Clears the underlying continuity storage.
     *
     * WARNING:
     *
     * This affects ALL continuity records, not only Z Archive.
     *
     * A dedicated archive-management confirmation flow should
     * be added before exposing this operation in the Guardian UI.
     */
    fun clearAll(
        context: Context
    ): Boolean {
        return ZContinuityStorage.clearAll(
            context.applicationContext
        )
    }

    /**
     * Internal archive writer.
     *
     * All archive record types eventually pass through the
     * existing ZContinuityStorage security boundary.
     */
    private fun record(
        context: Context,
        category: String,
        title: String,
        content: String
    ): String? {

        val safeContext =
            context.applicationContext

        if (title.trim().isBlank()) {
            return null
        }

        if (content.trim().isBlank()) {
            return null
        }

        if (
            content.length >
            MAX_ARCHIVE_CONTENT_LENGTH
        ) {
            return null
        }

        /*
         * Perform an additional archive-level credential check
         * before handing the record to ZContinuityStorage.
         *
         * ZContinuityStorage performs its own independent
         * protected-data check as the final storage boundary.
         */
        if (
            containsProtectedCredential(title) ||
            containsProtectedCredential(content)
        ) {
            return null
        }

        val archiveTitle =
            buildArchiveTitle(title)

        return ZContinuityStorage.recordProjectEvent(
            context = safeContext,
            category = category,
            title = archiveTitle,
            content = content
        )
    }

    /**
     * Adds the archive identity to a record title without
     * modifying the user's actual historical content.
     */
    private fun buildArchiveTitle(
        title: String
    ): String {
        val cleanTitle =
            title
                .trim()
                .replace(
                    Regex("\\s+"),
                    " "
                )

        return "$ARCHIVE_PREFIX — $cleanTitle"
    }

    /**
     * Extra local credential protection.
     *
     * ZContinuityStorage remains the authoritative final
     * security boundary.
     */
    private fun containsProtectedCredential(
        value: String
    ): Boolean {
        return runCatching {

            if (
                AzimiAuth.isProtectedCredential(
                    value
                )
            ) {
                return true
            }

            val normalized =
                value
                    .trim()
                    .lowercase()

            val blockedPatterns =
                listOf(
                    "password=",
                    "passwd=",
                    "secret=",
                    "api_key=",
                    "apikey=",
                    "access_token=",
                    "refresh_token=",
                    "authorization: bearer",
                    "bearer ",
                    "recovery_code=",
                    "verification_code=",
                    "private_key=",
                    "client_secret=",
                    "service_role_key=",
                    "openai_api_key=",
                    "supabase_service_role_key="
                )

            blockedPatterns.any {
                normalized.contains(it)
            }

        }.getOrDefault(true)
    }
}
