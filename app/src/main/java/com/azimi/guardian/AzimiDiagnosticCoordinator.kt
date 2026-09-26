package com.azimi.guardian

/**
 * Central coordinator for AZIMI diagnostic snapshots.
 *
 * The coordinator connects snapshot creation with the
 * snapshot registry while keeping both responsibilities
 * separate.
 *
 * It observes system state and records diagnostic evidence.
 * It does not start, stop, recover, authorize, or modify
 * components.
 */
object AzimiDiagnosticCoordinator {

    /**
     * Creates and records a system diagnostic snapshot.
     *
     * Returns the snapshot even when registry recording
     * fails, so diagnostic information is not silently lost
     * from the caller.
     */
    @Synchronized
    fun capture(
        time: String
    ): AzimiSystemDiagnosticSnapshot {

        val snapshot =
            AzimiSystemDiagnosticSnapshotFactory.create(
                time
            )

        AzimiDiagnosticSnapshotRegistry.record(
            snapshot
        )

        return snapshot
    }

    /**
     * Returns the most recently recorded snapshot.
     *
     * Observation only.
     */
    @Synchronized
    fun latest():
        AzimiSystemDiagnosticSnapshot? {

        return AzimiDiagnosticSnapshotRegistry.latest()
    }

    /**
     * Returns all retained snapshots, newest first.
     *
     * Observation only.
     */
    @Synchronized
    fun history():
        List<AzimiSystemDiagnosticSnapshot> {

        return AzimiDiagnosticSnapshotRegistry.all()
    }

    /**
     * Returns the number of retained snapshots.
     */
    @Synchronized
    fun snapshotCount(): Int {

        return AzimiDiagnosticSnapshotRegistry.size()
    }

    /**
     * Checks whether diagnostic history exists.
     */
    @Synchronized
    fun hasHistory(): Boolean {

        return AzimiDiagnosticSnapshotRegistry
            .hasSnapshot()
    }

    /**
     * Clears diagnostic snapshot history.
     *
     * This affects only retained diagnostic evidence.
     * It does not modify component or system state.
     */
    @Synchronized
    fun clearHistory() {

        AzimiDiagnosticSnapshotRegistry.clear()
    }
}
