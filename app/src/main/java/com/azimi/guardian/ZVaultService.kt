package com.azimi.guardian

import android.content.Context

/**
 * ZVaultService
 *
 * High-level security boundary for AZIMI Z Vault.
 *
 * Responsibilities:
 * - Keep Vault access separate from Atlas session state.
 * - Keep individual Vault compartments independently locked/unlocked.
 * - Enforce security policy before protected data is accessed.
 * - Never expose VaultCrypto directly to Atlas tools.
 * - Fail closed when authorization is missing.
 *
 * Security model:
 *
 *     IDENTITY
 *         ↓
 *     AUTHORITY
 *         ↓
 *     VAULT ROOT ACCESS
 *         ↓
 *     COMPARTMENT ACCESS
 *
 * Atlas being active does NOT automatically grant Vault access.
 *
 * Unlocking one compartment does NOT unlock other compartments.
 */
object ZVaultService {

    enum class Compartment(
        val id: String,
        val displayName: String,
        val requiredPermission: AtlasPermission
    ) {

        Z_MEMORY(
            id = "z_memory",
            displayName = "Z Memory",
            requiredPermission = AtlasPermission.VAULT
        ),

        Z_PROJECT(
            id = "z_project",
            displayName = "Z Project",
            requiredPermission = AtlasPermission.VAULT
        ),

        Z_RECOVERY(
            id = "z_recovery",
            displayName = "Z Recovery",
            requiredPermission = AtlasPermission.OWNER
        ),

        Z_ORIGIN(
            id = "z_origin",
            displayName = "Z Origin",
            requiredPermission = AtlasPermission.OWNER
        ),

        Z_SOVEREIGN(
            id = "z_sovereign",
            displayName = "Z Sovereign",
            requiredPermission = AtlasPermission.SOVEREIGN
        )
    }

    data class CompartmentState(
        val compartment: Compartment,
        val locked: Boolean,
        val accessGranted: Boolean
    )

    data class VaultState(
        val rootLocked: Boolean,
        val rootAccessGranted: Boolean,
        val unlockedCompartments: List<Compartment>
    )

    /*
     * Compartment unlock state is intentionally held only for
     * the current application security session.
     *
     * When the application/process loses its security session,
     * compartments fail closed.
     */
    private val unlockedCompartments =
        mutableSetOf<String>()

    private fun appContext(context: Context): Context {
        return context.applicationContext
    }

    /**
     * Returns whether the main Z Vault root is currently unlocked.
     */
    fun isRootUnlocked(
        context: Context
    ): Boolean {

        val appContext = appContext(context)

        return ZSecuritySession.isAuthenticated(appContext) &&
            !ZSecuritySession.isVaultLocked(appContext)
    }

    /**
     * Root Vault access requires an authenticated Guardian
     * security session and an explicitly unlocked Vault.
     *
     * Atlas activity alone is NOT sufficient.
     */
    fun canAccessRoot(
        context: Context
    ): Boolean {

        return isRootUnlocked(context)
    }

    /**
     * Returns whether a specific compartment is currently unlocked
     * and its required security permission is satisfied.
     */
    fun canAccess(
        context: Context,
        compartment: Compartment
    ): Boolean {

        val appContext = appContext(context)

        if (!isRootUnlocked(appContext)) {
            return false
        }

        if (!AtlasPermissionChecker.isAllowed(
                appContext,
                compartment.requiredPermission
            )
        ) {
            return false
        }

        return unlockedCompartments.contains(compartment.id)
    }

    /**
     * Unlock the main Vault root.
     *
     * This does NOT unlock any individual compartment.
     */
    fun unlockRoot(
        context: Context
    ): Boolean {

        val appContext = appContext(context)

        if (!ZSecuritySession.isAuthenticated(appContext)) {
            return false
        }

        return try {

            ZSecuritySession.unlockVault(appContext)

            true

        } catch (_: Exception) {

            false
        }
    }

    /**
     * Locks the Vault root and immediately closes every
     * currently unlocked compartment.
     */
    fun lockRoot(
        context: Context
    ) {

        val appContext = appContext(context)

        unlockedCompartments.clear()

        try {
            ZSecuritySession.lockVaultOnly(appContext)
        } catch (_: Exception) {
            // Fail closed even if the underlying operation reports an error.
        }
    }

