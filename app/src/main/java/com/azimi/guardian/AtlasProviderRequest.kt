package com.azimi.guardian

/**
 * Safe request envelope sent from Atlas Guardian Bridge
 * to an intelligence provider.
 *
 * This object deliberately contains only information that
 * Atlas has already classified as safe for the intelligence
 * boundary.
 *
 * It never contains:
 * - Vault secrets
 * - authentication tokens
 * - passwords
 * - API keys
 * - recovery codes
 * - biometric material
 * - private credentials
 */
data class AtlasProviderRequest(
    val message: String,
    val history: List<AzimiAiClient.ChatMessage> = emptyList(),
    val approvedMemory: List<AzimiAiClient.ChatMessage> = emptyList(),
    val atlasContext: String = ""
)
