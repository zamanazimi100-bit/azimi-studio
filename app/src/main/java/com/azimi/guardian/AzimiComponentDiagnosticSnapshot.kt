package com.azimi.guardian

/**
 * Read-only diagnostic snapshot for one AZIMI component.
 *
 * This snapshot combines information from the existing
 * observation, runtime, failure, recovery, checkpoint,
 * and fault-isolation layers.
 *
 * It contains diagnostic information only.
 * It does not authorize, recover, start, stop, or modify
 * any component.
 */
data class AzimiComponentDiagnosticSnapshot(

    /**
     * Stable component identifier.
     */
    val componentId: String,

    /**
     * Whether the component exists in the
     * central component registry.
     */
    val registered: Boolean,

    /**
     * Whether a runtime boundary exists for
     * this component.
     */
    val runtimeRegistered: Boolean,

    /**
     * Current lifecycle state when known.
     */
    val state: AzimiComponentState,

    /**
     * Latest known health observation.
     */
    val health: AzimiComponentHealth,

    /**
     * Current recovery information when available.
     */
    val recovery: AzimiRecoveryStatus? = null,

    /**
     * Most recent checkpoint belonging to the component.
     */
    val latestCheckpoint: AzimiCheckpoint? = null,

    /**
     * Fault-isolation boundary when configured.
     */
    val isolation: AzimiFaultIsolation? = null,

    /**
     * Most recent recorded failure for the component.
     */
    val latestFailure: AzimiFailureEvent? = null,

    /**
     * Short diagnostic message.
     *
     * Secrets and protected credentials must never
     * be placed in this field.
     */
    val message: String = "NONE"
) {

    /**
     * Validates the minimum identity required
     * for a useful diagnostic snapshot.
     */
    fun isValid(): Boolean {

        return componentId
            .trim()
            .isNotEmpty()
    }
}

/**
 * Creates safe diagnostic snapshots from the existing
 * AZIMI foundation registries.
 *
 * This object observes registry state only.
 * It does not execute component operations.
 */
object AzimiComponentDiagnosticSnapshotFactory {

    /**
     * Builds a diagnostic snapshot for one component.
     */
    @Synchronized
    fun create(
        componentId: String
    ): AzimiComponentDiagnosticSnapshot? {

        val id =
            componentId.trim()

        if (id.isEmpty()) {
            return null
        }

        val component =
            AzimiComponentRegistry.get(id)

        val runtime =
            AzimiComponentRuntimeRegistry.get(id)

        val registered =
            component != null

        val runtimeRegistered =
            runtime != null

        val componentStatus =
            if (runtime != null) {
                AzimiComponentRuntimeInspector.inspect(
                    runtime
                )
            } else if (component != null) {
                AzimiComponentStatus(
                    componentId = id,
                    state = runCatching {
                        component.state()
                    }.getOrDefault(
                        AzimiComponentState.UNAVAILABLE
                    ),
                    healthy = runCatching {
                        component.health().healthy
                    }.getOrDefault(false),
                    message = runCatching {
                        component.health().message
                    }.getOrDefault(
                        "COMPONENT_INSPECTION_FAILED"
                    )
                )
            } else {
                AzimiComponentStatus(
                    componentId = id,
                    state = AzimiComponentState.UNAVAILABLE,
                    healthy = false,
                    message = "COMPONENT_NOT_REGISTERED"
                )
            }

        val health =
            if (runtime != null) {
                runCatching {
                    runtime.health()
                }.getOrElse {
                    AzimiComponentHealth(
                        healthy = false,
                        state = AzimiComponentState.UNAVAILABLE,
                        message = "RUNTIME_HEALTH_FAILED"
                    )
                }
            } else if (component != null) {
                runCatching {
                    component.health()
                }.getOrElse {
                    AzimiComponentHealth(
                        healthy = false,
                        state = AzimiComponentState.UNAVAILABLE,
                        message = "COMPONENT_HEALTH_FAILED"
                    )
                }
            } else {
                AzimiComponentHealth(
                    healthy = false,
                    state = AzimiComponentState.UNAVAILABLE,
                    message = "COMPONENT_NOT_REGISTERED"
                )
            }

        val recovery =
            AzimiRecoveryRegistry.get(id)

        val latestCheckpoint =
            AzimiCheckpointRegistry
                .forComponent(id)
                .lastOrNull()

        val isolation =
            AzimiFaultIsolationRegistry.get(id)

        val latestFailure =
            AzimiFailureRegistry
                .forComponent(id)
                .firstOrNull()

        return AzimiComponentDiagnosticSnapshot(
            componentId = id,
            registered = registered,
            runtimeRegistered = runtimeRegistered,
            state = componentStatus.state,
            health = health,
            recovery = recovery,
            latestCheckpoint = latestCheckpoint,
            isolation = isolation,
            latestFailure = latestFailure,
            message = componentStatus.message
        )
    }

    /**
     * Builds diagnostic snapshots for every component
     * currently registered with AZIMI.
     *
     * Observation only.
     */
    @Synchronized
    fun createAll(): List<AzimiComponentDiagnosticSnapshot> {

        return AzimiComponentRegistry
            .all()
            .mapNotNull {
                create(it.componentId)
            }
    }
}
