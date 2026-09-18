package com.azimi.guardian

data class GuardianState(
    val vaultLocked: Boolean = true,
    val aiRestricted: Boolean = true,
    val cloudSeparated: Boolean = true,
    val connectionsAuthorized: Boolean = false,
    val shieldConfigured: Boolean = false
)
