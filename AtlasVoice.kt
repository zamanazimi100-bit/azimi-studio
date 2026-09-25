package com.azimi.guardian

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * AZIMI Atlas Voice
 *
 * Normal Atlas speech only.
 *
 * IMPORTANT:
 * This is NOT Voice Lock.
 * It does not authenticate the owner.
 * It does not identify a speaker.
 *
 * It only converts an already-approved Atlas response
 * into speech through Android TextToSpeech.
 */
object AtlasVoice {

    private const val UTTERANCE_ID =
        "AZIMI_ATLAS_VOICE"

    private var tts: TextToSpeech? = null

    @Volatile
    private var initialized = false

    @Volatile
    private var enabled = true

    @Volatile
    private var currentLanguage =
        "ENGLISH"

    private var initializationCallback:
        ((Boolean, String) -> Unit)? = null

    fun initialize(
        context: Context,
        onReady: ((Boolean, String) -> Unit)? = null
    ) {

        initializationCallback =
            onReady

        val appContext =
            context.applicationContext

        if (tts != null) {
            onReady?.invoke(
                initialized,
                if (initialized) {
                    "ATLAS VOICE READY"
                } else {
                    "ATLAS VOICE UNAVAILABLE"
                }
            )
            return
        }

        tts =
            TextToSpeech(
                appContext
            ) { status ->

                if (
                    status ==
                    TextToSpeech.SUCCESS
                ) {

                    initialized = true

                    configureLanguage(
                        appContext
                    )

                    initializationCallback?.invoke(
                        true,
                        "ATLAS VOICE READY"
                    )

                } else {

                    initialized = false

                    initializationCallback?.invoke(
                        false,
                        "Android Text-to-Speech initialization failed."
                    )
                }
            }
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    fun setEnabled(
        value: Boolean
    ) {

        enabled =
            value

        if (!value) {
            stop()
        }
    }

    fun toggle(): Boolean {

        setEnabled(
            !enabled
        )

        return enabled
    }

    fun setEnglish(): Boolean {

        return setLanguage(
            Locale.US,
            "ENGLISH"
        )
    }

    fun setDari(): Boolean {

        /*
         * Android TTS engines commonly expose Persian
         * rather than a dedicated Dari voice.
         *
         * We first request fa-AF.
         */
        val dari =
            Locale(
                "fa",
                "AF"
            )

        return setLanguage(
            dari,
            "DARI"
        )
    }

    private fun setLanguage(
        locale: Locale,
        languageName: String
    ): Boolean {

        val engine =
            tts
                ?: return false

        if (!initialized) {
            return false
        }

        val availability =
            engine.isLanguageAvailable(
                locale
            )

        if (
            availability ==
            TextToSpeech.LANG_MISSING_DATA ||
            availability ==
            TextToSpeech.LANG_NOT_SUPPORTED
        ) {

            return false
        }

        val result =
            engine.setLanguage(
                locale
            )

        if (
            result ==
            TextToSpeech.LANG_MISSING_DATA ||
            result ==
            TextToSpeech.LANG_NOT_SUPPORTED
        ) {

            return false
        }

        currentLanguage =
            languageName

        return true
    }

    fun getLanguageName(): String {
        return currentLanguage
    }

    private fun configureLanguage(
        context: Context
    ) {

        if (
            ZLanguage.isDari(
                context
            )
        ) {

            if (
                !setDari()
            ) {
                setEnglish()
            }

        } else {

            setEnglish()
        }
    }

    fun speak(
        context: Context,
        message: String,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {

        val clean =
            message.trim()

        if (clean.isBlank()) {

            onResult?.invoke(
                false,
                "Atlas voice received an empty message."
            )

            return
        }

        if (!enabled) {

            onResult?.invoke(
                false,
                "Atlas Voice is disabled."
            )

            return
        }

        if (!AtlasSession.isActive(context)) {

            onResult?.invoke(
                false,
                "Atlas is sleeping or locked."
            )

            return
        }

        if (!initialized) {

            initialize(
                context
            ) { success, status ->

                if (!success) {

                    onResult?.invoke(
                        false,
                        status
                    )

                    return@initialize
                }

                speakInternal(
                    clean,
                    onResult
                )
            }

            return
        }

        speakInternal(
            clean,
            onResult
        )
    }

    private fun speakInternal(
        message: String,
        onResult: ((Boolean, String) -> Unit)?
    ) {

        val engine =
            tts

        if (engine == null) {

            onResult?.invoke(
                false,
                "Atlas Voice engine is unavailable."
            )

            return
        }

        engine.setSpeechRate(
            1.0f
        )

        engine.setPitch(
            1.0f
        )

        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {

                override fun onStart(
                    utteranceId: String?
                ) {
                    onResult?.invoke(
                        true,
                        "ATLAS SPEAKING"
                    )
                }

                override fun onDone(
                    utteranceId: String?
                ) {
                    onResult?.invoke(
                        true,
                        "ATLAS VOICE READY"
                    )
                }

                override fun onError(
                    utteranceId: String?
                ) {
                    onResult?.invoke(
                        false,
                        "Atlas Voice playback failed."
                    )
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int
                ) {
                    onResult?.invoke(
                        false,
                        "Atlas Voice playback error: $errorCode"
                    )
                }
            }
        )

        val result =
            engine.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                UTTERANCE_ID
            )

        if (
            result ==
            TextToSpeech.ERROR
        ) {

            onResult?.invoke(
                false,
                "Atlas Voice could not start speech."
            )
        }
    }

    fun stop() {

        tts?.stop()
    }

    fun shutdown() {

        stop()

        tts?.shutdown()

        tts =
            null

        initialized =
            false
    }
}
