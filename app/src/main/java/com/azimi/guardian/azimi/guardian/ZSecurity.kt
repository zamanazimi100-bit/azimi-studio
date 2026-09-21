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
        return SecuritySession()
    }

    fun authenticatedSession(
        method: AuthenticationMethod
    ): SecuritySession {
        return SecuritySession(
            authenticated = true,
            accessLevel = AccessLevel.AUTHENTICATED,
            method = method
        )
    }

    fun protectedSession(
        method: AuthenticationMethod
    ): SecuritySession {
        return SecuritySession(
            authenticated = true,
            accessLevel = AccessLevel.PROTECTED,
            method = method
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
}
