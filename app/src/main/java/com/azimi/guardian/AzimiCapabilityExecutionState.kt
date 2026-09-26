package com.azimi.guardian

/**
 * Explicit lifecycle states for AZIMI capability execution.
 *
 * Execution state is intentionally separate from authorization.
 *
 * Authorization determines whether execution may proceed.
 * Execution state describes what is happening to the execution
 * request itself.
 *
 * This enum does not execute, authorize, authenticate, or
 * modify any capability.
 */
enum class AzimiCapabilityExecutionState {

    /**
     * The execution request has been created but has not
     * yet been authorized for execution.
     */
    READY,

    /**
     * Authorization has been confirmed and the execution
     * boundary may proceed.
     */
    AUTHORIZED,

    /**
     * The capability is currently being executed.
     */
    EXECUTING,

    /**
     * The capability execution completed successfully.
     */
    COMPLETED,

    /**
     * Execution was denied because the required
     * authorization conditions were not satisfied.
     */
    DENIED,

    /**
     * Execution was attempted but failed.
     */
    FAILED,

    /**
     * The capability cannot currently be executed.
     */
    UNAVAILABLE,

    /**
     * Execution was intentionally cancelled before completion.
     */
    CANCELLED
}
