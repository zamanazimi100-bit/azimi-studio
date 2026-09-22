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
 * Authentication proves an active AZIMI session.
 *
 * Owner authorization requires the configured Android owner
 * verification flow.
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
 * This component does not bypass Android, Supabase, GitHub,
 * Vercel, or any other security boundary.
 */
object AtlasOwnerAuthority {

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
        val authenticated: Boolean,
        val ownerAuthorized: Boolean,
        val ownerId: String?,
        val authorizationMethod: String?,
        val authorizedAt: Long?,
        val message: String,

        /*
         * Persistent identity is separate from active authority.
         */
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
     * Initializes the permanent local AZIMI owner identity.
     *
     * This is an identity record, not an authentication mechanism.
     *
     * It does not grant owner authority.
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
                return true
            }

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

        }.getOrDefault(false)
    }

    /**
     * Returns the permanently remembered owner identity.
     *
     * This does NOT mean the owner is currently authenticated.
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

        return mapOf(
            "owner_id" to (
                prefs.getString(
                    KEY_DECLARED_OWNER_ID,
                    OWNER_ID
                ) ?: OWNER_ID
            ),

            "owner_name" to (
                prefs.getString(
                    KEY_DECLARED_OWNER_NAME,
                    getOwnerName()
                ) ?: getOwnerName()
            ),

            "owner_role" to (
                prefs.getString(
                    KEY_DECLARED_OWNER_ROLE,
                    OWNER_ROLE
                ) ?: OWNER_ROLE
            )
        )
    }

    /**
     * Returns the canonical AZIMI owner identity.
     */
    fun getOwnerIdentity(): String =
        OWNER_ID

    fun getOwnerName(): String =
        AtlasKnowledge.OWNER_NAME

    /**
     * Returns current authority state.
     *
     * Persistent owner identity survives sessions.
     *
     * Active owner authorization does NOT survive as permission merely
     * because the identity is remembered.
     */
    fun getState(
        context: Context
    ): AuthorityState {

        val appContext =
            context.applicationContext

        initializeOwnerIdentity(appContext)

        val remembered =
            getRememberedOwnerIdentity(
                appContext
            )

        val authenticated =
            AzimiAuth.hasSession(
                appContext
            )

        if (!authenticated) {
            return AuthorityState(
                actorType =
                    ActorType.GUEST,
                authorityLevel =
                    AuthorityLevel.NONE,
                authenticated = false,
                ownerAuthorized = false,
                ownerId = null,
                authorizationMethod = null,
                authorizedAt = null,
                message =
                    "No authenticated AZIMI session is available. Atlas still remembers the declared AZIMI owner identity.",
                rememberedOwnerId =
                    remembered["owner_id"],
                rememberedOwnerName =
                    remembered["owner_name"],
                rememberedOwnerRole =
                    remembered["owner_role"]
            )
        }

        val prefs =
            appContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val ownerAuthorized =
            prefs.getBoolean(
                KEY_OWNER_AUTHORIZED,
                false
            )

        val ownerId =
            prefs.getString(
                KEY_OWNER_ID,
                null
            )

        val method =
            prefs.getString(
                KEY_AUTH_METHOD,
                null
            )

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

        if (
            ownerAuthorized &&
            ownerId == OWNER_ID &&
            method == "ANDROID_BIOMETRIC_STRONG"
        ) {
            return AuthorityState(
                actorType =
                    ActorType.OWNER,
                authorityLevel =
                    AuthorityLevel.OWNER,
                authenticated = true,
                ownerAuthorized = true,
                ownerId = ownerId,
                authorizationMethod = method,
                authorizedAt = authorizedAt,
                message =
                    "Strong Android biometric owner authorization is active for Zaman Azimi.",
                rememberedOwnerId =
                    remembered["owner_id"],
                rememberedOwnerName =
                    remembered["owner_name"],
                rememberedOwnerRole =
                    remembered["owner_role"]
            )
        }

        return AuthorityState(
            actorType =
                ActorType.AUTHENTICATED_USER,
            authorityLevel =
                AuthorityLevel.USER,
            authenticated = true,
            ownerAuthorized = false,
            ownerId = null,
            authorizationMethod = null,
            authorizedAt = null,
            message =
                "Authenticated AZIMI user. The declared owner identity remains remembered, but owner authority is not currently active.",
            rememberedOwnerId =
                remembered["owner_id"],
            rememberedOwnerName =
                remembered["owner_name"],
            rememberedOwnerRole =
                remembered["owner_role"]
        )
    }

    /**
     * Starts the Android owner verification flow.
     */
    fun verifyOwner(
        activity: Activity,
        onResult: (OwnerVerificationResult) -> Unit
    ) {

        val appContext =
            activity.applicationContext

        initializeOwnerIdentity(appContext)

        if (!AzimiAuth.hasSession(appContext)) {
            onResult(
                OwnerVerificationResult(
                    success = false,
                    ownerAuthorized = false,
                    message =
                        "Authenticate to AZIMI AI before owner verification."
                )
            )
            return
        }

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

                    onResult(
                        OwnerVerificationResult(
                            success = false,
                            ownerAuthorized = false,
                            message =
                                "Owner verification failed: ${errString.toString().trim()}"
                        )
                    )
                }

                override fun onAuthenticationFailed() {
                    // Keep the prompt alive for another attempt.
                }
            }
        )
    }

    private fun activateOwnerAuthorityAfterVerification(
        context: Context
    ): Boolean {

        if (
            !AzimiAuth.hasSession(
                context.applicationContext
            )
        ) {
            return false
        }

        initializeOwnerIdentity(context)

        val now =
            System.currentTimeMillis()

        return context
            .applicationContext
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
                "ANDROID_BIOMETRIC_STRONG"
            )
            .putLong(
                KEY_AUTHORIZED_AT,
                now
            )
            .commit()
    }

    /**
     * Ends active owner authority.
     *
     * The remembered identity remains intact.
     */
    fun revokeOwnerAuthorization(
        context: Context
    ) {

        context
            .applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(KEY_OWNER_AUTHORIZED)
            .remove(KEY_OWNER_ID)
            .remove(KEY_AUTH_METHOD)
            .remove(KEY_AUTHORIZED_AT)
            .apply()
    }

    fun clearOwnerAuthority(
        context: Context
    ) {
        revokeOwnerAuthorization(context)
    }

    /**
     * Completely forgets the locally remembered owner identity.
     *
     * This is intentionally separate from revokeOwnerAuthorization().
     */
    fun forgetRememberedOwner(
        context: Context
    ) {

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
        return getState(context).authorityLevel ==
            AuthorityLevel.OWNER
    }

    fun isAuthenticatedUser(
        context: Context
    ): Boolean {
        return getState(context).authorityLevel ==
            AuthorityLevel.USER
    }

    fun hasOwnerAuthorization(
        context: Context
    ): Boolean {
        return getState(context).ownerAuthorized
    }

    /**
     * Determines whether the current actor may perform
     * an operation.
     */
    fun authorizeOperation(
        context: Context,
        operation: SensitiveOperation
    ): AuthorizationDecision {

        val state =
            getState(context)

        if (!state.authenticated) {
            return AuthorizationDecision(
                allowed = false,
                actorType = state.actorType,
                authorityLevel = state.authorityLevel,
                operation = operation,
                reason =
                    "Authentication is required."
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
                actorType = state.actorType,
                authorityLevel = state.authorityLevel,
                operation = operation,
                reason =
                    "Owner authorization is required for this operation."
            )
        }

        return AuthorizationDecision(
            allowed = true,
            actorType = state.actorType,
            authorityLevel = state.authorityLevel,
            operation = operation,
            reason =
                "Operation is permitted by the current authority level."
        )
    }

    fun isOwnershipSensitiveRequest(
        request: String
    ): Boolean {

        val text =
            request.trim().lowercase()

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

    fun requiresOwnerAuthorization(
        request: String
    ): Boolean {

        if (
            isOwnershipSensitiveRequest(request)
        ) {
            return true
        }

        val text =
            request.trim().lowercase()

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

        return sha256(normalized)
    }

    fun createOwnershipRecord(
        context: Context,
        projectName: String = "AZIMI",
        projectVersion: String =
            AtlasKnowledge.KNOWLEDGE_VERSION
    ): JSONObject? {

        val state =
            getState(context)

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
                projectName,
                getOwnerName(),
                projectVersion,
                timestamp
            )

        return JSONObject().apply {

            put(
                "schema_version",
                "1.0.0"
            )

            put(
                "project",
                projectName
            )

            put(
                "owner",
                getOwnerName()
            )

            put(
                "owner_id",
                OWNER_ID
            )

            put(
                "role",
                OWNER_ROLE
            )

            put(
                "project_version",
                projectVersion
            )

            put(
                "recorded_at",
                timestamp
            )

            put(
                "provenance_digest",
                digest
            )

            put(
                "statement",
                "This record identifies Zaman Azimi as the declared creator and owner of the AZIMI project within the project's ownership architecture."
            )

            put(
                "legal_status",
                "Technical provenance record; not a substitute for jurisdiction-specific legal registration or legal advice."
            )
        }
    }

    /**
     * Safe Atlas context.
     *
     * Atlas can remember who the owner is without treating
     * that identity as active authorization.
     */
    fun getSafeContext(
        context: Context
    ): Map<String, String> {

        val state =
            getState(context)

        return mapOf(
            "actor_type" to
                state.actorType.name,

            "authority_level" to
                state.authorityLevel.name,

            "authenticated" to
                state.authenticated.toString(),

            "owner_authorized" to
                state.ownerAuthorized.toString(),

            /*
             * Persistent identity.
             */
            "remembered_owner_identity" to
                (state.rememberedOwnerId ?: OWNER_ID),

            "remembered_owner_name" to
                (
                    state.rememberedOwnerName
                        ?: getOwnerName()
                    ),

            "remembered_owner_role" to
                (
                    state.rememberedOwnerRole
                        ?: OWNER_ROLE
                    ),

            /*
             * Active authority is intentionally separate.
             */
            "active_owner_identity" to
                if (state.ownerAuthorized) {
                    OWNER_ID
                } else {
                    "NOT_ACTIVE"
                },

            "authorization_method" to
                sanitizeAuthorizationMethod(
                    state.authorizationMethod.orEmpty()
                ),

            "authority_message" to
                state.message
        )
    }

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

    private fun sanitizeAuthorizationMethod(
        value: String
    ): String {

        val clean =
            value.trim()

        if (
            AzimiAuth.isProtectedCredential(clean)
        ) {
            return ""
        }

        return clean
            .take(80)
            .replace("\n", " ")
            .replace("\r", " ")
    }

    private fun sha256(
        value: String
    ): String {

        val digest =
            MessageDigest
                .getInstance("SHA-256")
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

    fun generateSecurityNonce(): ByteArray {

        val nonce =
            ByteArray(32)

        SecureRandom()
            .nextBytes(nonce)

        return nonce
    }

    fun diagnostics(
        context: Context
    ): String {

        val state =
            getState(context)

        return buildString {

            appendLine(
                "ATLAS OWNER AUTHORITY"
            )

            appendLine(
                "REMEMBERED OWNER: ${state.rememberedOwnerName ?: getOwnerName()}"
            )

            appendLine(
                "REMEMBERED OWNER ID: ${state.rememberedOwnerId ?: OWNER_ID}"
            )

            appendLine(
                "REMEMBERED OWNER ROLE: ${state.rememberedOwnerRole ?: OWNER_ROLE}"
            )

            appendLine()

            appendLine(
                "ACTIVE ACTOR: ${state.actorType}"
            )

            appendLine(
                "ACTIVE AUTHORITY: ${state.authorityLevel}"
            )

            appendLine(
                "AUTHENTICATED: ${state.authenticated}"
            )

            appendLine(
                "OWNER AUTHORIZED: ${state.ownerAuthorized}"
            )

            appendLine(
                "AUTHORIZATION METHOD: ${state.authorizationMethod ?: "NONE"}"
            )

            appendLine(
                "AUTHORIZED AT: ${state.authorizedAt ?: "NONE"}"
            )

            appendLine()

            appendLine(
                "STATUS: ${state.message}"
            )
        }
    }
}
