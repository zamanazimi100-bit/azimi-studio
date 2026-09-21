package com.azimi.guardian

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
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

    private const val CONNECT_TIMEOUT =
        10_000

    private const val READ_TIMEOUT =
        15_000

    private const val AUTH_PREFS =
        "azimi_guardian_auth"

    private const val PKCE_VERIFIER =
        "pkce_verifier"

    private const val ACCESS_TOKEN =
        "access_token"

    private const val REFRESH_TOKEN =
        "refresh_token"

    private const val EXPIRES_AT =
        "expires_at"

    data class AuthConfig(
        val supabaseUrl: String,
        val publishableKey: String
    )

    data class Session(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAt: Long?
    )

    data class AuthResult(
        val success: Boolean,
        val message: String
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
                connection.requestMethod =
                    "GET"

                connection.connectTimeout =
                    CONNECT_TIMEOUT

                connection.readTimeout =
                    READ_TIMEOUT

                connection.useCaches =
                    false

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
                    readLimitedResponse(
                        connection
                    )

                val json =
                    JSONObject(body)

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
                    supabaseUrl =
                        supabaseUrl.trimEnd('/'),
                    publishableKey =
                        publishableKey
                )

            } finally {
                connection.disconnect()
            }

        }.getOrNull()
    }

    /**
     * Starts the Supabase passwordless email flow.
     *
     * The PKCE verifier remains on this device.
     * The verifier is never sent to the email service.
     */
    fun requestMagicLink(
        context: Context,
        email: String
    ): AuthResult {

        val cleanEmail =
            email.trim()

        if (!isValidEmail(cleanEmail)) {
            return AuthResult(
                false,
                "Enter a valid email address."
            )
        }

        return runCatching {

            val config =
                loadAuthConfig()
                    ?: return AuthResult(
                        false,
                        "AZIMI authentication configuration unavailable."
                    )

            val verifier =
                createPkceVerifier()

            val challenge =
                createPkceChallenge(
                    verifier
                )

            savePkceVerifier(
                context,
                verifier
            )

            val endpoint =
                "${config.supabaseUrl}/auth/v1/otp"

            val connection =
                URL(endpoint)
                    .openConnection() as HttpURLConnection

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
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.setRequestProperty(
                    "apikey",
                    config.publishableKey
                )

                connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
                )

                val body =
                    JSONObject().apply {

                        put(
                            "email",
                            cleanEmail
                        )

                        put(
                            "create_user",
                            true
                        )

                        put(
                            "code_challenge",
                            challenge
                        )

                        put(
                            "code_challenge_method",
                            "s256"
                        )

                        put(
                            "redirect_to",
                            callbackUri().toString()
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

                val responseBody =
                    readResponse(
                        connection,
                        responseCode
                    )

                if (
                    responseCode !in 200..299
                ) {

                    clearPkceVerifier(
                        context
                    )

                    return AuthResult(
                        false,
                        parseAuthError(
                            responseBody,
                            responseCode
                        )
                    )
                }

                AuthResult(
                    true,
                    "Magic link sent. Open the email on this device."
                )

            } finally {
                connection.disconnect()
            }

        }.getOrElse {

            clearPkceVerifier(
                context
            )

            AuthResult(
                false,
                "Could not start AZIMI authentication."
            )
        }
    }

    /**
     * Handles:
     *
     * azimi://auth-callback?code=...
     *
     * The code is exchanged for the Supabase session.
     */
    fun handleCallback(
        context: Context,
        uri: Uri
    ): AuthResult {

        if (
            uri.scheme != CALLBACK_SCHEME ||
            uri.host != CALLBACK_HOST
        ) {
            return AuthResult(
                false,
                "Invalid AZIMI authentication callback."
            )
        }

        val error =
            uri.getQueryParameter(
                "error"
            )

        if (!error.isNullOrBlank()) {

            clearPkceVerifier(
                context
            )

            return AuthResult(
                false,
                "Authentication was not completed."
            )
        }

        val code =
            uri.getQueryParameter(
                "code"
            )?.trim()

        if (code.isNullOrBlank()) {

            clearPkceVerifier(
                context
            )

            return AuthResult(
                false,
                "Authentication code missing."
            )
        }

        val verifier =
            loadPkceVerifier(
                context
            )

        if (verifier.isNullOrBlank()) {

            return AuthResult(
                false,
                "Authentication verifier is missing. Start login again."
            )
        }

        return runCatching {

            val config =
                loadAuthConfig()
                    ?: return AuthResult(
                        false,
                        "AZIMI authentication configuration unavailable."
                    )

            val endpoint =
                "${config.supabaseUrl}/auth/v1/token?grant_type=pkce"

            val connection =
                URL(endpoint)
                    .openConnection() as HttpURLConnection

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
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                connection.setRequestProperty(
                    "apikey",
                    config.publishableKey
                )

                connection.setRequestProperty(
                    "User-Agent",
                    USER_AGENT
                )

                val body =
                    JSONObject().apply {

                        put(
                            "auth_code",
                            code
                        )

                        put(
                            "code_verifier",
                            verifier
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

                val responseBody =
                    readResponse(
                        connection,
                        responseCode
                    )

                if (
                    responseCode !in 200..299
                ) {

                    clearPkceVerifier(
                        context
                    )

                    return AuthResult(
                        false,
                        parseAuthError(
                            responseBody,
                            responseCode
                        )
                    )
                }

                val json =
                    JSONObject(
                        responseBody
                    )

                val accessToken =
                    json.optString(
                        "access_token",
                        ""
                    ).trim()

                val refreshToken =
                    json.optString(
                        "refresh_token",
                        ""
                    ).trim()

                val expiresIn =
                    json.optLong(
                        "expires_in",
                        0L
                    )

                if (accessToken.isBlank()) {

                    clearPkceVerifier(
                        context
                    )

                    return AuthResult(
                        false,
                        "Supabase returned no access token."
                    )
                }

                saveSession(
                    context,
                    Session(
                        accessToken =
                            accessToken,
                        refreshToken =
                            refreshToken.ifBlank {
                                null
                            },
                        expiresAt =
                            if (expiresIn > 0L) {
                                System.currentTimeMillis() +
                                    expiresIn * 1000L
                            } else {
                                null
                            }
                    )
                )

                clearPkceVerifier(
                    context
                )

                AuthResult(
                    true,
                    "AZIMI authentication successful."
                )

            } finally {
                connection.disconnect()
            }

        }.getOrElse {

            clearPkceVerifier(
                context
            )

            AuthResult(
                false,
                "Could not complete AZIMI authentication."
            )
        }
    }

    fun getAccessToken(
        context: Context
    ): String? {

        return runCatching {

            context.getSharedPreferences(
                AUTH_PREFS,
                Context.MODE_PRIVATE
            )
                .getString(
                    ACCESS_TOKEN,
                    null
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        }.getOrNull()
    }

    fun getSession(
        context: Context
    ): Session? {

        val token =
            getAccessToken(
                context
            )
                ?: return null

        val refreshToken =
            context.getSharedPreferences(
                AUTH_PREFS,
                Context.MODE_PRIVATE
            )
                .getString(
                    REFRESH_TOKEN,
                    null
                )

        val expiresAt =
            context.getSharedPreferences(
                AUTH_PREFS,
                Context.MODE_PRIVATE
            )
                .getLong(
                    EXPIRES_AT,
                    0L
                )

        return Session(
            accessToken =
                token,
            refreshToken =
                refreshToken,
            expiresAt =
                if (expiresAt > 0L) {
                    expiresAt
                } else {
                    null
                }
        )
    }

    fun hasSession(
        context: Context
    ): Boolean {

        return getAccessToken(
            context
        ) != null
    }

    /**
     * Ends the local AZIMI authentication session.
     *
     * No remote credential material is retained by Guardian.
     */
    fun signOut(
        context: Context
    ) {

        context.getSharedPreferences(
            AUTH_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(ACCESS_TOKEN)
            .remove(REFRESH_TOKEN)
            .remove(EXPIRES_AT)
            .remove(PKCE_VERIFIER)
            .commit()
    }

    fun isValidEmail(
        email: String
    ): Boolean {

        val value =
            email.trim()

        if (value.isBlank()) {
            return false
        }

        if (value.length > 254) {
            return false
        }

        return android.util.Patterns
            .EMAIL_ADDRESS
            .matcher(value)
            .matches()
    }

    fun isProtectedCredential(
        value: String
    ): Boolean {

        val text =
            value.lowercase()

        val protectedMarkers =
            listOf(
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

    fun sanitizeInput(
        value: String
    ): String {

        if (
            isProtectedCredential(
                value
            )
        ) {
            return ""
        }

        return value.trim()
    }

    private fun createPkceVerifier(): String {

        val bytes =
            ByteArray(32)

        SecureRandom().nextBytes(
            bytes
        )

        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or
                Base64.NO_WRAP or
                Base64.NO_PADDING
        )
    }

    private fun createPkceChallenge(
        verifier: String
    ): String {

        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )
                .digest(
                    verifier.toByteArray(
                        Charsets.US_ASCII
                    )
                )

        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or
                Base64.NO_WRAP or
                Base64.NO_PADDING
        )
    }

    private fun savePkceVerifier(
        context: Context,
        verifier: String
    ) {

        context.getSharedPreferences(
            AUTH_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                PKCE_VERIFIER,
                verifier
            )
            .commit()
    }

    private fun loadPkceVerifier(
        context: Context
    ): String? {

        return context.getSharedPreferences(
            AUTH_PREFS,
            Context.MODE_PRIVATE
        )
            .getString(
                PKCE_VERIFIER,
                null
            )
    }

    private fun clearPkceVerifier(
        context: Context
    ) {

        context.getSharedPreferences(
            AUTH_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove(
                PKCE_VERIFIER
            )
            .commit()
    }

    private fun saveSession(
        context: Context,
        session: Session
    ) {

        context.getSharedPreferences(
            AUTH_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                ACCESS_TOKEN,
                session.accessToken
            )
            .putString(
                REFRESH_TOKEN,
                session.refreshToken
            )
            .putLong(
                EXPIRES_AT,
                session.expiresAt ?: 0L
            )
            .commit()
    }

    private fun readLimitedResponse(
        connection: HttpURLConnection
    ): String {

        val input =
            connection.inputStream

        val reader =
            BufferedReader(
                InputStreamReader(
                    input,
                    Charsets.UTF_8
                )
            )

        val builder =
            StringBuilder()

        reader.use {

            var total =
                0

            while (true) {

                val line =
                    it.readLine()
                        ?: break

                total +=
                    line.length + 1

                if (
                    total > MAX_RESPONSE_SIZE
                ) {
                    throw IllegalStateException(
                        "Authentication response too large."
                    )
                }

                builder
                    .append(line)
                    .append('\n')
            }
        }

        return builder.toString()
    }

    private fun readResponse(
        connection: HttpURLConnection,
        responseCode: Int
    ): String {

        val stream =
            if (
                responseCode in 200..299
            ) {
                connection.inputStream
            } else {
                connection.errorStream
            }
                ?: return ""

        val reader =
            BufferedReader(
                InputStreamReader(
                    stream,
                    Charsets.UTF_8
                )
            )

        return reader.use {

            val builder =
                StringBuilder()

            var total =
                0

            while (true) {

                val line =
                    it.readLine()
                        ?: break

                total +=
                    line.length + 1

                if (
                    total > MAX_RESPONSE_SIZE
                ) {
                    break
                }

                builder
                    .append(line)
                    .append('\n')
            }

            builder.toString()
        }
    }

    private fun parseAuthError(
        responseBody: String,
        responseCode: Int
    ): String {

        if (responseBody.isBlank()) {
            return "Authentication request failed: $responseCode"
        }

        return runCatching {

            val json =
                JSONObject(
                    responseBody
                )

            json.optString(
                "msg",
                json.optString(
                    "message",
                    "Authentication request failed: $responseCode"
                )
            )

        }.getOrElse {
            "Authentication request failed: $responseCode"
        }
    }
}
