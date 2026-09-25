package com.azimi.guardian

import android.content.Context

class AtlasMemoryTool : AtlasTool {

    override val id = "atlas_memory"

    override val name = "Atlas Memory"

    override val description =
        "Owner-controlled approved memory for Atlas project context."

    override val permission =
        AtlasPermission.VAULT

    override val supportsOffline = true

    override val requiresVaultAccess = true

    override val consequential = true

    override fun canHandle(
        request: String
    ): Boolean {

        val text =
            request.trim().lowercase()

        if (text.isBlank()) {
            return false
        }

        val keywords =
            listOf(
                "remember",
                "save to memory",
                "save this to memory",
                "atlas memory",
                "memory status",
                "show memory",
                "view memory",
                "memory list",
                "forget",
                "forget this",
                "clear memory",
                "delete memory"
            )

        return keywords.any { keyword ->
            text.contains(keyword)
        }
    }

    override fun execute(
        context: Context,
        request: String
    ): AtlasToolResult {

        val appContext =
            context.applicationContext

        val text =
            request.trim()

        if (text.isBlank()) {

            return AtlasToolResult.error(
                id,
                "Memory request is empty.",
                "REQUEST_EMPTY"
            )
        }

        if (
            !AtlasPermissionChecker.isAllowed(
                appContext,
                AtlasPermission.VAULT
            )
        ) {

            return AtlasToolResult.blocked(
                id,
                "Atlas Memory is locked. Open Z Vault before accessing approved memory.",
                "MEMORY_LOCKED",
                "VAULT_ACCESS_REQUIRED"
            )
        }

        val normalized =
            text.lowercase()

        if (
            normalized == "memory status" ||
            normalized == "atlas memory status"
        ) {

            val count =
                AtlasMemoryStore.count(
                    appContext
                )

            return AtlasToolResult.success(
                id,
                "Atlas Memory is online. Approved memory items: $count.",
                "MEMORY_STATUS",
                true
            )
        }

        if (
            normalized == "show memory" ||
            normalized == "view memory" ||
            normalized == "memory list" ||
            normalized == "atlas memory"
        ) {

            val memories =
                AtlasMemoryStore.getMemoryItems(
                    appContext
                )

            if (memories.isEmpty()) {

                return AtlasToolResult.success(
                    id,
                    "Atlas Memory is empty. No approved memory items are stored.",
                    "MEMORY_EMPTY",
                    true
                )
            }

            val safeOutput =
                buildString {

                    appendLine(
                        "ATLAS APPROVED MEMORY"
                    )

                    appendLine(
                        "ITEMS: ${memories.size}"
                    )

                    appendLine()

                    memories.forEachIndexed {
                            index,
                            memory ->

                        appendLine(
                            "${index + 1}. ${memory.role.uppercase()}"
                        )

                        appendLine(
                            memory.content
                        )

                        appendLine()
                    }
                }

            return AtlasToolResult.success(
                id,
                safeOutput.trim(),
                "MEMORY_LIST",
                true
            )
        }

        if (
            normalized == "clear memory" ||
            normalized == "clear atlas memory" ||
            normalized == "delete memory"
        ) {

            val cleared =
                AtlasMemoryStore.clear(
                    appContext
                )

            return if (cleared) {

                AtlasToolResult.success(
                    id,
                    "Atlas Memory cleared. No approved memory items remain.",
                    "MEMORY_CLEARED",
                    true
                )

            } else {

                AtlasToolResult.error(
                    id,
                    "Atlas Memory could not be cleared.",
                    "MEMORY_CLEAR_FAILED"
                )
            }
        }

        if (
            normalized.startsWith("forget ")
        ) {

            val target =
                text
                    .substringAfter(
                        " ",
                        ""
                    )
                    .trim()

            if (target.isBlank()) {

                return AtlasToolResult.error(
                    id,
                    "Specify the memory you want Atlas to forget.",
                    "FORGET_TARGET_EMPTY"
                )
            }

            return forgetMemory(
                appContext,
                target
            )
        }

        if (
            normalized.startsWith(
                "remember "
            )
        ) {

            val content =
                text
                    .substringAfter(
                        " ",
                        ""
                    )
                    .trim()

            return rememberMemory(
                appContext,
                content
            )
        }

        if (
            normalized.startsWith(
                "save this to memory"
            )
        ) {

            val content =
                text
                    .substringAfter(
                        "save this to memory",
                        ""
                    )
                    .trim()

            return rememberMemory(
                appContext,
                content
            )
        }

        if (
            normalized.startsWith(
                "save to memory"
            )
        ) {

            val content =
                text
                    .substringAfter(
                        "save to memory",
                        ""
                    )
                    .trim()

            return rememberMemory(
                appContext,
                content
            )
        }

        return AtlasToolResult.success(
            id,
            "Atlas Memory is available. Use 'remember ...', 'memory status', 'show memory', or 'forget ...'.",
            "MEMORY_READY",
            true
        )
    }

