package com.azimi.guardian

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
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
 * Atlas remembers the declared AZIMI owner identity locally.
 *
 * Declared owner:
 *
 *     Zaman Azimi
 *     OWNER_ID = ZAMAN_AZIMI
 *     ROLE = FOUNDER_CREATOR_OWNER
 *
 * Remembered identity does NOT grant permission.
 *
 * Active owner authority requires successful Guardian-local
 * biometric verification.
 *
 * Current supported Android verification:
 *
 *     BIOMETRIC_STRONG
 *
 * Face Lock:
 *
 *     Face authentication is supported through Android's secure
 *     biometric framework when the device exposes a compatible
 *     strong biometric face authenticator.
 *
 *     Guardian does NOT access, copy, store, or process raw face
 *     biometric templates.
 *
 * Important Android limitation:
 *
 *     The generic Android biometric API does not guarantee that
 *     an application can force a specific biometric modality such
 *     as "face only". The Android system decides which enrolled
 *     strong biometric is used by the secure biometric prompt.
 *
 * Therefore:
 *
 *     FACE_SUPPORTED
 *     FACE_CAPABLE
 *
 * are capability/security states, not claims that Guardian has
 * obtained or stored a face template.
 *
 * Future:
 *
 *     VOICE_LOCK
 *
 * must be implemented as a separate security mechanism and must
 * not be considered active until actually implemented and tested.
 *
 * SECURITY PRINCIPLES:
 *
 * - Owner identity is persistent safe context.
 * - Owner authority is session-based.
 * - Remote authentication never becomes local owner authority.
 * - No Supabase dependency for Guardian owner authorization.
 * - No email dependency for Guardian owner authorization.
 * - No biometric template is stored by Atlas.
 * - No password/API key/token/recovery code is stored as memory.
 * - Vault authority remains protected by Guardian.
 * - Cloud cannot silently unlock the Vault.
 */
object AtlasOwnerAuthority {

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

    private const val AUTH_METHOD_FACE =
        "ANDROID_BIOMETRIC_STRONG_FACE_CAPABLE"

    private const val AUTH_METHOD_FINGERPRINT =
        "ANDROID_BIOMETRIC_STRONG_FINGERPRINT_OR_OTHER"

    private const val AUTH_METHOD_UNKNOWN =
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

    enum class BiometricCapability {
        FACE_CAPABLE,
        FINGERPRINT_CAPABLE,
        BIOMETRIC_STRONG_AVAILABLE,
        BIOMETRIC_UNAVAILABLE,
        NOT_SUPPORTED
    }

    data class BiometricCapabilities(
        val faceCapable: Boolean,
        val fingerprintCapable: Boolean,
        val strongBiometricAvailable: Boolean,
        val primaryCapability: BiometricCapability,
        val message: String
    )

