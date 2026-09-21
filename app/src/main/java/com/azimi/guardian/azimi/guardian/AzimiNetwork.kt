package com.azimi.guardian

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object AzimiNetwork {

    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    fun requestMagicLink(
        context: Context,
        email: String,
        onResult: (AzimiAuth.AuthResult) -> Unit
    ) {
        executor.execute {
            val result = AzimiAuth.requestMagicLink(
                context.applicationContext,
                email
            )

            mainHandler.post {
                onResult(result)
            }
        }
    }

    fun handleCallback(
        context: Context,
        uri: Uri,
        onResult: (AzimiAuth.AuthResult) -> Unit
    ) {
        executor.execute {
            val result = AzimiAuth.handleCallback(
                context.applicationContext,
                uri
            )

            mainHandler.post {
                onResult(result)
            }
        }
    }

    fun askAI(
        accessToken: String,
        message: String,
        history: List<AzimiAiClient.ChatMessage>,
        onResult: (AzimiAiClient.AIResponse) -> Unit
    ) {
        executor.execute {
            val result = AzimiAiClient.ask(
                accessToken,
                message,
                history
            )

            mainHandler.post {
                onResult(result)
            }
        }
    }
}
