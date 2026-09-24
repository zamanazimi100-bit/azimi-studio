package com.azimi.guardian

import android.content.Context

/**
 * AZIMI Workspace Integrity
 *
 * Read-only structural health checker for the AZIMI-owned workspace.
 *
 * Purpose:
 *
 *     AZIMI Workspace
 *            │
 *            ▼
 *     Integrity Checker
 *            │
 *            ├── Workspace structure
 *            ├── Workspace manifest
 *            ├── Workspace identity
 *            └── Required directories
 *
 * Design rules:
 *
 * - Read-only.
 * - Never deletes data.
 * - Never migrates data.
 * - Never modifies the manifest.
 * - Never modifies Z Continuity.
 * - Never requires an external provider.
 * - Never stores secrets.
 * - A diagnostic failure must never crash Guardian.
 */
object AZIMIWorkspaceIntegrity {

    /**
     * Overall integrity states.
     */
    const val HEALTHY =
        "HEALTHY"

    const val WORKSPACE_MISSING =
        "WORKSPACE_MISSING"

    const val WORKSPACE_PARTIAL =
        "WORKSPACE_PARTIAL"

    const val MANIFEST_MISSING =
        "MANIFEST_MISSING"

    const val MANIFEST_INVALID =
        "MANIFEST_INVALID"

    const val IDENTITY_INVALID =
        "IDENTITY_INVALID"

    const val CHECK_FAILED =
        "CHECK_FAILED"

    /**
     * Immutable integrity result.
     *
     * No sensitive file contents are included.
     */
    data class Result(
        val status: String,
        val workspaceStatus: String,
        val manifestStatus: String,
        val workspaceIdPresent: Boolean,
        val requiredDirectoryCount: Int,
        val existingDirectoryCount: Int,
        val missingDirectoryCount: Int,
        val healthy: Boolean,
        val message: String
    )

