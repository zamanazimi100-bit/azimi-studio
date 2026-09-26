package com.azimi.guardian

/**
 * Describes the operational status of an AZIMI component.
 *
 * This is observation data only.
 * It does not authorize or execute actions.
 */
data class AzimiComponentStatus(

    /**
     * Stable component identifier.
     */
    val componentId: String,

    /**
     * Current lifecycle state.
     */
    val state: AzimiComponentState,

    /**
     * Whether the component currently reports
     * a healthy operating condition.
     */
    val healthy: Boolean,

    /**
     * Short diagnostic message.
     */
    val message: String = "NONE"
) {

    /**
     * Returns true when the status contains
     * a usable component identifier.
     */
    fun isValid(): Boolean {
        return componentId
            .trim()
            .isNotEmpty()
    }
}
