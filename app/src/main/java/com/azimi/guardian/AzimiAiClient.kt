package com.azimi.guardian

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
     * Sends a normal user message to AZIMI's protected
     * server gateway.
     *
     * Guardian never talks directly to an external AI
     * provider.
     *
     * Guardian also never sends:
     * - ATLAS_INTERNAL_SECRET
     * - service-role keys
     * - provider API keys
     */
    fun ask(
        accessToken: String,
        message: String,
        history: List<ChatMessage> = emptyList()
    ): AIResponse {

        val cleanToken =
            accessToken.trim()

        val cleanMessage =
            message.trim()

        if (cleanToken.isBlank()) {
            return failure(
                "Authentication token is missing."
            )
        }

        if (cleanMessage.isBlank()) {
            return failure(
                "Message cannot be empty."
            )
        }

        if (cleanMessage.length > MAX_MESSAGE_LENGTH) {
            return failure(
                "Message is too long."
            )
        }

        /*
         * Security gate.
         *
         * Protected credentials should never reach Atlas.
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

        val safeHistory =
            sanitizeHistory(history)

        return runCatching {

            val connection =
                URL(API_ENDPOINT)
                    .openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.connectTimeout =
                    CONNECT_TIMEOUT
                connection.readTimeout =
                    READ_TIMEOUT

                connection.doOutput = true
                connection.useCaches = false

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $cleanToken"
                )

                connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
                )

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

                        /*
                         * Guardian does not automatically
                         * send private local memory.
                         *
                         * Memory will be connected through
                         * an explicit AZIMI memory boundary.
                         */
                        put(
                            "memory",
                            JSONArray()
                        )

                    }.toString()

                connection.outputStream
                    .bufferedWriter()
                    .use { writer ->
                        writer.write(body)
                        writer.flush()
                    }

                val responseCode =
                    connection.responseCode

                val responseText =
                    if (responseCode in 200..299) {
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

                if (content.isBlank()) {
                    return@mapNotNull null
                }

                if (
                    content.length >
                    4_000
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
                        "Atlas authentication required."

                    403 ->
                        "Atlas access denied."

                    429 ->
                        "Atlas request limit reached."

                    502 ->
                        "Atlas AI engine unavailable."

                    else ->
                        "Atlas gateway error: $responseCode"
                }
            )
        }

        if (responseText.isBlank()) {
            return failure(
                "Atlas returned an empty response."
            )
        }

        return runCatching {

            val json =
                JSONObject(responseText)

            val reply =
                json.optString(
                    "reply",
                    ""
                )

            if (reply.isBlank()) {
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
