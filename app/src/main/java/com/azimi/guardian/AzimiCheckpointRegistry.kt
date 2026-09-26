package com.azimi.guardian

/**
 * Describes a verified AZIMI checkpoint.
 *
 * A checkpoint represents a known state that can be
 * referenced by diagnostics and future recovery systems.
 *
 * This contract does not perform rollback, restoration,
 * or any other consequential action.
 */
data class AzimiCheckpoint(

    /**
     * Stable identifier for this checkpoint.
     */
    val checkpointId: String,

    /**
     * Component associated with the checkpoint.
     */
    val componentId: String,

    /**
     * Human-readable checkpoint name.
     */
    val name: String = "UNKNOWN",

    /**
     * Time at which the checkpoint was recorded.
     */
    val time: String,

    /**
     * Build associated with the checkpoint.
     */
    val build: String = "UNKNOWN",

    /**
     * Whether the checkpoint has been verified.
     */
    val verified: Boolean,

    /**
     * Optional diagnostic description.
     *
     * Secrets and protected credentials must never
     * be placed in this field.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the minimum identity required
     * for a usable checkpoint.
     */
    fun isValid(): Boolean {

        return checkpointId
            .trim()
            .isNotEmpty() &&
            componentId
                .trim()
                .isNotEmpty() &&
            time
                .trim()
                .isNotEmpty()
    }
}

/**
 * Central registry for AZIMI verified checkpoints.
 *
 * The registry stores checkpoint evidence only.
 * It does not restore, roll back, modify, or execute
 * any component.
 */
object AzimiCheckpointRegistry {

    private val checkpoints =
        LinkedHashMap<String, AzimiCheckpoint>()

    /**
     * Registers or replaces a checkpoint using
     * its stable checkpoint ID.
     */
    @Synchronized
    fun register(
        checkpoint: AzimiCheckpoint
    ): Boolean {

        if (!checkpoint.isValid()) {
            return false
        }

        val checkpointId =
            checkpoint.checkpointId.trim()

        val componentId =
            checkpoint.componentId.trim()

        val name =
            checkpoint.name.trim()

        val build =
            checkpoint.build.trim()

        val message =
            checkpoint.message.trim()

        checkpoints[checkpointId] =
            checkpoint.copy(
                checkpointId = checkpointId,
                componentId = componentId,
                name =
                    if (name.isEmpty()) {
                        "UNKNOWN"
                    } else {
                        name
                    },
                build =
                    if (build.isEmpty()) {
                        "UNKNOWN"
                    } else {
                        build
                    },
                message =
                    if (message.isEmpty()) {
                        "NONE"
                    } else {
                        message
                    }
            )

        return true
    }

    /**
     * Removes a checkpoint from the registry.
     *
     * This does not delete or modify any actual
     * component state.
     */
    @Synchronized
    fun unregister(
        checkpointId: String
    ): Boolean {

        val id =
            checkpointId.trim()

        if (id.isEmpty()) {
            return false
        }

        return checkpoints.remove(id) != null
    }

    /**
     * Returns one checkpoint by ID.
     */
    @Synchronized
    fun get(
        checkpointId: String
    ): AzimiCheckpoint? {

        val id =
            checkpointId.trim()

        if (id.isEmpty()) {
            return null
        }

        return checkpoints[id]
    }

    /**
     * Returns all registered checkpoints
     * as a stable snapshot.
     */
    @Synchronized
    fun all(): List<AzimiCheckpoint> {
        return checkpoints.values.toList()
    }

    /**
     * Returns all checkpoints belonging
     * to one component.
     */
    @Synchronized
    fun forComponent(
        componentId: String
    ): List<AzimiCheckpoint> {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return emptyList()
        }

        return checkpoints.values
            .filter {
                it.componentId == id
            }
    }

    /**
     * Returns all verified checkpoints.
     */
    @Synchronized
    fun verified(): List<AzimiCheckpoint> {

        return checkpoints.values
            .filter {
                it.verified
            }
    }

    /**
     * Checks whether a checkpoint exists.
     */
    @Synchronized
    fun contains(
        checkpointId: String
    ): Boolean {

        val id =
            checkpointId.trim()

        if (id.isEmpty()) {
            return false
        }

        return checkpoints.containsKey(id)
    }

    /**
     * Returns the number of registered checkpoints.
     */
    @Synchronized
    fun size(): Int {
        return checkpoints.size
    }

    /**
     * Clears only the in-memory checkpoint registry.
     *
     * No component state is modified.
     * No recovery or rollback is performed.
     */
    @Synchronized
    fun clear() {
        checkpoints.clear()
    }
}
