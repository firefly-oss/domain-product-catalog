package com.firefly.domain.product.catalog.core.products.services;
import org.fireflyframework.orchestration.saga.engine.SagaResult;

import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.commands.CloneProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductFeeStructureCommand;
import com.firefly.domain.product.catalog.core.products.commands.RetireWithMigrationCommand;
import com.firefly.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import com.firefly.domain.product.catalog.core.products.simulation.ProductSimulationResponse;
import com.firefly.domain.product.catalog.core.products.simulation.SimulateProductRequest;
import com.firefly.domain.product.catalog.core.products.tree.CatalogTreeResponse;
import com.firefly.domain.product.catalog.core.products.versions.ProductVersionSummary;
import com.firefly.domain.product.catalog.core.products.versions.VersionComparisonResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface ProductCatalogService {


    /**
     * Registers a new product with its associated features, pricing, lifecycle, and other configurations.
     *
     * @param command the command containing the product details and related configurations
     * @return a Mono containing the result of the saga process
     */
    Mono<SagaResult> registerProduct(RegisterProductCommand command);


    /**
     * Updates an existing product's information, including its attributes, features, or configurations.
     *
     * @param command the command containing the updated product information and associated configurations
     * @return a Mono containing the result of the saga process, representing the outcome of the update operation
     */
    Mono<SagaResult> updateProduct(UpdateProductInfoCommand command);

    /**
     * Links a general ledger (GL) posting rule set to a product.
     *
     * @param command the command containing the product ID and the fee structure ID to be linked
     * @return a Mono containing the result of the saga process, representing the outcome of the operation
     */
    Mono<SagaResult> linkPostingRuleSet(RegisterProductFeeStructureCommand command);

    /**
     * Retrieves product information based on the provided product ID.
     *
     * @param productId the ID of the product to retrieve information for
     * @return a Mono containing the ProductQuery with product information
     */
    Mono<ProductDTO> getProductInfo(UUID productId);

    /**
     * Clones an existing product into DRAFT status with a new code, duplicating its
     * configurations, localizations, relationships and documentation requirements in
     * a compensatable saga. If any step fails all created entities are rolled back.
     *
     * @param command source product ID, new product code and (optional) tenant override
     * @return a Mono emitting the saga outcome; the cloned product ID is available via
     *         {@code result.variables().get("clonedProductId")}
     */
    Mono<SagaResult> cloneProduct(CloneProductCommand command);

    /**
     * Project eligibility and pricing for a product against a customer profile.
     * Reads pricing and eligibility configurations from the product's existing
     * {@code ProductConfiguration} records (no separate pricing service is consulted).
     *
     * @param productId the product to simulate
     * @param request simulation input (customer profile, amount, tenor)
     * @return a Mono emitting the simulation outcome (never an error signal — missing
     *         data maps to structured ineligibility reasons)
     */
    Mono<ProductSimulationResponse> simulateProduct(UUID productId, SimulateProductRequest request);

    /**
     * Retires a product and attaches a migration pointer configuration so
     * downstream consumers can steer existing contracts toward the target
     * product during the grace period. Target must already be ACTIVE.
     *
     * @param command source + target product IDs, grace period end date and
     *                human-readable reason
     * @return a Mono emitting the saga outcome; the migration pointer
     *         configuration id is available in saga variables
     */
    Mono<SagaResult> retireWithMigration(RetireWithMigrationCommand command);

    /**
     * Build the catalog tree (categories + attached products) for a tenant.
     */
    Mono<CatalogTreeResponse> getCatalogTree(UUID tenantId);

    /**
     * List all versions for a product, newest first as returned by the core service.
     */
    Mono<List<ProductVersionSummary>> listVersions(UUID productId);

    /**
     * Compute a naive field-by-field diff between two product versions.
     */
    Mono<VersionComparisonResponse> compareVersions(UUID productId, UUID versionIdA, UUID versionIdB);
}
