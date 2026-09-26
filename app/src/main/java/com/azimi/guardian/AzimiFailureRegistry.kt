package com.azimi.guardian

/**
 * Central registry for AZIMI failure evidence.
 *
 * The registry stores diagnostic events only.
 * It does not execute recovery, modify components,
 * grant authority, or perform consequential actions.
 */
object AzimiFailureRegistry {

    private const val MAX_EVENTS = 100

    private val events =
        ArrayDeque<AzimiFailureEvent>()

    /**
     * Records a valid failure event.
     *
     * The registry keeps the most recent events and
     * automatically removes the oldest entries when
     * the maximum capacity is reached.
     */
    @Synchronized
    fun record(
        event: AzimiFailureEvent
    ): Boolean {

        if (!event.isValid()) {
            return false
        }

        if (events.size >= MAX_EVENTS) {
            events.removeFirst()
        }

        events.addLast(event)

        return true
    }

    /**
     * Returns the most recent failure event.
     */
    @Synchronized
    fun latest(): AzimiFailureEvent? {
        return events.lastOrNull()
    }

    /**
     * Returns a snapshot of all currently retained
     * failure events, newest first.
     */
    @Synchronized
    fun all(): List<AzimiFailureEvent> {
        return events
            .toList()
            .asReversed()
    }

    /**
     * Returns failure events belonging to one component.
     */
    @Synchronized
    fun forComponent(
        componentId: String
    ): List<AzimiFailureEvent> {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return emptyList()
        }

        return events
            .filter {
                it.componentId == id
            }
            .asReversed()
    }

    /**
     * Returns failure events associated with a
     * particular checkpoint.
     */
    @Synchronized
    fun forCheckpoint(
        checkpoint: String
    ): List<AzimiFailureEvent> {

        val value =
            checkpoint.trim()

        if (value.isEmpty()) {
            return emptyList()
        }

        return events
            .filter {
                it.checkpoint == value
            }
            .asReversed()
    }

    /**
     * Returns the number of retained failure events.
     */
    @Synchronized
    fun size(): Int {
        return events.size
    }

    /**
     * Removes all retained diagnostic events.
     *
     * This changes only in-memory diagnostic history.
     * It does not modify the affected components.
     */
    @Synchronized
    fun clear() {
        events.clear()
    }
}
