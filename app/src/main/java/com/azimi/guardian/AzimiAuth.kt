package com.azimi.guardian

import android.content.Context
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

object AzimiAuth {

    private const val AUTH_CONFIG_ENDPOINT =
        "https://azimi-studio-unique-vercel-coral.vercel.app/api/auth-config"

    private const val CALLBACK_SCHEME =
        "azimi"

    private const val CALLBACK_HOST =
        "auth-callback"

    private const val USER_AGENT =
        "AZIMI-Guardian/1.0"

    private const val MAX_RESPONSE_SIZE =
        64 * 1024

    data class AuthConfig(
        val supabaseUrl: String,
        val publishableKey: String
    )

    fun callbackUri(): Uri {
        return Uri.Builder()
            .scheme(CALLBACK_SCHEME)
            .authority(CALLBACK_HOST)
            .build()
    }

    fun loadAuthConfig(): AuthConfig? {
        return runCatching {

            val connection =
                URL(AUTH_CONFIG_ENDPOINT)
                    .openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.useCaches = false

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
                )

                if (connection.responseCode !in 200..299) {
                    return null
                }

                val body =
                    connection.inputStream
                        .bufferedReader()
                        .use { reader ->
                            reader.readText()
                        }

                if (body.length > MAX_RESPONSE_SIZE) {
                    return null
                }

                val json = JSONObject(body)

                val supabaseUrl =
                    json.optString(
                        "supabaseUrl",
                        ""
                    ).trim()

                val publishableKey =
                    json.optString(
                        "supabasePublishableKey",
                        ""
                    ).trim()

                if (
                    supabaseUrl.isBlank() ||
                    publishableKey.isBlank()
                ) {
                    return null
                }

                AuthConfig(
                    supabaseUrl = supabaseUrl,
                    publishableKey = publishableKey
                )

            } finally {
                connection.disconnect()
            }

        }.getOrNull()
    }

    fun isValidEmail(email: String): Boolean {
        val value = email.trim()

        if (value.isBlank()) {
            return false
        }

        if (value.length > 254) {
            return false
        }

        return android.util.Patterns.EMAIL_ADDRESS
            .matcher(value)
            .matches()
    }

    fun isProtectedCredential(value: String): Boolean {
        val text = value.lowercase()

        val protectedMarkers = listOf(
            "sk-",
            "api_key",
            "apikey",
            "password",
            "passwd",
            "access_token",
            "refresh_token",
            "authorization",
            "bearer ",
            "verification code",
            "mfa",
            "recovery code",
            "private key",
            "begin private key"
        )

        return protectedMarkers.any {
            text.contains(it)
        }
    }

    fun sanitizeInput(value: String): String {
        if (isProtectedCredential(value)) {
            return ""
        }

        return value.trim()
    }

    fun hasSession(context: Context): Boolean {
        return false
    }
}
