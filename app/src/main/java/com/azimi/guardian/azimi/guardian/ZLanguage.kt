package com.azimi.guardian

import android.content.Context
import android.view.Gravity
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
            Language.valueOf(
                saved ?: Language.ENGLISH.name
            )
        }.getOrDefault(
            Language.ENGLISH
        )
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
                Language.ENGLISH ->
                    Language.DARI

                Language.DARI ->
                    Language.ENGLISH
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

    fun isEnglish(
        context: Context
    ): Boolean {
        return getLanguage(context) ==
            Language.ENGLISH
    }

    fun direction(
        context: Context
    ): Int {
        return if (isDari(context)) {
            Gravity.RIGHT
        } else {
            Gravity.LEFT
        }
    }

    fun locale(
        context: Context
    ): Locale {
        return if (isDari(context)) {
            Locale("fa", "AF")
        } else {
            Locale.ENGLISH
        }
    }

    fun languageName(
        context: Context
    ): String {
        return if (isDari(context)) {
            "دری"
        } else {
            "English"
        }
    }

    fun languageCode(
        context: Context
    ): String {
        return if (isDari(context)) {
            "fa-AF"
        } else {
            "en"
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

    fun applyDirection(
        context: Context,
        view: android.view.View
    ) {
        view.textDirection =
            if (isDari(context)) {
                android.view.View.TEXT_DIRECTION_RTL
            } else {
                android.view.View.TEXT_DIRECTION_LTR
            }
    }
}
