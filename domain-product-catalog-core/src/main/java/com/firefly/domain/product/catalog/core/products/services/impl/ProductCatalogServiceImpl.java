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
import org.fireflyframework.transactional.saga.core.SagaResult;
import org.fireflyframework.transactional.saga.engine.ExpandEach;
import org.fireflyframework.transactional.saga.engine.SagaEngine;
import org.fireflyframework.transactional.saga.engine.StepInputs;
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
                .forStep(RegisterProductSaga::registerProductCategory, command.getProductCategory())
                .forStep(RegisterProductSaga::registerProductSubtype, command.getProductSubtype())
                .forStep(RegisterProductSaga::registerFeeStructure, command.getFeeStructure())
                .forStep(RegisterProductSaga::registerProductBundle, command.getProductBundle())
                .forStep(RegisterProductSaga::registerFeeComponent, command.getFeeComponent())
                .forStep(RegisterProductSaga::registerFeeApplicationRule, command.getFeeApplicationRule())
                .forStep(RegisterProductSaga::registerProduct, command.getProduct())
                .forStep(RegisterProductSaga::registerProductFeeStructure, ExpandEach.of(command.getProductFeeStructures()))
                .forStep(RegisterProductSaga::registerProductBundleItems, ExpandEach.of(command.getProductBundleItems()))
                .forStep(RegisterProductSaga::registerProductPricing, command.getProductPricing())
                .forStep(RegisterProductSaga::registerProductRelationship, ExpandEach.of(command.getProductRelationships()))
                .forStep(RegisterProductSaga::registerProductDocumentation, ExpandEach.of(command.getProductDocumentation()))
                .forStep(RegisterProductSaga::registerProductDocumentationRequirement, ExpandEach.of(command.getProductDocumentationRequirements()))
                .forStep(RegisterProductSaga::registerProductFeatures, ExpandEach.of(command.getProductFeatures()))
                .forStep(RegisterProductSaga::registerProductLifecycle, ExpandEach.of(command.getProductLifecycle()))
                .forStep(RegisterProductSaga::registerProductLimits, ExpandEach.of(command.getProductLimits()))
                .forStep(RegisterProductSaga::registerProductLocalization, ExpandEach.of(command.getProductLocalizations()))
                .forStep(RegisterProductSaga::registerVersion, ExpandEach.of(command.getProductVersions()))
                .forStep(RegisterProductSaga::registerProductPricingLocalization, command.getProductPricingLocalization())

                .build();

        return engine.execute(RegisterProductSaga.class, inputs);
    }

    @Override
    public Mono<SagaResult> updateProduct(UpdateProductInfoCommand updateProductInfoCommand) {
        StepInputs inputs = StepInputs.builder()
                .forStep(UpdateProductSaga::updateProduct, updateProductInfoCommand)
                .build();
        return engine.execute(UpdateProductSaga.class, inputs);
    }

    @Override
    public Mono<SagaResult> linkPostingRuleSet(RegisterProductFeeStructureCommand registerProductFeeStructureCommand) {
        StepInputs inputs = StepInputs.builder()
                .forStep(RegisterProductFeeStructureSaga::registerProductFeeStructure, registerProductFeeStructureCommand)
                .build();
        return engine.execute(RegisterProductFeeStructureSaga.class, inputs);
    }

    @Override
    public Mono<ProductDTO> getProductInfo(UUID productId) {
        return queryBus.query(ProductQuery.builder().productId(productId).build());
    }
}
