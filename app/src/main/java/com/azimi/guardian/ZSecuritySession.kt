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

    private const val VAULT_LOCKED_KEY =
        "vault_locked"

    private const val ATLAS_SLEEPING_KEY =
        "atlas_sleeping"

    fun clear(
        context: Context
    ) {

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .clear()
            .putBoolean(
                VAULT_LOCKED_KEY,
                true
            )
            .putBoolean(
                ATLAS_SLEEPING_KEY,
                true
            )
            .apply()

        closeVaultCompartments(
            context
        )
    }

    fun save(
        context: Context,
        session: ZSecurity.SecuritySession
    ) {

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        prefs.edit()
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
                    )
                        ?: ZSecurity.AccessLevel.PUBLIC.name
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
                    )
                        ?: ZSecurity.AuthenticationMethod.NONE.name
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
            ZSecurity.protectedSession(
                method
            )
        )

        setVaultLocked(
            context,
            true
        )

        setAtlasSleeping(
            context,
            false
        )

        closeVaultCompartments(
            context
        )
    }

    fun startAuthenticatedSession(
        context: Context,
        method: ZSecurity.AuthenticationMethod
    ) {

        save(
            context,
            ZSecurity.authenticatedSession(
                method
            )
        )

        setVaultLocked(
            context,
            true
        )

        setAtlasSleeping(
            context,
            false
        )

        closeVaultCompartments(
            context
        )
    }

    fun startOwnerSession(
        context: Context,
        method: ZSecurity.AuthenticationMethod
    ) {

        save(
            context,
            ZSecurity.ownerSession(
                method
            )
        )

        setVaultLocked(
            context,
            true
        )

        setAtlasSleeping(
            context,
            false
        )

        closeVaultCompartments(
            context
        )
    }

    fun startSovereignSession(
        context: Context,
        method: ZSecurity.AuthenticationMethod
    ) {

        save(
            context,
            ZSecurity.sovereignSession(
                method
            )
        )

        setVaultLocked(
            context,
            true
        )

        setAtlasSleeping(
            context,
            false
        )

        closeVaultCompartments(
            context
        )
    }

    fun isAuthenticated(
        context: Context
    ): Boolean {

        return get(
            context
        ).authenticated
    }

    fun isOwnerVerified(
        context: Context
    ): Boolean {

        return get(
            context
        ).ownerVerified
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

    /*
     * ---------------------------------------------------------
     * Z VAULT LOCK
     * ---------------------------------------------------------
     *
     * This lock affects the Vault only.
     *
     * Atlas remains available.
     *
     * IMPORTANT:
     * Locking the Vault also closes all Vault compartments.
     */

    fun lockVaultOnly(
        context: Context
    ) {

        setVaultLocked(
            context,
            true
        )

        setAtlasSleeping(
            context,
            false
        )

        closeVaultCompartments(
            context
        )
    }

    /*
     * ---------------------------------------------------------
     * Z VAULT UNLOCK
     * ---------------------------------------------------------
     *
     * This is the synchronization point between the existing
     * Guardian Vault session and the Z Vault compartment layer.
     *
     * The caller must already have an authenticated Guardian
     * security session.
     *
     * Unlocking the Vault root grants the normal VAULT-level
     * compartments:
     *
     *     Z Memory
     *     Z Project
     *
     * It does NOT unlock:
     *
     *     Z Recovery
     *     Z Origin
     *     Z Sovereign
     *
     * Those remain protected by their own permission boundaries.
     */

    fun unlockVault(
        context: Context
    ) {

        val appContext =
            context.applicationContext

        if (
            !isAuthenticated(
                appContext
            )
        ) {
            return
        }

        setVaultLocked(
            appContext,
            false
        )

        val memoryUnlocked =
            runCatching {

                ZVaultService.unlockCompartment(
                    appContext,
                    ZVaultService.Compartment.Z_MEMORY
                )

            }.getOrDefault(false)

        val projectUnlocked =
            runCatching {

                ZVaultService.unlockCompartment(
                    appContext,
                    ZVaultService.Compartment.Z_PROJECT
                )

            }.getOrDefault(false)

        /*
         * If the normal VAULT-level compartments could not be
         * synchronized, fail closed for those compartments.
         *
         * The root state itself remains controlled by the
         * existing ZSecuritySession lifecycle.
         */

        if (!memoryUnlocked) {

            runCatching {

                ZVaultService.lockCompartment(
                    appContext,
                    ZVaultService.Compartment.Z_MEMORY
                )

            }
        }

        if (!projectUnlocked) {

            runCatching {

                ZVaultService.lockCompartment(
                    appContext,
                    ZVaultService.Compartment.Z_PROJECT
                )

            }
        }
    }

    fun isVaultLocked(
        context: Context
    ): Boolean {

        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .getBoolean(
                VAULT_LOCKED_KEY,
                true
            )
    }

    /*
     * ---------------------------------------------------------
     * ATLAS SLEEP LOCK
     * ---------------------------------------------------------
     *
     * This is the stronger lock.
     *
     * Vault becomes locked.
     * Atlas becomes inactive.
     */

    fun sleepAtlas(
        context: Context
    ) {

        setVaultLocked(
            context,
            true
        )

        setAtlasSleeping(
            context,
            true
        )

        closeVaultCompartments(
            context
        )
    }

    fun wakeAtlas(
        context: Context
    ) {

        /*
         * Waking Atlas does NOT itself authenticate
         * the owner.
         *
         * The caller must first complete the required
         * Guardian owner-authentication flow.
         */

        setAtlasSleeping(
            context,
            false
        )

        setVaultLocked(
            context,
            true
        )

        closeVaultCompartments(
            context
        )
    }

    fun isAtlasSleeping(
        context: Context
    ): Boolean {

        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .getBoolean(
                ATLAS_SLEEPING_KEY,
                true
            )
    }

    fun isAtlasActive(
        context: Context
    ): Boolean {

        return !isAtlasSleeping(
            context
        )
    }

    /*
     * ---------------------------------------------------------
     * INTERNAL STATE
     * ---------------------------------------------------------
     */

    private fun setVaultLocked(
        context: Context,
        locked: Boolean
    ) {

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                VAULT_LOCKED_KEY,
                locked
            )
            .apply()
    }

    private fun setAtlasSleeping(
        context: Context,
        sleeping: Boolean
    ) {

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                ATLAS_SLEEPING_KEY,
                sleeping
            )
            .apply()
    }

    /**
     * Close every Z Vault compartment without changing
     * the Atlas session itself.
     *
     * This helper intentionally ignores failures because
     * the security session must fail closed.
     */
    private fun closeVaultCompartments(
        context: Context
    ) {

        runCatching {

            ZVaultService.lockAllCompartments(
                context.applicationContext
            )

        }
    }
}
