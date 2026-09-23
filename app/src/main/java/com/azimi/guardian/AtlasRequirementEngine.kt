package com.azimi.guardian

/**
 * AZIMI Atlas Requirement Engine
 *
 * Converts a natural-language request into a structured
 * requirement profile that AtlasCore can understand.
 *
 * Responsibilities:
 * - Detect the user's likely intent
 * - Classify the task
 * - Identify relevant AZIMI components
 * - Detect dependencies
 * - Identify security sensitivity
 * - Determine whether permission is required
 * - Determine whether owner authority is required
 * - Determine whether the request is ownership-sensitive
 * - Determine whether a backup checkpoint is recommended
 * - Identify capability gaps
 * - Suggest the safest next step
 *
 * This engine is:
 * - Local
 * - Deterministic
 * - Read-only
 * - Provider-independent
 * - Safe to run without external AI
 *
 * It does not:
 * - Execute actions
 * - Access private credentials
 * - Change files
 * - Contact external providers
 * - Bypass Guardian permissions
 * - Grant owner authority
 */
object AtlasRequirementEngine {

    enum class Intent {
        UNDERSTAND,
        BUILD,
        MODIFY,
        DEBUG,
        CONNECT,
        PROTECT,
        BACKUP,
        RECOVER,
        LEARN,
        DESIGN,
        ANALYZE,
        VERIFY,
        UNKNOWN
    }

    enum class TaskCategory {
        AI_INTELLIGENCE,
        ANDROID_DEVELOPMENT,
        WEB_DEVELOPMENT,
        SECURITY,
        AUTHENTICATION,
        CLOUD,
        PROJECT_PRESERVATION,
        BACKUP_RECOVERY,
        AUTOMATION,
        LANGUAGE,
        LEARNING,
        GENERAL
    }

    enum class SecurityLevel {
        NORMAL,
        SENSITIVE,
        PROTECTED
    }

    enum class RequirementStatus {
        READY,
        PARTIALLY_READY,
        MISSING_CAPABILITY,
        BLOCKED,
        NEEDS_OWNER_PERMISSION,
        NEEDS_MORE_INFORMATION
    }

    data class RequirementAnalysis(
        val originalRequest: String,
        val normalizedRequest: String,
        val intent: Intent,
        val category: TaskCategory,
        val requiredComponents: List<String>,
        val dependencies: List<String>,
        val missingCapabilities: List<String>,
        val securityLevel: SecurityLevel,
        val permissionRequired: Boolean,
        val ownerAuthorizationRequired: Boolean,
        val ownerAuthorized: Boolean,
        val actorType: AtlasOwnerAuthority.ActorType,
        val authorityLevel: AtlasOwnerAuthority.AuthorityLevel,
        val ownershipSensitive: Boolean,
        val backupRecommended: Boolean,
        val externalAIHelpful: Boolean,
        val status: RequirementStatus,
        val nextSafeAction: String,
        val warnings: List<String>
    )

