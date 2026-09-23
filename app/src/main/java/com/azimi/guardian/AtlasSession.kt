package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas active-session controller.
 *
 * Atlas session lifetime is intentionally different from
 * the visual Z Vault screen lifetime.
 *
 * Vault EXIT / ordinary Vault lock:
 *     - closes the protected Vault UI
 *     - Atlas remains active
 *
 * FULL LOCK:
 *     - ends Atlas
 *     - revokes owner authority
 *     - clears the protected security session
 *
 * The active state is kept in memory so that restarting the
 * application does not silently recreate an authorized Atlas
 * session.
 */
object AtlasSession {

    @Volatile
    private var active = false

    fun start(
        context: Context
    ) {
        active = true
    }

    fun isActive(
        context: Context
    ): Boolean {
        return active
    }

    /**
     * Ordinary Vault exit.
     *
     * This intentionally does NOT deactivate Atlas.
     */
    fun onVaultExit(
        context: Context
    ) {
        // Atlas intentionally remains active.
        active = active
    }

    /**
     * Full Atlas/Vault lock.
     *
     * This is the ONLY normal session action that ends
     * the active Atlas session.
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
     */
    fun stop(
        context: Context
    ) {
        active = false
    }
}
