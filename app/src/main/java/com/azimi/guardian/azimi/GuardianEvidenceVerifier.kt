package com.azimi.guardian

import android.content.Context
import org.json.JSONObject

/**
 * AZIMI Guardian Evidence Verifier
 *
 * Read-only verification layer for the machine-readable
 * AZIMI evidence record.
 *
 * This component does not:
 * - modify the evidence record
 * - access Z Vault
 * - transmit data
 * - contact external providers
 * - grant authorization
 * - change Atlas routing
 * - handle credentials
 */
object GuardianEvidenceVerifier {

    const val STATUS_VALID = "VALID"
    const val STATUS_INVALID = "INVALID"
    const val STATUS_INCOMPLETE = "INCOMPLETE"

    private const val EXPECTED_RECORD_ID =
        "AZI-EVIDENCE-001"

    private const val EXPECTED_OWNER =
        "Zaman Azimi"

    private const val EXPECTED_OWNER_ROLE =
        "FOUNDER_CREATOR_OWNER"

    private const val EXPECTED_OWNER_AUTHORITY =
        "FINAL_OWNER_AUTHORITY"

    private const val EXPECTED_PROJECT =
        "AZIMI"

    private const val EXPECTED_STUDIO =
        "AZIMI.STUDIO"

    private const val EXPECTED_REPOSITORY =
        "zamanazimi100-bit/azimi-studio"

    private const val EXPECTED_BUILD =
        "49"

    private const val EXPECTED_BUILD_STATUS =
        "SUCCESSFUL"

    private const val EXPECTED_INSTALLATION_STATUS =
        "VERIFIED_BY_OWNER"

    fun verify(
        context: Context,
        recordJson: String
    ): VerificationResult {

        context.applicationContext

        val normalized =
            recordJson.trim()

        if (normalized.isEmpty()) {
            return invalid(
                "Evidence record is empty."
            )
        }

        val json =
            try {
                JSONObject(normalized)
            } catch (exception: Exception) {
                return invalid(
                    "Evidence record contains invalid JSON."
                )
            }

        return verifyJson(json)
    }

