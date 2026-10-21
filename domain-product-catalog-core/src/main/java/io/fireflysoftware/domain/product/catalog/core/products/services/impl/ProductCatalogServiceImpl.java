package io.fireflysoftware.domain.product.catalog.core.products.services.impl;

import io.fireflysoftware.core.product.sdk.model.ProductDTO;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductConfigurationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.ChangeProductStatusCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import io.fireflysoftware.domain.product.catalog.core.products.queries.ProductQuery;
import io.fireflysoftware.domain.product.catalog.core.products.services.ProductCatalogService;
import org.fireflyframework.cqrs.query.QueryBus;
import org.fireflyframework.orchestration.saga.engine.ExpandEach;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final SagaEngine engine;
    private final QueryBus queryBus;

    @Autowired
    public ProductCatalogServiceImpl(SagaEngine engine, QueryBus queryBus) {
        this.engine = engine;
        this.queryBus = queryBus;
    }

    @Override
    public Mono<SagaResult> registerProduct(RegisterProductCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId("registerProductCategory", command.getProductCategory())
                .forStepId("registerProduct", command.getProduct())
                .forStepId("registerProductConfiguration", ExpandEach.of(nullSafe(command.getProductConfigurations())))
                .forStepId("registerProductRelationship", ExpandEach.of(nullSafe(command.getProductRelationships())))
                .forStepId("registerProductDocumentation", ExpandEach.of(nullSafe(command.getProductDocumentation())))
                .forStepId("registerProductDocumentationRequirement", ExpandEach.of(nullSafe(command.getProductDocumentationRequirements())))
                .forStepId("registerProductLocalization", ExpandEach.of(nullSafe(command.getProductLocalizations())))
                .forStepId("registerVersion", ExpandEach.of(nullSafe(command.getProductVersions())))
                .build();

        return engine.execute("RegisterProductSaga", inputs);
    }

    @Override
    public Mono<SagaResult> updateProduct(UpdateProductInfoCommand updateProductInfoCommand) {
        StepInputs inputs = StepInputs.builder()
                .forStepId("readProduct", updateProductInfoCommand)
                .forStepId("updateProduct", updateProductInfoCommand)
                .build();
        return engine.execute("UpdateProductSaga", inputs);
    }

    @Override
    public Mono<SagaResult> changeStatus(UUID productId, String status) {
        ChangeProductStatusCommand command = new ChangeProductStatusCommand(productId, status);
        StepInputs inputs = StepInputs.builder()
                .forStepId("readProductStatus", command)
                .forStepId("changeProductStatus", command)
                .build();
        return engine.execute("ChangeProductStatusSaga", inputs);
    }

    @Override
    public Mono<SagaResult> linkPostingRuleSet(RegisterProductConfigurationCommand command) {
        StepInputs inputs = StepInputs.builder()
                .forStepId("registerProductConfiguration", command)
                .build();
        return engine.execute("RegisterProductConfigurationSaga", inputs);
    }

    @Override
    public Mono<ProductDTO> getProductInfo(UUID productId) {
        return queryBus.query(ProductQuery.builder().productId(productId).build());
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
