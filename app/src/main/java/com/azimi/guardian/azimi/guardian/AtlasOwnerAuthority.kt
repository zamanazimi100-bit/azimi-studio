package com.azimi.guardian

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * AZIMI — Atlas Owner Authority
 *
 * Purpose:
 * - Establish a clear distinction between the AZIMI owner and other users.
 * - Keep ownership authority separate from ordinary authentication.
 * - Provide deterministic authorization decisions for Atlas.
 * - Protect ownership, provenance, recovery and security operations.
 *
 * Important:
 * This component does NOT bypass Android, Supabase, GitHub, Vercel,
 * or any other platform security boundary.
 *
 * Authentication proves that a session exists.
 * Owner authority requires an additional owner-authorization state.
 */
object AtlasOwnerAuthority {

    private const val PREFS_NAME = "azimi_owner_authority"

    private const val KEY_OWNER_AUTHORIZED = "owner_authorized"
    private const val KEY_OWNER_ID = "owner_id"
    private const val KEY_AUTH_METHOD = "authorization_method"
    private const val KEY_AUTHORIZED_AT = "authorized_at"

    private const val OWNER_ID = "ZAMAN_AZIMI"

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
        val message: String
    )

    data class AuthorizationDecision(
        val allowed: Boolean,
        val actorType: ActorType,
        val authorityLevel: AuthorityLevel,
        val operation: SensitiveOperation,
        val reason: String
    )

    /**
     * Returns the canonical AZIMI owner identity.
     *
     * This is an application-level identity label.
     * It is NOT a cryptographic proof by itself.
     */
    fun getOwnerIdentity(): String = OWNER_ID

    fun getOwnerName(): String = AtlasKnowledge.OWNER_NAME

    /**
     * Returns the current authority state.
     *
     * A stored owner authorization is never treated as equivalent
     * to a valid authentication session.
     */
    fun getState(context: Context): AuthorityState {
        val appContext = context.applicationContext
        val authenticated = AzimiAuth.hasSession(appContext)

        if (!authenticated) {
            return AuthorityState(
                actorType = ActorType.GUEST,
                authorityLevel = AuthorityLevel.NONE,
                authenticated = false,
                ownerAuthorized = false,
                ownerId = null,
                authorizationMethod = null,
                authorizedAt = null,
                message = "No authenticated AZIMI session is available."
            )
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val ownerAuthorized =
            prefs.getBoolean(KEY_OWNER_AUTHORIZED, false)

        val ownerId =
            prefs.getString(KEY_OWNER_ID, null)

        val method =
            prefs.getString(KEY_AUTH_METHOD, null)

        val authorizedAt =
            if (prefs.contains(KEY_AUTHORIZED_AT)) {
                prefs.getLong(KEY_AUTHORIZED_AT, 0L)
            } else {
                null
            }

        if (ownerAuthorized && ownerId == OWNER_ID) {
            return AuthorityState(
                actorType = ActorType.OWNER,
                authorityLevel = AuthorityLevel.OWNER,
                authenticated = true,
                ownerAuthorized = true,
                ownerId = ownerId,
                authorizationMethod = method,
                authorizedAt = authorizedAt,
                message = "Owner authorization is active for Zaman Azimi."
            )
        }

        return AuthorityState(
            actorType = ActorType.AUTHENTICATED_USER,
            authorityLevel = AuthorityLevel.USER,
            authenticated = true,
            ownerAuthorized = false,
            ownerId = null,
            authorizationMethod = null,
            authorizedAt = null,
            message = "Authenticated AZIMI user. Owner authority is not active."
        )
    }

    /**
     * Owner authorization must be explicitly established.
     *
     * This method intentionally does NOT allow an arbitrary caller
     * to simply claim to be Zaman.
     *
     * A real production implementation should connect this step
     * to a stronger owner-authentication mechanism such as Android
     * BiometricPrompt, a hardware-backed credential, or another
     * cryptographically verifiable owner factor.
     *
     * For now this method only records an authorization state when
     * the caller has already supplied a valid owner authorization
     * decision through the trusted Guardian layer.
     */
    fun authorizeOwner(
        context: Context,
        authorizationMethod: String
    ): Boolean {
        val appContext = context.applicationContext

        if (!AzimiAuth.hasSession(appContext)) {
            return false
        }

        val cleanMethod = authorizationMethod.trim()

        if (cleanMethod.isBlank()) {
            return false
        }

        val safeMethod = sanitizeAuthorizationMethod(cleanMethod)

        if (safeMethod.isBlank()) {
            return false
        }

        val now = System.currentTimeMillis()

        appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OWNER_AUTHORIZED, true)
            .putString(KEY_OWNER_ID, OWNER_ID)
            .putString(KEY_AUTH_METHOD, safeMethod)
            .putLong(KEY_AUTHORIZED_AT, now)
            .apply()

        return true
    }

    /**
     * Ends the current owner-authority state.
     *
     * This does not sign the user out of AZIMI AI.
     */
    fun revokeOwnerAuthorization(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    /**
     * Completely clears owner-authority state.
     *
     * Alias kept explicit for recovery/security workflows.
     */
    fun clearOwnerAuthority(context: Context) {
        revokeOwnerAuthorization(context)
    }

    fun isOwner(context: Context): Boolean {
        return getState(context).authorityLevel == AuthorityLevel.OWNER
    }

    fun isAuthenticatedUser(context: Context): Boolean {
        return getState(context).authorityLevel == AuthorityLevel.USER
    }

    fun hasOwnerAuthorization(context: Context): Boolean {
        return getState(context).ownerAuthorized
    }

    /**
     * Determines whether the current actor may perform an operation.
     *
     * Owner-sensitive operations require OWNER authority.
     * Ordinary CREATE operations may be performed by an authenticated
     * user, but ownership/security/recovery operations remain protected.
     */
    fun authorizeOperation(
        context: Context,
        operation: SensitiveOperation
    ): AuthorizationDecision {

        val state = getState(context)

        if (!state.authenticated) {
            return AuthorizationDecision(
                allowed = false,
                actorType = state.actorType,
                authorityLevel = state.authorityLevel,
                operation = operation,
                reason = "Authentication is required."
            )
        }

        val ownerOnly = when (operation) {
            SensitiveOperation.OWNERSHIP_RECORD,
            SensitiveOperation.PROVENANCE_RECORD,
            SensitiveOperation.RECOVERY,
            SensitiveOperation.SECURITY_CONFIGURATION,
            SensitiveOperation.MEMORY_CONFIGURATION,
            SensitiveOperation.PROVIDER_CONFIGURATION,
            SensitiveOperation.SYSTEM_CONFIGURATION -> true

            SensitiveOperation.CREATE,
            SensitiveOperation.MODIFY,
            SensitiveOperation.DELETE,
            SensitiveOperation.BACKUP -> false
        }

        if (ownerOnly && state.authorityLevel != AuthorityLevel.OWNER) {
            return AuthorizationDecision(
                allowed = false,
                actorType = state.actorType,
                authorityLevel = state.authorityLevel,
                operation = operation,
                reason = "Owner authorization is required for this operation."
            )
        }

        return AuthorizationDecision(
            allowed = true,
            actorType = state.actorType,
            authorityLevel = state.authorityLevel,
            operation = operation,
            reason = "Operation is permitted by the current authority level."
        )
    }

    /**
     * Determines whether a request appears to be attempting to alter
     * ownership or authority.
     */
    fun isOwnershipSensitiveRequest(request: String): Boolean {
        val text = request.trim().lowercase()

        if (text.isBlank()) return false

        val markers = listOf(
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

        return markers.any { text.contains(it) }
    }

    /**
     * Determines whether a request should be treated as owner-sensitive.
     */
    fun requiresOwnerAuthorization(request: String): Boolean {
        if (isOwnershipSensitiveRequest(request)) {
            return true
        }

        val text = request.trim().lowercase()

        val sensitiveMarkers = listOf(
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

        return sensitiveMarkers.any { text.contains(it) }
    }

    /**
     * Creates a deterministic digest for a provenance record.
     *
     * This does NOT constitute legal ownership proof.
     * It can help establish that the exact recorded content existed
     * in a particular form at the time the digest was created.
     */
    fun createProvenanceDigest(
        projectName: String,
        ownerName: String,
        projectVersion: String,
        recordedAt: Long
    ): String {

        val normalized = listOf(
            projectName.trim(),
            ownerName.trim(),
            projectVersion.trim(),
            recordedAt.toString()
        ).joinToString("|")

        return sha256(normalized)
    }

    /**
     * Builds a safe ownership/provenance record.
     *
     * No passwords, tokens, API keys, recovery codes or private
     * credentials are accepted into the record.
     */
    fun createOwnershipRecord(
        context: Context,
        projectName: String = "AZIMI",
        projectVersion: String = AtlasKnowledge.KNOWLEDGE_VERSION
    ): JSONObject? {

        val state = getState(context)

        if (state.authorityLevel != AuthorityLevel.OWNER) {
            return null
        }

        val timestamp = System.currentTimeMillis()

        val digest = createProvenanceDigest(
            projectName = projectName,
            ownerName = getOwnerName(),
            projectVersion = projectVersion,
            recordedAt = timestamp
        )

        return JSONObject().apply {
            put("schema_version", "1.0.0")
            put("project", projectName)
            put("owner", getOwnerName())
            put("owner_id", OWNER_ID)
            put("role", "FOUNDER_CREATOR_OWNER")
            put("project_version", projectVersion)
            put("recorded_at", timestamp)
            put("provenance_digest", digest)
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
     * Safe context for Atlas.
     *
     * This deliberately exposes authority state, not credentials.
     */
    fun getSafeContext(context: Context): Map<String, String> {
        val state = getState(context)

        return mapOf(
            "actor_type" to state.actorType.name,
            "authority_level" to state.authorityLevel.name,
            "authenticated" to state.authenticated.toString(),
            "owner_authorized" to state.ownerAuthorized.toString(),
            "owner_identity" to if (state.ownerAuthorized) OWNER_ID else "NOT_DISCLOSED",
            "owner_name" to if (state.ownerAuthorized) getOwnerName() else "NOT_DISCLOSED",
            "authorization_method" to state.authorizationMethod.orEmpty(),
            "authority_message" to state.message
        )
    }

    /**
     * Never permit credential-like authorization method labels
     * to become part of the safe project context.
     */
    private fun sanitizeAuthorizationMethod(value: String): String {
        val clean = value.trim()

        if (AzimiAuth.isProtectedCredential(clean)) {
            return ""
        }

        return clean
            .take(80)
            .replace("\n", " ")
            .replace("\r", " ")
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(
            digest,
            Base64.NO_WRAP or Base64.URL_SAFE
        )
    }

    /**
     * Generates random local entropy for future owner-security
     * mechanisms.
     *
     * The generated bytes are intentionally returned only to the
     * caller and are never written to Atlas memory.
     */
    fun generateSecurityNonce(): ByteArray {
        val nonce = ByteArray(32)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    fun diagnostics(context: Context): String {
        val state = getState(context)

        return buildString {
            appendLine("ATLAS OWNER AUTHORITY")
            appendLine("Owner: ${getOwnerName()}")
            appendLine("Owner ID: $OWNER_ID")
            appendLine("Actor: ${state.actorType}")
            appendLine("Authority: ${state.authorityLevel}")
            appendLine("Authenticated: ${state.authenticated}")
            appendLine("Owner Authorized: ${state.ownerAuthorized}")
            appendLine("Authorization Method: ${state.authorizationMethod ?: "NONE"}")
            appendLine("Authorized At: ${state.authorizedAt ?: "NONE"}")
            appendLine("STATUS: ${state.message}")
        }
    }
}
