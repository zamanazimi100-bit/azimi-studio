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
 * SECURITY MODEL
 *
 * Declared identity and active authority are separate.
 *
 * Declared owner:
 *
 *     Zaman Azimi
 *     OWNER_ID = ZAMAN_AZIMI
 *     ROLE = FOUNDER_CREATOR_OWNER
 *
 * Remembered identity is safe context only.
 * It is NOT authorization.
 *
 * OWNER AUTHORIZATION
 *
 * Full owner authority requires every configured security
 * factor to be successfully verified.
 *
 * Current factor architecture:
 *
 *     FACTOR 1 — Android strong biometric
 *     FACTOR 2 — Voice Lock
 *
 * Face/fingerprint capability is detected separately.
 *
 * IMPORTANT ANDROID LIMITATION
 *
 * Android's generic BiometricPrompt API does not guarantee
 * that an application can force:
 *
 *     fingerprint AND face
 *
 * as two independent factors.
 *
 * BIOMETRIC_STRONG means Android verifies an enrolled strong
 * biometric and Android controls the actual modality.
 *
 * Therefore Guardian NEVER falsely records a separate face
 * verification merely because the device has face hardware.
 *
 * Voice Lock is a separate factor and MUST NOT be considered
 * verified until a real secure voice-verification mechanism
 * has completed successfully.
 *
 * Until then:
 *
 *     biometric verified
 *         +
 *     voice verified
 *         +
 *     policy valid
 *
 * are required for FULL OWNER AUTHORITY.
 *
 * SECURITY PRINCIPLES
 *
 * - No biometric template storage.
 * - No raw face data.
 * - No raw fingerprint data.
 * - No password/API key/token/recovery-code memory.
 * - No Supabase dependency for local owner authority.
 * - No email dependency for local owner authority.
 * - No remote service can silently authorize the owner.
 * - Cloud cannot unlock Z Vault.
 * - Partial authentication never opens Z Vault.
 * - Failed/revoked sessions clear factor state.
 * - Full authority is session-based.
 * - Recovery must never become an authentication bypass.
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

    /*
     * Multi-factor state.
     *
     * These are only local authorization state markers.
     *
     * They do NOT contain biometric templates.
     */

    private const val KEY_BIOMETRIC_VERIFIED =
        "factor_biometric_verified"

    private const val KEY_VOICE_VERIFIED =
        "factor_voice_verified"

    private const val KEY_BIOMETRIC_VERIFIED_AT =
        "factor_biometric_verified_at"

    private const val KEY_VOICE_VERIFIED_AT =
        "factor_voice_verified_at"

    private const val KEY_FACTOR_SESSION_ID =
        "factor_session_id"

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

    data class FactorState(
        val biometricVerified: Boolean,
        val voiceVerified: Boolean,
        val allRequiredFactorsVerified: Boolean,
        val biometricVerifiedAt: Long? = null,
        val voiceVerifiedAt: Long? = null,
        val sessionId: String? = null
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
        val fingerprintAvailable: Boolean = false,
        val biometricFactorVerified: Boolean = false,
        val voiceFactorVerified: Boolean = false,
        val allRequiredFactorsVerified: Boolean = false
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
     * Initialize persistent declared owner identity.
     *
     * This does NOT authorize the owner.
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
     * Return remembered AZIMI owner identity.
     *
     * Identity is not authorization.
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

    fun getOwnerIdentity(): String {
        return OWNER_ID
    }

    fun getOwnerName(): String {
        return AtlasKnowledge.OWNER_NAME
    }

    /**
     * Read current multi-factor state.
     */
    fun getFactorState(
        context: Context
    ): FactorState {

        val prefs =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

        val biometricVerified =
            prefs.getBoolean(
                KEY_BIOMETRIC_VERIFIED,
                false
            )

        val voiceVerified =
            prefs.getBoolean(
                KEY_VOICE_VERIFIED,
                false
            )

        val biometricVerifiedAt =
            if (
                prefs.contains(
                    KEY_BIOMETRIC_VERIFIED_AT
                )
            ) {
                prefs.getLong(
                    KEY_BIOMETRIC_VERIFIED_AT,
                    0L
                )
            } else {
                null
            }

        val voiceVerifiedAt =
            if (
                prefs.contains(
                    KEY_VOICE_VERIFIED_AT
                )
            ) {
                prefs.getLong(
                    KEY_VOICE_VERIFIED_AT,
                    0L
                )
            } else {
                null
            }

        val sessionId =
            prefs.getString(
                KEY_FACTOR_SESSION_ID,
                null
            )

        val allRequired =
            biometricVerified &&
                voiceVerified

        return FactorState(
            biometricVerified =
                biometricVerified,

            voiceVerified =
                voiceVerified,

            allRequiredFactorsVerified =
                allRequired,

            biometricVerifiedAt =
                biometricVerifiedAt,

            voiceVerifiedAt =
                voiceVerifiedAt,

            sessionId =
                sessionId
        )
    }

    /**
     * Current Guardian authority state.
     *
     * FULL OWNER authority exists only when every required
     * factor has been verified in the current authorization
     * session.
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

        val factors =
            getFactorState(
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
                ) &&
                factors.allRequiredFactorsVerified

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
                    "All required owner authentication factors are verified. AZIMI owner authority is active for the current Guardian session.",

                rememberedOwnerId =
                    remembered["owner_id"],

                rememberedOwnerName =
                    remembered["owner_name"],

                rememberedOwnerRole =
                    remembered["owner_role"],

                faceLockAvailable =
                    capabilities.faceCapable,

                fingerprintAvailable =
                    capabilities.fingerprintCapable,

                biometricFactorVerified =
                    factors.biometricVerified,

                voiceFactorVerified =
                    factors.voiceVerified,

                allRequiredFactorsVerified =
                    factors.allRequiredFactorsVerified
            )
        }

        val partialMessage =
            when {

                factors.biometricVerified &&
                    !factors.voiceVerified ->
                    "Biometric factor verified. Voice Lock verification is still required. Z Vault remains locked."

                !factors.biometricVerified &&
                    factors.voiceVerified ->
                    "Voice factor state exists without the required biometric factor. Full owner authority remains locked."

                else ->
                    "Owner authentication is incomplete. Z Vault remains locked."
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
                partialMessage,

            rememberedOwnerId =
                remembered["owner_id"],

            rememberedOwnerName =
                remembered["owner_name"],

            rememberedOwnerRole =
                remembered["owner_role"],

            faceLockAvailable =
                capabilities.faceCapable,

            fingerprintAvailable =
                capabilities.fingerprintCapable,

            biometricFactorVerified =
                factors.biometricVerified,

            voiceFactorVerified =
                factors.voiceVerified,

            allRequiredFactorsVerified =
                factors.allRequiredFactorsVerified
        )
    }

    /**
     * Start the Android strong-biometric factor.
     *
     * IMPORTANT:
     *
     * Successful biometric authentication alone does NOT
     * activate full owner authority.
     *
     * Voice Lock must still be verified.
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
         * Start a fresh authentication session.
         *
         * A previous partial factor state must never silently
         * become a new authorization session.
         */
        clearFactorVerificationState(
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
                        "Strong Biometric Factor"
                    } else {
                        "Strong Biometric Security"
                    }
                )
                .setDescription(
                    "Complete the biometric factor. Android protects biometric data. Guardian never stores biometric templates."
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
                                "Owner verification was cancelled. Z Vault remains locked."
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

                    val biometricRecorded =
                        recordBiometricFactor(
                            appContext,
                            authorizationMethod
                        )

                    if (!biometricRecorded) {

                        revokeOwnerAuthorization(
                            appContext
                        )

                        onResult(
                            OwnerVerificationResult(
                                success = false,
                                ownerAuthorized = false,
                                message =
                                    "Biometric verification succeeded, but the biometric factor could not be safely recorded. Z Vault remains locked."
                            )
                        )

                        return
                    }

                    /*
                     * IMPORTANT:
                     *
                     * Do NOT activate full owner authority here.
                     *
                     * Voice Lock is still required.
                     */
                    onResult(
                        OwnerVerificationResult(
                            success = true,
                            ownerAuthorized = false,
                            message =
                                "Biometric factor verified. Voice Lock is still required before full Z Vault owner authority can be granted."
                        )
                    )
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
                                    "Owner biometric verification failed: $message. Z Vault remains locked."
                                } else {
                                    "Owner biometric verification failed. Z Vault remains locked."
                                }
                        )
                    )
                }

                override fun onAuthenticationFailed() {
                    /*
                     * Keep the Android biometric prompt active.
                     *
                     * A failed attempt does not grant any factor.
                     */
                }
            }
        )
    }

    /**
     * Record successful biometric verification.
     *
     * This does NOT grant full owner authority.
     */
    private fun recordBiometricFactor(
        context: Context,
        authorizationMethod: String
    ): Boolean {

        val appContext =
            context.applicationContext

        val sessionId =
            generateSessionIdentifier()

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
                    KEY_BIOMETRIC_VERIFIED,
                    true
                )
                .putLong(
                    KEY_BIOMETRIC_VERIFIED_AT,
                    now
                )
                .putString(
                    KEY_FACTOR_SESSION_ID,
                    sessionId
                )
                .putString(
                    KEY_AUTH_METHOD,
                    authorizationMethod
                )
                .remove(
                    KEY_VOICE_VERIFIED
                )
                .remove(
                    KEY_VOICE_VERIFIED_AT
                )
                .remove(
                    KEY_OWNER_AUTHORIZED
                )
                .remove(
                    KEY_OWNER_ID
                )
                .remove(
                    KEY_AUTHORIZED_AT
                )
                .commit()

        activeOwnerAuthorization = false

        return committed
    }

    /**
     * Voice Lock completion hook.
     *
     * SECURITY:
     *
     * This function intentionally does NOT accept an arbitrary
     * Boolean such as "voice=true".
     *
     * A future real Voice Lock implementation must call
     * completeVoiceFactorVerification() only after its own
     * secure verification process has succeeded.
     *
     * Until that implementation exists, this method returns
     * false and cannot unlock the Vault.
     */
    fun completeVoiceFactorVerification(
        context: Context
    ): Boolean {

        val appContext = context.applicationContext

        /*
         * VoiceLock is the only component allowed to establish the
         * voice factor. This method does not accept an arbitrary
         * Boolean and cannot be used to bypass the verifier.
         */
        if (!VoiceLock.consumeVerifiedFactor(appContext)) {
            return false
        }

        val factors = getFactorState(appContext)

        if (!factors.biometricVerified) {
            return false
        }

        val method =
            getStoredAuthorizationMethod(appContext)
                ?: AUTH_METHOD_UNKNOWN

        if (!recordVoiceFactor(appContext)) {
            return false
        }

        return activateOwnerAuthorityAfterAllFactors(
            appContext,
            method
        )
    }

    /**
     * Internal authorization completion function for the
     * future real Voice Lock implementation.
     *
     * This remains private so ordinary application code cannot
     * simply claim that voice authentication succeeded.
     */
    private fun getStoredAuthorizationMethod(
        context: Context
    ): String? {
        return context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_AUTH_METHOD,
                null
            )
    }

    private fun recordVoiceFactor(
        context: Context
    ): Boolean {

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val sessionId = prefs.getString(
            KEY_FACTOR_SESSION_ID,
            null
        ) ?: return false

        return prefs.edit()
            .putBoolean(
                KEY_VOICE_VERIFIED,
                true
            )
            .putLong(
                KEY_VOICE_VERIFIED_AT,
                System.currentTimeMillis()
            )
            .putString(
                KEY_FACTOR_SESSION_ID,
                sessionId
            )
            .commit()
    }

    private fun activateOwnerAuthorityAfterAllFactors(
        context: Context,
        authorizationMethod: String
    ): Boolean {

        val appContext =
            context.applicationContext

        val factors =
            getFactorState(
                appContext
            )

        if (
            !factors.biometricVerified ||
            !factors.voiceVerified
        ) {
            return false
        }

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
     * Clear all partial factor state.
     *
     * This is used whenever a security session is revoked.
     */
    private fun clearFactorVerificationState(
        context: Context
    ) {

        context
            .applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                KEY_BIOMETRIC_VERIFIED
            )
            .remove(
                KEY_VOICE_VERIFIED
            )
            .remove(
                KEY_BIOMETRIC_VERIFIED_AT
            )
            .remove(
                KEY_VOICE_VERIFIED_AT
            )
            .remove(
                KEY_FACTOR_SESSION_ID
            )
            .apply()
    }

    /**
     * Revoke the complete owner authorization session.
     *
     * Remembered identity remains intact.
     */
    fun revokeOwnerAuthorization(
        context: Context
    ) {

        activeOwnerAuthorization = false
        VoiceLock.clearVerification(context)

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
            .remove(
                KEY_BIOMETRIC_VERIFIED
            )
            .remove(
                KEY_VOICE_VERIFIED
            )
            .remove(
                KEY_BIOMETRIC_VERIFIED_AT
            )
            .remove(
                KEY_VOICE_VERIFIED_AT
            )
            .remove(
                KEY_FACTOR_SESSION_ID
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
     * This is stronger than locking the session.
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
                    "All required Guardian owner authentication factors must be verified."
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
                    "Full AZIMI owner authorization is required for this operation."
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
     * Atlas may know the declared owner identity while
     * authorization is locked.
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

            "biometric_factor_verified" to
                state.biometricFactorVerified.toString(),

            "voice_factor_verified" to
                state.voiceFactorVerified.toString(),

            "all_required_factors_verified" to
                state.allRequiredFactorsVerified.toString(),

            "authority_message" to
                state.message
        )
    }

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
     * Discover biometric capabilities.
     *
     * No biometric templates are accessed.
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

    fun generateSecurityNonce(): ByteArray {

        val nonce =
            ByteArray(32)

        SecureRandom()
            .nextBytes(
                nonce
            )

        return nonce
    }

    private fun generateSessionIdentifier(): String {

        return Base64.encodeToString(
            generateSecurityNonce(),
            Base64.NO_WRAP or
                Base64.URL_SAFE
        )
    }

    /**
     * Human-readable diagnostics.
     *
     * No credentials or biometric templates are included.
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

        val factors =
            getFactorState(
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

            appendLine()

            appendLine(
                "REQUIRED FACTORS"
            )

            appendLine(
                "BIOMETRIC FACTOR VERIFIED: ${
                    factors.biometricVerified
                }"
            )

            appendLine(
                "VOICE FACTOR VERIFIED: ${
                    factors.voiceVerified
                }"
            )

            appendLine(
                "ALL REQUIRED FACTORS VERIFIED: ${
                    factors.allRequiredFactorsVerified
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
                "VOICE TEMPLATE STORAGE: NOT IMPLEMENTED"
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
