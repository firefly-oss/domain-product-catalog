/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
