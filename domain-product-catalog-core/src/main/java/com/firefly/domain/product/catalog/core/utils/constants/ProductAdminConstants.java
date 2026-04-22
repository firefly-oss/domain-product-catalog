package com.firefly.domain.product.catalog.core.utils.constants;

/**
 * Command names and audit event types for the non-saga admin flows introduced in
 * Phase 4B: catalog tree read model, version listing/comparison and the
 * synchronous product simulation service.
 *
 * <p>Per Step 3B.1 these flows do not introduce new downstream dependencies —
 * pricing scheme data is read from {@code core-common-product-mgmt} via the
 * {@code ProductConfigurationApi} already wired in {@code ClientFactory}.
 */
public final class ProductAdminConstants {

    private ProductAdminConstants() {
    }

    // ============================== COMMAND / QUERY NAMES ==============================
    public static final String CATALOG_TREE = "catalog-tree";
    public static final String VERSIONS_COMPARE = "versions-compare";
    public static final String VERSIONS_LIST = "versions-list";
    public static final String PRODUCT_SIMULATION = "product-simulation";

    // ============================== AUDIT EVENT TYPES ==============================
    public static final String EVENT_CATALOG_TREE_REQUESTED = "catalog.tree.requested";
    public static final String EVENT_VERSIONS_LISTED = "catalog.versions.listed";
    public static final String EVENT_VERSIONS_COMPARED = "catalog.versions.compared";
    public static final String EVENT_PRODUCT_SIMULATED = "catalog.product.simulated";

    // ============================== SIMULATION CONFIG TOKENS ==============================
    public static final String CONFIG_TYPE_PRICING = "PRICING";
    public static final String CONFIG_TYPE_ELIGIBILITY_RULE = "ELIGIBILITY_RULE";
    public static final String REASON_PRODUCT_NOT_ACTIVE = "PRODUCT_NOT_ACTIVE";
    public static final String REASON_NO_PRICING_SCHEME = "NO_PRICING_SCHEME";
    public static final String REASON_ELIGIBILITY_RULE_FAILED = "ELIGIBILITY_RULE_FAILED";
}
