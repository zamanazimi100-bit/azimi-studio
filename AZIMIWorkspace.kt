package com.azimi.guardian

import android.content.Context
import java.io.File

/**
 * AZIMI Workspace
 *
 * Local, app-private workspace foundation for AZIMI.
 *
 * This is the first layer of the AZIMI-owned architecture.
 *
 * Current location:
 *
 *     Android app private storage
 *             │
 *             ▼
 *     /filesDir/azimi_workspace/
 *
 * Architecture:
 *
 *                    AZIMI WORKSPACE
 *                           │
 *       ┌───────────┬───────┼────────┬───────────┐
 *       ▼           ▼       ▼        ▼           ▼
 *     CORE       GUARDIAN  ATLAS   STORAGE    HISTORY
 *       │
 *       ├── DIAGNOSTICS
 *       ├── BUILDS
 *       ├── BACKUPS
 *       ├── RECOVERY
 *       ├── PROJECTS
 *       ├── CONFIG
 *       └── PROVIDERS
 *
 * Design rules:
 *
 * - AZIMI owns the logical workspace structure.
 * - External providers are not required for this layer.
 * - Directory creation must never crash Guardian.
 * - Initialization is idempotent.
 * - Existing data is never deleted automatically.
 * - Existing Z Continuity Storage remains untouched.
 * - Migration into this workspace happens only in a later step.
 * - Secrets are not stored by this class.
 * - This class does not encrypt data itself.
 * - VaultCrypto remains responsible for encrypted values.
 * - ZContinuityStorage remains the source of truth for continuity
 *   records until a deliberate migration is designed.
 */
object AZIMIWorkspace {

    private const val ROOT_FOLDER =
        "azimi_workspace"

    private const val CORE_FOLDER =
        "core"

    private const val GUARDIAN_FOLDER =
        "guardian"

    private const val ATLAS_FOLDER =
        "atlas"

    private const val STORAGE_FOLDER =
        "storage"

    private const val HISTORY_FOLDER =
        "history"

    private const val DIAGNOSTICS_FOLDER =
        "diagnostics"

    private const val BUILDS_FOLDER =
        "builds"

    private const val BACKUPS_FOLDER =
        "backups"

    private const val RECOVERY_FOLDER =
        "recovery"

    private const val PROJECTS_FOLDER =
        "projects"

    private const val CONFIG_FOLDER =
        "config"

    private const val PROVIDERS_FOLDER =
        "providers"

    /**
     * Returns the AZIMI workspace root.
     *
     * The returned location is inside Android's private
     * application storage.
     */
    fun root(
        context: Context
    ): File {

        return File(
            context.applicationContext.filesDir,
            ROOT_FOLDER
        )
    }

    fun core(
        context: Context
    ): File {

        return File(
            root(context),
            CORE_FOLDER
        )
    }

    fun guardian(
        context: Context
    ): File {

        return File(
            root(context),
            GUARDIAN_FOLDER
        )
    }

    fun atlas(
        context: Context
    ): File {

        return File(
            root(context),
            ATLAS_FOLDER
        )
    }

    fun storage(
        context: Context
    ): File {

        return File(
            root(context),
            STORAGE_FOLDER
        )
    }

    fun history(
        context: Context
    ): File {

        return File(
            root(context),
            HISTORY_FOLDER
        )
    }

    fun diagnostics(
        context: Context
    ): File {

        return File(
            root(context),
            DIAGNOSTICS_FOLDER
        )
    }

    fun builds(
        context: Context
    ): File {

        return File(
            root(context),
            BUILDS_FOLDER
        )
    }

    fun backups(
        context: Context
    ): File {

        return File(
            root(context),
            BACKUPS_FOLDER
        )
    }

    fun recovery(
        context: Context
    ): File {

        return File(
            root(context),
            RECOVERY_FOLDER
        )
    }

    fun projects(
        context: Context
    ): File {

        return File(
            root(context),
            PROJECTS_FOLDER
        )
    }

    fun config(
        context: Context
    ): File {

        return File(
            root(context),
            CONFIG_FOLDER
        )
    }

    fun providers(
        context: Context
    ): File {

        return File(
            root(context),
            PROVIDERS_FOLDER
        )
    }

    /**
     * Initializes the complete AZIMI workspace structure.
     *
     * Safe to call repeatedly.
     *
     * Existing directories and files are preserved.
     */
    fun initialize(
        context: Context
    ): Boolean {

        return runCatching {

            val directories =
                listOf(
                    root(context),
                    core(context),
                    guardian(context),
                    atlas(context),
                    storage(context),
                    history(context),
                    diagnostics(context),
                    builds(context),
                    backups(context),
                    recovery(context),
                    projects(context),
                    config(context),
                    providers(context)
                )

            directories.all {
                ensureDirectory(it)
            }

        }.getOrElse { throwable ->

            safeRecordError(
                context = context,
                message =
                    (
                        "AZIMI Workspace initialization failed: " +
                            (
                                throwable.message
                                    ?: throwable.javaClass.simpleName
                            )
                        ).take(500)
            )

            false
        }
    }

    /**
     * Checks whether the workspace root exists.
     *
     * This does not create anything.
     */
    fun isInitialized(
        context: Context
    ): Boolean {

        return runCatching {

            root(context).isDirectory

        }.getOrDefault(false)
    }

    /**
     * Checks whether all required workspace directories exist.
     *
     * This does not create or modify anything.
     */
    fun isComplete(
        context: Context
    ): Boolean {

        return runCatching {

            val directories =
                listOf(
                    root(context),
                    core(context),
                    guardian(context),
                    atlas(context),
                    storage(context),
                    history(context),
                    diagnostics(context),
                    builds(context),
                    backups(context),
                    recovery(context),
                    projects(context),
                    config(context),
                    providers(context)
                )

            directories.all {
                it.isDirectory
            }

        }.getOrDefault(false)
    }

    /**
     * Returns the current workspace directory list.
     *
     * This is useful for diagnostics and future backup systems.
     */
    fun directories(
        context: Context
    ): List<File> {

        return runCatching {

            listOf(
                root(context),
                core(context),
                guardian(context),
                atlas(context),
                storage(context),
                history(context),
                diagnostics(context),
                builds(context),
                backups(context),
                recovery(context),
                projects(context),
                config(context),
                providers(context)
            )

        }.getOrDefault(
            emptyList()
        )
    }

    /**
     * Returns a compact workspace status.
     *
     * No file contents are exposed here.
     */
    fun status(
        context: Context
    ): String {

        return runCatching {

            when {
                isComplete(context) ->
                    "READY"

                isInitialized(context) ->
                    "PARTIAL"

                else ->
                    "NOT_INITIALIZED"
            }

        }.getOrDefault(
            "UNKNOWN"
        )
    }

    /**
     * Ensures one directory exists.
     *
     * Existing directories are preserved.
     */
    private fun ensureDirectory(
        directory: File
    ): Boolean {

        return if (directory.isDirectory) {
            true
        } else {
            directory.mkdirs() ||
                directory.isDirectory
        }
    }

    /**
     * Failure handling must never become a new failure.
     *
     * We intentionally use the existing lightweight GuardianStorage
     * error channel here instead of ZFailureLocator.
     *
     * Reason:
     *
     *     AZIMIWorkspace
     *          ↓
     *     ZFailureLocator
     *          ↓
     *     ZContinuityEvents
     *          ↓
     *     ZContinuityStorage
     *
     * would create an unnecessary dependency chain during the
     * workspace's own initialization.
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
