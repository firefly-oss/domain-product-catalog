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


package io.fireflysoftware.domain.product.catalog.web.controller;

import io.fireflysoftware.core.product.sdk.model.ProductDTO;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductConfigurationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.services.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "CQ queries and registration for product catalog")
public class ProductCatalogController {

    private final ProductCatalogService productCatalogService;

    @Operation(summary = "Register product", description = "Define product (purpose, currency, base rules).")
    @PostMapping
    public Mono<ResponseEntity<Object>> registerProduct(@Valid @RequestBody RegisterProductCommand command) {
        return productCatalogService.registerProduct(command)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Publish product", description = "Moves a draft or proposed product to active.")
    @PostMapping("/{productId}/publish")
    public Mono<ResponseEntity<Object>> publishProduct(@PathVariable UUID productId) {
        return productCatalogService.changeStatus(productId, "ACTIVE")
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Suspend product", description = "Temporarily suspend eligibility.")
    @PostMapping("/{productId}/suspend")
    public Mono<ResponseEntity<Object>> suspendProduct(@PathVariable UUID productId) {
        return productCatalogService.changeStatus(productId, "SUSPENDED")
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Resume product", description = "Returns a suspended product to active when the SDK exposes that status.")
    @PostMapping("/{productId}/resume")
    public Mono<ResponseEntity<Object>> resumeProduct(@PathVariable UUID productId) {
        return productCatalogService.changeStatus(productId, "ACTIVE")
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Retire product", description = "Retire product; existing accounts/loans remain.")
    @PostMapping("/{productId}/retire")
    public Mono<ResponseEntity<Object>> retireProduct(@PathVariable UUID productId) {
        return productCatalogService.changeStatus(productId, "RETIRED")
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Store product configuration", description = "Stores one product_configuration row. Loan fees are not a separate aggregate.")
    @PostMapping("/{productId}/configuration")
    public Mono<ResponseEntity<Object>> linkPostingRuleSet(@PathVariable UUID productId,
                                                           @Valid @RequestBody RegisterProductConfigurationCommand command) {
        return productCatalogService.linkPostingRuleSet(command.withProductId(productId))
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Get product info", description = "Retrieve product information by product ID.")
    @GetMapping("/{productId}")
    public Mono<ResponseEntity<ProductDTO>> getProductInfo(@PathVariable UUID productId) {
        return productCatalogService.getProductInfo(productId)
                .map(ResponseEntity::ok);
    }
}