    /**
     * Unlock one specific compartment.
     *
     * No other compartment is affected.
     */
    fun unlockCompartment(
        context: Context,
        compartment: Compartment
    ): Boolean {

        val appContext = appContext(context)

        if (!isRootUnlocked(appContext)) {
            return false
        }

        if (!AtlasPermissionChecker.isAllowed(
                appContext,
                compartment.requiredPermission
            )
        ) {
            return false
        }

        unlockedCompartments.add(compartment.id)

        return true
    }

    /**
     * Lock one specific compartment.
     *
     * Other compartments remain unchanged.
     */
    fun lockCompartment(
        context: Context,
        compartment: Compartment
    ) {

        unlockedCompartments.remove(compartment.id)
    }

    /**
     * Lock every compartment while leaving the root Vault state
     * unchanged.
     */
    fun lockAllCompartments(
        context: Context
    ) {

        unlockedCompartments.clear()
    }

    /**
     * Returns the current state of one compartment.
     */
    fun getCompartmentState(
        context: Context,
        compartment: Compartment
    ): CompartmentState {

        val accessGranted =
            canAccess(context, compartment)

        return CompartmentState(
            compartment = compartment,
            locked = !unlockedCompartments.contains(compartment.id),
            accessGranted = accessGranted
        )
    }

    /**
     * Returns the complete high-level Vault security state.
     *
     * This contains status information only.
     * It never exposes Vault secrets or cryptographic keys.
     */
    fun getState(
        context: Context
    ): VaultState {

        val appContext = appContext(context)

        val rootUnlocked =
            isRootUnlocked(appContext)

        val activeCompartments =
            if (rootUnlocked) {
                Compartment.entries.filter {
                    unlockedCompartments.contains(it.id) &&
                        AtlasPermissionChecker.isAllowed(
                            appContext,
                            it.requiredPermission
                        )
                }
            } else {
                emptyList()
            }

        return VaultState(
            rootLocked = !rootUnlocked,
            rootAccessGranted = rootUnlocked,
            unlockedCompartments = activeCompartments
        )
    }

    /**
     * Read encrypted Vault data through the Z Vault boundary.
     *
     * The caller must have access to the specified compartment.
     */
    fun get(
        context: Context,
        compartment: Compartment,
        key: String
    ): String? {

        val appContext = appContext(context)

        if (!canAccess(appContext, compartment)) {
            return null
        }

        return try {
            VaultCrypto.get(appContext, key)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Write encrypted Vault data through the Z Vault boundary.
     *
     * The caller must have access to the specified compartment.
     */
    fun put(
        context: Context,
        compartment: Compartment,
        key: String,
        value: String
    ): Boolean {

        val appContext = appContext(context)

        if (!canAccess(appContext, compartment)) {
            return false
        }

        return try {
            VaultCrypto.put(
                appContext,
                key,
                value
            )
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Delete encrypted Vault data through the Z Vault boundary.
     */
    fun delete(
        context: Context,
        compartment: Compartment,
        key: String
    ): Boolean {

        val appContext = appContext(context)

        if (!canAccess(appContext, compartment)) {
            return false
        }

        return try {
            VaultCrypto.delete(
                appContext,
                key
            )
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Security shutdown.
     *
     * Every compartment is closed first.
     * The root Vault is then locked.
     *
     * This does NOT terminate Atlas by itself.
     * Atlas has its own lifecycle.
     */
    fun securityLock(
        context: Context
    ) {

        val appContext = appContext(context)

        unlockedCompartments.clear()

        try {
            ZSecuritySession.lockVaultOnly(appContext)
        } catch (_: Exception) {
            // Fail closed.
        }
    }

    /**
     * Diagnostics contain only security-state metadata.
     */
    fun diagnostics(
        context: Context
    ): String {

        val state =
            getState(context)

        val compartments =
            Compartment.entries.joinToString(", ") { compartment ->

                val unlocked =
                    state.unlockedCompartments.contains(compartment)

                "${compartment.id}=${if (unlocked) "UNLOCKED" else "LOCKED"}"
            }

        return buildString {

            append("Z VAULT SERVICE\n")
            append("ROOT=")
            append(
                if (state.rootLocked) {
                    "LOCKED"
                } else {
                    "UNLOCKED"
                }
            )
            append("\n")

            append("ROOT_ACCESS=")
            append(state.rootAccessGranted)
            append("\n")

            append("COMPARTMENTS=")
            append(compartments)
        }
    }
}
