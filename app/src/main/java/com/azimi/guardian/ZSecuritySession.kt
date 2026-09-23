package com.azimi.guardian

import android.content.Context

object ZSecuritySession {

    private const val PREFS =
        "azimi_security_session"

    private const val AUTHENTICATED_KEY =
        "authenticated"

    private const val ACCESS_LEVEL_KEY =
        "access_level"

    private const val AUTH_METHOD_KEY =
        "auth_method"

    private const val OWNER_VERIFIED_KEY =
        "owner_verified"

    fun clear(
        context: Context
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()
    }

    fun save(
        context: Context,
        session: ZSecurity.SecuritySession
    ) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                AUTHENTICATED_KEY,
                session.authenticated
            )
            .putString(
                ACCESS_LEVEL_KEY,
                session.accessLevel.name
            )
            .putString(
                AUTH_METHOD_KEY,
                session.method.name
            )
            .putBoolean(
                OWNER_VERIFIED_KEY,
                session.ownerVerified
            )
            .apply()
    }

    fun get(
        context: Context
    ): ZSecurity.SecuritySession {

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val authenticated =
            prefs.getBoolean(
                AUTHENTICATED_KEY,
                false
            )

        val accessLevel =
            runCatching {
                ZSecurity.AccessLevel.valueOf(
                    prefs.getString(
                        ACCESS_LEVEL_KEY,
                        ZSecurity.AccessLevel.PUBLIC.name
                    ) ?: ZSecurity.AccessLevel.PUBLIC.name
                )
            }.getOrDefault(
                ZSecurity.AccessLevel.PUBLIC
            )

        val method =
            runCatching {
                ZSecurity.AuthenticationMethod.valueOf(
                    prefs.getString(
                        AUTH_METHOD_KEY,
                        ZSecurity.AuthenticationMethod.NONE.name
                    ) ?: ZSecurity.AuthenticationMethod.NONE.name
                )
            }.getOrDefault(
                ZSecurity.AuthenticationMethod.NONE
            )

        val ownerVerified =
            prefs.getBoolean(
                OWNER_VERIFIED_KEY,
                false
            )

        return ZSecurity.SecuritySession(
            authenticated = authenticated,
            accessLevel = accessLevel,
            method = method,
            ownerVerified = ownerVerified
        )
    }

    fun startProtectedSession(
        context: Context,
        method: ZSecurity.AuthenticationMethod
    ) {
        save(
            context,
            ZSecurity.protectedSession(method)
        )
    }

    fun startAuthenticatedSession(
        context: Context,
        method: ZSecurity.AuthenticationMethod
    ) {
        save(
            context,
            ZSecurity.authenticatedSession(method)
        )
    }

    fun startOwnerSession(
        context: Context,
        method: ZSecurity.AuthenticationMethod
    ) {
        save(
            context,
            ZSecurity.ownerSession(method)
        )
    }

    fun startSovereignSession(
        context: Context,
        method: ZSecurity.AuthenticationMethod
    ) {
        save(
            context,
            ZSecurity.sovereignSession(method)
        )
    }

    fun isAuthenticated(
        context: Context
    ): Boolean {
        return get(context).authenticated
    }

    fun isOwnerVerified(
        context: Context
    ): Boolean {
        return get(context).ownerVerified
    }

    fun canAccess(
        context: Context,
        requiredLevel: ZSecurity.AccessLevel
    ): Boolean {
        return ZSecurity.canAccess(
            get(context),
            requiredLevel
        )
    }
}
