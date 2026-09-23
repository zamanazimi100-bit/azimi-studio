package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas active-session controller.
 *
 * Atlas and Z Vault have independent lifecycle states.
 *
 * VAULT LOCK:
 *     - locks Z Vault
 *     - Atlas remains active
 *
 * ATLAS SLEEP LOCK:
 *     - locks Z Vault
 *     - puts Atlas to sleep
 *     - ends active Atlas session
 *
 * FULL LOCK:
 *     - locks Z Vault
 *     - puts Atlas to sleep
 *     - revokes owner authority
 *     - clears protected security session
 *
 * Atlas activation remains an in-memory authorization state.
 * Restarting the application does not silently recreate
 * an authorized Atlas session.
 */
object AtlasSession {

    @Volatile
    private var active = false

    /**
     * Starts an authorized Atlas session.
     *
     * This does not unlock Z Vault.
     */
    fun start(
        context: Context
    ) {
        active = true

        ZSecuritySession.wakeAtlas(
            context
        )
    }

    /**
     * Returns whether Atlas is currently active.
     *
     * Z Vault being locked does NOT make this false.
     */
    fun isActive(
        context: Context
    ): Boolean {
        return active &&
            !ZSecuritySession.isAtlasSleeping(
                context
            )
    }

    /**
     * Ordinary Vault exit.
     *
     * This intentionally does NOT deactivate Atlas.
     */
    fun onVaultExit(
        context: Context
    ) {
        ZSecuritySession.lockVaultOnly(
            context
        )

        /*
         * Atlas intentionally remains active.
         */
        active = active
    }

    /**
     * Locks only the Z Vault.
     *
     * Atlas remains available outside the Vault.
     */
    fun lockVaultOnly(
        context: Context
    ) {
        ZSecuritySession.lockVaultOnly(
            context
        )

        /*
         * Important:
         * Atlas remains active.
         */
        active = active
    }

    /**
     * Puts Atlas into the stronger sleep state.
     *
     * Z Vault is also locked.
     */
    fun sleep(
        context: Context
    ) {
        active = false

        ZSecuritySession.sleepAtlas(
            context
        )
    }

    /**
     * Full Guardian security lock.
     *
     * This is the strongest normal session action.
     */
    fun fullLock(
        context: Context
    ) {
        active = false

        AtlasOwnerAuthority.revokeOwnerAuthorization(
            context
        )

        ZSecuritySession.clear(
            context
        )
    }

    /**
     * Explicit Atlas shutdown.
     *
     * This stops Atlas without pretending that
     * the Vault was unlocked.
     */
    fun stop(
        context: Context
    ) {
        active = false

        ZSecuritySession.sleepAtlas(
            context
        )
    }

    /**
     * Wakes Atlas after the caller has completed
     * the required Guardian authentication flow.
     *
     * This function itself does not perform authentication.
     */
    fun wake(
        context: Context
    ) {
        active = true

        ZSecuritySession.wakeAtlas(
            context
        )
    }
}