    fun analyze(
        request: String,
        ownerAuthority: AtlasOwnerAuthority.AuthorityState? = null
    ): RequirementAnalysis {

        val cleanRequest = request.trim()

        if (cleanRequest.isBlank()) {
            return RequirementAnalysis(
                originalRequest = request,
                normalizedRequest = "",
                intent = Intent.UNKNOWN,
                category = TaskCategory.GENERAL,
                requiredComponents = emptyList(),
                dependencies = emptyList(),
                missingCapabilities = listOf(
                    "A clear user request"
                ),
                securityLevel = SecurityLevel.NORMAL,
                permissionRequired = false,
                ownerAuthorizationRequired = false,
                ownerAuthorized =
                    ownerAuthority?.ownerAuthorized == true,
                actorType =
                    ownerAuthority?.actorType
                        ?: AtlasOwnerAuthority.ActorType.UNKNOWN,
                authorityLevel =
                    ownerAuthority?.authorityLevel
                        ?: AtlasOwnerAuthority.AuthorityLevel.NONE,
                ownershipSensitive = false,
                backupRecommended = false,
                externalAIHelpful = false,
                status = RequirementStatus.NEEDS_MORE_INFORMATION,
                nextSafeAction =
                    "Ask the user to describe the intended task.",
                warnings = listOf(
                    "Atlas received an empty request."
                )
            )
        }

        val normalized = cleanRequest.lowercase()

        val authority =
            ownerAuthority
                ?: AtlasOwnerAuthority.AuthorityState(
                    actorType =
                        AtlasOwnerAuthority.ActorType.UNKNOWN,
                    authorityLevel =
                        AtlasOwnerAuthority.AuthorityLevel.NONE,
                    authenticated = false,
                    ownerAuthorized = false,
                    ownerId = null,
                    authorizationMethod = null,
                    authorizedAt = null,
                    message =
                        "Owner authority state was not supplied."
                )

        val intent =
            detectIntent(normalized)

        val category =
            detectCategory(normalized)

        val requiredComponents =
            detectRequiredComponents(
                normalized,
                category
            )

        val dependencies =
            detectDependencies(
                normalized,
                category
            )

        val missingCapabilities =
            detectMissingCapabilities(
                normalized,
                category
            )

        val securityLevel =
            detectSecurityLevel(normalized)

        val ownershipSensitive =
            AtlasOwnerAuthority.isOwnershipSensitiveRequest(
                normalized
            )

        val ownerAuthorizationRequired =
            AtlasOwnerAuthority.requiresOwnerAuthorization(
                normalized
            ) || ownershipSensitive

        val permissionRequired =
            requiresPermission(
                normalized,
                intent,
                category
            ) || ownerAuthorizationRequired

        val backupRecommended =
            recommendsBackup(
                normalized,
                intent,
                category
            )

        val externalAIHelpful =
            isExternalAIHelpful(
                normalized,
                category
            )

        val warnings =
            buildWarnings(
                normalized = normalized,
                securityLevel = securityLevel,
                permissionRequired = permissionRequired,
                ownerAuthorizationRequired =
                    ownerAuthorizationRequired,
                ownerAuthorized =
                    authority.ownerAuthorized,
                ownershipSensitive =
                    ownershipSensitive,
                backupRecommended =
                    backupRecommended
            )

        val status =
            determineStatus(
                missingCapabilities =
                    missingCapabilities,
                permissionRequired =
                    permissionRequired,
                ownerAuthorizationRequired =
                    ownerAuthorizationRequired,
                ownerAuthorized =
                    authority.ownerAuthorized,
                securityLevel =
                    securityLevel
            )

        val nextSafeAction =
            determineNextSafeAction(
                intent = intent,
                category = category,
                status = status,
                permissionRequired =
                    permissionRequired,
                ownerAuthorizationRequired =
                    ownerAuthorizationRequired,
                ownerAuthorized =
                    authority.ownerAuthorized,
                backupRecommended =
                    backupRecommended
            )

        return RequirementAnalysis(
            originalRequest = cleanRequest,
            normalizedRequest = normalized,
            intent = intent,
            category = category,
            requiredComponents =
                requiredComponents,
            dependencies =
                dependencies,
            missingCapabilities =
                missingCapabilities,
            securityLevel =
                securityLevel,
            permissionRequired =
                permissionRequired,
            ownerAuthorizationRequired =
                ownerAuthorizationRequired,
            ownerAuthorized =
                authority.ownerAuthorized,
            actorType =
                authority.actorType,
            authorityLevel =
                authority.authorityLevel,
            ownershipSensitive =
                ownershipSensitive,
            backupRecommended =
                backupRecommended,
            externalAIHelpful =
                externalAIHelpful,
            status =
                status,
            nextSafeAction =
                nextSafeAction,
            warnings =
                warnings
        )
    }

