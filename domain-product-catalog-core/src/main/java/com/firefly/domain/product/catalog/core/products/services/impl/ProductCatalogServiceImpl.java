package com.firefly.domain.product.catalog.core.products.services.impl;

import org.fireflyframework.cqrs.query.QueryBus;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.commands.CloneProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductFeeStructureCommand;
import com.firefly.domain.product.catalog.core.products.commands.RetireWithMigrationCommand;
import com.firefly.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import com.firefly.domain.product.catalog.core.products.queries.ProductQuery;
import com.firefly.domain.product.catalog.core.products.services.ProductCatalogService;
import com.firefly.domain.product.catalog.core.products.simulation.ProductSimulationResponse;
import com.firefly.domain.product.catalog.core.products.simulation.ProductSimulationService;
import com.firefly.domain.product.catalog.core.products.simulation.SimulateProductRequest;
import com.firefly.domain.product.catalog.core.products.tree.CatalogTreeResponse;
import com.firefly.domain.product.catalog.core.products.tree.ProductCatalogTreeService;
import com.firefly.domain.product.catalog.core.products.versions.ProductVersionService;
import com.firefly.domain.product.catalog.core.products.versions.ProductVersionSummary;
import com.firefly.domain.product.catalog.core.products.versions.VersionComparisonResponse;
import com.firefly.domain.product.catalog.core.products.workflows.CloneProductSaga;
import com.firefly.domain.product.catalog.core.products.workflows.GetProductInfoSaga;
import com.firefly.domain.product.catalog.core.products.workflows.RegisterProductFeeStructureSaga;
import com.firefly.domain.product.catalog.core.products.workflows.RegisterProductSaga;
import com.firefly.domain.product.catalog.core.products.workflows.RetireWithMigrationSaga;
import com.firefly.domain.product.catalog.core.products.workflows.UpdateProductSaga;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.ExpandEach;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.CloneProductSagaConstants.SAGA_CLONE_PRODUCT;
import static com.firefly.domain.product.catalog.core.utils.constants.CloneProductSagaConstants.STEP_LOAD_SOURCE_PRODUCT;
import static com.firefly.domain.product.catalog.core.utils.constants.RetireWithMigrationSagaConstants.SAGA_RETIRE_WITH_MIGRATION;
import static com.firefly.domain.product.catalog.core.utils.constants.RetireWithMigrationSagaConstants.STEP_VALIDATE_TARGET_PRODUCT;

@Service
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final SagaEngine engine;
    private final QueryBus queryBus;
    private final ProductSimulationService productSimulationService;
    private final ProductCatalogTreeService productCatalogTreeService;
    private final ProductVersionService productVersionService;


    @Autowired
    public ProductCatalogServiceImpl(SagaEngine engine,
                                     QueryBus queryBus,
                                     ProductSimulationService productSimulationService,
                                     ProductCatalogTreeService productCatalogTreeService,
                                     ProductVersionService productVersionService){
        this.engine=engine;
        this.queryBus = queryBus;
        this.productSimulationService = productSimulationService;
        this.productCatalogTreeService = productCatalogTreeService;
        this.productVersionService = productVersionService;
    }

    @Override
    public Mono<SagaResult> registerProduct(RegisterProductCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId("registerProductCategory", command.getProductCategory())
                .forStepId("registerProductSubtype", command.getProductSubtype())
                .forStepId("registerFeeStructure", command.getFeeStructure())
                .forStepId("registerProductBundle", command.getProductBundle())
                .forStepId("registerFeeComponent", command.getFeeComponent())
                .forStepId("registerFeeApplicationRule", command.getFeeApplicationRule())
                .forStepId("registerProduct", command.getProduct())
                .forStepId("registerProductFeeStructure", ExpandEach.of(command.getProductFeeStructures()))
                .forStepId("registerProductBundleItems", ExpandEach.of(command.getProductBundleItems()))
                .forStepId("registerProductPricing", command.getProductPricing())
                .forStepId("registerProductRelationship", ExpandEach.of(command.getProductRelationships()))
                .forStepId("registerProductDocumentation", ExpandEach.of(command.getProductDocumentation()))
                .forStepId("registerProductDocumentationRequirement", ExpandEach.of(command.getProductDocumentationRequirements()))
                .forStepId("registerProductFeatures", ExpandEach.of(command.getProductFeatures()))
                .forStepId("registerProductLifecycle", ExpandEach.of(command.getProductLifecycle()))
                .forStepId("registerProductLimits", ExpandEach.of(command.getProductLimits()))
                .forStepId("registerProductLocalization", ExpandEach.of(command.getProductLocalizations()))
                .forStepId("registerVersion", ExpandEach.of(command.getProductVersions()))
                .forStepId("registerProductPricingLocalization", command.getProductPricingLocalization())

                .build();

        return engine.execute("RegisterProductSaga", inputs);
    }

    @Override
    public Mono<SagaResult> updateProduct(UpdateProductInfoCommand updateProductInfoCommand) {
        StepInputs inputs = StepInputs.builder()
                .forStepId("updateProduct", updateProductInfoCommand)
                .build();
        return engine.execute("UpdateProductSaga", inputs);
    }

    @Override
    public Mono<SagaResult> linkPostingRuleSet(RegisterProductFeeStructureCommand registerProductFeeStructureCommand) {
        StepInputs inputs = StepInputs.builder()
                .forStepId("registerProductFeeStructure", registerProductFeeStructureCommand)
                .build();
        return engine.execute("RegisterProductFeeStructureSaga", inputs);
    }

    @Override
    public Mono<ProductDTO> getProductInfo(UUID productId) {
        return queryBus.query(ProductQuery.builder().productId(productId).build());
    }

    @Override
    public Mono<SagaResult> cloneProduct(CloneProductCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_LOAD_SOURCE_PRODUCT, command)
                .build();
        ExecutionContext ctx = ExecutionContext.forSaga(UUID.randomUUID().toString(), SAGA_CLONE_PRODUCT);
        return engine.execute(SAGA_CLONE_PRODUCT, inputs, ctx);
    }

    @Override
    public Mono<ProductSimulationResponse> simulateProduct(UUID productId, SimulateProductRequest request) {
        return productSimulationService.simulate(productId, request);
    }

    @Override
    public Mono<SagaResult> retireWithMigration(RetireWithMigrationCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(STEP_VALIDATE_TARGET_PRODUCT, command)
                .build();
        ExecutionContext ctx = ExecutionContext.forSaga(UUID.randomUUID().toString(), SAGA_RETIRE_WITH_MIGRATION);
        return engine.execute(SAGA_RETIRE_WITH_MIGRATION, inputs, ctx);
    }

    @Override
    public Mono<CatalogTreeResponse> getCatalogTree(UUID tenantId) {
        return productCatalogTreeService.getCatalogTree(tenantId);
    }

    @Override
    public Mono<List<ProductVersionSummary>> listVersions(UUID productId) {
        return productVersionService.listVersions(productId);
    }

    @Override
    public Mono<VersionComparisonResponse> compareVersions(UUID productId, UUID versionIdA, UUID versionIdB) {
        return productVersionService.compareVersions(productId, versionIdA, versionIdB);
    }
}
