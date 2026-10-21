package io.fireflysoftware.domain.product.catalog.core.products.services;

import io.fireflysoftware.core.product.sdk.model.ProductDTO;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductConfigurationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductCatalogService {

    /**
     * Registers a product. Category, versions, relationships, localizations,
     * documents, and configuration rows are optional. A product that is not a loan
     * succeeds without amount, rate, or fee configuration.
     */
    Mono<SagaResult> registerProduct(RegisterProductCommand command);

    Mono<SagaResult> updateProduct(UpdateProductInfoCommand command);

    Mono<SagaResult> changeStatus(UUID productId, String status);

    /**
     * Stores one product_configuration row and deletes it if a later step fails.
     */
    Mono<SagaResult> linkPostingRuleSet(RegisterProductConfigurationCommand command);

    Mono<ProductDTO> getProductInfo(UUID productId);
}
