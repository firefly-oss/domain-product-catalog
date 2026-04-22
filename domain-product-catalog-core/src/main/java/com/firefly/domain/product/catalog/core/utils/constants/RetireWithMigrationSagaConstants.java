package com.firefly.domain.product.catalog.core.utils.constants;

/**
 * Symbolic constants for the {@code RetireWithMigrationSaga} introduced in
 * Step 4B.3. The saga atomically retires a source product and records a
 * {@code MIGRATION_POINTER} configuration pointing to the target product.
 */
public final class RetireWithMigrationSagaConstants {

    private RetireWithMigrationSagaConstants() {
    }

    // ============================== SAGA NAME ==============================
    public static final String SAGA_RETIRE_WITH_MIGRATION = "RetireWithMigrationSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_VALIDATE_TARGET_PRODUCT = "validateTargetProduct";
    public static final String STEP_RETIRE_SOURCE_PRODUCT = "retireSourceProduct";
    public static final String STEP_CREATE_MIGRATION_POINTER = "createMigrationPointer";

    // ============================== COMPENSATION METHODS ==============================
    /**
     * Explicit no-op compensation for {@link #STEP_VALIDATE_TARGET_PRODUCT}. The
     * step only reads the target product and stores ctx — no mutation to roll
     * back. Declared so {@code OrchestrationValidator} does not warn about a
     * missing compensation.
     */
    public static final String COMPENSATE_NOOP_VALIDATE_TARGET_PRODUCT = "noopValidateTargetProductCompensation";
    public static final String COMPENSATE_REACTIVATE_SOURCE_PRODUCT = "reactivateSourceProduct";
    public static final String COMPENSATE_DELETE_MIGRATION_POINTER = "deleteMigrationPointer";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_SOURCE_PRODUCT_ID = "sourceProductId";
    public static final String CTX_TARGET_PRODUCT_ID = "targetProductId";
    public static final String CTX_PREVIOUS_SOURCE_STATUS = "previousSourceStatus";
    public static final String CTX_GRACE_PERIOD_END_DATE = "gracePeriodEndDate";
    public static final String CTX_REASON = "reason";
    public static final String CTX_MIGRATION_POINTER_ID = "migrationPointerId";

    // ============================== STEP EVENTS ==============================
    public static final String EVENT_TARGET_PRODUCT_VALIDATED = "catalog.retire-with-migration.target-validated";
    public static final String EVENT_SOURCE_PRODUCT_RETIRED = "catalog.retire-with-migration.source-retired";
    public static final String EVENT_MIGRATION_POINTER_CREATED = "catalog.retire-with-migration.migration-pointer-created";

    // ============================== MIGRATION POINTER CONFIG KEYS ==============================
    public static final String MIGRATION_POINTER_CONFIG_KEY = "MIGRATION_POINTER";
    public static final String MIGRATION_POINTER_CONFIG_TYPE = "MIGRATION";
}
