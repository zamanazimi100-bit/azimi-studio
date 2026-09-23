package com.azimi.guardian

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.Executor

/**
 * AZIMI — Atlas Owner Authority
 *
 * Identity and authority are intentionally separate.
 *
 * Atlas permanently remembers the declared AZIMI owner identity
 * locally, across application restarts and authentication sessions.
 *
 * Remembered identity does NOT grant permission.
 *
 * Owner authorization requires the configured Guardian owner
 * verification flow.
 *
 * Current implemented owner verification:
 * - Android BIOMETRIC_STRONG
 *
 * Planned future verification:
 * - Face Lock
 * - Voice Lock
 *
 * Those future methods must not be treated as active until
 * actually implemented and verified.
 *
 * Atlas may remember:
 * - owner identity
 * - owner name
 * - owner role
 * - safe ownership context
 *
 * Atlas must never remember:
 * - passwords
 * - access tokens
 * - refresh tokens
 * - API keys
 * - recovery codes
 * - private credentials
 * - biometric templates
 *
 * IMPORTANT SECURITY ARCHITECTURE:
 *
 * Guardian owner authority is LOCAL Guardian authority.
 *
 * It is intentionally independent from:
 * - Supabase
 * - AZIMI Studio website authentication
 * - email magic links
 * - browser authentication
 * - remote AI provider sessions
 *
 * Remote AZIMI authentication may still exist for separate
 * website/cloud operations, but it is NOT a prerequisite for
 * Atlas owner verification or Atlas availability.
 *
 * Active owner authorization is intentionally process/session
 * based.
 *
 * A successful Android biometric verification activates owner
 * authority for the current Guardian process/session.
 *
 * Persisted authorization metadata is NOT sufficient by itself
 * to restore owner authority after process restart.
 */
object AtlasOwnerAuthority {

    /**
     * Active owner authorization exists only in the current
     * Guardian process/session.
     */
    @Volatile
    private var activeOwnerAuthorization = false

    private const val PREFS_NAME =
        "azimi_owner_authority"

    private const val KEY_OWNER_AUTHORIZED =
        "owner_authorized"

    private const val KEY_OWNER_ID =
        "owner_id"

    private const val KEY_AUTH_METHOD =
        "authorization_method"

    private const val KEY_AUTHORIZED_AT =
        "authorized_at"

    /*
     * Persistent identity record.
     *
     * This is intentionally NOT an authentication credential.
     */
    private const val KEY_IDENTITY_INITIALIZED =
        "identity_initialized"

    private const val KEY_DECLARED_OWNER_ID =
        "declared_owner_id"

    private const val KEY_DECLARED_OWNER_NAME =
        "declared_owner_name"

    private const val KEY_DECLARED_OWNER_ROLE =
        "declared_owner_role"

    private const val OWNER_ID =
        "ZAMAN_AZIMI"

    private const val OWNER_ROLE =
        "FOUNDER_CREATOR_OWNER"

    private const val AUTH_METHOD_BIOMETRIC_STRONG =
        "ANDROID_BIOMETRIC_STRONG"

    enum class ActorType {
        OWNER,
        AUTHENTICATED_USER,
        GUEST,
        UNKNOWN
    }

    enum class AuthorityLevel {
        OWNER,
        USER,
        GUEST,
        NONE
    }

    enum class SensitiveOperation {
        CREATE,
        MODIFY,
        DELETE,
        BACKUP,
        RECOVERY,
        OWNERSHIP_RECORD,
        PROVENANCE_RECORD,
        SECURITY_CONFIGURATION,
        MEMORY_CONFIGURATION,
        PROVIDER_CONFIGURATION,
        SYSTEM_CONFIGURATION
    }

    data class AuthorityState(
        val actorType: ActorType,
        val authorityLevel: AuthorityLevel,

        /**
         * For Atlas/Guardian this means an active local
         * Guardian authorization session.
         *
         * It does NOT mean Supabase/AZIMI Studio authentication.
         */
        val authenticated: Boolean,

        val ownerAuthorized: Boolean,
        val ownerId: String?,
        val authorizationMethod: String?,
        val authorizedAt: Long?,
        val message: String,

        val rememberedOwnerId: String? = null,
        val rememberedOwnerName: String? = null,
        val rememberedOwnerRole: String? = null
    )

