package com.azimi.guardian

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper

object AzimiNetwork {

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    /**
     * Guardian-authorized Atlas AI request.
     *
     * No Supabase session is required.
     * No email authentication is required.
     *
     * The actual cryptographic Guardian authorization
     * is performed by AzimiAiClient.
     */
    fun askAI(
        context: Context,
        message: String,
        history: List<AzimiAiClient.ChatMessage> =
            emptyList(),
        memory: List<AzimiAiClient.ChatMessage> =
            emptyList(),
        onResult:
            (AzimiAiClient.AIResponse) -> Unit
    ) {

        val appContext =
            context.applicationContext

        AzimiAiClient.ask(
            context = appContext,
            message = message,
            history = history,
            memory = memory
        ) { result ->

            onResult(
                result
            )
        }
    }

    // ============================================================
    // OPTIONAL AZIMI STUDIO CLOUD IDENTITY
    // ============================================================
    //
    // These functions are ONLY for the optional
    // AZIMI Studio / Supabase identity flow.
    //
    // They are NOT required for normal Atlas operation.
    //
    // Atlas itself uses:
    //
    // Guardian owner authorization
    //        ↓
    // Guardian cryptographic identity
    //        ↓
    // AzimiAiClient
    //
    // ============================================================

    /**
     * Starts the optional AZIMI Studio passwordless
     * email authentication flow.
     *
     * Network work runs away from the Android main thread.
     */
    fun requestMagicLink(
        context: Context,
        email: String,
        onResult:
            (AzimiAuth.AuthResult) -> Unit
    ) {

        val appContext =
            context.applicationContext

        Thread {

            val result =
                AzimiAuth.requestMagicLink(
                    appContext,
                    email
                )

            mainHandler.post {
                onResult(
                    result
                )
            }

        }.start()
    }

    /**
     * Handles the optional AZIMI Studio authentication
     * callback.
     *
     * The callback exchanges the PKCE authorization code
     * for the optional Supabase session.
     *
     * This does NOT activate or authorize Atlas.
     */
    fun handleCallback(
        context: Context,
        uri: Uri,
        onResult:
            (AzimiAuth.AuthResult) -> Unit
    ) {

        val appContext =
            context.applicationContext

        Thread {

            val result =
                AzimiAuth.handleCallback(
                    appContext,
                    uri
                )

            mainHandler.post {
                onResult(
                    result
                )
            }

        }.start()
    }
}
