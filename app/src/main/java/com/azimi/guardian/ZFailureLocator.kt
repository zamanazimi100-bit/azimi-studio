package com.azimi.guardian

import android.content.Context
import org.json.JSONObject
import java.util.UUID

object ZFailureLocator {

    private const val MAX_MESSAGE_LENGTH = 1000
    private const val MAX_STACK_LENGTH = 4000

    private const val CATEGORY_FAILURE = "FAILURE"

    data class FailureInfo(
        val failureId: String,
        val timestamp: Long,
        val component: String,
        val file: String,
        val function: String,
        val stage: String,
        val exceptionType: String,
        val message: String,
        val line: Int?,
        val stackTrace: String?
    )

    fun recordFailure(
        context: Context,
        component: String,
        file: String,
        function: String,
        stage: String,
        throwable: Throwable? = null,
        message: String? = null
    ): String? {

        return runCatching {

            val failureId =
                "ZF-" +
                    System.currentTimeMillis() +
                    "-" +
                    UUID.randomUUID()
                        .toString()

            val timestamp =
                System.currentTimeMillis()

            val exceptionType =
                throwable
                    ?.javaClass
                    ?.simpleName
                    ?.take(200)
                    ?: "NONE"

            val rawMessage =
                message
                    ?: throwable?.message
                    ?: "Unknown failure"

            val safeMessage =
                sanitize(
                    rawMessage,
                    MAX_MESSAGE_LENGTH
                )

            val stackElement =
                throwable
                    ?.stackTrace
                    ?.firstOrNull()

            val detectedFile =
                stackElement
                    ?.fileName
                    ?: file

            val detectedFunction =
                stackElement
                    ?.methodName
                    ?: function

            val detectedLine =
                stackElement
                    ?.lineNumber
                    ?.takeIf {
                        it >= 0
                    }

            val stackTrace =
                throwable
                    ?.stackTraceToString()
                    ?.let {
                        sanitize(
                            it,
                            MAX_STACK_LENGTH
                        )
                    }

            val info =
                FailureInfo(
                    failureId = failureId,
                    timestamp = timestamp,
                    component =
                        sanitize(
                            component,
                            200
                        ),
                    file =
                        sanitize(
                            detectedFile,
                            300
                        ),
                    function =
                        sanitize(
                            detectedFunction,
                            300
                        ),
                    stage =
                        sanitize(
                            stage,
                            300
                        ),
                    exceptionType =
                        exceptionType,
                    message =
                        safeMessage,
                    line =
                        detectedLine,
                    stackTrace =
                        stackTrace
                )

            val content =
                JSONObject()
                    .put(
                        "failure_id",
                        info.failureId
                    )
                    .put(
                        "timestamp",
                        info.timestamp
                    )
                    .put(
                        "component",
                        info.component
                    )
                    .put(
                        "file",
                        info.file
                    )
                    .put(
                        "function",
                        info.function
                    )
                    .put(
                        "stage",
                        info.stage
                    )
                    .put(
                        "exception_type",
                        info.exceptionType
                    )
                    .put(
                        "message",
                        info.message
                    )
                    .put(
                        "line",
                        info.line
                    )
                    .put(
                        "stack_trace",
                        info.stackTrace
                    )
                    .toString()

            ZContinuityStorage.recordProjectEvent(
                context = context,
                category = CATEGORY_FAILURE,
                title =
                    "$component failure at $stage",
                content = content
            ) ?: return null

        }.getOrNull()
    }

    fun recordCheckpoint(
        context: Context,
        component: String,
        file: String,
        function: String,
        stage: String
    ): String? {

        return runCatching {

            val content =
                JSONObject()
                    .put(
                        "timestamp",
                        System.currentTimeMillis()
                    )
                    .put(
                        "component",
                        sanitize(
                            component,
                            200
                        )
                    )
                    .put(
                        "file",
                        sanitize(
                            file,
                            300
                        )
                    )
                    .put(
                        "function",
                        sanitize(
                            function,
                            300
                        )
                    )
                    .put(
                        "stage",
                        sanitize(
                            stage,
                            300
                        )
                    )
                    .put(
                        "status",
                        "REACHED"
                    )
                    .toString()

            ZContinuityStorage.recordDiagnostic(
                context = context,
                title =
                    "$component checkpoint: $stage",
                content = content
            )

        }.getOrNull()
    }

    fun recordSuccess(
        context: Context,
        component: String,
        file: String,
        function: String,
        stage: String
    ): String? {

        return runCatching {

            val content =
                JSONObject()
                    .put(
                        "timestamp",
                        System.currentTimeMillis()
                    )
                    .put(
                        "component",
                        sanitize(
                            component,
                            200
                        )
                    )
                    .put(
                        "file",
                        sanitize(
                            file,
                            300
                        )
                    )
                    .put(
                        "function",
                        sanitize(
                            function,
                            300
                        )
                    )
                    .put(
                        "stage",
                        sanitize(
                            stage,
                            300
                        )
                    )
                    .put(
                        "status",
                        "SUCCESS"
                    )
                    .toString()

            ZContinuityStorage.recordDiagnostic(
                context = context,
                title =
                    "$component completed: $stage",
                content = content
            )

        }.getOrNull()
    }

    fun describe(
        throwable: Throwable?
    ): String {

        return runCatching {

            if (throwable == null) {
                return "NONE"
            }

            val element =
                throwable
                    .stackTrace
                    .firstOrNull()

            val file =
                element
                    ?.fileName
                    ?: "UNKNOWN_FILE"

            val function =
                element
                    ?.methodName
                    ?: "UNKNOWN_FUNCTION"

            val line =
                element
                    ?.lineNumber
                    ?.takeIf {
                        it >= 0
                    }
                    ?.toString()
                    ?: "UNKNOWN_LINE"

            val type =
                throwable
                    .javaClass
                    .simpleName

            val message =
                sanitize(
                    throwable.message
                        ?: "Unknown failure",
                    500
                )

            "$type: $message at $file:$line ($function)"

        }.getOrDefault(
            "Unable to describe failure."
        )
    }

    private fun sanitize(
        value: String,
        maxLength: Int
    ): String {

        return runCatching {

            value
                .replace(
                    Regex(
                        "(?i)(password|passwd|secret|api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret|private[_-]?key|service[_-]?role[_-]?key)\\s*[=:]\\s*[^\\s,;]+"
                    ),
                    "$1=[REDACTED]"
                )
                .replace(
                    Regex(
                        "(?i)bearer\\s+[A-Za-z0-9._~+/=-]+"
                    ),
                    "Bearer [REDACTED]"
                )
                .take(maxLength)

        }.getOrDefault(
            "[REDACTED]"
        )
    }
}