    private fun detectIntent(
        text: String
    ): Intent {

        return when {

            containsAny(
                text,
                "who are you",
                "what are you",
                "explain",
                "what is",
                "how does"
            ) -> Intent.UNDERSTAND

            containsAny(
                text,
                "build",
                "create",
                "develop",
                "implement",
                "make"
            ) -> Intent.BUILD

            containsAny(
                text,
                "change",
                "modify",
                "update",
                "edit",
                "refactor",
                "upgrade"
            ) -> Intent.MODIFY

            containsAny(
                text,
                "fix",
                "debug",
                "error",
                "broken",
                "crash",
                "not working",
                "failed"
            ) -> Intent.DEBUG

            containsAny(
                text,
                "connect",
                "integrate",
                "link",
                "connect guardian",
                "connect ai"
            ) -> Intent.CONNECT

            containsAny(
                text,
                "protect",
                "secure",
                "shield",
                "lock",
                "permission",
                "privacy"
            ) -> Intent.PROTECT

            containsAny(
                text,
                "backup",
                "preserve",
                "snapshot",
                "archive",
                "save project"
            ) -> Intent.BACKUP

            containsAny(
                text,
                "recover",
                "restore",
                "rollback",
                "recovery"
            ) -> Intent.RECOVER

            containsAny(
                text,
                "learn",
                "teach me",
                "study",
                "lesson",
                "understand how"
            ) -> Intent.LEARN

            containsAny(
                text,
                "design",
                "architecture",
                "structure",
                "plan"
            ) -> Intent.DESIGN

            containsAny(
                text,
                "analyze",
                "inspect",
                "review",
                "check",
                "investigate"
            ) -> Intent.ANALYZE

            containsAny(
                text,
                "test",
                "verify",
                "validate",
                "confirm"
            ) -> Intent.VERIFY

            else -> Intent.UNKNOWN
        }
    }

    private fun detectCategory(
        text: String
    ): TaskCategory {

        return when {

            containsAny(
                text,
                "atlas",
                "ai",
                "intelligence",
                "assistant",
                "reasoning",
                "memory",
                "language model"
            ) -> TaskCategory.AI_INTELLIGENCE

            containsAny(
                text,
                "android",
                "guardian",
                "apk",
                "kotlin",
                "mainactivity",
                "mobile app"
            ) -> TaskCategory.ANDROID_DEVELOPMENT

            containsAny(
                text,
                "website",
                "web",
                "next.js",
                "javascript",
                "html",
                "css",
                "frontend",
                "backend"
            ) -> TaskCategory.WEB_DEVELOPMENT

            containsAny(
                text,
                "security",
                "secure",
                "vault",
                "shield",
                "encryption",
                "credential",
                "privacy"
            ) -> TaskCategory.SECURITY

            containsAny(
                text,
                "login",
                "authenticate",
                "authentication",
                "magic link",
                "supabase auth",
                "session",
                "token"
            ) -> TaskCategory.AUTHENTICATION

            containsAny(
                text,
                "cloud",
                "supabase",
                "vercel",
                "cloudflare",
                "server",
                "api"
            ) -> TaskCategory.CLOUD

            containsAny(
                text,
                "github",
                "repository",
                "repo",
                "source code",
                "project ownership",
                "ownership",
                "provenance",
                "creator",
                "founder"
            ) -> TaskCategory.PROJECT_PRESERVATION

            containsAny(
                text,
                "backup",
                "restore",
                "recovery",
                "rollback",
                "checkpoint"
            ) -> TaskCategory.BACKUP_RECOVERY

            containsAny(
                text,
                "automation",
                "workflow",
                "automate",
                "power automate"
            ) -> TaskCategory.AUTOMATION

            containsAny(
                text,
                "dari",
                "english",
                "translation",
                "language support",
                "multilingual"
            ) -> TaskCategory.LANGUAGE

            containsAny(
                text,
                "learn",
                "study",
                "lesson",
                "teach",
                "tutorial"
            ) -> TaskCategory.LEARNING

            else -> TaskCategory.GENERAL
        }
    }

