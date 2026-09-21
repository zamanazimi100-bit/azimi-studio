package com.azimi.guardian

import android.content.Context
import java.util.Locale

object ZLanguage {

    enum class Language {
        ENGLISH,
        DARI
    }

    private const val PREFS =
        "azimi_language_settings"

    private const val LANGUAGE_KEY =
        "selected_language"

    fun getLanguage(
        context: Context
    ): Language {

        val saved =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
                .getString(
                    LANGUAGE_KEY,
                    Language.ENGLISH.name
                )

        return runCatching {
            Language.valueOf(saved ?: Language.ENGLISH.name)
        }.getOrDefault(Language.ENGLISH)
    }

    fun setLanguage(
        context: Context,
        language: Language
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                LANGUAGE_KEY,
                language.name
            )
            .apply()
    }

    fun toggle(
        context: Context
    ): Language {

        val next =
            when (getLanguage(context)) {
                Language.ENGLISH -> Language.DARI
                Language.DARI -> Language.ENGLISH
            }

        setLanguage(
            context,
            next
        )

        return next
    }

    fun isDari(
        context: Context
    ): Boolean {
        return getLanguage(context) ==
            Language.DARI
    }

    fun direction(
        context: Context
    ): Int {
        return if (isDari(context)) {
            android.view.Gravity.RIGHT
        } else {
            android.view.Gravity.LEFT
        }
    }

    fun text(
        context: Context,
        english: String,
        dari: String
    ): String {
        return if (isDari(context)) {
            dari
        } else {
            english
        }
    }
}
