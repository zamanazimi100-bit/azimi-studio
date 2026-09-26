package com.azimi.guardian

/**
 * Central construction boundary for AZIMI components.
 *
 * This factory does not own component behavior.
 * It only provides a stable place where component
 * creation can be coordinated without forcing
 * MainActivity or other callers to know implementation
 * details.
 *
 * Components remain independently replaceable.
 */
object AzimiComponentFactory {

    /**
     * Creates an AZIMI component from the supplied builder.
     *
     * The factory does not register the component
     * automatically. Registration remains the responsibility
     * of the component registration architecture.
     */
    fun create(
        builder: () -> AzimiComponent
    ): AzimiComponent? {

        return runCatching {
            builder()
        }.getOrNull()
    }

    /**
     * Creates and validates an AZIMI component.
     *
     * A component must have a non-empty identifier and name
     * before it can cross the factory boundary.
     */
    fun createValidated(
        builder: () -> AzimiComponent
    ): AzimiComponent? {

        return runCatching {

            val component =
                builder()

            val id =
                component.componentId.trim()

            val name =
                component.componentName.trim()

            if (id.isEmpty() || name.isEmpty()) {
                return null
            }

            component

        }.getOrNull()
    }
}
