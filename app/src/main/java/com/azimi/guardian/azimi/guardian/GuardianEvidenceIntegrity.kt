package com.azimi.guardian

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * AZIMI Guardian Evidence Integrity
 *
 * Provider-independent integrity foundation for AZIMI provenance evidence.
 *
 * This component:
 * - creates deterministic SHA-256 fingerprints
 * - does not transmit evidence
 * - does not access Z Vault secrets
 * - does not store credentials
 * - does not grant authorization
 * - does not replace the Ownership Registry
 * - does not replace backups
 *
 * The output is an integrity fingerprint, not a legal certification.
 */
object GuardianEvidenceIntegrity {

    const val ALGORITHM = "SHA-256"
    const val VERSION = "1.0.0"

    /**
     * Creates a SHA-256 fingerprint for arbitrary text.
     *
     * The input should never contain passwords, API keys,
     * authentication tokens, recovery codes, biometric material,
     * or other protected credentials.
     */
    fun sha256(
        value: String
    ): String {

        require(value.isNotEmpty()) {
            "Evidence value must not be empty."
        }

        val digest =
            MessageDigest.getInstance(ALGORITHM)

        val bytes =
            digest.digest(
                value.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        return bytes.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    /**
     * Creates a fingerprint for a named evidence item.
     *
     * The name is included in the fingerprint so that the same
     * content used for different evidence categories produces
     * distinguishable records.
     */
    fun fingerprint(
        evidenceName: String,
        value: String
    ): EvidenceFingerprint {

        val normalizedName =
            evidenceName.trim()

        require(normalizedName.isNotEmpty()) {
            "Evidence name must not be empty."
        }

        val normalizedValue =
            value.trim()

        require(normalizedValue.isNotEmpty()) {
            "Evidence value must not be empty."
        }

        val canonical =
            buildString {
                append("AZIMI-EVIDENCE|")
                append(VERSION)
                append("|")
                append(normalizedName)
                append("|")
                append(normalizedValue)
            }

        return EvidenceFingerprint(
            evidenceName = normalizedName,
            algorithm = ALGORITHM,
            integrityVersion = VERSION,
            sha256 = sha256(canonical)
        )
    }

    /**
     * Creates a fingerprint for a Git commit SHA.
     *
     * The commit SHA is treated as public version metadata.
     */
    fun fingerprintCommit(
        commitSha: String
    ): EvidenceFingerprint {

        return fingerprint(
            evidenceName = "GIT_COMMIT",
            value = commitSha
        )
    }

    /**
     * Creates a fingerprint for an APK SHA-256 value.
     *
     * The APK hash itself may be recorded as evidence metadata.
     */
    fun fingerprintApkHash(
        apkSha256: String
    ): EvidenceFingerprint {

        return fingerprint(
            evidenceName = "APK_SHA256",
            value = apkSha256
        )
    }

    /**
     * Creates a fingerprint for a build identifier.
     */
    fun fingerprintBuild(
        buildIdentifier: String
    ): EvidenceFingerprint {

        return fingerprint(
            evidenceName = "BUILD",
            value = buildIdentifier
        )
    }

    /**
     * Creates a fingerprint for a verification statement.
     *
     * Verification statements must not contain protected credentials.
     */
    fun fingerprintVerification(
        verificationStatement: String
    ): EvidenceFingerprint {

        return fingerprint(
            evidenceName = "VERIFICATION",
            value = verificationStatement
        )
    }

    /**
     * Checks whether a value appears to contain protected credentials
     * before allowing it to become an evidence fingerprint.
     *
     * This is intentionally conservative.
     */
    fun isSafeEvidenceValue(
        value: String
    ): Boolean {

        val text =
            value.trim()

        if (text.isEmpty()) {
            return false
        }

        return !AzimiAuth.isProtectedCredential(
            text
        )
    }

    /**
     * Safe fingerprint creation.
     *
     * Returns null when the supplied evidence appears protected.
     */
    fun safeFingerprint(
        evidenceName: String,
        value: String
    ): EvidenceFingerprint? {

        if (!isSafeEvidenceValue(value)) {
            return null
        }

        return runCatching {
            fingerprint(
                evidenceName = evidenceName,
                value = value
            )
        }.getOrNull()
    }

    data class EvidenceFingerprint(
        val evidenceName: String,
        val algorithm: String,
        val integrityVersion: String,
        val sha256: String
    )
}
