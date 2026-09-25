package com.azimi.guardian

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

object ZContinuityStorage {

    private const val ROOT_FOLDER =
        "z_continuity"

    private const val RECORDS_FOLDER =
        "records"

    private const val TEMP_FOLDER =
        "temp"

    private const val SCHEMA_VERSION =
        1

    private const val MAX_TITLE_LENGTH =
        200

    private const val MAX_CONTENT_LENGTH =
        100_000

    private const val CATEGORY_BUILD =
        "BUILD"

    private const val CATEGORY_FAILURE =
        "FAILURE"

    private const val CATEGORY_DECISION =
        "DECISION"

    private const val CATEGORY_DIAGNOSTIC =
        "DIAGNOSTIC"

    private const val CATEGORY_RECOVERY =
        "RECOVERY"

    private const val CATEGORY_PROJECT =
        "PROJECT"

    private const val CATEGORY_CONFIGURATION =
        "CONFIGURATION"

    private const val CATEGORY_TOOL =
        "TOOL"

    private const val CATEGORY_ATLAS =
        "ATLAS"

    private fun root(
        context: Context
    ): File {
        return File(
            context.filesDir,
            ROOT_FOLDER
        )
    }

    private fun recordsDirectory(
        context: Context
    ): File {
        return File(
            root(context),
            RECORDS_FOLDER
        )
    }

    private fun tempDirectory(
        context: Context
    ): File {
        return File(
            root(context),
            TEMP_FOLDER
        )
    }

    private fun ensureDirectories(
        context: Context
    ): Boolean {
        return runCatching {
            val root = root(context)
            val records = recordsDirectory(context)
            val temp = tempDirectory(context)

            if (!root.exists()) {
                require(root.mkdirs() || root.exists()) {
                    "Z Continuity root could not be created."
                }
            }

            if (!records.exists()) {
                require(
                    records.mkdirs() ||
                        records.exists()
                ) {
                    "Z Continuity records folder could not be created."
                }
            }

            if (!temp.exists()) {
                require(
                    temp.mkdirs() ||
                        temp.exists()
                ) {
                    "Z Continuity temp folder could not be created."
                }
            }

            true
        }.getOrDefault(false)
    }

    private fun sanitizeTitle(
        title: String
    ): String {
        return title
            .trim()
            .replace(
                Regex("\\s+"),
                " "
            )
            .take(MAX_TITLE_LENGTH)
    }

    private fun sanitizeCategory(
        category: String
    ): String {
        return category
            .trim()
            .uppercase()
            .replace(
                Regex("[^A-Z0-9_-]"),
                "_"
            )
            .take(50)
            .ifBlank {
                CATEGORY_PROJECT
            }
    }