    /**
     * Performs a complete read-only workspace integrity check.
     *
     * Nothing is created or modified.
     */
    fun check(
        context: Context
    ): Result {

        return runCatching {

            val workspaceStatus =
                AZIMIWorkspace.status(context)

            val directories =
                AZIMIWorkspace.directories(context)

            val requiredDirectoryCount =
                directories.size

            val existingDirectoryCount =
                directories.count {
                    it.isDirectory
                }

            val missingDirectoryCount =
                requiredDirectoryCount -
                    existingDirectoryCount

            when {

                workspaceStatus ==
                    "NOT_INITIALIZED" -> {

                    Result(
                        status =
                            WORKSPACE_MISSING,
                        workspaceStatus =
                            workspaceStatus,
                        manifestStatus =
                            "NOT_CHECKED",
                        workspaceIdPresent =
                            false,
                        requiredDirectoryCount =
                            requiredDirectoryCount,
                        existingDirectoryCount =
                            existingDirectoryCount,
                        missingDirectoryCount =
                            missingDirectoryCount,
                        healthy =
                            false,
                        message =
                            "AZIMI Workspace has not been initialized."
                    )
                }

                missingDirectoryCount > 0 -> {

                    Result(
                        status =
                            WORKSPACE_PARTIAL,
                        workspaceStatus =
                            workspaceStatus,
                        manifestStatus =
                            "NOT_CHECKED",
                        workspaceIdPresent =
                            false,
                        requiredDirectoryCount =
                            requiredDirectoryCount,
                        existingDirectoryCount =
                            existingDirectoryCount,
                        missingDirectoryCount =
                            missingDirectoryCount,
                        healthy =
                            false,
                        message =
                            "AZIMI Workspace is missing required directories."
                    )
                }

                !AZIMIWorkspaceManifest.exists(
                    context
                ) -> {

                    Result(
                        status =
                            MANIFEST_MISSING,
                        workspaceStatus =
                            workspaceStatus,
                        manifestStatus =
                            "MISSING",
                        workspaceIdPresent =
                            false,
                        requiredDirectoryCount =
                            requiredDirectoryCount,
                        existingDirectoryCount =
                            existingDirectoryCount,
                        missingDirectoryCount =
                            missingDirectoryCount,
                        healthy =
                            false,
                        message =
                            "AZIMI Workspace Manifest is missing."
                    )
                }

                !AZIMIWorkspaceManifest.validate(
                    context
                ) -> {

                    Result(
                        status =
                            MANIFEST_INVALID,
                        workspaceStatus =
                            workspaceStatus,
                        manifestStatus =
                            "INVALID",
                        workspaceIdPresent =
                            false,
                        requiredDirectoryCount =
                            requiredDirectoryCount,
                        existingDirectoryCount =
                            existingDirectoryCount,
                        missingDirectoryCount =
                            missingDirectoryCount,
                        healthy =
                            false,
                        message =
                            "AZIMI Workspace Manifest failed validation."
                    )
                }

                AZIMIWorkspaceManifest.workspaceId(
                    context
                ).isNullOrBlank() -> {

                    Result(
                        status =
                            IDENTITY_INVALID,
                        workspaceStatus =
                            workspaceStatus,
                        manifestStatus =
                            "READY",
                        workspaceIdPresent =
                            false,
                        requiredDirectoryCount =
                            requiredDirectoryCount,
                        existingDirectoryCount =
                            existingDirectoryCount,
                        missingDirectoryCount =
                            missingDirectoryCount,
                        healthy =
                            false,
                        message =
                            "AZIMI Workspace identity is unavailable."
                    )
                }

                else -> {

                    Result(
                        status =
                            HEALTHY,
                        workspaceStatus =
                            workspaceStatus,
                        manifestStatus =
                            "READY",
                        workspaceIdPresent =
                            true,
                        requiredDirectoryCount =
                            requiredDirectoryCount,
                        existingDirectoryCount =
                            existingDirectoryCount,
                        missingDirectoryCount =
                            missingDirectoryCount,
                        healthy =
                            true,
                        message =
                            "AZIMI Workspace integrity check passed."
                    )
                }
            }

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace integrity check failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            Result(
                status =
                    CHECK_FAILED,
                workspaceStatus =
                    "UNKNOWN",
                manifestStatus =
                    "UNKNOWN",
                workspaceIdPresent =
                    false,
                requiredDirectoryCount =
                    0,
                existingDirectoryCount =
                    0,
                missingDirectoryCount =
                    0,
                healthy =
                    false,
                message =
                    "AZIMI Workspace integrity check could not be completed."
            )
        }
    }

    /**
     * Simple health check.
     *
     * Returns true only when the complete workspace is healthy.
     */
    fun isHealthy(
        context: Context
    ): Boolean {

        return runCatching {

            check(context).healthy

        }.getOrDefault(false)
    }

    /**
     * Returns only the current integrity status.
     */
    fun status(
        context: Context
    ): String {

        return runCatching {

            check(context).status

        }.getOrDefault(
            CHECK_FAILED
        )
    }

    /**
     * Returns a safe human-readable diagnostic summary.
     *
     * No manifest contents or sensitive values are exposed.
     */
    fun summary(
        context: Context
    ): String {

        return runCatching {

            val result =
                check(context)

            buildString {

                append(
                    "AZIMI Workspace Integrity"
                )

                append("\nStatus: ")
                append(result.status)

                append("\nWorkspace: ")
                append(result.workspaceStatus)

                append("\nManifest: ")
                append(result.manifestStatus)

                append("\nWorkspace ID: ")
                append(
                    if (result.workspaceIdPresent) {
                        "AVAILABLE"
                    } else {
                        "UNAVAILABLE"
                    }
                )

                append("\nDirectories: ")
                append(
                    result.existingDirectoryCount
                )

                append("/")
                append(
                    result.requiredDirectoryCount
                )

                append("\nMissing: ")
                append(
                    result.missingDirectoryCount
                )

                append("\nHealthy: ")
                append(result.healthy)

                append("\nMessage: ")
                append(result.message)
            }

        }.getOrElse {

            "AZIMI Workspace Integrity: CHECK_FAILED"
        }
    }

    /**
     * Failure handling must never become a new failure.
     */
    private fun safeRecordError(
        context: Context,
        message: String
    ) {

        runCatching {

            GuardianStorage.recordError(
                context,
                message
            )

        }
    }
}