    private fun detectRequiredComponents(
        text: String,
        category: TaskCategory
    ): List<String> {

        val components =
            mutableListOf<String>()

        when (category) {

            TaskCategory.AI_INTELLIGENCE -> {
                components += "AtlasCore"
                components += "AtlasKnowledge"
                components += "AtlasRequirementEngine"
                components += "AtlasGuardianBridge"
                components += "AI Engine Adapter"
            }

            TaskCategory.ANDROID_DEVELOPMENT -> {
                components +=
                    "Guardian Android application"
                components += "MainActivity"
                components += "AtlasGuardianBridge"
            }

            TaskCategory.WEB_DEVELOPMENT -> {
                components += "AZIMI.STUDIO"
                components += "Source repository"
                components += "Web application files"
            }

            TaskCategory.SECURITY -> {
                components +=
                    "Guardian Security Policy"
                components += "Z Vault"
                components += "Z Shield"
                components += "Permission Engine"
                components += "AtlasOwnerAuthority"
            }

            TaskCategory.AUTHENTICATION -> {
                components += "AzimiAuth"
                components += "AzimiNetwork"
                components +=
                    "Supabase Authentication"
                components +=
                    "Authentication callback"
                components +=
                    "Owner Authority verification"
            }

            TaskCategory.CLOUD -> {
                components +=
                    "Cloud provider adapter"
                components += "API endpoint"
                components +=
                    "Network security policy"
            }

            TaskCategory.PROJECT_PRESERVATION -> {
                components += "Git repository"
                components += "Ownership records"
                components += "Provenance records"
                components += "Backup manifest"
                components += "Recovery procedure"
                components += "AtlasOwnerAuthority"
            }

            TaskCategory.BACKUP_RECOVERY -> {
                components += "Backup system"
                components +=
                    "Integrity manifest"
                components +=
                    "Recovery checkpoint"
                components +=
                    "Owner-controlled storage"
                components += "AtlasOwnerAuthority"
            }

            TaskCategory.AUTOMATION -> {
                components +=
                    "Automation capability registry"
                components += "Permission Engine"
                components += "Audit Trail"
                components += "AtlasOwnerAuthority"
            }

            TaskCategory.LANGUAGE -> {
                components += "ZLanguage"
                components += "Language resources"
                components += "Localization system"
            }

            TaskCategory.LEARNING -> {
                components += "Atlas Knowledge"
                components +=
                    "Learning explanation system"
                components +=
                    "Verified reference material"
            }

            TaskCategory.GENERAL -> {
                components += "AtlasCore"
                components += "Atlas Knowledge"
            }
        }

        if (
            containsAny(
                text,
                "memory",
                "remember",
                "context"
            )
        ) {
            components +=
                "Approved Memory System"
        }

        if (
            containsAny(
                text,
                "permission",
                "owner",
                "authorize",
                "authority"
            )
        ) {
            components +=
                "AtlasOwnerAuthority"
            components +=
                "Owner Permission Engine"
        }

        if (
            containsAny(
                text,
                "provenance",
                "ownership record",
                "proof of ownership",
                "creator",
                "founder"
            )
        ) {
            components +=
                "Ownership Registry"
            components +=
                "Provenance Record System"
        }

        if (
            containsAny(
                text,
                "test",
                "build",
                "compile"
            )
        ) {
            components +=
                "Build and Verification System"
        }

        return components.distinct()
    }

    private fun detectDependencies(
        text: String,
        category: TaskCategory
    ): List<String> {

        val dependencies =
            mutableListOf<String>()

        when (category) {

            TaskCategory.AUTHENTICATION -> {
                dependencies +=
                    "Valid authentication configuration"
                dependencies +=
                    "Valid redirect URI"
                dependencies +=
                    "Working authentication provider"
                dependencies +=
                    "Internet connectivity"
            }

            TaskCategory.CLOUD -> {
                dependencies +=
                    "Network connectivity"
                dependencies +=
                    "Valid endpoint"
                dependencies +=
                    "Provider adapter"
            }

            TaskCategory.ANDROID_DEVELOPMENT -> {
                dependencies +=
                    "Android project source"
                dependencies +=
                    "Compatible Android SDK"
                dependencies +=
                    "Build environment"
            }

            TaskCategory.WEB_DEVELOPMENT -> {
                dependencies +=
                    "Source files"
                dependencies +=
                    "Deployment configuration"
                dependencies +=
                    "Build verification"
            }

            TaskCategory.BACKUP_RECOVERY,
            TaskCategory.PROJECT_PRESERVATION -> {
                dependencies +=
                    "Accessible source files"
                dependencies +=
                    "Integrity verification"
                dependencies +=
                    "Owner-controlled backup destination"
                dependencies +=
                    "Ownership/provenance record capability"
            }

            TaskCategory.AI_INTELLIGENCE -> {
                dependencies +=
                    "Atlas Knowledge"
                dependencies +=
                    "Requirement analysis"
                dependencies +=
                    "Security policy"
                dependencies +=
                    "Replaceable AI adapter when needed"
            }

            else -> {
                dependencies +=
                    "Relevant project context"
                dependencies +=
                    "Available capability information"
            }
        }

        if (
            containsAny(
                text,
                "owner",
                "ownership",
                "provenance",
                "creator",
                "founder",
                "authority"
            )
        ) {
            dependencies +=
                "Atlas Owner Authority"
            dependencies +=
                "Verified owner authorization when required"
        }

        if (
            containsAny(
                text,
                "github",
                "repository",
                "repo"
            )
        ) {
            dependencies +=
                "Repository access"
        }

        if (
            containsAny(
                text,
                "supabase",
                "magic link",
                "login"
            )
        ) {
            dependencies +=
                "Supabase configuration"
        }

        if (
            containsAny(
                text,
                "apk",
                "build",
                "compile"
            )
        ) {
            dependencies +=
                "Build workflow"
        }

        return dependencies.distinct()
    }

