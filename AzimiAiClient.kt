package com.azimi.guardian

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AzimiAiClient {

    private const val API_ENDPOINT =
        "https://azimi-studio-unique-vercel-coral.vercel.app/api/chat"

    private const val USER_AGENT =
        "AZIMI-Guardian/1.0"

    private const val CONNECT_TIMEOUT =
        15_000

    private const val READ_TIMEOUT =
        30_000

    private const val MAX_MESSAGE_LENGTH =
        12_000

    private const val MAX_MEMORY_ITEMS =
        50

    private const val MAX_MEMORY_ITEM_LENGTH =
        4_000

    data class AIResponse(
        val success: Boolean,
        val reply: String,
        val assistant: String,
        val engine: String,
        val model: String,
        val fallback: Boolean,
        val status: String,
        val error: String? = null
    )

    data class ChatMessage(
        val role: String,
        val content: String
    )

    /**
     * Guardian-authorized Atlas request.
     *
     * No Supabase access token is accepted.
     *
     * No website authentication is required.
     */
    fun ask(
        context: Context,
        message: String,
        history: List<ChatMessage> = emptyList(),
        memory: List<ChatMessage> = emptyList(),
        onResult: (AIResponse) -> Unit
    ) {

        val appContext =
            context.applicationContext

        val result =
            askBlocking(
                appContext,
                message,
                history,
                memory
            )

        onResult(result)
    }

    /**
     * Synchronous implementation executed by the existing
     * provider callback path.
     *
     * If this function is eventually called from the main
     * thread, it should be moved to a background dispatcher.
     */
    private fun askBlocking(
        context: Context,
        message: String,
        history: List<ChatMessage>,
        memory: List<ChatMessage>
    ): AIResponse {

        val cleanMessage =
            message.trim()

        if (
            cleanMessage.isBlank()
        ) {
            return failure(
                "Message cannot be empty."
            )
        }

        if (
            cleanMessage.length >
            MAX_MESSAGE_LENGTH
        ) {
            return failure(
                "Message is too long."
            )
        }

        /*
         * Existing AZIMI protected-credential gate remains.
         */
        if (
            AzimiAuth.isProtectedCredential(
                cleanMessage
            )
        ) {
            return failure(
                "Protected information was blocked by AZIMI Security Gate."
            )
        }

        /*
         * Online Atlas requires an active local Guardian
         * owner session.
         */
        if (
            !AtlasOwnerAuthority.hasOwnerAuthorization(
                context
            )
        ) {
            return failure(
                "Guardian owner authorization is required."
            )
        }

        /*
         * Make sure the Guardian installation has its
         * device-bound cryptographic identity.
         */
        if (
            !GuardianAtlasIdentity.ensureIdentity(
                context
            )
        ) {
            return failure(
                "Guardian Atlas cryptographic identity is unavailable."
            )
        }

        val safeHistory =
            sanitizeHistory(history)

        val safeMemory =
            sanitizeMemory(memory)

        /*
         * Build the exact body once.
         *
         * The exact same string is:
         *
         * 1. hashed
         * 2. signed
         * 3. sent to Vercel
         *
         * This prevents body substitution.
         */
        val body =
            JSONObject().apply {

                put(
                    "message",
                    cleanMessage
                )

                put(
                    "history",
                    historyToJson(
                        safeHistory
                    )
                )

                put(
                    "memory",
                    historyToJson(
                        safeMemory
                    )
                )

            }.toString()

        val timestamp =
            System.currentTimeMillis()

        val requestId =
            GuardianAtlasIdentity
                .createRequestId()

        val signature =
            GuardianAtlasIdentity.signRequest(
                context = context,
                timestamp = timestamp,
                requestId = requestId,
                body = body
            )

        if (
            signature.isNullOrBlank()
        ) {
            return failure(
                "Guardian could not authorize the Atlas request."
            )
        }

        return runCatching {

            val connection =
                URL(API_ENDPOINT)
                    .openConnection()
                    as HttpURLConnection

            try {

                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    CONNECT_TIMEOUT

                connection.readTimeout =
                    READ_TIMEOUT

                connection.doOutput =
                    true

                connection.useCaches =
                    false

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
                )

                /*
                 * Guardian cryptographic identity.
                 */
                connection.setRequestProperty(
                    "X-AZIMI-GUARDIAN-KEY-ID",
                    GuardianAtlasIdentity.KEY_ID
                )

                connection.setRequestProperty(
                    "X-AZIMI-GUARDIAN-TIMESTAMP",
                    timestamp.toString()
                )

                connection.setRequestProperty(
                    "X-AZIMI-GUARDIAN-REQUEST-ID",
                    requestId
                )

                connection.setRequestProperty(
                    "X-AZIMI-GUARDIAN-SIGNATURE",
                    signature
                )

                connection.outputStream
                    .bufferedWriter(
                        Charsets.UTF_8
                    )
                    .use { writer ->

                        writer.write(body)
                        writer.flush()
                    }

                val responseCode =
                    connection.responseCode

                val responseText =
                    if (
                        responseCode in 200..299
                    ) {

                        connection.inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    } else {

                        connection.errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }
                            ?: ""
                    }

                parseResponse(
                    responseCode,
                    responseText
                )

            } finally {

                connection.disconnect()
            }

        }.getOrElse {

            failure(
                "Atlas connection failed."
            )
        }
    }

    private fun sanitizeHistory(
        history: List<ChatMessage>
    ): List<ChatMessage> {

        return history
            .takeLast(12)
            .mapNotNull { item ->

                val role =
                    item.role.trim()

                val content =
                    item.content.trim()

                if (
                    role != "user" &&
                    role != "assistant"
                ) {
                    return@mapNotNull null
                }

                if (
                    content.isBlank()
                ) {
                    return@mapNotNull null
                }

                if (
                    content.length > 4_000
                ) {
                    return@mapNotNull null
                }

                if (
                    AzimiAuth.isProtectedCredential(
                        content
                    )
                ) {
                    return@mapNotNull null
                }

                ChatMessage(
                    role = role,
                    content = content
                )
            }
    }

    private fun sanitizeMemory(
        memory: List<ChatMessage>
    ): List<ChatMessage> {

        return memory
            .takeLast(MAX_MEMORY_ITEMS)
            .mapNotNull { item ->

                val role =
                    item.role.trim()

                val content =
                    item.content.trim()

                if (
                    role != "user" &&
                    role != "assistant" &&
                    role != "system"
                ) {
                    return@mapNotNull null
                }

                if (
                    content.isBlank()
                ) {
                    return@mapNotNull null
                }

                if (
                    content.length >
                    MAX_MEMORY_ITEM_LENGTH
                ) {
                    return@mapNotNull null
                }

                if (
                    AzimiAuth.isProtectedCredential(
                        content
                    )
                ) {
                    return@mapNotNull null
                }

                ChatMessage(
                    role = role,
                    content = content
                )
            }
    }

    private fun historyToJson(
        history: List<ChatMessage>
    ): JSONArray {

        val array =
            JSONArray()

        history.forEach { item ->

            array.put(
                JSONObject().apply {

                    put(
                        "role",
                        item.role
                    )

                    put(
                        "content",
                        item.content
                    )
                }
            )
        }

        return array
    }

    private fun parseResponse(
        responseCode: Int,
        responseText: String
    ): AIResponse {

        if (
            responseCode !in 200..299
        ) {

            return failure(
                when (responseCode) {

                    401 ->
                        "Guardian Atlas authentication required."

                    403 ->
                        "Guardian Atlas access denied."

                    408 ->
                        "Guardian Atlas request expired."

                    429 ->
                        "Atlas request limit reached."

                    502 ->
                        "Atlas AI engine unavailable."

                    else ->
                        "Atlas gateway error: $responseCode"
                }
            )
        }

        if (
            responseText.isBlank()
        ) {
            return failure(
                "Atlas returned an empty response."
            )
        }

        return runCatching {

            val json =
                JSONObject(
                    responseText
                )

            val reply =
                json.optString(
                    "reply",
                    ""
                )

            if (
                reply.isBlank()
            ) {
                return failure(
                    "Atlas returned no reply."
                )
            }

            AIResponse(
                success = true,
                reply = reply,
                assistant =
                    json.optString(
                        "assistant",
                        "ATLAS CORE"
                    ),
                engine =
                    json.optString(
                        "engine",
                        "UNKNOWN"
                    ),
                model =
                    json.optString(
                        "model",
                        "UNKNOWN"
                    ),
                fallback =
                    json.optBoolean(
                        "fallback",
                        false
                    ),
                status =
                    json.optString(
                        "status",
                        "UNKNOWN"
                    )
            )

        }.getOrElse {

            failure(
                "Atlas response could not be understood."
            )
        }
    }

    private fun failure(
        message: String
    ): AIResponse {

        return AIResponse(
            success = false,
            reply = "",
            assistant = "ATLAS CORE",
            engine = "NONE",
            model = "NONE",
            fallback = true,
            status = "ERROR",
            error = message
        )
    }
}
