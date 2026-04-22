package com.firefly.domain.product.catalog.core.utils.constants;

/**
 * Symbolic constants for the {@code CloneProductSaga} introduced in Step 4B.1.
 * The saga duplicates a product into DRAFT status along with its configurations,
 * localizations, relationships and documentation requirements.
 */
public final class CloneProductSagaConstants {

    private CloneProductSagaConstants() {
    }

    // ============================== SAGA NAME ==============================
    public static final String SAGA_CLONE_PRODUCT = "CloneProductSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_LOAD_SOURCE_PRODUCT = "loadSourceProduct";
    public static final String STEP_CREATE_CLONED_PRODUCT = "createClonedProduct";
    public static final String STEP_CLONE_CONFIGURATIONS = "cloneConfigurations";
    public static final String STEP_CLONE_LOCALIZATIONS = "cloneLocalizations";
    public static final String STEP_CLONE_RELATIONSHIPS = "cloneRelationships";
    public static final String STEP_CLONE_DOCUMENTATION_REQUIREMENTS = "cloneDocumentationRequirements";

    // ============================== COMPENSATION METHODS ==============================
    /**
     * Explicit no-op compensation for {@link #STEP_LOAD_SOURCE_PRODUCT}. The
     * step is read-only — it fetches the source product without mutating state.
     * Declared so the framework's {@code OrchestrationValidator} does not warn
     * about a missing compensation; intent is encoded here rather than left
     * implicit.
     */
    public static final String COMPENSATE_NOOP_LOAD_SOURCE_PRODUCT = "noopLoadSourceProductCompensation";
    public static final String COMPENSATE_DELETE_CLONED_PRODUCT = "deleteClonedProduct";
    public static final String COMPENSATE_DELETE_CLONED_CONFIGURATIONS = "deleteClonedConfigurations";
    public static final String COMPENSATE_DELETE_CLONED_LOCALIZATIONS = "deleteClonedLocalizations";
    public static final String COMPENSATE_DELETE_CLONED_RELATIONSHIPS = "deleteClonedRelationships";
    public static final String COMPENSATE_DELETE_CLONED_DOCUMENTATION_REQUIREMENTS = "deleteClonedDocumentationRequirements";

    // ============================== CONTEXT VARIABLES ==============================
    public static final String CTX_SOURCE_PRODUCT = "sourceProduct";
    public static final String CTX_SOURCE_PRODUCT_ID = "sourceProductId";
    public static final String CTX_CLONED_PRODUCT_ID = "clonedProductId";
    public static final String CTX_NEW_PRODUCT_CODE = "newProductCode";
    public static final String CTX_TENANT_ID = "tenantId";
    public static final String CTX_CLONED_CONFIG_IDS = "clonedConfigIds";
    public static final String CTX_CLONED_LOCALIZATION_IDS = "clonedLocalizationIds";
    public static final String CTX_CLONED_RELATIONSHIP_IDS = "clonedRelationshipIds";
    public static final String CTX_CLONED_DOCUMENTATION_REQUIREMENT_IDS = "clonedDocumentationRequirementIds";

    // ============================== STEP EVENTS ==============================
    public static final String EVENT_SOURCE_PRODUCT_LOADED = "catalog.clone.source-loaded";
    public static final String EVENT_CLONED_PRODUCT_CREATED = "catalog.clone.product-created";
    public static final String EVENT_CONFIGURATIONS_CLONED = "catalog.clone.configurations-created";
    public static final String EVENT_LOCALIZATIONS_CLONED = "catalog.clone.localizations-created";
    public static final String EVENT_RELATIONSHIPS_CLONED = "catalog.clone.relationships-created";
    public static final String EVENT_DOCUMENTATION_REQUIREMENTS_CLONED = "catalog.clone.documentation-requirements-created";

    // ============================== SENTINELS (no-op step returns) ==============================
    public static final String SENTINEL_NO_CONFIGURATIONS = "no-configurations";
    public static final String SENTINEL_NO_LOCALIZATIONS = "no-localizations";
    public static final String SENTINEL_NO_RELATIONSHIPS = "no-relationships";
    public static final String SENTINEL_NO_DOCUMENTATION_REQUIREMENTS = "no-documentation-requirements";
}
