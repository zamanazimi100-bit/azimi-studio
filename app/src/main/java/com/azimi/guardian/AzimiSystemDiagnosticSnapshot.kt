package com.azimi.guardian

/**
 * Read-only diagnostic snapshot for the AZIMI system.
 *
 * This snapshot provides a system-level view of registered
 * components without taking control of those components.
 *
 * It contains observation data only.
 * It does not authorize, start, stop, recover, modify,
 * or execute consequential actions.
 */
data class AzimiSystemDiagnosticSnapshot(

    /**
     * Time at which this snapshot was created.
     *
     * Stored as a string so the foundation does not depend
     * on a particular time library.
     */
    val time: String,

    /**
     * Number of registered AZIMI components.
     */
    val componentCount: Int,

    /**
     * Number of registered runtime boundaries.
     */
    val runtimeCount: Int,

    /**
     * Number of registered recovery states.
     */
    val recoveryCount: Int,

    /**
     * Number of registered checkpoints.
     */
    val checkpointCount: Int,

    /**
     * Number of registered fault-isolation boundaries.
     */
    val isolationCount: Int,

    /**
     * Number of retained failure events.
     */
    val failureCount: Int,

    /**
     * Number of components currently reporting healthy.
     */
    val healthyComponentCount: Int,

    /**
     * Number of components currently reporting unhealthy.
     */
    val unhealthyComponentCount: Int,

    /**
     * Diagnostic snapshots for registered components.
     */
    val components:
        List<AzimiComponentDiagnosticSnapshot>,

    /**
     * Short system-level diagnostic message.
     *
     * Secrets and protected credentials must never
     * be placed in this field.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the minimum information required
     * for a useful system snapshot.
     */
    fun isValid(): Boolean {

        return time
            .trim()
            .isNotEmpty() &&
            componentCount >= 0 &&
            runtimeCount >= 0 &&
            recoveryCount >= 0 &&
            checkpointCount >= 0 &&
            isolationCount >= 0 &&
            failureCount >= 0 &&
            healthyComponentCount >= 0 &&
            unhealthyComponentCount >= 0
    }
}

/**
 * Creates system-level diagnostic snapshots from the
 * existing AZIMI foundation registries.
 *
 * This object observes registry state only.
 * It does not execute component operations.
 */
object AzimiSystemDiagnosticSnapshotFactory {

    /**
     * Creates a read-only snapshot of the current
     * AZIMI foundation state.
     */
    @Synchronized
    fun create(
        time: String
    ): AzimiSystemDiagnosticSnapshot {

        val safeTime =
            time.trim()

        val componentSnapshots =
            AzimiComponentDiagnosticSnapshotFactory
                .createAll()

        val healthyCount =
            componentSnapshots.count {
                it.health.healthy
            }

        val unhealthyCount =
            componentSnapshots.count {
                !it.health.healthy
            }

        val message =
            if (safeTime.isEmpty()) {
                "INVALID_SNAPSHOT_TIME"
            } else {
                "SYSTEM_DIAGNOSTIC_READY"
            }

        return AzimiSystemDiagnosticSnapshot(
            time =
                if (safeTime.isEmpty()) {
                    "UNKNOWN"
                } else {
                    safeTime
                },
            componentCount =
                AzimiComponentRegistry.size(),
            runtimeCount =
                AzimiComponentRuntimeRegistry.size(),
            recoveryCount =
                AzimiRecoveryRegistry.size(),
            checkpointCount =
                AzimiCheckpointRegistry.size(),
            isolationCount =
                AzimiFaultIsolationRegistry.size(),
            failureCount =
                AzimiFailureRegistry.size(),
            healthyComponentCount =
                healthyCount,
            unhealthyComponentCount =
                unhealthyCount,
            components =
                componentSnapshots,
            message = message
        )
    }
}
