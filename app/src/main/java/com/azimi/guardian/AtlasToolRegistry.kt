package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Atlas Tool Registry
 *
 * Central registry for modular Atlas capabilities.
 *
 * Atlas Core does not need to know how every tool works.
 * It can discover a capability through this registry.
 *
 * Adding a new capability should normally require:
 *
 * 1. Create one AtlasTool implementation.
 * 2. Register it here.
 *
 * The rest of Atlas remains unchanged.
 */
object AtlasToolRegistry {

    /**
     * All registered Atlas tools.
     *
     * Keep this list small and declarative.
     *
     * Tool implementation belongs in its own file.
     */
    private val tools: List<AtlasTool> =
        listOf(
            AtlasVaultTool()
        )

    /**
     * Return every registered tool.
     */
    fun allTools(): List<AtlasTool> {
        return tools.toList()
    }

    /**
     * Find a tool by its stable ID.
     */
    fun getTool(
        id: String
    ): AtlasTool? {

        val normalizedId =
            id.trim().lowercase()

        if (normalizedId.isBlank()) {
            return null
        }

        return tools.firstOrNull {
            it.id.trim().lowercase() ==
                normalizedId
        }
    }

    /**
     * Find tools capable of handling a request.
     *
     * This only asks the tools whether they can handle
     * the request. It does not execute anything.
     */
    fun findTools(
        request: String
    ): List<AtlasTool> {

        val cleanRequest =
            request.trim()

        if (cleanRequest.isBlank()) {
            return emptyList()
        }

        return tools.filter { tool ->

            runCatching {
                tool.canHandle(
                    cleanRequest
                )
            }.getOrDefault(false)
        }
    }

    /**
     * Find the first available tool for a request.
     *
     * Availability is evaluated separately from capability.
     */
    fun findAvailableTool(
        context: Context,
        request: String
    ): AtlasTool? {

        val appContext =
            context.applicationContext

        return findTools(request)
            .firstOrNull { tool ->

                AtlasPermissionChecker.isAllowed(
                    appContext,
                    tool.permission
                )
            }
    }

    /**
     * Determine whether a tool exists.
     */
    fun hasTool(
        id: String
    ): Boolean {

        return getTool(id) != null
    }

    /**
     * Number of registered capabilities.
     */
    fun count(): Int {
        return tools.size
    }

    /**
     * Safe diagnostic summary.
     *
     * Does not expose secrets or protected data.
     */
    fun diagnostics(): String {

        return buildString {

            appendLine(
                "ATLAS TOOL REGISTRY"
            )

            appendLine()

            appendLine(
                "REGISTERED TOOLS: ${tools.size}"
            )

            if (tools.isEmpty()) {

                appendLine(
                    "STATUS: FOUNDATION READY"
                )

                appendLine(
                    "TOOLS: NONE REGISTERED YET"
                )

            } else {

                appendLine(
                    "STATUS: ONLINE"
                )

                appendLine()

                tools.forEach { tool ->

                    appendLine(
                        "${tool.id} | ${tool.name}"
                    )

                    appendLine(
                        "PERMISSION: ${tool.permission}"
                    )

                    appendLine(
                        "OFFLINE: ${tool.supportsOffline}"
                    )

                    appendLine(
                        "VAULT ACCESS: ${tool.requiresVaultAccess}"
                    )

                    appendLine(
                        "CONSEQUENTIAL: ${tool.consequential}"
                    )

                    appendLine()
                }
            }
        }
    }
}
