package com.azimi.guardian

/**
 * ATLAS KNOWLEDGE
 *
 * The read-only project knowledge layer for AZIMI.
 *
 * Purpose:
 * - Give Atlas durable structured knowledge about AZIMI.
 * - Distinguish existing, in-development, planned, and blocked components.
 * - Preserve ownership, security, recovery, portability, and provider-independence principles.
 * - Help Atlas understand what a request may require before producing a plan.
 *
 * Important:
 * - This file contains project knowledge, not secrets.
 * - Never place passwords, API keys, access tokens, refresh tokens,
 *   verification codes, recovery codes, private keys, or other credentials here.
 * - This layer does not execute actions.
 * - Guardian remains the security and permission authority.
 * - External AI providers remain replaceable engines, not Atlas itself.
 */
object AtlasKnowledge {

    const val KNOWLEDGE_VERSION = "1.0.0"
    const val SYSTEM_NAME = "AZIMI"
    const val INTELLIGENCE_NAME = "ATLAS"
    const val OWNER_NAME = "Zaman Azimi"

    // -------------------------------------------------------------------------
    // STATUS MODEL
    // -------------------------------------------------------------------------

    enum class ComponentStatus {
        EXISTS,
        IN_DEVELOPMENT,
        PLANNED,
        BLOCKED
    }

    enum class KnowledgeArea {
        IDENTITY,
        MISSION,
        PRINCIPLES,
        ARCHITECTURE,
        SECURITY,
        MEMORY,
        RECOVERY,
        AI,
        ANDROID,
        WEB,
        CLOUD,
        DEVELOPMENT,
        LANGUAGE,
        OWNERSHIP,
        CURRENT_STATE
    }

    // -------------------------------------------------------------------------
    // PROJECT IDENTITY
    // -------------------------------------------------------------------------

    data class ProjectIdentity(
        val systemName: String,
        val intelligenceName: String,
        val owner: String,
        val creator: String,
        val purpose: String,
        val identityRule: String
    )

    val identity = ProjectIdentity(
        systemName = "AZIMI",
        intelligenceName = "Atlas",
        owner = "Zaman Azimi",
        creator = "Zaman Azimi",
        purpose = """
            AZIMI is an owner-controlled technology system and long-term
            technology studio architecture built to turn ideas into real,
            testable, recoverable, portable technology.
        """.trimIndent(),
        identityRule = """
            Zaman Azimi is the founder, creator, owner, and ultimate authority
            for AZIMI. Atlas assists and coordinates according to authorized
            instructions. Atlas does not replace the owner's authority.
        """.trimIndent()
    )

    // -------------------------------------------------------------------------
    // MISSION
    // -------------------------------------------------------------------------

    val mission = listOf(
        "Turn curiosity and ideas into real technology that can be built, tested, published, and improved.",
        "Build an independent and owner-controlled technology system.",
        "Protect project source code, architecture, approved memory, credentials, recovery information, and continuity.",
        "Make AZIMI portable across devices, platforms, providers, and deployment environments.",
        "Reduce dependence on any single AI, cloud, authentication, storage, or deployment provider.",
        "Build useful intelligence while keeping security and permission boundaries explicit."
    )

    // -------------------------------------------------------------------------
    // CORE PRINCIPLES
    // -------------------------------------------------------------------------

    val principles = listOf(
        "OWNER_CONTROLLED",
        "PRIVACY_FIRST",
        "PHONE_FIRST",
        "LOW_COST_FIRST",
        "PROVIDER_INDEPENDENT",
        "PORTABLE",
        "RECOVERABLE",
        "REPAIRABLE",
        "AUDITABLE",
        "SECURITY_BY_BOUNDARY",
        "EXPLICIT_PERMISSION",
        "APPROVED_MEMORY_ONLY",
        "NO_SECRET_MEMORY",
        "NO_DESTRUCTIVE_ACTION_WITHOUT_AUTHORIZATION",
        "CONTINUITY_OVER_REBUILDING"
    )

    // -------------------------------------------------------------------------
    // ARCHITECTURE
    // -------------------------------------------------------------------------

