package com.azimi.guardian

/**
 * Describes the recovery condition of an AZIMI component.
 *
 * This contract describes recovery state only.
 * It does not execute recovery operations.
 */
enum class AzimiRecoveryState {

    /**
     * No recovery is currently required.
     */
    STABLE,

    /**
     * A recovery condition has been detected.
     */
    RECOVERY_REQUIRED,

    /**
     * Recovery has started.
     */
    RECOVERING,

    /**
     * Recovery completed successfully.
     */
    RECOVERED,

    /**
     * Recovery completed, but the component
     * is operating with reduced capability.
     */
    DEGRADED,

    /**
     * Recovery could not restore the component.
     */
    FAILED,

    /**
     * Recovery cannot currently proceed.
     */
    UNAVAILABLE
}

/**
 * Describes recovery information for one AZIMI component.
 */
data class AzimiRecoveryStatus(

    /**
     * Stable component identifier.
     */
    val componentId: String,

    /**
     * Current recovery state.
     */
    val state: AzimiRecoveryState,

    /**
     * Last known-good checkpoint.
     */
    val checkpoint: String = "UNKNOWN",

    /**
     * Short diagnostic message.
     *
     * Secrets and protected credentials must never
     * be placed in this field.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the minimum information required
     * for a useful recovery status.
     */
    fun isValid(): Boolean {

        return componentId
            .trim()
            .isNotEmpty()
    }
}
