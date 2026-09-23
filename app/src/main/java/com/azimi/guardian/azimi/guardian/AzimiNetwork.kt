package com.azimi.guardian

import android.content.Context

object AzimiNetwork {

    /**
     * Guardian-authorized Atlas AI request.
     *
     * No Supabase session is required.
     * No email authentication is required.
     */
    fun askAI(
        context: Context,
        message: String,
        history: List<AzimiAiClient.ChatMessage> = emptyList(),
        memory: List<AzimiAiClient.ChatMessage> = emptyList(),
        onResult: (AzimiAiClient.AIResponse) -> Unit
    ) {

        val result =
            AzimiAiClient.ask(
                context = context.applicationContext,
                message = message,
                history = history,
                memory = memory
            )

        onResult(result)
    }

    /*
     * Keep your existing requestMagicLink() and
     * handleCallback() functions below this point
     * unchanged.
     */
}