    data class Component(
        val name: String,
        val role: String,
        val status: ComponentStatus,
        val dependencies: List<String> = emptyList(),
        val notes: String = ""
    )

    val architecture = listOf(

        Component(
            name = "Atlas Core",
            role = "Central intelligence, reasoning, orchestration, requirement understanding, and planning layer.",
            status = ComponentStatus.EXISTS,
            dependencies = listOf(
                "Guardian",
                "Atlas Knowledge",
                "AI Engine Adapter"
            ),
            notes = """
                Atlas Core should understand requests, identify requirements,
                create plans, and coordinate capabilities. It must not become
                a direct bypass around Guardian security controls.
            """.trimIndent()
        ),

        Component(
            name = "Atlas Knowledge",
            role = "Structured knowledge about AZIMI, its architecture, principles, modules, state, and terminology.",
            status = ComponentStatus.IN_DEVELOPMENT,
            dependencies = listOf(
                "Atlas Core"
            ),
            notes = """
                Read-only knowledge layer. It should evolve into a portable
                project knowledge system rather than a provider-specific prompt.
            """.trimIndent()
        ),

        Component(
            name = "Guardian",
            role = "Security, authentication, policy, permission, and device-side control boundary.",
            status = ComponentStatus.EXISTS,
            dependencies = emptyList(),
            notes = """
                Guardian is the security authority around Atlas on Android.
                Atlas must respect Guardian policy and permission boundaries.
            """.trimIndent()
        ),

        Component(
            name = "AtlasGuardianBridge",
            role = "Controlled bridge between Guardian and Atlas Core/AI.",
            status = ComponentStatus.EXISTS,
            dependencies = listOf(
                "Guardian",
                "Atlas Core",
                "AzimiNetwork"
            ),
            notes = """
                The bridge should validate authentication, security policy,
                protected material, and safe context before allowing an
                Atlas request to continue.
            """.trimIndent()
        ),

        Component(
            name = "AzimiNetwork",
            role = "Controlled background network layer for authentication and AI requests.",
            status = ComponentStatus.EXISTS,
            dependencies = listOf(
                "AzimiAuth",
                "AzimiAiClient"
            )
        ),

        Component(
            name = "AzimiAiClient",
            role = "Replaceable network client for the AZIMI AI gateway.",
            status = ComponentStatus.EXISTS,
            dependencies = listOf(
                "AZIMI AI gateway"
            ),
            notes = """
                This is an adapter/client, not the identity of Atlas.
                The architecture must allow the engine behind it to change.
            """.trimIndent()
        ),

        Component(
            name = "AzimiAuth",
            role = "Authentication and session handling for AZIMI AI access.",
            status = ComponentStatus.EXISTS,
            dependencies = listOf(
                "Supabase authentication"
            ),
            notes = """
                Authentication is an infrastructure dependency.
                Authentication failure must not be treated as Atlas Core failure.
            """.trimIndent()
        ),

        Component(
            name = "Z Vault",
            role = "Protected owner-controlled storage architecture for sensitive AZIMI data.",
            status = ComponentStatus.IN_DEVELOPMENT,
            dependencies = listOf(
                "Guardian",
                "VaultCrypto"
            ),
            notes = """
                One portable Z Vault architecture should adapt to different
                Android versions, devices, manufacturers, and hardware through
                capability adapters rather than separate Vault architectures.
            """.trimIndent()
        ),

        Component(
            name = "Z Origin",
            role = "Special owner-controlled private area within the AZIMI security architecture.",
            status = ComponentStatus.IN_DEVELOPMENT,
            dependencies = listOf(
                "Guardian",
                "Z Vault",
                "Owner authentication"
            )
        ),

        Component(
            name = "Z Recovery",
            role = "Recovery, backup, continuity, rollback, and restoration architecture.",
            status = ComponentStatus.IN_DEVELOPMENT,
            dependencies = listOf(
                "Z Vault",
                "Project records",
                "Backups"
            ),
            notes = """
                Recovery must survive provider outages, deployment problems,
                authentication failures, device replacement, and code regressions.
            """.trimIndent()
        ),

        Component(
            name = "Z Shield",
            role = "Security protection and defensive controls.",
            status = ComponentStatus.IN_DEVELOPMENT,
            dependencies = listOf(
                "Guardian"
            )
        ),

        Component(
            name = "Z Control",
            role = "Controlled device/system information and management interface.",
            status = ComponentStatus.EXISTS,
            dependencies = listOf(
                "Guardian"
            )
        ),

        Component(
            name = "Z Cloud",
            role = "Portable cloud/storage/synchronization layer for AZIMI.",
            status = ComponentStatus.PLANNED,
            dependencies = listOf(
                "Z Vault",
                "Z Recovery",
                "Identity",
                "Provider adapters"
            ),
            notes = """
                Z Cloud must not become permanently dependent on one cloud provider.
            """.trimIndent()
        ),

        Component(
            name = "Z Launcher",
            role = "Future AZIMI-oriented launcher and entry point for the owner/device.",
            status = ComponentStatus.PLANNED,
            dependencies = listOf(
                "Guardian",
                "Z Control",
                "Atlas"
            )
        ),

        Component(
            name = "Z Lab",
            role = "Experimental and development environment for AZIMI capabilities.",
            status = ComponentStatus.EXISTS,
            dependencies = listOf(
                "Guardian"
            )
        ),

        Component(
            name = "Z Scan / Capture",
            role = "Future controlled scanning, capture, and input capability.",
            status = ComponentStatus.PLANNED,
            dependencies = listOf(
                "Guardian",
                "Permission system",
                "Atlas Core"
            )
        ),

        Component(
            name = "Audit Trail",
            role = "Record of important system events, decisions, changes, and security-relevant operations.",
            status = ComponentStatus.PLANNED,
            dependencies = listOf(
                "Guardian",
                "Atlas Core",
                "Z Recovery"
            )
        ),

        Component(
            name = "AI Engine Adapter",
            role = "Provider-independent interface between Atlas and external/local AI engines.",
            status = ComponentStatus.IN_DEVELOPMENT,
            dependencies = emptyList(),
            notes = """
                External AI engines are replaceable modules. Atlas must not
                depend on a single provider for its identity or continuity.
            """.trimIndent()
        )
    )

