package com.azimi.guardian

import android.content.Context
import org.json.JSONObject

/**
 * Z ARCHIVE
 *
 * Preserves the authentic history of AZIMI:
 *
 * - Origin
 * - Journey
 * - Milestones
 * - Failures
 * - Recovery
 * - Lessons
 * - Decisions
 * - Architecture
 * - Evidence
 * - Vision
 * - Legacy
 * - Checkpoints
 *
 * Z Archive is intentionally a thin layer over ZContinuityStorage.
 *
 * Security:
 * - Records are protected by ZContinuityStorage/VaultCrypto.
 * - Protected credentials are rejected.
 * - Archive data must never contain passwords, tokens, keys,
 *   recovery codes, private keys, session cookies, or similar secrets.
 *
 * Important:
 * Z Archive does not bypass Z Vault security and does not alter
 * the existing Vault authorization model.
 */
object ZArchive {

    private const val TAG = "ZArchive"

    const val ARCHIVE_ORIGIN = "ARCHIVE_ORIGIN"
    const val ARCHIVE_JOURNEY = "ARCHIVE_JOURNEY"
    const val ARCHIVE_MILESTONE = "ARCHIVE_MILESTONE"
    const val ARCHIVE_FAILURE = "ARCHIVE_FAILURE"
    const val ARCHIVE_RECOVERY = "ARCHIVE_RECOVERY"
    const val ARCHIVE_LESSON = "ARCHIVE_LESSON"
    const val ARCHIVE_DECISION = "ARCHIVE_DECISION"
    const val ARCHIVE_ARCHITECTURE = "ARCHIVE_ARCHITECTURE"
    const val ARCHIVE_EVIDENCE = "ARCHIVE_EVIDENCE"
    const val ARCHIVE_VISION = "ARCHIVE_VISION"
    const val ARCHIVE_LEGACY = "ARCHIVE_LEGACY"
    const val ARCHIVE_CHECKPOINT = "ARCHIVE_CHECKPOINT"

    private const val DAY_ONE_TITLE = "DAY ONE — THE BEGINNING OF AZIMI"

    /**
     * Initialize the archive layer.
     */
    fun initialize(context: Context) {
        ZContinuityStorage.initialize(context.applicationContext)
    }