    private fun detectMissingCapabilities(
        text: String,
        category: TaskCategory
    ): List<String> {

        val missing =
            mutableListOf<String>()

        when (category) {

            TaskCategory.AUTOMATION -> {
                missing +=
                    "Registered automation tools must be verified"
                missing +=
                    "Explicit action permissions must be configured"
            }

            TaskCategory.BACKUP_RECOVERY -> {
                missing +=
                    "Backup destination must be confirmed"
                missing +=
                    "Recovery verification must be performed"
            }

            TaskCategory.PROJECT_PRESERVATION -> {
                missing +=
                    "Repository access must be verified"
                missing +=
                    "Integrity snapshot capability must be verified"
            }

            TaskCategory.CLOUD -> {
                missing +=
                    "Available provider capabilities must be verified"
            }

            TaskCategory.ANDROID_DEVELOPMENT -> {
                missing +=
                    "Current Android source and build state must be verified"
            }

            else -> {
                // No automatic capability gap identified.
            }
        }

        if (
            containsAny(
                text,
                "execute",
                "delete",
                "publish",
                "deploy",
                "send",
                "install"
            )
        ) {
            missing +=
                "Explicit action authorization must be confirmed"
        }

        if (
            containsAny(
                text,
                "ownership",
                "provenance",
                "proof of ownership",
                "transfer ownership",
                "change owner"
            )
        ) {
            missing +=
                "Verified owner authorization must be established before protected ownership changes."
        }

        return missing.distinct()
    }

    private fun detectSecurityLevel(
        text: String
    ): SecurityLevel {

        if (
            AzimiAuth.isProtectedCredential(text)
        ) {
            return SecurityLevel.PROTECTED
        }

        if (
            containsAny(
                text,
                "password",
                "token",
                "credential",
                "private",
                "vault",
                "authentication",
                "security",
                "encryption",
                "owner",
                "ownership",
                "provenance",
                "permission",
                "recovery",
                "authority"
            )
        ) {
            return SecurityLevel.SENSITIVE
        }

        return SecurityLevel.NORMAL
    }

    private fun requiresPermission(
        text: String,
        intent: Intent,
        category: TaskCategory
    ): Boolean {

        if (
            intent == Intent.PROTECT ||
            intent == Intent.BACKUP ||
            intent == Intent.RECOVER ||
            intent == Intent.MODIFY
        ) {
            return true
        }

        if (
            category == TaskCategory.SECURITY ||
            category == TaskCategory.AUTHENTICATION ||
            category == TaskCategory.PROJECT_PRESERVATION ||
            category == TaskCategory.BACKUP_RECOVERY
        ) {
            return true
        }

        return containsAny(
            text,
            "delete",
            "deploy",
            "publish",
            "install",
            "send",
            "change settings",
            "access files",
            "execute"
        )
    }

