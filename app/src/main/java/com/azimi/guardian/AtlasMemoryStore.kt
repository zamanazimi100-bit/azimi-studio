package com.azimi.guardian

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AtlasMemoryStore {

    private const val STORAGE_KEY = "atlas_approved_memory_v1"

    private const val MAX_MEMORY_ITEMS = 100
    private const val MAX_CONTENT_LENGTH = 4000

    data class MemoryItem(
        val role: String,
        val content: String
    )

    fun remember(
        context: Context,
        role: String,
        content: String
    ): Boolean {

        val safeRole = role.trim().lowercase()
        val safeContent = content.trim()

        if (safeContent.isBlank()) {
            return false
        }

        if (safeContent.length > MAX_CONTENT_LENGTH) {
            return false
        }

        if (safeRole !in setOf("user", "assistant", "system")) {
            return false
        }

        if (AzimiAuth.isProtectedCredential(safeContent)) {
            return false
        }

        val existing =
            read(context).toMutableList()

        existing.add(
            MemoryItem(
                role = safeRole,
                content = safeContent
            )
        )

        val limited =
            existing.takeLast(MAX_MEMORY_ITEMS)

        return save(
            context,
            limited
        )
    }

    fun getMemory(
        context: Context
    ): List<AzimiAiClient.ChatMessage> {

        return read(context)
            .mapNotNull { item ->

                if (
                    AzimiAuth.isProtectedCredential(
                        item.content
                    )
                ) {
                    null
                } else {
                    AzimiAiClient.ChatMessage(
                        role = item.role,
                        content = item.content
                    )
                }
            }
    }

    fun getMemoryItems(
        context: Context
    ): List<MemoryItem> {

        return read(context)
    }

    fun clear(
        context: Context
    ): Boolean {

        return runCatching {

            VaultCrypto.delete(
                context,
                STORAGE_KEY
            )

            true

        }.getOrDefault(false)
    }

    fun count(
        context: Context
    ): Int {

        return read(context).size
    }

    private fun read(
        context: Context
    ): List<MemoryItem> {

        val encrypted =
            runCatching {

                VaultCrypto.get(
                    context,
                    STORAGE_KEY
                )

            }.getOrNull()

        if (encrypted.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {

            val array =
                JSONArray(encrypted)

            val result =
                mutableListOf<MemoryItem>()

            for (index in 0 until array.length()) {

                val item =
                    array.optJSONObject(index)
                        ?: continue

                val role =
                    item
                        .optString("role")
                        .trim()
                        .lowercase()

                val content =
                    item
                        .optString("content")
                        .trim()

                if (
                    role !in setOf(
                        "user",
                        "assistant",
                        "system"
                    )
                ) {
                    continue
                }

                if (content.isBlank()) {
                    continue
                }

                if (
                    content.length >
                    MAX_CONTENT_LENGTH
                ) {
                    continue
                }

                if (
                    AzimiAuth.isProtectedCredential(
                        content
                    )
                ) {
                    continue
                }

                result.add(
                    MemoryItem(
                        role = role,
                        content = content
                    )
                )
            }

            result.takeLast(
                MAX_MEMORY_ITEMS
            )

        }.getOrDefault(
            emptyList()
        )
    }

    private fun save(
        context: Context,
        memories: List<MemoryItem>
    ): Boolean {

        return runCatching {

            val array =
                JSONArray()

            memories
                .takeLast(MAX_MEMORY_ITEMS)
                .forEach { memory ->

                    if (
                        memory.content.isBlank() ||
                        memory.content.length >
                        MAX_CONTENT_LENGTH ||
                        memory.role !in setOf(
                            "user",
                            "assistant",
                            "system"
                        ) ||
                        AzimiAuth.isProtectedCredential(
                            memory.content
                        )
                    ) {
                        return@forEach
                    }

                    val item =
                        JSONObject()
                            .put(
                                "role",
                                memory.role
                            )
                            .put(
                                "content",
                                memory.content
                            )

                    array.put(item)
                }

            VaultCrypto.put(
                context,
                STORAGE_KEY,
                array.toString()
            )

        }.getOrDefault(false)
    }
}