    private fun verifyJson(
        json: JSONObject
    ): VerificationResult {

        val record =
            json.optJSONObject("record")
                ?: return invalid(
                    "Missing record section."
                )

        val owner =
            json.optJSONObject("owner")
                ?: return invalid(
                    "Missing owner section."
                )

        val integrity =
            json.optJSONObject("integrity")
                ?: return invalid(
                    "Missing integrity section."
                )

        val project =
            json.optJSONObject("project")
                ?: return invalid(
                    "Missing project section."
                )

        val buildEvidence =
            json.optJSONObject("build_evidence")
                ?: return invalid(
                    "Missing build_evidence section."
                )

        val security =
            json.optJSONObject("security")
                ?: return invalid(
                    "Missing security section."
                )

        if (
            record.optString("id") !=
            EXPECTED_RECORD_ID
        ) {
            return invalid(
                "Unexpected evidence record ID."
            )
        }

        if (
            record.optString("version").isBlank()
        ) {
            return invalid(
                "Evidence record version is missing."
            )
        }

        if (
            owner.optString("name") !=
            EXPECTED_OWNER
        ) {
            return invalid(
                "Owner identity does not match."
            )
        }

        if (
            owner.optString("role") !=
            EXPECTED_OWNER_ROLE
        ) {
            return invalid(
                "Owner role does not match."
            )
        }

        if (
            owner.optString("authority") !=
            EXPECTED_OWNER_AUTHORITY
        ) {
            return invalid(
                "Owner authority does not match."
            )
        }

        if (
            integrity.optString("algorithm") !=
            GuardianEvidenceIntegrity.ALGORITHM
        ) {
            return invalid(
                "Unsupported integrity algorithm."
            )
        }

        if (
            integrity.optString("integrity_component") !=
            "GuardianEvidenceIntegrity"
        ) {
            return invalid(
                "Integrity component does not match."
            )
        }

        if (
            integrity.optString("integrity_version") !=
            GuardianEvidenceIntegrity.VERSION
        ) {
            return invalid(
                "Integrity component version does not match."
            )
        }

        if (
            project.optString("identity") !=
            EXPECTED_PROJECT
        ) {
            return invalid(
                "Project identity is invalid."
            )
        }

        if (
            project.optString("studio_identity") !=
            EXPECTED_STUDIO
        ) {
            return invalid(
                "Studio identity is invalid."
            )
        }

        if (
            project.optString("repository") !=
            EXPECTED_REPOSITORY
        ) {
            return invalid(
                "Repository identity is invalid."
            )
        }

        if (
            buildEvidence.optString("guardian_build") !=
            EXPECTED_BUILD
        ) {
            return invalid(
                "Guardian build checkpoint does not match."
            )
        }

        if (
            buildEvidence.optString("build_status") !=
            EXPECTED_BUILD_STATUS
        ) {
            return invalid(
                "Build status is invalid."
            )
        }

        if (
            buildEvidence.optString(
                "installation_status"
            ) != EXPECTED_INSTALLATION_STATUS
        ) {
            return invalid(
                "Installation verification status is invalid."
            )
        }

        if (
            security.optBoolean(
                "secrets_allowed",
                true
            )
        ) {
            return invalid(
                "Secret storage policy is unsafe."
            )
        }

        if (
            security.optBoolean(
                "passwords_allowed",
                true
            )
        ) {
            return invalid(
                "Password storage policy is unsafe."
            )
        }

        if (
            security.optBoolean(
                "api_keys_allowed",
                true
            )
        ) {
            return invalid(
                "API key storage policy is unsafe."
            )
        }

        if (
            security.optBoolean(
                "authentication_tokens_allowed",
                true
            )
        ) {
            return invalid(
                "Authentication token policy is unsafe."
            )
        }

        if (
            security.optBoolean(
                "recovery_codes_allowed",
                true
            )
        ) {
            return invalid(
                "Recovery code policy is unsafe."
            )
        }

        if (
            security.optBoolean(
                "private_credentials_allowed",
                true
            )
        ) {
            return invalid(
                "Private credential policy is unsafe."
            )
        }

        if (
            security.optBoolean(
                "biometric_material_allowed",
                true
            )
        ) {
            return invalid(
                "Biometric material policy is unsafe."
            )
        }

        val sourceEvidence =
            json.optJSONObject(
                "source_evidence"
            ) ?: return incomplete(
                "Source evidence section is missing."
            )

        val applicationEvidence =
            json.optJSONObject(
                "application_evidence"
            ) ?: return incomplete(
                "Application evidence section is missing."
            )

        val buildIntegrity =
            json.optJSONObject(
                "build_integrity"
            ) ?: return incomplete(
                "Build integrity section is missing."
            )

        val verification =
            json.optJSONObject(
                "verification"
            ) ?: return incomplete(
                "Verification section is missing."
            )

        val commitSha =
            sourceEvidence.optString(
                "commit_sha"
            ).trim()

        val commitFingerprint =
            sourceEvidence.optString(
                "commit_sha_fingerprint"
            ).trim()

        val apkSha256 =
            applicationEvidence.optString(
                "apk_sha256"
            ).trim()

        val apkFingerprint =
            applicationEvidence.optString(
                "apk_sha256_fingerprint"
            ).trim()

        val buildFingerprint =
            buildIntegrity.optString(
                "build_fingerprint"
            ).trim()

        val verificationStatement =
            verification.optString(
                "statement"
            ).trim()

        val verificationFingerprint =
            verification.optString(
                "statement_fingerprint"
            ).trim()

        if (
            commitSha.isBlank() ||
            commitFingerprint.isBlank() ||
            apkSha256.isBlank() ||
            apkFingerprint.isBlank() ||
            buildFingerprint.isBlank() ||
            verificationStatement.isBlank() ||
            verificationFingerprint.isBlank()
        ) {
            return incomplete(
                "Evidence values are not fully populated."
            )
        }

        val expectedCommitFingerprint =
            GuardianEvidenceIntegrity.safeFingerprint(
                "GIT_COMMIT",
                commitSha
            ) ?: return invalid(
                "Git commit evidence is not safe."
            )

        if (
            expectedCommitFingerprint.sha256 !=
            commitFingerprint
        ) {
            return invalid(
                "Git commit fingerprint does not match."
            )
        }

        val expectedApkFingerprint =
            GuardianEvidenceIntegrity.safeFingerprint(
                "APK_SHA256",
                apkSha256
            ) ?: return invalid(
                "APK evidence is not safe."
            )

        if (
            expectedApkFingerprint.sha256 !=
            apkFingerprint
        ) {
            return invalid(
                "APK fingerprint does not match."
            )
        }

        val expectedBuildFingerprint =
            GuardianEvidenceIntegrity.safeFingerprint(
                "BUILD",
                EXPECTED_BUILD
            ) ?: return invalid(
                "Build evidence could not be fingerprinted."
            )

        if (
            expectedBuildFingerprint.sha256 !=
            buildFingerprint
        ) {
            return invalid(
                "Build fingerprint does not match."
            )
        }

        val expectedVerificationFingerprint =
            GuardianEvidenceIntegrity.safeFingerprint(
                "VERIFICATION",
                verificationStatement
            ) ?: return invalid(
                "Verification statement is not safe."
            )

        if (
            expectedVerificationFingerprint.sha256 !=
            verificationFingerprint
        ) {
            return invalid(
                "Verification fingerprint does not match."
            )
        }

        return VerificationResult(
            status = STATUS_VALID,
            message =
                "AZIMI evidence record integrity is valid.",
            recordId =
                record.optString("id"),
            integrityAlgorithm =
                integrity.optString("algorithm"),
            integrityVersion =
                integrity.optString("integrity_version")
        )
    }

    private fun invalid(
        message: String
    ): VerificationResult {

        return VerificationResult(
            status = STATUS_INVALID,
            message = message,
            recordId = "",
            integrityAlgorithm = "",
            integrityVersion = ""
        )
    }

    private fun incomplete(
        message: String
    ): VerificationResult {

        return VerificationResult(
            status = STATUS_INCOMPLETE,
            message = message,
            recordId = "",
            integrityAlgorithm = "",
            integrityVersion = ""
        )
    }

    data class VerificationResult(
        val status: String,
        val message: String,
        val recordId: String,
        val integrityAlgorithm: String,
        val integrityVersion: String
    )
}