    private fun recommendsBackup(
        text: String,
        intent: Intent,
        category: TaskCategory
    ): Boolean {

        if (
            intent == Intent.MODIFY ||
            intent == Intent.DEBUG ||
            intent == Intent.RECOVER ||
            intent == Intent.BACKUP
        ) {
            return true
        }

        if (
            category == TaskCategory.ANDROID_DEVELOPMENT ||
            category == TaskCategory.WEB_DEVELOPMENT ||
            category == TaskCategory.AUTHENTICATION ||
            category == TaskCategory.SECURITY ||
            category == TaskCategory.PROJECT_PRESERVATION
        ) {
            return containsAny(
                text,
                "change",
                "update",
                "replace",
                "fix",
                "refactor",
                "upgrade",
                "connect",
                "integrate",
                "ownership",
                "provenance"
            )
        }

        return false
    }

    private fun isExternalAIHelpful(
        text: String,
        category: TaskCategory
    ): Boolean {

        if (
            category == TaskCategory.AI_INTELLIGENCE ||
            category == TaskCategory.LEARNING ||
            category == TaskCategory.GENERAL
        ) {
            return true
        }

        return containsAny(
            text,
            "explain",
            "analyze",
            "research",
            "translate",
            "understand",
            "write",
            "generate"
        )
    }

    private fun buildWarnings(
        normalized: String,
        securityLevel: SecurityLevel,
        permissionRequired: Boolean,
        ownerAuthorizationRequired: Boolean,
        ownerAuthorized: Boolean,
        ownershipSensitive: Boolean,
        backupRecommended: Boolean
    ): List<String> {

        val warnings =
            mutableListOf<String>()

        if (
            securityLevel ==
                SecurityLevel.PROTECTED
        ) {
            warnings +=
                "Protected credential material must never be processed as ordinary context."
        }

        if (
            securityLevel ==
                SecurityLevel.SENSITIVE
        ) {
            warnings +=
                "Sensitive information detected. Guardian security boundaries remain active."
        }

        if (ownershipSensitive) {
            warnings +=
                "This request is ownership/provenance-sensitive."
        }

        if (ownerAuthorizationRequired) {
            if (ownerAuthorized) {
                warnings +=
                    "Owner authorization is currently active. Consequential actions still require their specific permission boundary."
            } else {
                warnings +=
                    "Verified owner authorization is required before protected owner operations."
            }
        }

        if (permissionRequired) {
            warnings +=
                "Permission may be required before any consequential action."
        }

        if (backupRecommended) {
            warnings +=
                "Create or verify a recovery checkpoint before changing project state."
        }

        if (
            containsAny(
                normalized,
                "delete",
                "destroy",
                "remove permanently"
            )
        ) {
            warnings +=
                "Destructive operations require explicit confirmation and verified recovery."
        }

        return warnings.distinct()
    }

    private fun determineStatus(
        missingCapabilities: List<String>,
        permissionRequired: Boolean,
        ownerAuthorizationRequired: Boolean,
        ownerAuthorized: Boolean,
        securityLevel: SecurityLevel
    ): RequirementStatus {

        if (
            securityLevel ==
                SecurityLevel.PROTECTED
        ) {
            return RequirementStatus.BLOCKED
        }

        if (
            ownerAuthorizationRequired &&
            !ownerAuthorized
        ) {
            return RequirementStatus.NEEDS_OWNER_PERMISSION
        }

        if (
            missingCapabilities.isNotEmpty()
        ) {
            return RequirementStatus.PARTIALLY_READY
        }

        if (permissionRequired) {
            return RequirementStatus.NEEDS_OWNER_PERMISSION
        }

        return RequirementStatus.READY
    }