    // -------------------------------------------------------------------------
    // SECURITY KNOWLEDGE
    // -------------------------------------------------------------------------

    val securityRules = listOf(
        "Guardian is the security authority.",
        "Atlas must respect Android and platform security boundaries.",
        "Atlas must not silently bypass permissions.",
        "Atlas must not bypass authentication.",
        "Consequential actions require explicit authorization and appropriate permission.",
        "Protected credentials must never enter Atlas approved memory.",
        "Sensitive credentials must remain outside ordinary AI context.",
        "Raw biometric data must not be stored by Atlas.",
        "Android biometric APIs should be used where supported.",
        "Security-sensitive actions require stronger authorization than ordinary conversation.",
        "A provider outage is not an Atlas Core failure.",
        "Authentication failure is not an Atlas intelligence failure.",
        "Network failure is not a reason to destroy or rebuild project state.",
        "Destructive changes require a recoverable checkpoint."
    )

    val protectedInformation = listOf(
        "PASSWORDS",
        "API_KEYS",
        "ACCESS_TOKENS",
        "REFRESH_TOKENS",
        "VERIFICATION_CODES",
        "RECOVERY_CODES",
        "PRIVATE_KEYS",
        "AUTHENTICATION_SECRETS",
        "PRIVATE_CREDENTIALS",
        "LIVE_AUTHENTICATION_LINKS",
        "RAW_BIOMETRIC_DATA"
    )

    // -------------------------------------------------------------------------
    // MEMORY KNOWLEDGE
    // -------------------------------------------------------------------------