    private fun containsProtectedCredential(
        value: String
    ): Boolean {
        return runCatching {
            if (AzimiAuth.isProtectedCredential(value)) {
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

    private fun isSafeProjectData(
        title: String,
        content: String
    ): Boolean {
        return !containsProtectedCredential(
            title
        ) &&
            !containsProtectedCredential(
                content
            )
    }

    private fun checksum(
        value: String
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        val bytes =
            digest.digest(
                value.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }

    private fun now(): Long {
        return System.currentTimeMillis()
    }

    fun initialize(
        context: Context
    ): Boolean {
        return runCatching {
            ensureDirectories(context)
        }.getOrDefault(false)
    }

    fun recordProjectEvent(
        context: Context,
        category: String,
        title: String,
        content: String
    ): String? {

        return runCatching {

            require(
                title.isNotBlank()
            ) {
                "Continuity title cannot be empty."
            }

            require(
                content.isNotBlank()
            ) {
                "Continuity content cannot be empty."
            }

            require(
                content.length <=
                    MAX_CONTENT_LENGTH
            ) {
                "Continuity content is too large."
            }

            val safeTitle =
                sanitizeTitle(title)

            val safeCategory =
                sanitizeCategory(category)

            require(
                isSafeProjectData(
                    safeTitle,
                    content
                )
            ) {
                "Protected credential detected. Record rejected."
            }

            require(
                ensureDirectories(context)
            ) {
                "Z Continuity storage could not be initialized."
            }

            val recordId =
                "ZC-" +
                    now() +
                    "-" +
                    UUID.randomUUID()
                        .toString()

            val timestamp =
                now()

            val payload =
                JSONObject()
                    .put(
                        "schema_version",
                        SCHEMA_VERSION
                    )
                    .put(
                        "record_id",
                        recordId
                    )
                    .put(
                        "timestamp",
                        timestamp
                    )
                    .put(
                        "category",
                        safeCategory
                    )
                    .put(
                        "title",
                        safeTitle
                    )
                    .put(
                        "content",
                        content
                    )

            val payloadText =
                payload.toString()

            val record =
                JSONObject()
                    .put(
                        "schema_version",
                        SCHEMA_VERSION
                    )
                    .put(
                        "record_id",
                        recordId
                    )
                    .put(
                        "timestamp",
                        timestamp
                    )
                    .put(
                        "category",
                        safeCategory
                    )
                    .put(
                        "checksum",
                        checksum(
                            payloadText
                        )
                    )
                    .put(
                        "payload",
                        payloadText
                    )

            val encrypted =
                VaultCrypto.encrypt(
                    record.toString()
                )

            val finalFile =
                File(
                    recordsDirectory(context),
                    "$recordId.zcr"
                )

            val tempFile =
                File(
                    tempDirectory(context),
                    "$recordId.tmp"
                )

            tempFile.writeText(
                encrypted,
                StandardCharsets.UTF_8
            )

            require(
                tempFile.exists() &&
                    tempFile.length() > 0
            ) {
                "Temporary continuity record was not written."
            }

            if (
                finalFile.exists() &&
                !finalFile.delete()
            ) {
                throw IllegalStateException(
                    "Existing continuity record could not be replaced."
                )
            }

            require(
                tempFile.renameTo(finalFile)
            ) {
                "Continuity record could not be committed."
            }

            recordId

        }.getOrNull()
    }

    fun recordBuild(
        context: Context,
        title: String,
        content: String
    ): String? {
        return recordProjectEvent(
            context,
            CATEGORY_BUILD,
            title,
            content
        )
    }

    fun recordFailure(
        context: Context,
        title: String,
        content: String
    ): String? {
        return recordProjectEvent(
            context,
            CATEGORY_FAILURE,
            title,
            content
        )
    }

    fun recordDecision(
        context: Context,
        title: String,
        content: String
    ): String? {
        return recordProjectEvent(
            context,
            CATEGORY_DECISION,
            title,
            content
        )
    }

    fun recordDiagnostic(
        context: Context,
        title: String,
        content: String
    ): String? {
        return recordProjectEvent(
            context,
            CATEGORY_DIAGNOSTIC,
            title,
            content
        )
    }

    fun recordRecoveryPoint(
        context: Context,
        title: String,
        content: String
    ): String? {
        return recordProjectEvent(
            context,
            CATEGORY_RECOVERY,
            title,
            content
        )
    }

    fun recordConfiguration(
        context: Context,
        title: String,
        content: String
    ): String? {
        return recordProjectEvent(
            context,
            CATEGORY_CONFIGURATION,
            title,
            content
        )
    }

    fun recordToolEvent(
        context: Context,
        title: String,
        content: String
    ): String? {
        return recordProjectEvent(
            context,
            CATEGORY_TOOL,
            title,
            content
        )
    }

    fun recordAtlasContext(
        context: Context,
        title: String,
        content: String
    ): String? {
        return recordProjectEvent(
            context,
            CATEGORY_ATLAS,
            title,
            content
        )
    }

    fun readRecord(
        context: Context,
        recordId: String
    ): String? {
        return runCatching {

            require(
                recordId.matches(
                    Regex(
                        "^ZC-[0-9]+-[a-f0-9-]+$"
                    )
                )
            ) {
                "Invalid continuity record ID."
            }

            val file =
                File(
                    recordsDirectory(context),
                    "$recordId.zcr"
                )

            if (!file.exists()) {
                return null
            }

            val encrypted =
                file.readText(
                    StandardCharsets.UTF_8
                )

            val decrypted =
                VaultCrypto.decrypt(
                    encrypted
                )

            val record =
                JSONObject(decrypted)

            val payloadText =
                record.getString(
                    "payload"
                )

            val storedChecksum =
                record.getString(
                    "checksum"
                )

            require(
                checksum(
                    payloadText
                ) == storedChecksum
            ) {
                "Continuity record integrity check failed."
            }

            payloadText

        }.getOrNull()
    }

    fun listRecordIds(
        context: Context
    ): List<String> {
        return runCatching {

            if (
                !ensureDirectories(context)
            ) {
                return emptyList()
            }

            recordsDirectory(context)
                .listFiles()
                ?.filter {
                    it.isFile &&
                        it.name.endsWith(
                            ".zcr"
                        )
                }
                ?.mapNotNull {
                    it.name.removeSuffix(
                        ".zcr"
                    )
                }
                ?.sortedDescending()
                ?: emptyList()

        }.getOrDefault(
            emptyList()
        )
    }

    fun deleteRecord(
        context: Context,
        recordId: String
    ): Boolean {
        return runCatching {

            require(
                recordId.matches(
                    Regex(
                        "^ZC-[0-9]+-[a-f0-9-]+$"
                    )
                )
            ) {
                "Invalid continuity record ID."
            }

            val file =
                File(
                    recordsDirectory(context),
                    "$recordId.zcr"
                )

            if (!file.exists()) {
                return true
            }

            file.delete()

        }.getOrDefault(false)
    }

    fun clearAll(
        context: Context
    ): Boolean {
        return runCatching {

            if (
                !ensureDirectories(context)
            ) {
                return false
            }

            var success = true

            recordsDirectory(context)
                .listFiles()
                ?.forEach { file ->
                    if (
                        file.isFile &&
                        !file.delete()
                    ) {
                        success = false
                    }
                }

            tempDirectory(context)
                .listFiles()
                ?.forEach { file ->
                    if (
                        file.isFile &&
                        !file.delete()
                    ) {
                        success = false
                    }
                }

            success

        }.getOrDefault(false)
    }

    fun getRecordCount(
        context: Context
    ): Int {
        return runCatching {
            listRecordIds(context).size
        }.getOrDefault(0)
    }
}
