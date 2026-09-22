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
 *
 * It only evaluates the supplied evidence record.
 */
object GuardianEvidenceVerifier {

    const val STATUS_VALID = "VALID"
    const val STATUS_INVALID = "INVALID"
    const val STATUS_INCOMPLETE = "INCOMPLETE"

    const val RECORD_FILE =
        "AZIMI-EVIDENCE-RECORD.json"

    fun verify(
        context: Context,
        recordJson: String
    ): VerificationResult {

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

        return verifyJson(
            context.applicationContext,
            json
        )
    }

    private fun verifyJson(
        context: Context,
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

        if (record.optString("id") != "AZI-EVIDENCE-001") {
            return invalid(
                "Unexpected evidence record ID."
            )
        }

        if (record.optString("version").isBlank()) {
            return invalid(
                "Evidence record version is missing."
            )
        }

        if (owner.optString("name") != "Zaman Azimi") {
            return invalid(
                "Owner identity does not match the registry."
            )
        }

        if (
            owner.optString("role") !=
            "FOUNDER_CREATOR_OWNER"
        ) {
            return invalid(
                "Owner role does not match the registry."
            )
        }

        if (
            owner.optString("authority") !=
            "FINAL_OWNER_AUTHORITY"
        ) {
            return invalid(
                "Owner authority does not match the registry."
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
            "AZIMI"
        ) {
            return invalid(
                "Project identity is invalid."
            )
        }

        if (
            project.optString("studio_identity") !=
            "AZIMI.STUDIO"
        ) {
            return invalid(
                "Studio identity is invalid."
            )
        }

        if (
            project.optString("repository") !=
            "zamanazimi100-bit/azimi-studio"
        ) {
            return invalid(
                "Repository identity is invalid."
            )
        }

        if (
            buildEvidence.optString("guardian_build") !=
            "49"
        ) {
            return invalid(
                "Guardian build checkpoint does not match."
            )
        }

        if (
            buildEvidence.optString("build_status") !=
            "SUCCESSFUL"
        ) {
            return invalid(
                "Build status is invalid."
            )
        }

        if (
            buildEvidence.optString("installation_status") !=
            "VERIFIED_BY_OWNER"
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
            json.optJSONObject("source_evidence")

        val applicationEvidence =
            json.optJSONObject("application_evidence")

        val buildIntegrity =
            json.optJSONObject("build_integrity")

        val verification =
            json.optJSONObject("verification")

        if (
            sourceEvidence == null ||
            applicationEvidence == null ||
            buildIntegrity == null ||
            verification == null
        ) {
            return incomplete(
                "Required evidence sections are incomplete."
            )
        }

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
           