    val memoryRules = listOf(
        "Only explicitly approved project context may become durable Atlas memory.",
        "Memory should preserve architecture, decisions, progress, relationships, and approved project knowledge.",
        "Passwords must never be stored in AI memory.",
        "API keys must never be stored in AI memory.",
        "Access tokens must never be stored in AI memory.",
        "Refresh tokens must never be stored in AI memory.",
        "Verification codes must never be stored in AI memory.",
        "Recovery codes must never be stored in AI memory.",
        "Private credentials must never be stored in AI memory.",
        "Users must be able to explicitly forget approved memory.",
        "Memory should remain portable and recoverable.",
        "Memory should not make AZIMI permanently dependent on one AI provider."
    )

    // -------------------------------------------------------------------------
    // RECOVERY KNOWLEDGE
    // -------------------------------------------------------------------------

    val recoveryRules = listOf(
        "BACKUP",
        "CHECKPOINT",
        "CHANGE",
        "BUILD",
        "TEST",
        "VERIFY",
        "CHECKPOINT"
    )

    val failureRecoveryFlow = listOf(
        "ROLLBACK",
        "LAST_KNOWN_GOOD",
        "DIAGNOSE",
        "FIX",
        "BUILD",
        "TEST",
        "VERIFY",
        "CHECKPOINT"
    )

    const val CURRENT_RECOVERY_POINT = "ATLAS-AUTH-FLOW-BEFORE-FIX"
    const val NEXT_TARGET_CHECKPOINT = "ATLAS-AUTH-HANDOFF-WORKING"

    // -------------------------------------------------------------------------
    // CURRENT PROJECT STATE
    // -------------------------------------------------------------------------

    val currentState = mapOf(
        "Atlas Core" to "EXISTS",
        "Atlas Knowledge" to "IN_DEVELOPMENT",
        "Guardian" to "EXISTS",
        "Guardian-to-Atlas architecture" to "EXISTS",
        "AI Engine Adapter architecture" to "IN_DEVELOPMENT",
        "Real AI connection" to "IN_DEVELOPMENT",
        "Authentication flow" to "BLOCKED",
        "Z Vault" to "IN_DEVELOPMENT",
        "Z Recovery" to "IN_DEVELOPMENT",
        "Z Shield" to "IN_DEVELOPMENT",
        "Z Cloud" to "PLANNED",
        "Z Launcher" to "PLANNED",
        "Z Lab" to "EXISTS",
        "Dari + English support" to "PLANNED"
    )

    const val AUTHENTICATION_STATE =
        "Supabase Magic Link authentication flow is being repaired; observed service throttling must not be confused with an Atlas Core failure."

    // -------------------------------------------------------------------------
    // DEVELOPMENT KNOWLEDGE
    // -------------------------------------------------------------------------

    val developmentRules = listOf(
        "Do not rebuild functioning components unnecessarily.",
        "Inspect existing architecture before creating a replacement.",
        "Preserve previously accepted decisions unless explicitly changed.",
        "Every major change should have a recoverable checkpoint.",
        "Test before declaring a component working.",
        "Do not claim a deployment, backup, build, or fix occurred unless it was actually verified.",
        "Record important failures so the same problem does not cause a project restart.",
        "Prefer small reversible changes over destructive rewrites.",
        "Keep project architecture portable.",
        "Keep external providers behind replaceable adapters."
    )

    // -------------------------------------------------------------------------
    // LANGUAGE KNOWLEDGE
    // -------------------------------------------------------------------------

    val supportedLanguages = listOf(
        "English",
        "Dari"
    )

    const val LANGUAGE_RULE =
        "Atlas should be designed with internationalization in mind so additional languages can be added without rebuilding the intelligence architecture."

    // -------------------------------------------------------------------------
    // AZIMI.STUDIO KNOWLEDGE
    // -------------------------------------------------------------------------

    val azimiStudio = mapOf(
        "purpose" to "Independent technology studio and public-facing AZIMI project.",
        "relationship" to "AZIMI.STUDIO is part of the broader AZIMI ecosystem.",
        "owner" to "Zaman Azimi",
        "architecture_rule" to "AZIMI.STUDIO should follow the same ownership, protection, provenance, continuity, and recovery principles as AZIMI.",
        "public_role" to "Present real work, progress, projects, capabilities, and direction."
    )

