package com.azimi.guardian

/**
 * Central registry for AZIMI system diagnostic snapshots.
 *
 * The registry retains diagnostic observations only.
 * It does not execute recovery, modify components,
 * grant authority, or perform consequential actions.
 */
object AzimiDiagnosticSnapshotRegistry {

    private const val MAX_SNAPSHOTS = 50

    private val snapshots =
        ArrayDeque<AzimiSystemDiagnosticSnapshot>()

    /**
     * Records a valid system diagnostic snapshot.
     *
     * The registry keeps the most recent snapshots and
     * removes the oldest snapshot when the capacity limit
     * is reached.
     */
    @Synchronized
    fun record(
        snapshot: AzimiSystemDiagnosticSnapshot
    ): Boolean {

        if (!snapshot.isValid()) {
            return false
        }

        if (snapshots.size >= MAX_SNAPSHOTS) {
            snapshots.removeFirst()
        }

        snapshots.addLast(snapshot)

        return true
    }

    /**
     * Returns the most recent system diagnostic snapshot.
     */
    @Synchronized
    fun latest(): AzimiSystemDiagnosticSnapshot? {
        return snapshots.lastOrNull()
    }

    /**
     * Returns all retained snapshots, newest first.
     */
    @Synchronized
    fun all(): List<AzimiSystemDiagnosticSnapshot> {

        return snapshots
            .toList()
            .asReversed()
    }

    /**
     * Returns the number of retained snapshots.
     */
    @Synchronized
    fun size(): Int {
        return snapshots.size
    }

    /**
     * Checks whether at least one diagnostic snapshot
     * has been recorded.
     */
    @Synchronized
    fun hasSnapshot(): Boolean {
        return snapshots.isNotEmpty()
    }

    /**
     * Removes all retained diagnostic snapshots.
     *
     * This affects only in-memory diagnostic history.
     * It does not modify any component or system state.
     */
    @Synchronized
    fun clear() {
        snapshots.clear()
    }
}