    private fun determineNextSafeAction(
        intent: Intent,
        category: TaskCategory,
        status: RequirementStatus,
        permissionRequired: Boolean,
        ownerAuthorizationRequired: Boolean,
        ownerAuthorized: Boolean,
        backupRecommended: Boolean
    ): String {

        if (
            status == RequirementStatus.BLOCKED
        ) {
            return "Stop processing and protect the sensitive information."
        }

        if (
            ownerAuthorizationRequired &&
            !ownerAuthorized
        ) {
            return "Verify the actor's owner authority before performing protected owner operations."
        }

        if (backupRecommended) {
            return "Verify or create a recovery checkpoint before making changes."
        }

        if (permissionRequired) {
            return "Explain the proposed action and request explicit permission before consequential changes."
        }

        if (
            status ==
                RequirementStatus.PARTIALLY_READY
        ) {
            return "Inspect the missing capabilities and identify the exact limitation."
        }

        return when (category) {

            TaskCategory.AI_INTELLIGENCE ->
                "Use Atlas Knowledge and Requirement Analysis to produce a direct response."

            TaskCategory.ANDROID_DEVELOPMENT ->
                "Inspect the current Android source and verify the build state."

            TaskCategory.WEB_DEVELOPMENT ->
                "Inspect the relevant source files and deployment configuration."

            TaskCategory.AUTHENTICATION ->
                "Inspect the authentication flow without exposing tokens or private data."

            TaskCategory.SECURITY ->
                "Review the security boundary and identify the minimum required permission."

            TaskCategory.BACKUP_RECOVERY ->
                "Verify the backup source, integrity manifest, and recovery destination."

            TaskCategory.PROJECT_PRESERVATION ->
                "Inspect the project source and establish an integrity-preserving snapshot."

            TaskCategory.LEARNING ->
                "Explain the subject clearly using verified knowledge and practical examples."

            else ->
                "Understand the request, check available capabilities, and provide the safest useful response."
        }
    }

    private fun containsAny(
        text: String,
        vararg values: String
    ): Boolean {

        return values.any { value ->
            text.contains(value)
        }
    }

    fun buildSummary(
        analysis: RequirementAnalysis
    ): String {

        return buildString {

            appendLine(
                "ATLAS REQUIREMENT ANALYSIS"
            )
            appendLine()

            appendLine(
                "REQUEST: ${analysis.originalRequest}"
            )
            appendLine(
                "INTENT: ${analysis.intent}"
            )
            appendLine(
                "CATEGORY: ${analysis.category}"
            )
            appendLine(
                "SECURITY: ${analysis.securityLevel}"
            )
            appendLine(
                "STATUS: ${analysis.status}"
            )
            appendLine(
                "ACTOR: ${analysis.actorType}"
            )
            appendLine(
                "AUTHORITY: ${analysis.authorityLevel}"
            )
            appendLine(
                "OWNER AUTHORIZED: ${analysis.ownerAuthorized}"
            )
            appendLine(
                "OWNERSHIP SENSITIVE: ${analysis.ownershipSensitive}"
            )
            appendLine(
                "OWNER AUTHORIZATION REQUIRED: ${analysis.ownerAuthorizationRequired}"
            )
            appendLine(
                "PERMISSION REQUIRED: ${analysis.permissionRequired}"
            )
            appendLine(
                "BACKUP RECOMMENDED: ${analysis.backupRecommended}"
            )
            appendLine(
                "EXTERNAL AI HELPFUL: ${analysis.externalAIHelpful}"
            )

            appendLine()

            appendLine("REQUIRED COMPONENTS:")

            if (
                analysis.requiredComponents.isEmpty()
            ) {
                appendLine("- None identified")
            } else {
                analysis.requiredComponents.forEach {
                    appendLine("- $it")
                }
            }

            appendLine()

            appendLine("DEPENDENCIES:")

            if (
                analysis.dependencies.isEmpty()
            ) {
                appendLine("- None identified")
            } else {
                analysis.dependencies.forEach {
                    appendLine("- $it")
                }
            }

            appendLine()

            appendLine("MISSING CAPABILITIES:")

            if (
                analysis.missingCapabilities.isEmpty()
            ) {
                appendLine("- None identified")
            } else {
                analysis.missingCapabilities.forEach {
                    appendLine("- $it")
                }
            }

            appendLine()

            appendLine("WARNINGS:")

            if (
                analysis.warnings.isEmpty()
            ) {
                appendLine("- None")
            } else {
                analysis.warnings.forEach {
                    appendLine("- $it")
                }
            }

            appendLine()

            appendLine("NEXT SAFE ACTION:")

            appendLine(
                analysis.nextSafeAction
            )
        }
    }
}