    // -------------------------------------------------------------------------
    // PROVIDER INDEPENDENCE
    // -------------------------------------------------------------------------

    val providerIndependenceRules = listOf(
        "No single AI provider should define Atlas.",
        "No single cloud provider should define AZIMI.",
        "No single authentication provider should define AZIMI identity.",
        "No single storage provider should be the only recovery copy.",
        "No single deployment platform should be required for project continuity.",
        "External services must be replaceable through adapters.",
        "Portable project data should remain understandable without the original provider.",
        "Recovery procedures must account for provider loss."
    )

    // -------------------------------------------------------------------------
    // ATLAS BEHAVIOR
    // -------------------------------------------------------------------------

    val atlasBehaviorRules = listOf(
        "Understand the request before proposing implementation.",
        "Use project knowledge before inventing new architecture.",
        "Check what already exists.",
        "Identify missing requirements.",
        "Identify dependencies.",
        "Identify security implications.",
        "Identify required permissions.",
        "Separate planning from execution.",
        "Ask for explicit authorization before consequential actions.",
        "Never pretend an unverified action succeeded.",
        "Protect project continuity.",
        "Preserve approved architecture.",
        "Prefer reversible changes.",
        "Explain blockers accurately.",
        "Treat provider, authentication, and network failures as infrastructure conditions rather than automatically as intelligence failures."
    )

    // -------------------------------------------------------------------------
    // TERMINOLOGY
    // -------------------------------------------------------------------------

    val terminology = mapOf(
        "AZIMI" to "The overall owner-controlled technology ecosystem.",
        "Atlas" to "The intelligence/orchestration system within AZIMI.",
        "Atlas Core" to "The central reasoning and orchestration layer.",
        "Atlas Knowledge" to "Structured knowledge Atlas uses to understand AZIMI.",
        "Guardian" to "Security, authentication, permission, and policy boundary.",
        "Z Vault" to "Protected owner-controlled storage architecture.",
        "Z Origin" to "Special owner-controlled private space.",
        "Z Recovery" to "Backup, rollback, restoration, and continuity architecture.",
        "Z Shield" to "Security protection layer.",
        "Z Connect" to "Controlled connection/integration layer.",
        "Z Control" to "Controlled device/system management layer.",
        "Z Cloud" to "Future portable cloud/synchronization layer.",
        "Z Launcher" to "Future AZIMI launcher/entry environment.",
        "Z Lab" to "Experimental/development environment.",
        "Z Scan/Capture" to "Future controlled scanning and capture capability.",
        "AI Engine Adapter" to "Replaceable interface to an AI engine.",
        "Z Sovereign" to "The long-term owner-controlled, portable AZIMI architecture."
    )

    // -------------------------------------------------------------------------
    // REQUIREMENT DETECTION
    // -------------------------------------------------------------------------

    data class RequirementProfile(
        val request: String,
        val areas: List<KnowledgeArea>,
        val relevantComponents: List<Component>,
        val securityRequirements: List<String>,
        val likelyDependencies: List<String>,
        val warnings: List<String>
    )

