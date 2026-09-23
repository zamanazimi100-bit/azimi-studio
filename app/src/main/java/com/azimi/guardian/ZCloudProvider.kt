package com.azimi.guardian

import android.content.Context

/**
 * Z Cloud Provider Contract
 *
 * Z Cloud owns this interface.
 *
 * A storage provider is only an implementation underneath
 * Z Cloud. It must never become the owner of AZIMI cloud
 * architecture.
 *
 * Examples of future implementations:
 *
 * - self-hosted storage
 * - object storage
 * - WebDAV
 * - S3-compatible storage
 * - Supabase Storage
 * - another independent cloud
 * - local/offline backup
 *
 * Security boundary:
 *
 * - Provider receives encrypted payloads only for protected data.
 * - Provider never receives Vault decryption keys.
 * - Provider never unlocks Z Vault.
 * - Provider never controls Atlas.
 * - Provider cannot change Guardian authorization.
 * - Provider credentials must never become Atlas memory.
 */
interface ZCloudProvider {

    /**
     * Stable provider identifier.
     */
    val id: String

    /**
     * Human-readable provider name.
     */
    val displayName: String

    /**
     * Determines whether this provider can currently be used.
     *
     * This method must not unlock the Vault or bypass
     * Guardian security.
     */
    fun isAvailable(
        context: Context
    ): Boolean

    /**
     * Uploads an already-prepared payload.
     *
     * For Vault data, payload MUST already be encrypted.
     *
     * The provider must not attempt to decrypt it.
     */
    fun upload(
        context: Context,
        objectId: String,
        payload: ByteArray,
        integrityHash: String,
        onResult: (UploadResult) -> Unit
    )

    /**
     * Downloads a stored payload.
     *
     * The returned payload remains encrypted.
     */
    fun download(
        context: Context,
        objectId: String,
        onResult: (DownloadResult) -> Unit
    )

    /**
     * Deletes a cloud object.
     *
     * Actual deletion policy will be controlled by Z Cloud,
     * not by the provider.
     */
    fun delete(
        context: Context,
        objectId: String,
        onResult: (DeleteResult) -> Unit
    )

    /**
     * Returns provider diagnostics without exposing secrets.
     */
    fun diagnostics(
        context: Context
    ): String

    data class UploadResult(
        val success: Boolean,
        val objectId: String,
        val providerId: String,
        val message: String,
        val integrityHash: String = "",
        val error: String = ""
    )

    data class DownloadResult(
        val success: Boolean,
        val objectId: String,
        val providerId: String,
        val payload: ByteArray? = null,
        val integrityHash: String = "",
        val message: String = "",
        val error: String = ""
    )

    data class DeleteResult(
        val success: Boolean,
        val objectId: String,
        val providerId: String,
        val message: String,
        val error: String = ""
    )
}
