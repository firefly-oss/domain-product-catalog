package com.firefly.domain.product.catalog.web.controller;

import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.commands.CloneProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductFeeStructureCommand;
import com.firefly.domain.product.catalog.core.products.commands.RetireWithMigrationCommand;
import com.firefly.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import com.firefly.domain.product.catalog.core.products.queries.ProductQuery;
import com.firefly.domain.product.catalog.core.products.services.ProductCatalogService;
import com.firefly.domain.product.catalog.core.products.simulation.ProductSimulationResponse;
import com.firefly.domain.product.catalog.core.products.simulation.SimulateProductRequest;
import com.firefly.domain.product.catalog.core.products.tree.CatalogTreeResponse;
import com.firefly.domain.product.catalog.core.products.versions.ProductVersionSummary;
import com.firefly.domain.product.catalog.core.products.versions.VersionComparisonResponse;
import com.firefly.domain.product.catalog.interfaces.rest.CloneProductRequest;
import com.firefly.domain.product.catalog.interfaces.rest.RetireWithMigrationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
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

    @Operation(summary = "Publish product", description = "Publish a sellable version (freeze linked configs).")
    @PostMapping("/{productId}/publish")
    public Mono<ResponseEntity<Object>> publishProduct(@PathVariable UUID productId) {
        return productCatalogService.updateProduct(new UpdateProductInfoCommand()
                        .withProductId(productId)
                        .withProductStatus(ProductDTO.ProductStatusEnum.ACTIVE))
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Suspend product", description = "Temporarily suspend eligibility.")
    @PostMapping("/{productId}/suspend")
    public Mono<ResponseEntity<Object>> suspendProduct(@PathVariable UUID productId) {
        return productCatalogService.updateProduct(new UpdateProductInfoCommand()
                        .withProductId(productId)
                        .withProductStatus(ProductDTO.ProductStatusEnum.PROPOSED))
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Resume product", description = "Resume product eligibility.")
    @PostMapping("/{productId}/resume")
    public Mono<ResponseEntity<Object>> resumeProduct(@PathVariable UUID productId) {
        return productCatalogService.updateProduct(new UpdateProductInfoCommand()
                        .withProductId(productId)
                        .withProductStatus(ProductDTO.ProductStatusEnum.ACTIVE))
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Retire product", description = "Retire product; existing accounts/loans remain.")
    @PostMapping("/{productId}/retire")
    public Mono<ResponseEntity<Object>> retireProduct(@PathVariable UUID productId) {
        return productCatalogService.updateProduct(new UpdateProductInfoCommand()
                        .withProductId(productId)
                        .withProductStatus(ProductDTO.ProductStatusEnum.RETIRED))
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Link posting rule set", description = "Link the GL mapping ruleset to the product.")
    @PostMapping("/{productId}/posting-rule-set")
    public Mono<ResponseEntity<Object>> linkPostingRuleSet(@PathVariable UUID productId,
                                                           @Valid @RequestBody RegisterProductFeeStructureCommand command) {
        return productCatalogService.linkPostingRuleSet(command.withProductId(productId))
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Get product info", description = "Retrieve product information by product ID.")
    @GetMapping("/{productId}")
    public Mono<ResponseEntity<ProductDTO>> getProductInfo(@PathVariable UUID productId) {
        return productCatalogService.getProductInfo(productId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Clone product",
            description = "Atomically duplicates a product into DRAFT status with a new code, including configurations, localizations, relationships and documentation requirements.")
    @ApiResponse(responseCode = "200", description = "Clone accepted; the saga result is emitted in the response body.")
    @PostMapping("/{productId}/clone")
    public Mono<ResponseEntity<Object>> cloneProduct(@PathVariable UUID productId,
                                                     @Valid @RequestBody CloneProductRequest request) {
        CloneProductCommand cmd = new CloneProductCommand(productId, request.newProductCode(), request.tenantId());
        return productCatalogService.cloneProduct(cmd)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Simulate product",
            description = "Projects eligibility and pricing for the given product and customer profile. Reads existing product configurations; does not create any persistent artifacts.")
    @ApiResponse(responseCode = "200", description = "Simulation outcome with eligibility flag and pricing projection.")
    @PostMapping("/{productId}/simulate")
    public Mono<ResponseEntity<ProductSimulationResponse>> simulateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody SimulateProductRequest request) {
        return productCatalogService.simulateProduct(productId, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Retire with migration",
            description = "Retires the source product and records a migration pointer referencing an active target product for the grace period.")
    @ApiResponse(responseCode = "200", description = "Retirement accepted; source product is set to RETIRED and a MIGRATION_POINTER configuration is attached.")
    @PostMapping("/{productId}/retire-with-migration")
    public Mono<ResponseEntity<Object>> retireWithMigration(
            @PathVariable UUID productId,
            @Valid @RequestBody RetireWithMigrationRequest request) {
        RetireWithMigrationCommand cmd = new RetireWithMigrationCommand(
                productId, request.targetProductId(), request.gracePeriodEndDate(), request.reason());
        return productCatalogService.retireWithMigration(cmd)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Catalog tree",
            description = "Returns the full category hierarchy for a tenant with products attached at each level.")
    @ApiResponse(responseCode = "200", description = "Nested catalog tree with roots, subcategories and products.")
    @GetMapping("/catalog-tree")
    public Mono<ResponseEntity<CatalogTreeResponse>> getCatalogTree(@RequestParam UUID tenantId) {
        return productCatalogService.getCatalogTree(tenantId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "List product versions",
            description = "Lists all versions of a product in the order returned by the core service.")
    @ApiResponse(responseCode = "200", description = "Array of version summaries.")
    @GetMapping("/{productId}/versions")
    public Mono<ResponseEntity<List<ProductVersionSummary>>> listVersions(@PathVariable UUID productId) {
        return productCatalogService.listVersions(productId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Compare product versions",
            description = "Naive JSON-path diff between two versions of the same product; includes associated configurations.")
    @ApiResponse(responseCode = "200", description = "Added / removed / changed field lists.")
    @GetMapping("/{productId}/versions/compare")
    public Mono<ResponseEntity<VersionComparisonResponse>> compareVersions(
            @PathVariable UUID productId,
            @RequestParam UUID v1,
            @RequestParam UUID v2) {
        return productCatalogService.compareVersions(productId, v1, v2)
                .map(ResponseEntity::ok);
    }
}