    /**
     * Lightweight first-generation requirement detector.
     *
     * This does not execute anything.
     * It only uses structured project knowledge to help Atlas Core
     * understand which parts of AZIMI may be relevant to a request.
     */
    fun analyzeRequest(request: String): RequirementProfile {
        val text = request.trim().lowercase()

        if (text.isBlank()) {
            return RequirementProfile(
                request = "",
                areas = emptyList(),
                relevantComponents = emptyList(),
                securityRequirements = listOf(
                    "A non-empty request is required."
                ),
                likelyDependencies = emptyList(),
                warnings = emptyList()
            )
        }

        val areas = linkedSetOf<KnowledgeArea>()
        val componentNames = linkedSetOf<String>()
        val dependencies = linkedSetOf<String>()
        val warnings = mutableListOf<String>()

        fun match(area: KnowledgeArea, vararg words: String) {
            if (words.any { text.contains(it) }) {
                areas.add(area)
            }
        }

        match(
            KnowledgeArea.AI,
            "ai",
            "atlas",
            "intelligence",
            "model",
            "assistant",
            "reason",
            "chat"
        )

        match(
            KnowledgeArea.SECURITY,
            "security",
            "secure",
            "protect",
            "shield",
            "permission",
            "credential",
            "password",
            "token",
            "biometric"
        )

        match(
            KnowledgeArea.MEMORY,
            "memory",
            "remember",
            "forget",
            "knowledge",
            "context"
        )

        match(
            KnowledgeArea.RECOVERY,
            "backup",
            "recover",
            "recovery",
            "rollback",
            "restore",
            "checkpoint"
        )

        match(
            KnowledgeArea.ANDROID,
            "android",
            "phone",
            "device",
            "launcher",
            "apk",
            "biometric"
        )

        match(
            KnowledgeArea.WEB,
            "web",
            "website",
            "vercel",
            "github",
            "api",
            "studio"
        )

        match(
            KnowledgeArea.CLOUD,
            "cloud",
            "sync",
            "storage",
            "server"
        )

        match(
            KnowledgeArea.DEVELOPMENT,
            "build",
            "compile",
            "code",
            "debug",
            "fix",
            "test",
            "deploy",
            "project"
        )

        match(
            KnowledgeArea.LANGUAGE,
            "dari",
            "english",
            "language",
            "translate",
            "multilingual"
        )

        match(
            KnowledgeArea.OWNERSHIP,
            "owner",
            "ownership",
            "protection",
            "provenance",
            "sovereign"
        )

        match(
            KnowledgeArea.ARCHITECTURE,
            "architecture",
            "module",
            "system",
            "core",
            "component",
            "dependency"
        )

        for (component in architecture) {
            val nameWords = component.name
                .lowercase()
                .split(" ", "/", "-")

            if (
                nameWords.any { word ->
                    word.length >= 3 && text.contains(word)
                }
            ) {
                componentNames.add(component.name)
            }
        }

        if (text.contains("vault")) {
            componentNames.add("Z Vault")
            areas.add(KnowledgeArea.SECURITY)
        }

        if (text.contains("recovery")) {
            componentNames.add("Z Recovery")
            areas.add(KnowledgeArea.RECOVERY)
        }

        if (text.contains("cloud")) {
            componentNames.add("Z Cloud")
            areas.add(KnowledgeArea.CLOUD)
        }

        if (text.contains("launcher")) {
            componentNames.add("Z Launcher")
            areas.add(KnowledgeArea.ANDROID)
        }

        if (text.contains("shield")) {
            componentNames.add("Z Shield")
            areas.add(KnowledgeArea.SECURITY)
        }

        if (text.contains("guardian")) {
            componentNames.add("Guardian")
            areas.add(KnowledgeArea.SECURITY)
        }

        if (text.contains("authentication") || text.contains("login")) {
            componentNames.add("AzimiAuth")
            areas.add(KnowledgeArea.SECURITY)
            dependencies.add("Authentication provider")
        }

        if (
            text.contains("ai") ||
            text.contains("atlas") ||
            text.contains("assistant")
        ) {
            componentNames.add("Atlas Core")
            componentNames.add("Atlas Knowledge")
            componentNames.add("AI Engine Adapter")
            dependencies.add("Guardian")
        }

        if (
            text.contains("build") ||
            text.contains("compile") ||
            text.contains("apk") ||
            text.contains("debug") ||
            text.contains("fix")
        ) {
            dependencies.add("Build system")
            dependencies.add("Testing/verification")
        }

        if (
            text.contains("backup") ||
            text.contains("recover") ||
            text.contains("rollback")
        ) {
            dependencies.add("Known-good checkpoint")
            dependencies.add("Portable backup")
            dependencies.add("Recovery manifest")
        }

        if (
            text.contains("security") ||
            text.contains("permission") ||
            text.contains("credential") ||
            text.contains("password") ||
            text.contains("token")
        ) {
            warnings.add(
                "Security-sensitive material must remain outside approved Atlas memory."
            )
            warnings.add(
                "Consequential operations require explicit authorization and appropriate Guardian controls."
            )
        }

        if (
            text.contains("provider") ||
            text.contains("openai") ||
            text.contains("cloud") ||
            text.contains("vercel") ||
            text.contains("supabase")
        ) {
            warnings.add(
                "Keep the capability behind a replaceable adapter so AZIMI remains provider-independent."
            )
        }

        if (text.contains("delete") || text.contains("remove") || text.contains("destroy")) {
            warnings.add(
                "Potentially destructive operation detected; preserve a recoverable checkpoint before execution."
            )
        }

        val relevantComponents = architecture.filter {
            componentNames.contains(it.name)
        }

        val securityRequirements = buildList {
            add("Guardian policy must remain authoritative.")
            if (
                areas.contains(KnowledgeArea.SECURITY) ||
                areas.contains(KnowledgeArea.AI)
            ) {
                add("Protected credentials must not enter Atlas memory.")
            }
            if (text.contains("execute") || text.contains("change") || text.contains("delete")) {
                add("Consequential actions require explicit authorization.")
            }
            if (
                text.contains("build") ||
                text.contains("change") ||
                text.contains("deploy")
            ) {
                add("A recoverable checkpoint should exist before a major change.")
            }
        }

        return RequirementProfile(
            request = request,
            areas = areas.toList(),
            relevantComponents = relevantComponents,
            securityRequirements = securityRequirements.distinct(),
            likelyDependencies = dependencies.toList(),
            warnings = warnings.distinct()
        )
    }

