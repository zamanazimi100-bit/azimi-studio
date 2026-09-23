package com.azimi.guardian

object ZSecurity {

    enum class AccessLevel {
        PUBLIC,
        AUTHENTICATED,
        PROTECTED,
        ELEVATED,
        OWNER_ONLY,
        SOVEREIGN
    }

    enum class AuthenticationMethod {
        NONE,
        DEVICE_CREDENTIAL,
        BIOMETRIC,
        OWNER_VOICE,
        OWNER_KEY,
        MULTI_FACTOR
    }

    data class SecuritySession(
        val authenticated: Boolean = false,
        val accessLevel: AccessLevel = AccessLevel.PUBLIC,
        val method: AuthenticationMethod = AuthenticationMethod.NONE,
        val ownerVerified: Boolean = false
    )

    fun publicSession(): SecuritySession {
        return SecuritySession(
            authenticated = false,
            accessLevel = AccessLevel.PUBLIC,
            method = AuthenticationMethod.NONE,
            ownerVerified = false
        )
    }

    fun authenticatedSession(
        method: AuthenticationMethod
    ): SecuritySession {
        return SecuritySession(
            authenticated = true,
            accessLevel = AccessLevel.AUTHENTICATED,
            method = method,
            ownerVerified = false
        )
    }

    fun protectedSession(
        method: AuthenticationMethod
    ): SecuritySession {
        return SecuritySession(
            authenticated = true,
            accessLevel = AccessLevel.PROTECTED,
            method = method,
            ownerVerified = false
        )
    }

    fun elevatedSession(
        method: AuthenticationMethod
    ): SecuritySession {
        return SecuritySession(
            authenticated = true,
            accessLevel = AccessLevel.ELEVATED,
            method = method,
            ownerVerified = false
        )
    }

    fun ownerSession(
        method: AuthenticationMethod
    ): SecuritySession {
        return SecuritySession(
            authenticated = true,
            accessLevel = AccessLevel.OWNER_ONLY,
            method = method,
            ownerVerified = true
        )
    }

    fun sovereignSession(
        method: AuthenticationMethod
    ): SecuritySession {
        return SecuritySession(
            authenticated = true,
            accessLevel = AccessLevel.SOVEREIGN,
            method = method,
            ownerVerified = true
        )
    }

    fun canAccess(
        session: SecuritySession,
        requiredLevel: AccessLevel
    ): Boolean {

        if (requiredLevel == AccessLevel.PUBLIC) {
            return true
        }

        if (!session.authenticated) {
            return false
        }

        if (
            requiredLevel == AccessLevel.OWNER_ONLY ||
            requiredLevel == AccessLevel.SOVEREIGN
        ) {
            return session.ownerVerified
        }

        return session.accessLevel.ordinal >=
            requiredLevel.ordinal
    }

    fun isOwnerArea(
        level: AccessLevel
    ): Boolean {
        return level == AccessLevel.OWNER_ONLY ||
            level == AccessLevel.SOVEREIGN
    }

    fun requiresAuthentication(
        level: AccessLevel
    ): Boolean {
        return level != AccessLevel.PUBLIC
    }

    fun requiresOwnerVerification(
        level: AccessLevel
    ): Boolean {
        return level == AccessLevel.OWNER_ONLY ||
            level == AccessLevel.SOVEREIGN
    }

    fun describeLevel(
        level: AccessLevel
    ): String {
        return when (level) {
            AccessLevel.PUBLIC ->
                "Public access"

            AccessLevel.AUTHENTICATED ->
                "Authenticated user access"

            AccessLevel.PROTECTED ->
                "Protected access"

            AccessLevel.ELEVATED ->
                "Elevated authorization required"

            AccessLevel.OWNER_ONLY ->
                "Owner verification required"

            AccessLevel.SOVEREIGN ->
                "Sovereign owner authorization required"
        }
    }
}
