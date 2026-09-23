package com.azimi.guardian

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.UUID

/**
 * AZIMI Guardian Atlas Cryptographic Identity
 *
 * This identity belongs to the Guardian installation.
 *
 * The private key:
 * - is generated inside Android Keystore
 * - never leaves the device
 * - is never returned to application code as raw bytes
 * - is never sent to Vercel
 * - is never sent to Cloudflare
 * - is never stored in SharedPreferences
 *
 * The public key may be registered with the AZIMI server
 * during one-time Guardian enrollment.
 *
 * Owner authorization remains controlled by
 * AtlasOwnerAuthority.
 *
 * Cryptographic identity proves:
 *
 * "This request was produced by the enrolled Guardian
 * installation."
 *
 * Owner authority proves:
 *
 * "Guardian currently has an active owner-authorized
 * session."
 */
object GuardianAtlasIdentity {

    private const val KEYSTORE_PROVIDER =
        "AndroidKeyStore"

    private const val KEY_ALIAS =
        "AZIMI_ATLAS_GUARDIAN_IDENTITY"

    private const val SIGNATURE_ALGORITHM =
        "SHA256withECDSA"

    private const val HASH_ALGORITHM =
        "SHA-256"

    /**
     * Stable identifier for the first AZIMI Guardian owner
     * installation.
     *
     * This is NOT a secret.
     */
    const val KEY_ID =
        "ZAMAN-AZIMI-GUARDIAN-01"

    /**
     * Create the Guardian key pair if it does not already exist.
     *
     * The private key remains inside Android Keystore.
     */
    fun ensureIdentity(
        context: Context
    ): Boolean {

        return runCatching {

            val keyStore =
                KeyStore.getInstance(
                    KEYSTORE_PROVIDER
                ).apply {
                    load(null)
                }

            if (
                keyStore.containsAlias(
                    KEY_ALIAS
                )
            ) {
                return@runCatching true
            }

            val generator =
                KeyPairGenerator.getInstance(
                    "EC",
                    KEYSTORE_PROVIDER
                )

            generator.initialize(
                ECGenParameterSpec(
                    "secp256r1"
                )
            )

            generator.generateKeyPair()

            true

        }.getOrDefault(false)
    }

    /**
     * Returns the public key in PEM format.
     *
     * Safe to expose during enrollment.
     *
     * NEVER expose a private key.
     */
    fun getPublicKeyPem(
        context: Context
    ): String? {

        return runCatching {

            if (
                !ensureIdentity(context)
            ) {
                return@runCatching null
            }

            val keyStore =
                KeyStore.getInstance(
                    KEYSTORE_PROVIDER
                ).apply {
                    load(null)
                }

            val certificate =
                keyStore.getCertificate(
                    KEY_ALIAS
                )
                    ?: return@runCatching null

            val encoded =
                certificate.publicKey.encoded

            val base64 =
                Base64.encodeToString(
                    encoded,
                    Base64.NO_WRAP
                )

            buildString {

                append(
                    "-----BEGIN PUBLIC KEY-----\n"
                )

                base64
                    .chunked(64)
                    .forEach { line ->
                        append(line)
                        append('\n')
                    }

                append(
                    "-----END PUBLIC KEY-----"
                )
            }

        }.getOrNull()
    }

    /**
     * Creates a request ID.
     *
     * Request IDs are not secrets.
     */
    fun createRequestId(): String =
        UUID.randomUUID().toString()

    /**
     * Calculate SHA-256 of the exact HTTP body.
     */
    fun sha256Hex(
        value: String
    ): String {

        val digest =
            MessageDigest.getInstance(
                HASH_ALGORITHM
            )

        val bytes =
            digest.digest(
                value.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }

    /**
     * Canonical signed payload.
     *
     * Both Guardian and Vercel construct the same form:
     *
     * timestamp
     * requestId
     * bodyHash
     */
    fun buildCanonicalPayload(
        timestamp: Long,
        requestId: String,
        bodyHash: String
    ): String {

        return buildString {

            append(timestamp)
            append('\n')

            append(requestId)
            append('\n')

            append(bodyHash)
        }
    }

    /**
     * Sign an Atlas request.
     *
     * The private key never leaves Android Keystore.
     */
    fun signRequest(
        context: Context,
        timestamp: Long,
        requestId: String,
        body: String
    ): String? {

        return runCatching {

            /*
             * Cryptographic signing is only available while
             * Guardian owner authority is active.
             */
            if (
                !AtlasOwnerAuthority.hasOwnerAuthorization(
                    context
                )
            ) {
                return@runCatching null
            }

            if (
                !ensureIdentity(context)
            ) {
                return@runCatching null
            }

            val keyStore =
                KeyStore.getInstance(
                    KEYSTORE_PROVIDER
                ).apply {
                    load(null)
                }

            val privateKey =
                keyStore.getKey(
                    KEY_ALIAS,
                    null
                ) as? PrivateKey
                    ?: return@runCatching null

            val bodyHash =
                sha256Hex(body)

            val canonicalPayload =
                buildCanonicalPayload(
                    timestamp = timestamp,
                    requestId = requestId,
                    bodyHash = bodyHash
                )

            val signer =
                Signature.getInstance(
                    SIGNATURE_ALGORITHM
                )

            signer.initSign(
                privateKey
            )

            signer.update(
                canonicalPayload.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

            Base64.encodeToString(
                signer.sign(),
                Base64.NO_WRAP
            )

        }.getOrNull()
    }

    /**
     * Diagnostic information only.
     *
     * Never returns the private key.
     */
    fun diagnostics(
        context: Context
    ): Map<String, String> {

        val present =
            runCatching {

                val keyStore =
                    KeyStore.getInstance(
                        KEYSTORE_PROVIDER
                    ).apply {
                        load(null)
                    }

                keyStore.containsAlias(
                    KEY_ALIAS
                )

            }.getOrDefault(false)

        return mapOf(
            "key_id" to KEY_ID,
            "keystore" to KEYSTORE_PROVIDER,
            "algorithm" to "ECDSA-P256",
            "private_key_exported" to "false",
            "identity_present" to present.toString(),
            "owner_authorized" to
                AtlasOwnerAuthority
                    .hasOwnerAuthorization(context)
                    .toString()
        )
    }
}
