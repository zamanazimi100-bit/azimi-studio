package com.azimi.guardian

/**
 * Coordinates registration of an AZIMI component and its
 * optional architectural boundaries.
 *
 * Registration is configuration/observation infrastructure.
 * It does not start components, grant authority, bypass
 * security, perform recovery, or execute consequential actions.
 */
object AzimiComponentRegistrationCoordinator {

    /**
     * Registers a component in the central component registry.
     *
     * Returns false when the component cannot be registered.
     */
    @Synchronized
    fun registerComponent(
        component: AzimiComponent
    ): Boolean {

        return runCatching {
            AzimiComponentRegistry.register(
                component
            )
        }.getOrDefault(false)
    }

    /**
     * Registers a component and its runtime boundary.
     *
     * The component and runtime are registered separately.
     * Both must use the same stable component ID.
     */
    @Synchronized
    fun registerComponentWithRuntime(
        component: AzimiComponent,
        runtime: AzimiComponentRuntime
    ): Boolean {

        val componentId =
            component.componentId.trim()

        val runtimeId =
            runtime.componentId.trim()

        if (
            componentId.isEmpty() ||
            runtimeId.isEmpty() ||
            componentId != runtimeId
        ) {
            return false
        }

        val componentRegistered =
            runCatching {
                AzimiComponentRegistry.register(
                    component
                )
            }.getOrDefault(false)

        if (!componentRegistered) {
            return false
        }

        val runtimeRegistered =
            runCatching {
                AzimiComponentRuntimeRegistry.register(
                    runtime
                )
            }.getOrDefault(false)

        if (!runtimeRegistered) {
            AzimiComponentRegistry.unregister(
                componentId
            )

            return false
        }

        return true
    }

    /**
     * Registers a component with its optional runtime,
     * recovery state, checkpoint, and fault-isolation
     * configuration.
     *
     * These are independent registry entries.
     *
     * No lifecycle or recovery operation is executed.
     */
    @Synchronized
    fun registerInfrastructure(
        component: AzimiComponent,
        runtime: AzimiComponentRuntime? = null,
        recovery: AzimiRecoveryStatus? = null,
        checkpoint: AzimiCheckpoint? = null,
        isolation: AzimiFaultIsolation? = null
    ): Boolean {

        val componentId =
            component.componentId.trim()

        if (componentId.isEmpty()) {
            return false
        }

        if (
            runtime != null &&
            runtime.componentId.trim() != componentId
        ) {
            return false
        }

        if (
            recovery != null &&
            recovery.componentId.trim() != componentId
        ) {
            return false
        }

        if (
            checkpoint != null &&
            checkpoint.componentId.trim() != componentId
        ) {
            return false
        }

        if (
            isolation != null &&
            isolation.componentId.trim() != componentId
        ) {
            return false
        }

        val componentRegistered =
            runCatching {
                AzimiComponentRegistry.register(
                    component
                )
            }.getOrDefault(false)

        if (!componentRegistered) {
            return false
        }

        if (runtime != null) {

            val runtimeRegistered =
                runCatching {
                    AzimiComponentRuntimeRegistry.register(
                        runtime
                    )
                }.getOrDefault(false)

            if (!runtimeRegistered) {
                AzimiComponentRegistry.unregister(
                    componentId
                )

                return false
            }
        }

        if (recovery != null) {

            val recoveryRegistered =
                runCatching {
                    AzimiRecoveryRegistry.register(
                        recovery
                    )
                }.getOrDefault(false)

            if (!recoveryRegistered) {
                rollbackRegistration(
                    componentId,
                    runtime != null
                )

                return false
            }
        }

        if (checkpoint != null) {

            val checkpointRegistered =
                runCatching {
                    AzimiCheckpointRegistry.register(
                        checkpoint
                    )
                }.getOrDefault(false)

            if (!checkpointRegistered) {
                rollbackRegistration(
                    componentId,
                    runtime != null
                )

                return false
            }
        }

        if (isolation != null) {

            val isolationRegistered =
                runCatching {
                    AzimiFaultIsolationRegistry.register(
                        isolation
                    )
                }.getOrDefault(false)

            if (!isolationRegistered) {
                rollbackRegistration(
                    componentId,
                    runtime != null
                )

                return false
            }
        }

        return true
    }

    /**
     * Records the current health observation for
     * a registered component.
     *
     * Observation only.
     */
    @Synchronized
    fun recordHealth(
        component: AzimiComponent
    ): Boolean {

        val componentId =
            component.componentId.trim()

        if (componentId.isEmpty()) {
            return false
        }

        if (
            !AzimiComponentRegistry.contains(
                componentId
            )
        ) {
            return false
        }

        return AzimiHealthRegistry.record(
            component
        )
    }

    /**
     * Captures a complete system diagnostic snapshot
     * through the existing diagnostic coordinator.
     *
     * Observation only.
     */
    @Synchronized
    fun captureDiagnostics(
        time: String
    ): AzimiSystemDiagnosticSnapshot {

        return AzimiDiagnosticCoordinator.capture(
            time
        )
    }

    /**
     * Removes registration entries owned by this
     * coordinator for one component.
     *
     * This changes registry state only.
     * It does not stop, recover, or modify the component.
     */
    @Synchronized
    fun unregisterComponent(
        componentId: String
    ): Boolean {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return false
        }

        val componentRemoved =
            AzimiComponentRegistry.unregister(
                id
            )

        val runtimeRemoved =
            AzimiComponentRuntimeRegistry.unregister(
                id
            )

        val recoveryRemoved =
            AzimiRecoveryRegistry.unregister(
                id
            )

        val healthRemoved =
            AzimiHealthRegistry.unregister(
                id
            )

        val isolationRemoved =
            AzimiFaultIsolationRegistry.unregister(
                id
            )

        return componentRemoved ||
            runtimeRemoved ||
            recoveryRemoved ||
            healthRemoved ||
            isolationRemoved
    }

    /**
     * Removes partial registration entries when a
     * multi-registry registration cannot be completed.
     */
    private fun rollbackRegistration(
        componentId: String,
        runtimeRegistered: Boolean
    ) {

        val id =
            componentId.trim()

        AzimiFaultIsolationRegistry.unregister(
            id
        )

        AzimiRecoveryRegistry.unregister(
            id
        )

        AzimiHealthRegistry.unregister(
            id
        )

        if (runtimeRegistered) {
            AzimiComponentRuntimeRegistry.unregister(
                id
            )
        }

        AzimiComponentRegistry.unregister(
            id
        )
    }
}
