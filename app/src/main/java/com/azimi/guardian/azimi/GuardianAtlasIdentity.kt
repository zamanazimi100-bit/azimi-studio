package com.azimi.guardian

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.KeyStore
import java.util.UUID

/**
 * Guardian-owned cryptographic identity for Atlas.
 *
 * The private key is generated and retained inside Android
 * Keystore. It is never exported and never sent to AZIMI
 * Studio, Supabase, Cloudflare, or any other provider.
 *
 * Guardian owner authentication controls whether Atlas may
 * use this identity during the active authorized session.
 */
object GuardianAtlasIdentity {

    private const val KEYSTORE_PROVIDER =
        "AndroidKeyStore"

    private const val KEY_ALIAS =
        "AZIMI_ATLAS_GUARDIAN_IDENTITY"

    const val KEY_ID =
        "ZAMAN-AZIMI-GUARDIAN-01"

    private const val SIGNATURE_ALGORITHM =
        "SHA256withECDSA"

    private const val HASH_ALGORITHM =
        "SHA-256"

    /**
     * Ensures the device has a Guardian Atlas signing identity.
     *
     * The private key is generated once and remains inside
     * Android Keystore.
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
     * Returns the public key encoded as PEM.
     *
     * This is safe to expose during one-time Guardian
     * enrollment because it is the public half of the key.
     */
    fun getPublicKeyPem(
        context: Context
    ): String? {

        return runCatching {

            if (!ensureIdentity(context)) {
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
                    .forEach {
                        append(it)
                        append('\n')
                    }

                append(
                    "-----END PUBLIC KEY-----"
                )
            }

        }.getOrNull()
    }

    /**
     * Creates a cryptographic proof for an Atlas request.
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

            val signature =
                Signature.getInstance(
                    SIGNATURE_ALGORITHM
                )

            signature.initSign(
                privateKey
            )

            signature.update(
                canonicalPayload.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

            Base64.encodeToString(
                signature.sign(),
                Base64.NO_WRAP
            )

        }.getOrNull()
    }

    /**
     * Creates a unique request identifier.
     */
    fun createRequestId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * SHA-256 body digest used in the signed request.
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
     * Canonical payload shared by Guardian and the
     * Vercel verification layer.
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
     * Diagnostic information.
     *
     * This never exposes the private key.
     */
    fun getDiagnostics(
        context: Context
    ): Map<String, String> {

        val exists =
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
            "identity" to KEY_ID,
            "key_alias" to KEY_ALIAS,
            "keystore" to KEYSTORE_PROVIDER,
            "private_key_exportable" to "false",
            "identity_present" to exists.toString(),
            "owner_authorized" to
                AtlasOwnerAuthority
                    .hasOwnerAuthorization(context)
                    .toString()
        )
    }
}