    data class AuthorizationDecision(
        val allowed: Boolean,
        val actorType: ActorType,
        val authorityLevel: AuthorityLevel,
        val operation: SensitiveOperation,
        val reason: String
    )

    data class OwnerVerificationResult(
        val success: Boolean,
        val ownerAuthorized: Boolean,
        val message: String
    )

    /**
     * Initialize the persistent declared owner identity.
     *
     * This does NOT activate owner authority.
     */
    fun initializeOwnerIdentity(
        context: Context
    ): Boolean {

        return runCatching {

            val prefs =
                context.applicationContext
                    .getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                    )

            val alreadyInitialized =
                prefs.getBoolean(
                    KEY_IDENTITY_INITIALIZED,
                    false
                )

            if (alreadyInitialized) {
                return@runCatching true
            }

            val committed =
                prefs.edit()
                    .putBoolean(
                        KEY_IDENTITY_INITIALIZED,
                        true
                    )
                    .putString(
                        KEY_DECLARED_OWNER_ID,
                        OWNER_ID
                    )
                    .putString(
                        KEY_DECLARED_OWNER_NAME,
                        getOwnerName()
                    )
                    .putString(
                        KEY_DECLARED_OWNER_ROLE,
                        OWNER_ROLE
                    )
                    .commit()

            committed

        }.getOrDefault(false)
    }

    /**
     * Return the remembered owner identity.
     *
     * This is safe identity context only.
     *
     * It does not mean that owner authority is active.
     */
    fun getRememberedOwnerIdentity(
        context: Context
    ): Map<String, String> {

        initializeOwnerIdentity(context)

        val prefs =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

        val ownerId =
            prefs.getString(
                KEY_DECLARED_OWNER_ID,
                OWNER_ID
            ) ?: OWNER_ID

        val ownerName =
            prefs.getString(
                KEY_DECLARED_OWNER_NAME,
                getOwnerName()
            ) ?: getOwnerName()

        val ownerRole =
            prefs.getString(
                KEY_DECLARED_OWNER_ROLE,
                OWNER_ROLE
            ) ?: OWNER_ROLE

        return mapOf(
            "owner_id" to ownerId,
            "owner_name" to ownerName,
            "owner_role" to ownerRole
        )
    }

    fun getOwnerIdentity(): String {
        return OWNER_ID
    }

    fun getOwnerName(): String {
        return AtlasKnowledge.OWNER_NAME
    }

    /**
     * Return the current authority state.
     *
     * Owner authority requires BOTH:
     *
     * 1. activeOwnerAuthorization == true
     * 2. matching persisted authorization metadata
     *
     * The active in-memory flag prevents stale persisted data
     * from automatically restoring owner authority after
     * process restart.
     *
     * IMPORTANT:
     *
     * No AzimiAuth session is consulted here.
     */
    fun getState(
        context: Context
    ): AuthorityState {

        val appContext =
            context.applicationContext

        initializeOwnerIdentity(
            appContext
        )

        val remembered =
            getRememberedOwnerIdentity(
                appContext
            )

        val prefs =
            appContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val storedOwnerAuthorized =
            prefs.getBoolean(
                KEY_OWNER_AUTHORIZED,
                false
            )

        val storedOwnerId =
            prefs.getString(
                KEY_OWNER_ID,
                null
            )

        val storedMethod =
            prefs.getString(
                KEY_AUTH_METHOD,
                null
            )

        val ownerAuthorized =
            activeOwnerAuthorization &&
                storedOwnerAuthorized &&
                storedOwnerId == OWNER_ID &&
                storedMethod == AUTH_METHOD_BIOMETRIC_STRONG

        if (ownerAuthorized) {

            val authorizedAt =
                if (
                    prefs.contains(
                        KEY_AUTHORIZED_AT
                    )
                ) {
                    prefs.getLong(
                        KEY_AUTHORIZED_AT,
                        0L
                    )
                } else {
                    null
                }

            return AuthorityState(
                actorType =
                    ActorType.OWNER,

                authorityLevel =
                    AuthorityLevel.OWNER,

                authenticated =
                    true,

                ownerAuthorized =
                    true,

                ownerId =
                    OWNER_ID,

                authorizationMethod =
                    storedMethod,

                authorizedAt =
                    authorizedAt,

                message =
                    "Strong Android biometric owner authorization is active for the current Guardian session.",

                rememberedOwnerId =
                    remembered["owner_id"],

                rememberedOwnerName =
                    remembered["owner_name"],

                rememberedOwnerRole =
                    remembered["owner_role"]
            )
        }

        /*
         * No local Guardian authorization is active.
         *
         * We intentionally do NOT fall back to AzimiAuth here.
         *
         * A remote website session must never silently become
         * Guardian owner authority.
         */
        return AuthorityState(
            actorType =
                ActorType.GUEST,

            authorityLevel =
                AuthorityLevel.NONE,

            authenticated =
                false,

            ownerAuthorized =
                false,

            ownerId =
                null,

            authorizationMethod =
                null,

            authorizedAt =
                null,

            message =
                "No active Guardian owner authorization is available. The declared AZIMI owner identity remains remembered, but remote website authentication is separate.",

            rememberedOwnerId =
                remembered["owner_id"],

            rememberedOwnerName =
                remembered["owner_name"],

            rememberedOwnerRole =
                remembered["owner_role"]
        )
    }

    /**
     * Start Android BIOMETRIC_STRONG owner verification.
     *
     * A successful verification activates owner authority
     * for the current Guardian process/session.
     *
     * IMPORTANT:
     *
     * No email, browser, Supabase session, magic link, or
     * remote AI authentication is required.
     */
    fun verifyOwner(
        activity: Activity,
        onResult: (OwnerVerificationResult) -> Unit
    ) {

        val appContext =
            activity.applicationContext

        initializeOwnerIdentity(
            appContext
        )

        /*
         * Owner verification is a Guardian-local security
         * operation.
         *
         * Do NOT require AzimiAuth here.
         */

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.P
        ) {

            onResult(
                OwnerVerificationResult(
                    success = false,
                    ownerAuthorized = false,
                    message =
                        "Android biometric owner verification requires Android 9 or newer."
                )
            )

            return
        }

        val biometricManager =
            activity.getSystemService(
                BiometricManager::class.java
            )

        if (biometricManager == null) {

            onResult(
                OwnerVerificationResult(
                    success = false,
                    ownerAuthorized = false,
                    message =
                        "Android biometric service is unavailable."
                )
            )

            return
        }

        val biometricStatus =
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )

        if (
            biometricStatus !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {

            onResult(
                OwnerVerificationResult(
                    success = false,
                    ownerAuthorized = false,
                    message =
                        biometricStatusMessage(
                            biometricStatus
                        )
                )
            )

            return
        }

        val executor: Executor =
            activity.mainExecutor

        val prompt =
            BiometricPrompt.Builder(
                activity
            )
                .setTitle(
                    "AZIMI Owner Verification"
                )
                .setSubtitle(
                    "Verify Zaman Azimi owner authority"
                )
                .setDescription(
                    "This verification protects AZIMI ownership, provenance, recovery and protected system operations."
                )
                .setNegativeButton(
                    "CANCEL",
                    executor
                ) { _, _ ->

                    revokeOwnerAuthorization(
                        appContext
                    )

                    onResult(
                        OwnerVerificationResult(
                            success = false,
                            ownerAuthorized = false,
                            message =
                                "Owner verification was cancelled."
                        )
                    )
                }
                .build()

        val cancellationSignal =
            CancellationSignal()

        prompt.authenticate(
            cancellationSignal,
            executor,
            object :
                BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result:
                        BiometricPrompt.AuthenticationResult
                ) {

                    val authorized =
                        activateOwnerAuthorityAfterVerification(
                            appContext
                        )

                    if (authorized) {

                        onResult(
                            OwnerVerificationResult(
                                success = true,
                                ownerAuthorized = true,
                                message =
                                    "Owner authority verified through Android BIOMETRIC_STRONG."
                            )
                        )

                    } else {

                        onResult(
                            OwnerVerificationResult(
                                success = false,
                                ownerAuthorized = false,
                                message =
                                    "Biometric verification succeeded, but AZIMI owner authority could not be activated."
                            )
                        )
                    }
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {

                    revokeOwnerAuthorization(
                        appContext
                    )

                    val message =
                        errString
                            .toString()
                            .trim()

                    onResult(
                        OwnerVerificationResult(
                            success = false,
                            ownerAuthorized = false,
                            message =
                                if (message.isNotBlank()) {
                                    "Owner verification failed: $message"
                                } else {
                                    "Owner verification failed."
                                }
                        )
                    )
                }

                override fun onAuthenticationFailed() {
                    /*
                     * Do not revoke here.
                     *
                     * Android may keep the biometric prompt alive
                     * for another attempt.
                     */
                }
            }
        )
    }

    /**
     * Activate owner authority after Android biometric
     * verification has already succeeded.
     *
     * This is a Guardian-local operation.
     *
     * No remote AZIMI authentication is required.
     */
    private fun activateOwnerAuthorityAfterVerification(
        context: Context
    ): Boolean {

        val appContext =
            context.applicationContext

        if (
            !initializeOwnerIdentity(
                appContext
            )
        ) {
            return false
        }

        val now =
            System.currentTimeMillis()

        val committed =
            appContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putBoolean(
                    KEY_OWNER_AUTHORIZED,
                    true
                )
                .putString(
                    KEY_OWNER_ID,
                    OWNER_ID
                )
                .putString(
                    KEY_AUTH_METHOD,
                    AUTH_METHOD_BIOMETRIC_STRONG
                )
                .putLong(
                    KEY_AUTHORIZED_AT,
                    now
                )
                .commit()

        if (committed) {
            activeOwnerAuthorization = true
        }

        return committed
    }

    /**
     * Revoke active owner authority.
     *
     * This immediately removes active in-memory authority
     * and clears persisted authorization metadata.
     *
     * Remembered owner identity remains intact.
     */
    fun revokeOwnerAuthorization(
        context: Context
    ) {

        activeOwnerAuthorization = false

        context
            .applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                KEY_OWNER_AUTHORIZED
            )
            .remove(
                KEY_OWNER_ID
            )
            .remove(
                KEY_AUTH_METHOD
            )
            .remove(
                KEY_AUTHORIZED_AT
            )
            .apply()
    }

    /**
     * Alias used by existing Guardian code.
     */
    fun clearOwnerAuthority(
        context: Context
    ) {

        revokeOwnerAuthorization(
            context
        )
    }

    /**
     * Forget the remembered owner identity.
     *
     * This is different from simply locking owner authority.
     */
    fun forgetRememberedOwner(
        context: Context
    ) {

        activeOwnerAuthorization = false

        context
            .applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }

    fun isOwner(
        context: Context
    ): Boolean {

        return getState(
            context
        ).authorityLevel ==
            AuthorityLevel.OWNER
    }

    fun isAuthenticatedUser(
        context: Context
    ): Boolean {

        return getState(
            context
        ).authorityLevel ==
            AuthorityLevel.USER
    }

    fun hasOwnerAuthorization(
        context: Context
    ): Boolean {

        return getState(
            context
        ).ownerAuthorized
    }

    /**
     * Check whether a sensitive operation is permitted
     * under the current Guardian authority state.
     */
    fun authorizeOperation(
        context: Context,
        operation: SensitiveOperation
    ): AuthorizationDecision {

        val state =
            getState(
                context
            )

        if (!state.authenticated) {

            return AuthorizationDecision(
                allowed = false,

                actorType =
                    state.actorType,

                authorityLevel =
                    state.authorityLevel,

                operation =
                    operation,

                reason =
                    "Active Guardian authorization is required."
            )
        }

        val ownerOnly =
            when (operation) {

                SensitiveOperation.OWNERSHIP_RECORD,
                SensitiveOperation.PROVENANCE_RECORD,
                SensitiveOperation.RECOVERY,
                SensitiveOperation.SECURITY_CONFIGURATION,
                SensitiveOperation.MEMORY_CONFIGURATION,
                SensitiveOperation.PROVIDER_CONFIGURATION,
                SensitiveOperation.SYSTEM_CONFIGURATION ->
                    true

                SensitiveOperation.CREATE,
                SensitiveOperation.MODIFY,
                SensitiveOperation.DELETE,
                SensitiveOperation.BACKUP ->
                    false
            }

        if (
            ownerOnly &&
            state.authorityLevel !=
            AuthorityLevel.OWNER
        ) {

            return AuthorizationDecision(
                allowed = false,

                actorType =
                    state.actorType,

                authorityLevel =
                    state.authorityLevel,

                operation =
                    operation,

                reason =
                    "Owner authorization is required for this operation."
            )
        }

        return AuthorizationDecision(
            allowed = true,

            actorType =
                state.actorType,

            authorityLevel =
                state.authorityLevel,

            operation =
                operation,

            reason =
                "Operation is permitted by the current Guardian authority level."
        )
    }

    /**
     * Detect requests involving ownership/provenance/authority.
     */
    fun isOwnershipSensitiveRequest(
        request: String
    ): Boolean {

        val text =
            request
                .trim()
                .lowercase()

        if (text.isBlank()) {
            return false
        }

        val markers =
            listOf(
                "ownership",
                "owner",
                "creator",
                "founder",
                "provenance",
                "intellectual property",
                "ip record",
                "ownership record",
                "proof of ownership",
                "change owner",
                "transfer ownership",
                "claim ownership",
                "authority",
                "owner authority"
            )

        return markers.any {
            text.contains(it)
        }
    }

    /**
     * Detect requests that require active owner authority.
     */
    fun requiresOwnerAuthorization(
        request: String
    ): Boolean {

        if (
            isOwnershipSensitiveRequest(
                request
            )
        ) {
            return true
        }

        val text =
            request
                .trim()
                .lowercase()

        val sensitiveMarkers =
            listOf(
                "change security policy",
                "change recovery",
                "delete vault",
                "reset vault",
                "change memory policy",
                "change provider",
                "replace core",
                "modify system authority",
                "change guardian policy"
            )

        return sensitiveMarkers.any {
            text.contains(it)
        }
    }

    /**
     * Create a deterministic SHA-256 provenance digest.
     *
     * This does not create legal ownership by itself.
     */
    fun createProvenanceDigest(
        projectName: String,
        ownerName: String,
        projectVersion: String,
        recordedAt: Long
    ): String {

        val normalized =
            listOf(
                projectName.trim(),
                ownerName.trim(),
                projectVersion.trim(),
                recordedAt.toString()
            ).joinToString("|")

        return sha256(
            normalized
        )
    }

    /**
     * Create an ownership/provenance record.
     *
     * Owner authority must already be active.
     */
    fun createOwnershipRecord(
        context: Context,
        projectName: String = "AZIMI",
        projectVersion: String =
            AtlasKnowledge.KNOWLEDGE_VERSION
    ): JSONObject? {

        val state =
            getState(
                context
            )

        if (
            state.authorityLevel !=
            AuthorityLevel.OWNER
        ) {
            return null
        }

        val timestamp =
            System.currentTimeMillis()

        val digest =
            createProvenanceDigest(
                projectName = projectName,
                ownerName = getOwnerName(),
                projectVersion = projectVersion,
                recordedAt = timestamp
            )

        val record =
            JSONObject()

        record.put(
            "schema_version",
            "1.0.0"
        )

        record.put(
            "project",
            projectName
        )

        record.put(
            "owner",
            getOwnerName()
        )

        record.put(
            "owner_id",
            OWNER_ID
        )

        record.put(
            "role",
            OWNER_ROLE
        )

        record.put(
            "project_version",
            projectVersion
        )

        record.put(
            "recorded_at",
            timestamp
        )

        record.put(
            "provenance_digest",
            digest
        )

        record.put(
            "statement",
            "This record identifies Zaman Azimi as the declared creator and owner of the AZIMI project within the project's ownership architecture."
        )

        record.put(
            "legal_status",
            "Technical provenance record; not a substitute for jurisdiction-specific legal registration or legal advice."
        )

        return record
    }

    /**
     * Safe Atlas context.
     *
     * Atlas can remember who the owner is without treating
     * remembered identity as active authorization.
     */
    fun getSafeContext(
        context: Context
    ): Map<String, String> {

        val state =
            getState(
                context
            )

        val rememberedOwnerId =
            state.rememberedOwnerId
                ?: OWNER_ID

        val rememberedOwnerName =
            state.rememberedOwnerName
                ?: getOwnerName()

        val rememberedOwnerRole =
            state.rememberedOwnerRole
                ?: OWNER_ROLE

        val activeOwnerIdentity =
            if (
                state.ownerAuthorized
            ) {
                OWNER_ID
            } else {
                "NOT_ACTIVE"
            }

        val authorizationMethod =
            sanitizeAuthorizationMethod(
                state.authorizationMethod
                    .orEmpty()
            )

        return mapOf(
            "actor_type" to
                state.actorType.name,

            "authority_level" to
                state.authorityLevel.name,

            "authenticated" to
                state.authenticated.toString(),

            "owner_authorized" to
                state.ownerAuthorized.toString(),

            "remembered_owner_identity" to
                rememberedOwnerId,

            "remembered_owner_name" to
                rememberedOwnerName,

            "remembered_owner_role" to
                rememberedOwnerRole,

            "active_owner_identity" to
                activeOwnerIdentity,

            "authorization_method" to
                authorizationMethod,

            "authority_message" to
                state.message
        )
    }

    /**
     * Convert biometric availability status into a safe
     * user-facing diagnostic message.
     */
    private fun biometricStatusMessage(
        status: Int
    ): String {

        return when (status) {

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "This device does not provide compatible biometric hardware."

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "The biometric hardware is currently unavailable."

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No strong biometric is enrolled on this device."

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "Android requires a biometric security update before owner verification can be used."

            else ->
                "Strong biometric owner verification is currently unavailable."
        }
    }

    /**
     * Sanitize authorization-method metadata before exposing it
     * to Atlas context.
     *
     * Protected credentials are never returned.
     */
    private fun sanitizeAuthorizationMethod(
        value: String
    ): String {

        val clean =
            value.trim()

        if (
            AzimiAuth.isProtectedCredential(
                clean
            )
        ) {
            return ""
        }

        return clean
            .take(80)
            .replace(
                "\n",
                " "
            )
            .replace(
                "\r",
                " "
            )
    }

    /**
     * SHA-256 helper.
     */
    private fun sha256(
        value: String
    ): String {

        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256"
                )
                .digest(
                    value.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return Base64.encodeToString(
            digest,
            Base64.NO_WRAP or
                Base64.URL_SAFE
        )
    }

    /**
     * Generate a cryptographically strong random security
     * nonce for future Guardian security operations.
     */
    fun generateSecurityNonce(): ByteArray {

        val nonce =
            ByteArray(32)

        SecureRandom()
            .nextBytes(
                nonce
            )

        return nonce
    }

    /**
     * Human-readable diagnostic report.
     *
     * No credentials are included.
     */
    fun diagnostics(
        context: Context
    ): String {

        val state =
            getState(
                context
            )

        return buildString {

            appendLine(
                "ATLAS OWNER AUTHORITY"
            )

            appendLine(
                "REMEMBERED OWNER: ${
                    state.rememberedOwnerName
                        ?: getOwnerName()
                }"
            )

            appendLine(
                "REMEMBERED OWNER ID: ${
                    state.rememberedOwnerId
                        ?: OWNER_ID
                }"
            )

            appendLine(
                "REMEMBERED OWNER ROLE: ${
                    state.rememberedOwnerRole
                        ?: OWNER_ROLE
                }"
            )

            appendLine()

            appendLine(
                "ACTIVE ACTOR: ${state.actorType}"
            )

            appendLine(
                "ACTIVE AUTHORITY: ${
                    state.authorityLevel
                }"
            )

            appendLine(
                "GUARDIAN SESSION AUTHORIZED: ${
                    state.authenticated
                }"
            )

            appendLine(
                "OWNER AUTHORIZED: ${
                    state.ownerAuthorized
                }"
            )

            appendLine(
                "AUTHORIZATION METHOD: ${
                    state.authorizationMethod
                        ?: "NONE"
                }"
            )

            appendLine(
                "AUTHORIZED AT: ${
                    state.authorizedAt
                        ?: "NONE"
                }"
            )

            appendLine()

            appendLine(
                "STATUS: ${state.message}"
            )
        }
    }
}