    private fun rememberMemory(
        context: Context,
        content: String
    ): AtlasToolResult {

        if (content.isBlank()) {

            return AtlasToolResult.error(
                id,
                "Nothing was provided to remember.",
                "MEMORY_CONTENT_EMPTY"
            )
        }

        if (
            AzimiAuth.isProtectedCredential(
                content
            )
        ) {

            return AtlasToolResult.blocked(
                id,
                "Protected credential material cannot be stored in Atlas Memory.",
                "MEMORY_BLOCKED",
                "PROTECTED_CREDENTIAL"
            )
        }

        val saved =
            AtlasMemoryStore.remember(
                context,
                "user",
                content
            )

        return if (saved) {

            AtlasToolResult.success(
                id,
                "Approved memory saved securely.",
                "MEMORY_SAVED",
                true
            )

        } else {

            AtlasToolResult.error(
                id,
                "Atlas Memory could not save this item.",
                "MEMORY_SAVE_FAILED"
            )
        }
    }

    private fun forgetMemory(
        context: Context,
        target: String
    ): AtlasToolResult {

        if (
            AzimiAuth.isProtectedCredential(
                target
            )
        ) {

            return AtlasToolResult.blocked(
                id,
                "Protected credential material cannot be searched or stored by Atlas Memory.",
                "MEMORY_BLOCKED",
                "PROTECTED_CREDENTIAL"
            )
        }

        val memories =
            AtlasMemoryStore.getMemoryItems(
                context
            )

        if (memories.isEmpty()) {

            return AtlasToolResult.success(
                id,
                "Atlas Memory is already empty.",
                "MEMORY_EMPTY",
                true
            )
        }

        val remaining =
            memories.filterNot { memory ->

                memory.content.equals(
                    target,
                    ignoreCase = true
                )
            }

        if (
            remaining.size ==
            memories.size
        ) {

            return AtlasToolResult.success(
                id,
                "No exact matching memory item was found.",
                "MEMORY_NOT_FOUND",
                true
            )
        }

        val cleared =
            AtlasMemoryStore.clear(
                context
            )

        if (!cleared) {

            return AtlasToolResult.error(
                id,
                "Atlas Memory could not update the stored memory.",
                "MEMORY_UPDATE_FAILED"
            )
        }

        var restored = true

        remaining.forEach { memory ->

            if (
                !AtlasMemoryStore.remember(
                    context,
                    memory.role,
                    memory.content
                )
            ) {
                restored = false
            }
        }

        return if (restored) {

            AtlasToolResult.success(
                id,
                "The matching approved memory item was forgotten.",
                "MEMORY_FORGOTTEN",
                true
            )

        } else {

            AtlasToolResult.error(
                id,
                "Memory deletion was completed, but rebuilding the remaining memory failed.",
                "MEMORY_REBUILD_FAILED"
            )
        }
    }
}