    data class AuthorityState(
        val actorType: ActorType,
        val authorityLevel: AuthorityLevel,
        val authenticated: Boolean,
        val ownerAuthorized: Boolean,
        val ownerId: String?,
        val authorizationMethod: String?,
        val authorizedAt: Long?,
        val message: String,
        val rememberedOwnerId: String? = null,
        val rememberedOwnerName: String? = null,
        val rememberedOwnerRole: String? = null,
        val faceLockAvailable: Boolean = false,
        val fingerprintAvailable: Boolean = false
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
     * Initialize the persistent declared AZIMI owner identity.
     *
     * This does NOT activate owner authority.
     *
     * The identity record is safe context only.
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
     * Return the remembered AZIMI owner identity.
     *
     * This identity is intentionally independent from the
     * active authorization session.
     */
    fun getRememberedOwnerIdentity(
        context: Context
    ): Map<String, String> {

        initializeOwnerIdentity(
            context
        )

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

    /**
     * Stable AZIMI owner identifier.
     */
    fun getOwnerIdentity(): String {
        return OWNER_ID
    }

    /**
     * Declared creator/owner name.
     *
     * This comes from the AZIMI knowledge identity record.
     */
    fun getOwnerName(): String {
        return AtlasKnowledge.OWNER_NAME
    }

    /**
     * Return the current Guardian authority state.
     *
     * Owner authority requires:
     *
     *     1. active in-memory authorization
     *     2. matching persisted authorization metadata
     *
     * Persisted authorization metadata alone cannot restore
     * owner authority after process restart.
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

        val capabilities =
            getBiometricCapabilities(
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
                isValidAuthorizationMethod(
                    storedMethod
                )

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
                    "AZIMI owner authority is active for the current Guardian session.",

                rememberedOwnerId =
                    remembered["owner_id"],

                rememberedOwnerName =
                    remembered["owner_name"],

                rememberedOwnerRole =
                    remembered["owner_role"],

                faceLockAvailable =
                    capabilities.faceCapable,

                fingerprintAvailable =
                    capabilities.fingerprintCapable
            )
        }

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
                "No active Guardian owner authorization is available. The declared AZIMI owner identity remains remembered, but authorization is locked.",

            rememberedOwnerId =
                remembered["owner_id"],

            rememberedOwnerName =
                remembered["owner_name"],

            rememberedOwnerRole =
                remembered["owner_role"],

            faceLockAvailable =
                capabilities.faceCapable,

            fingerprintAvailable =
                capabilities.fingerprintCapable
        )
    }

    /**
     * Start Guardian-local owner verification.
     *
     * Android's secure BIOMETRIC_STRONG prompt is used.
     *
     * If the device supports strong face authentication,
     * Android may present/use the face biometric.
     *
     * Guardian never receives or stores the biometric template.
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

            revokeOwnerAuthorization(
                appContext
            )

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

        val capabilities =
            getBiometricCapabilities(
                appContext
            )

        val executor: Executor =
            activity.mainExecutor

        val prompt =
            BiometricPrompt.Builder(
                activity
            )
                .setTitle(
                    "AZIMI OWNER LOCK"
                )
                .setSubtitle(
                    if (capabilities.faceCapable) {
                        "Face / Fingerprint Security"
                    } else {
                        "Strong Biometric Security"
                    }
                )
                .setDescription(
                    "Verify Zaman Azimi owner authority. Android protects the biometric data; Guardian never stores biometric templates."
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

                    val authorizationMethod =
                        determineAuthorizationMethod(
                            appContext
                        )

                    val authorized =
                        activateOwnerAuthorityAfterVerification(
                            appContext,
                            authorizationMethod
                        )

                    if (authorized) {

                        val methodMessage =
                            when (
                                authorizationMethod
                            ) {

                                AUTH_METHOD_FACE ->
                                    "Owner authority verified through Android strong biometric security. This device reports Face capability."

                                AUTH_METHOD_FINGERPRINT ->
                                    "Owner authority verified through Android strong biometric security. Fingerprint or another strong biometric may have been used."

                                else ->
                                    "Owner authority verified through Android BIOMETRIC_STRONG."
                            }

                        onResult(
                            OwnerVerificationResult(
                                success = true,
                                ownerAuthorized = true,
                                message =
                                    methodMessage
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
                     * Keep the biometric prompt active.
                     *
                     * Android may allow another attempt.
                     */
                }
            }
        )
    }

    /**
     * Determine the safest authorization metadata available
     * without accessing biometric templates.
     *
     * Android's generic biometric framework does not reliably
     * expose the exact modality as "face" versus "fingerprint"
     * to every application/device combination.
     *
     * Therefore this function reports FACE capability only when
     * the device declares the Android face hardware feature.
     */
    private fun determineAuthorizationMethod(
        context: Context
    ): String {

        val packageManager =
            context.packageManager

        val faceFeature =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                packageManager.hasSystemFeature(
                    PackageManager.FEATURE_FACE
                )
            } else {
                false
            }

        val fingerprintFeature =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {
                packageManager.hasSystemFeature(
                    PackageManager.FEATURE_FINGERPRINT
                )
            } else {
                false
            }

        return when {

            faceFeature ->
                AUTH_METHOD_FACE

            fingerprintFeature ->
                AUTH_METHOD_FINGERPRINT

            else ->
                AUTH_METHOD_UNKNOWN
        }
    }

    /**
     * Discover biometric capabilities without exposing
     * biometric templates or biometric identifiers.
     */
    fun getBiometricCapabilities(
        context: Context
    ): BiometricCapabilities {

        val appContext =
            context.applicationContext

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.P
        ) {
            return BiometricCapabilities(
                faceCapable = false,
                fingerprintCapable = false,
                strongBiometricAvailable = false,
                primaryCapability =
                    BiometricCapability.NOT_SUPPORTED,
                message =
                    "Strong Android biometric APIs are not supported on this Android version."
            )
        }

        val packageManager =
            appContext.packageManager

        val faceCapable =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {
                packageManager.hasSystemFeature(
                    PackageManager.FEATURE_FACE
                )
            } else {
                false
            }

        val fingerprintCapable =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M
            ) {
                packageManager.hasSystemFeature(
                    PackageManager.FEATURE_FINGERPRINT
                )
            } else {
                false
            }

        val biometricManager =
            appContext.getSystemService(
                BiometricManager::class.java
            )

        val strongAvailable =
            biometricManager != null &&
                biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                ) ==
                BiometricManager.BIOMETRIC_SUCCESS

        val capability =
            when {

                strongAvailable &&
                    faceCapable ->
                    BiometricCapability.FACE_CAPABLE

                strongAvailable &&
                    fingerprintCapable ->
                    BiometricCapability.FINGERPRINT_CAPABLE

                strongAvailable ->
                    BiometricCapability.BIOMETRIC_STRONG_AVAILABLE

                else ->
                    BiometricCapability.BIOMETRIC_UNAVAILABLE
            }

        val message =
            when (capability) {

                BiometricCapability.FACE_CAPABLE ->
                    "Strong biometric authentication is available and this device declares face biometric hardware."

                BiometricCapability.FINGERPRINT_CAPABLE ->
                    "Strong biometric authentication is available and this device declares fingerprint hardware."

                BiometricCapability.BIOMETRIC_STRONG_AVAILABLE ->
                    "Strong biometric authentication is available."

                BiometricCapability.BIOMETRIC_UNAVAILABLE ->
                    "Strong biometric authentication is currently unavailable."

                BiometricCapability.NOT_SUPPORTED ->
                    "Strong biometric authentication is not supported."
            }

        return BiometricCapabilities(
            faceCapable =
                faceCapable,

            fingerprintCapable =
                fingerprintCapable,

            strongBiometricAvailable =
                strongAvailable,

            primaryCapability =
                capability,

            message =
                message
        )
    }

    /**
     * Activate owner authority after Android biometric
     * verification has already succeeded.
     *
     * The authorization is session-based.
     */
    private fun activateOwnerAuthorityAfterVerification(
        context: Context,
        authorizationMethod: String
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
                    authorizationMethod
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
     * This is intentionally much stronger than simply locking
     * the current owner session.
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
     * Check whether a sensitive operation is permitted.
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
     * Detect ownership/provenance requests.
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
     * Safe Atlas owner context.
     *
     * Atlas may know the declared owner identity even while
     * the owner authority session is locked.
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
            "declared_creator" to
                rememberedOwnerName,

            "declared_owner" to
                rememberedOwnerName,

            "owner_id" to
                rememberedOwnerId,

            "owner_role" to
                rememberedOwnerRole,

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

            "face_lock_available" to
                state.faceLockAvailable.toString(),

            "fingerprint_available" to
                state.fingerprintAvailable.toString(),

            "authority_message" to
                state.message
        )
    }

    /**
     * Android biometric availability diagnostic.
     */
    private fun biometricStatusMessage(
        status: Int
    ): String {

        return when (status) {

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "This device does not provide compatible strong biometric hardware."

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
     * Validate authorization metadata.
     */
    private fun isValidAuthorizationMethod(
        method: String?
    ): Boolean {

        return when (method) {

            AUTH_METHOD_BIOMETRIC_STRONG,
            AUTH_METHOD_FACE,
            AUTH_METHOD_FINGERPRINT ->
                true

            else ->
                false
        }
    }

    /**
     * Prevent protected credential material from entering
     * Atlas context.
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
            .take(120)
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
     * Cryptographically strong random security nonce.
     *
     * No biometric information is involved.
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
     * Human-readable diagnostics.
     *
     * No credentials or biometric data are included.
     */
    fun diagnostics(
        context: Context
    ): String {

        val state =
            getState(
                context
            )

        val capabilities =
            getBiometricCapabilities(
                context
            )

        return buildString {

            appendLine(
                "ATLAS OWNER AUTHORITY"
            )

            appendLine(
                "DECLARED CREATOR: ${
                    state.rememberedOwnerName
                        ?: getOwnerName()
                }"
            )

            appendLine(
                "OWNER ID: ${
                    state.rememberedOwnerId
                        ?: OWNER_ID
                }"
            )

            appendLine(
                "OWNER ROLE: ${
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
                "FACE CAPABLE: ${
                    capabilities.faceCapable
                }"
            )

            appendLine(
                "FINGERPRINT CAPABLE: ${
                    capabilities.fingerprintCapable
                }"
            )

            appendLine(
                "STRONG BIOMETRIC AVAILABLE: ${
                    capabilities.strongBiometricAvailable
                }"
            )

            appendLine(
                "BIOMETRIC STATUS: ${
                    capabilities.primaryCapability
                }"
            )

            appendLine()

            appendLine(
                "BIOMETRIC TEMPLATE STORAGE: ANDROID CONTROLLED"
            )

            appendLine(
                "ATLAS BIOMETRIC TEMPLATE ACCESS: NONE"
            )

            appendLine(
                "REMOTE AUTHENTICATION REQUIRED: NO"
            )

            appendLine()

            appendLine(
                "STATUS: ${state.message}"
            )
        }
    }
}