    /**
     * Record the origin of AZIMI.
     */
    fun recordOrigin(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_ORIGIN,
            title = title,
            content = content
        )
    }

    /**
     * Record a journey entry.
     */
    fun recordJourney(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_JOURNEY,
            title = title,
            content = content
        )
    }

    /**
     * Record a milestone.
     */
    fun recordMilestone(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_MILESTONE,
            title = title,
            content = content
        )
    }

    /**
     * Record a failure.
     */
    fun recordFailure(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_FAILURE,
            title = title,
            content = content
        )
    }

    /**
     * Record a recovery.
     */
    fun recordRecovery(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_RECOVERY,
            title = title,
            content = content
        )
    }

    /**
     * Record a lesson.
     */
    fun recordLesson(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_LESSON,
            title = title,
            content = content
        )
    }

    /**
     * Record a decision.
     */
    fun recordDecision(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_DECISION,
            title = title,
            content = content
        )
    }

    /**
     * Record an architecture event.
     */
    fun recordArchitecture(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_ARCHITECTURE,
            title = title,
            content = content
        )
    }

    /**
     * Record evidence.
     */
    fun recordEvidence(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_EVIDENCE,
            title = title,
            content = content
        )
    }

    /**
     * Record a vision statement.
     */
    fun recordVision(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_VISION,
            title = title,
            content = content
        )
    }

    /**
     * Record a legacy statement.
     */
    fun recordLegacy(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_LEGACY,
            title = title,
            content = content
        )
    }

    /**
     * Record a checkpoint.
     */
    fun recordCheckpoint(
        context: Context,
        title: String,
        content: String
    ): String? {
        return record(
            context = context,
            category = ARCHIVE_CHECKPOINT,
            title = title,
            content = content
        )
    }

    /**
     * Creates the authentic Day One origin record if it does not
     * already exist.
     *
     * This is deliberately NOT called automatically by initialize().
     *
     * The caller must explicitly request the Day One seed so that
     * archive history is never silently created or modified.
     *
     * @return existing record ID when already present,
     *         new record ID when created,
     *         null when creation failed.
     */
    fun recordDayOneIfMissing(context: Context): String? {

        initialize(context)

        val existing = findDayOneRecord(context)

        if (existing != null) {
            return existing
        }

        return recordOrigin(
            context = context,
            title = DAY_ONE_TITLE,
            content = DAY_ONE_CONTENT
        )
    }

    /**
     * Find the existing Day One record without creating anything.
     */
    fun findDayOneRecord(context: Context): String? {

        initialize(context)

        val recordIds = listRecordIds(context)

        for (recordId in recordIds) {

            val payload = read(context, recordId)
                ?: continue

            try {

                val json = JSONObject(payload)

                val category =
                    json.optString("category", "")

                val title =
                    json.optString("title", "")

                if (
                    category == ARCHIVE_ORIGIN &&
                    title.contains("DAY ONE", ignoreCase = true)
                ) {
                    return recordId
                }

            } catch (_: Exception) {
                // Ignore malformed/unrelated records.
            }
        }

        return null
    }

    /**
     * Read one archive record.
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
     * List archive/continuity record IDs.
     */
    fun listRecordIds(context: Context): List<String> {

        return ZContinuityStorage.listRecordIds(
            context.applicationContext
        )
    }

    /**
     * Number of archive/continuity records.
     */
    fun getRecordCount(context: Context): Int {

        return ZContinuityStorage.getRecordCount(
            context.applicationContext
        )
    }

    /**
     * Delete one archive record.
     *
     * This delegates to ZContinuityStorage and does not bypass
     * its safety model.
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
     * Clear all continuity records.
     *
     * WARNING:
     * This is intentionally a broad operation because the underlying
     * storage contains more than Z Archive.
     *
     * A future Z Archive UI must require explicit confirmation before
     * calling this method.
     */
    fun clearAll(context: Context): Boolean {

        return ZContinuityStorage.clearAll(
            context.applicationContext
        )
    }

    /**
     * Common secure archive writer.
     */
    private fun record(
        context: Context,
        category: String,
        title: String,
        content: String
    ): String? {

        val appContext = context.applicationContext

        if (title.isBlank()) {
            return null
        }

        if (content.isBlank()) {
            return null
        }

        /*
         * Never allow protected credentials into Z Archive.
         *
         * This is an additional archive-level safety boundary.
         * ZContinuityStorage performs its own validation as well.
         */
        if (containsProtectedCredential(title)) {
            return null
        }

        if (containsProtectedCredential(content)) {
            return null
        }

        return ZContinuityStorage.recordProjectEvent(
            context = appContext,
            category = category,
            title = buildArchiveTitle(title),
            content = content
        )
    }

    /**
     * Archive-level credential protection.
     */
    private fun containsProtectedCredential(
        value: String
    ): Boolean {

        if (value.isBlank()) {
            return false
        }

        if (AzimiAuth.isProtectedCredential(value)) {
            return true
        }

        val text = value.lowercase()

        val blockedPatterns = listOf(
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

        return blockedPatterns.any {
            text.contains(it)
        }
    }

    /**
     * Gives every archive record a visible Z ARCHIVE identity.
     */
    private fun buildArchiveTitle(title: String): String {

        val cleanTitle = title.trim()

        return if (
            cleanTitle.startsWith("Z ARCHIVE", ignoreCase = true)
        ) {
            cleanTitle
        } else {
            "Z ARCHIVE — $cleanTitle"
        }
    }

    /**
     * Z ARCHIVE — DAY ONE
     *
     * This is based on Zaman's own account of the beginning of AZIMI.
     *
     * It is intentionally written as a human story rather than a
     * technical changelog.
     */
    private const val DAY_ONE_CONTENT = """
Before AZIMI, I was a boy who was not thinking much about the future.

I wanted to learn. I wanted to experience the opportunities that other people had through school, college, and education. My family did everything they could to give us a good life, but because we moved frequently inside Afghanistan, my education was not stable.

Looking back, I have regrets. I wish I had tried harder and understood earlier how important those opportunities were.

At one point I decided that I should leave Afghanistan, go to another country, work, and try to make something of myself. Some people in my family laughed at the idea and doubted that I could do it.

I left Afghanistan and went to Turkey.

I worked in a factory for about eight years. We produced packaging and protective materials used for things such as televisions, refrigerators, and other products.

Those years taught me something important: hard work is real, and behind many things people take for granted there are people who sacrificed their time, energy, and youth to make them possible.

I also began to understand more deeply what my family had sacrificed for me.

Then my mother died.

My sister called me crying. I was far away. I could not afford a plane ticket to return, and I could not attend my mother's funeral.

About one month later, my older brother died too.

Eventually I was able to afford a ticket and return to Afghanistan.

But when I returned, the happiness that had once existed in our family was gone.

Our family had once been nine people. My sisters had married, and I was left with my old father and the three children of my brother.

I felt responsible.

Within about eight months, I lost almost everything.

There were times when nobody helped me. Some people mocked me. Some people tried to harm me.

During those days, I wished there was simply someone who could say:

"Don't worry. Everything will be okay. You are not alone. I'm here for you."

But I did not have that person.

Still, somehow, I kept going.

I started thinking about an online clothes shop.

I created a Google site for it.

Then I reached a point where I needed a profile picture.

That was the first time I used an AI application.

I did not know then how much that small moment would change the direction of my life.

Later I became stuck while working on the website.

I went back to AI for help.

Instead of only solving the website problem, something much bigger happened.

I became excited about artificial intelligence.

I started wanting to learn.

I started wanting to build.

I started wanting to understand how technology works and what I could create with it.

Every day became another opportunity to learn something new.

And that was where the idea that eventually became AZIMI began to grow.

I started with almost nothing.

I had a phone.

I did not have an expensive computer.

I did not have formal technical education.

I did not have a team.

I did not have special resources.

People laughed at me.

People doubted me.

Some people could not understand why I was trying to build things that seemed too ambitious for someone in my situation.

But I did not want to become hateful because of that.

Instead, I wanted to build something useful.

I wanted to learn.

I wanted to create technology that could help other people.

AZIMI became more than a project.

It became a direction.

A reminder that a person's starting point does not have to determine where they finish.

I believe that people should not be told that special things belong only to people with money, powerful computers, formal education, or privileged circumstances.

Technology, knowledge, and opportunity should be available to everyone.

That does not mean everything is easy.

It does not mean every dream will happen exactly as imagined.

It means that if something is worth building, we can try.

We can fail.

We can learn.

We can try again.

We can find another way when the first way does not work.

Every failure can become a lesson.

Every small success can become evidence that progress is possible.

And every day can teach us something.

I also learned something else along the way:

It does not matter whether someone does something big or something small for you.

If they helped you when you needed help, value it.

Be grateful for it.

Remember it.

Because sometimes a small act of help can become part of a much bigger journey.

AZIMI is not finished.

This is only the beginning.

I will try again and again.

Failure will not be the reason I stop.

If there is a solution, I will keep looking for it.

If I make a mistake, I will learn from it.

If I have to start again, I will build again.

Every day is a lesson to learn.

Continue....
"""

}