    // -------------------------------------------------------------------------
    // KNOWLEDGE QUERIES
    // -------------------------------------------------------------------------

    fun getComponent(name: String): Component? {
        return architecture.firstOrNull {
            it.name.equals(name.trim(), ignoreCase = true)
        }
    }

    fun getComponentsByStatus(status: ComponentStatus): List<Component> {
        return architecture.filter { it.status == status }
    }

    fun getKnowledgeSummary(): String {
        return buildString {
            appendLine("AZIMI KNOWLEDGE")
            appendLine("Knowledge version: $KNOWLEDGE_VERSION")
            appendLine("Owner: $OWNER_NAME")
            appendLine("Intelligence: $INTELLIGENCE_NAME")
            appendLine()
            appendLine("Core principles:")
            principles.forEach {
                appendLine("- $it")
            }
            appendLine()
            appendLine("Architecture:")
            architecture.forEach {
                appendLine("- ${it.name}: ${it.status}")
            }
            appendLine()
            appendLine("Current recovery point: $CURRENT_RECOVERY_POINT")
            appendLine("Next target checkpoint: $NEXT_TARGET_CHECKPOINT")
            appendLine()
            appendLine("Supported languages: ${supportedLanguages.joinToString(", ")}")
        }
    }

    /**
     * Returns only non-secret project knowledge suitable for Atlas context.
     */
    fun getSafeContext(): Map<String, Any> {
        return mapOf(
            "knowledge_version" to KNOWLEDGE_VERSION,
            "system" to SYSTEM_NAME,
            "intelligence" to INTELLIGENCE_NAME,
            "owner" to OWNER_NAME,
            "principles" to principles,
            "architecture" to architecture.map {
                mapOf(
                    "name" to it.name,
                    "role" to it.role,
                    "status" to it.status.name,
                    "dependencies" to it.dependencies,
                    "notes" to it.notes
                )
            },
            "security_rules" to securityRules,
            "memory_rules" to memoryRules,
            "recovery_rules" to recoveryRules,
            "provider_independence_rules" to providerIndependenceRules,
            "development_rules" to developmentRules,
            "supported_languages" to supportedLanguages,
            "terminology" to terminology,
            "current_state" to currentState,
            "current_recovery_point" to CURRENT_RECOVERY_POINT,
            "next_target_checkpoint" to NEXT_TARGET_CHECKPOINT
        )
    }
}
