package com.azimi.guardian

/**
 * Standard diagnostic record for an AZIMI component failure.
 *
 * This contract describes evidence about a failure.
 * It does not perform recovery or execute consequential actions.
 */
data class AzimiFailureEvent(

    /**
     * Unique identifier for this failure event.
     */
    val failureId: String,

    /**
     * Time at which the failure was recorded.
     *
     * Stored as an ISO-8601-compatible string so the
     * contract does not depend on a particular time library.
     */
    val time: String,

    /**
     * Component associated with the failure.
     */
    val componentId: String,

    /**
     * File where the failure was detected.
     */
    val file: String = "UNKNOWN",

    /**
     * Function where the failure was detected.
     */
    val function: String = "UNKNOWN",

    /**
     * Operational stage where the failure occurred.
     */
    val stage: String = "UNKNOWN",

    /**
     * Source line when known.
     */
    val line: Int? = null,

    /**
     * Error type/class when known.
     */
    val errorType: String = "UNKNOWN",

    /**
     * Safe diagnostic error message.
     *
     * Secrets and protected credentials must never
     * be placed in this field.
     */
    val message: String = "UNKNOWN",

    /**
     * Dependency associated with the failure, when known.
     */
    val dependency: String = "NONE",

    /**
     * Simplified call path when known.
     */
    val callPath: String = "UNKNOWN",

    /**
     * Last verified AZIMI checkpoint.
     */
    val checkpoint: String = "UNKNOWN",

    /**
     * State of the affected component.
     */
    val componentState: AzimiComponentState =
        AzimiComponentState.UNAVAILABLE,

    /**
     * Recovery state associated with the failure.
     */
    val recoveryState: String = "UNKNOWN",

    /**
     * Build associated with the event.
     */
    val build: String = "UNKNOWN"
) {

    /**
     * Validates the minimum identity required for
     * a useful failure record.
     */
    fun isValid(): Boolean {

        return failureId
            .trim()
            .isNotEmpty() &&
            time
                .trim()
                .isNotEmpty() &&
            componentId
                .trim()
                .isNotEmpty()
    }
}
