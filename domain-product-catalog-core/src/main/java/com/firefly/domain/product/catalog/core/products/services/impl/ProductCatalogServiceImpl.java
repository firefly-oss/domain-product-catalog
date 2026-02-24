package com.firefly.domain.product.catalog.core.products.services.impl;

import org.fireflyframework.cqrs.query.QueryBus;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductFeeStructureCommand;
import com.firefly.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import com.firefly.domain.product.catalog.core.products.queries.ProductQuery;
import com.firefly.domain.product.catalog.core.products.services.ProductCatalogService;
import com.firefly.domain.product.catalog.core.products.workflows.GetProductInfoSaga;
import com.firefly.domain.product.catalog.core.products.workflows.RegisterProductFeeStructureSaga;
import com.firefly.domain.product.catalog.core.products.workflows.RegisterProductSaga;
import com.firefly.domain.product.catalog.core.products.workflows.UpdateProductSaga;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.ExpandEach;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final SagaEngine engine;
    private final QueryBus queryBus;


    @Autowired
    public ProductCatalogServiceImpl(SagaEngine engine, QueryBus queryBus){
        this.engine=engine;
        this.queryBus = queryBus;
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
}
