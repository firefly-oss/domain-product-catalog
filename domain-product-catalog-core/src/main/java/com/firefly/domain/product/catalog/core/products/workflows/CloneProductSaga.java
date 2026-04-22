/*
 * Copyright 2025 Firefly Software Solutions Inc
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

package com.firefly.domain.product.catalog.core.products.workflows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.core.product.sdk.api.ProductDocumentationRequirementsApi;
import com.firefly.core.product.sdk.api.ProductLocalizationApi;
import com.firefly.core.product.sdk.api.ProductRelationshipApi;
import com.firefly.core.product.sdk.model.FilterRequestProductConfigurationDTO;
import com.firefly.core.product.sdk.model.FilterRequestProductDocumentationRequirementDTO;
import com.firefly.core.product.sdk.model.FilterRequestProductLocalizationDTO;
import com.firefly.core.product.sdk.model.FilterRequestProductRelationshipDTO;
import com.firefly.core.product.sdk.model.PaginationResponse;
import com.firefly.core.product.sdk.model.ProductConfigurationDTO;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.core.product.sdk.model.ProductDocumentationRequirementDTO;
import com.firefly.core.product.sdk.model.ProductLocalizationDTO;
import com.firefly.core.product.sdk.model.ProductRelationshipDTO;
import com.firefly.domain.product.catalog.core.products.commands.CloneProductCommand;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.CloneProductSagaConstants.*;

/**
 * Atomically duplicates a product together with the four collections that define
 * its business surface: configurations, localizations, relationships and
 * documentation requirements.
 *
 * <p>All six saga steps invoke the {@code core-common-product-mgmt} SDK directly
 * (no {@code CommandBus}) because the work is a "clone" — list-then-recreate
 * sequences — and is clearer when co-located.
 *
 * <p>Each collection step (3-6) stores its newly-created child IDs in the
 * {@link ExecutionContext}; the matching compensation iterates those IDs and
 * issues {@code delete} requests explicitly. Step 2's compensation deletes the
 * cloned product itself. All compensation methods are independent: if the core
 * service cascades product-delete to its children the individual child deletes
 * surface 404s that are swallowed by {@code onErrorResume}; if it does not
 * cascade, each compensation removes its own children. Either way rollback is
 * complete.
 */
@Saga(name = SAGA_CLONE_PRODUCT)
@Service
@Slf4j
public class CloneProductSaga {

    private final ProductApi productApi;
    private final ProductConfigurationApi productConfigurationApi;
    private final ProductLocalizationApi productLocalizationApi;
    private final ProductRelationshipApi productRelationshipApi;
    private final ProductDocumentationRequirementsApi productDocumentationRequirementsApi;
    private final ObjectMapper objectMapper;

    @Autowired
    public CloneProductSaga(ProductApi productApi,
                            ProductConfigurationApi productConfigurationApi,
                            ProductLocalizationApi productLocalizationApi,
                            ProductRelationshipApi productRelationshipApi,
                            ProductDocumentationRequirementsApi productDocumentationRequirementsApi,
                            ObjectMapper objectMapper) {
        this.productApi = productApi;
        this.productConfigurationApi = productConfigurationApi;
        this.productLocalizationApi = productLocalizationApi;
        this.productRelationshipApi = productRelationshipApi;
        this.productDocumentationRequirementsApi = productDocumentationRequirementsApi;
        this.objectMapper = objectMapper;
    }

    // ============================== STEP 1: LOAD SOURCE ==============================

    @SagaStep(id = STEP_LOAD_SOURCE_PRODUCT, compensate = COMPENSATE_NOOP_LOAD_SOURCE_PRODUCT)
    @StepEvent(type = EVENT_SOURCE_PRODUCT_LOADED)
    public Mono<ProductDTO> loadSourceProduct(CloneProductCommand cmd, ExecutionContext ctx) {
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, cmd.getSourceProductId());
        ctx.putVariable(CTX_NEW_PRODUCT_CODE, cmd.getNewProductCode());
        if (cmd.getTenantId() != null) {
            ctx.putVariable(CTX_TENANT_ID, cmd.getTenantId());
        }
        return productApi.getProductById(cmd.getSourceProductId(), null)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Source product not found: " + cmd.getSourceProductId())))
                .doOnNext(product -> ctx.putVariable(CTX_SOURCE_PRODUCT, product));
    }

    /**
     * Intentional no-op. {@link #loadSourceProduct} is read-only (only fetches
     * the source product and writes ctx variables), so there is nothing to
     * revert if a later step fails.
     */
    public Mono<Void> noopLoadSourceProductCompensation() {
        return Mono.empty();
    }

    // ============================== STEP 2: CREATE CLONE ==============================

    @SagaStep(id = STEP_CREATE_CLONED_PRODUCT,
            compensate = COMPENSATE_DELETE_CLONED_PRODUCT,
            dependsOn = STEP_LOAD_SOURCE_PRODUCT)
    @StepEvent(type = EVENT_CLONED_PRODUCT_CREATED)
    public Mono<UUID> createClonedProduct(ExecutionContext ctx) {
        ProductDTO source = ctx.getVariableAs(CTX_SOURCE_PRODUCT, ProductDTO.class);
        String newCode = ctx.getVariableAs(CTX_NEW_PRODUCT_CODE, String.class);
        UUID tenantOverride = ctx.getVariableAs(CTX_TENANT_ID, UUID.class);

        ProductDTO clone = new ProductDTO()
                .tenantId(tenantOverride != null ? tenantOverride : source.getTenantId())
                .productCategoryId(source.getProductCategoryId())
                .productType(source.getProductType())
                .productName(source.getProductName())
                .productCode(newCode)
                .productDescription(source.getProductDescription())
                .productStatus(ProductDTO.ProductStatusEnum.DRAFT)
                .launchDate(source.getLaunchDate())
                .endDate(source.getEndDate());

        return productApi.createProduct(clone, UUID.randomUUID().toString())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Failed to create cloned product for source " + source.getProductId())))
                .map(resp -> requireId(resp.getProductId(), "clonedProduct"))
                .doOnNext(newId -> ctx.putVariable(CTX_CLONED_PRODUCT_ID, newId));
    }

    public Mono<Void> deleteClonedProduct(UUID clonedProductId) {
        if (clonedProductId == null) {
            log.error("saga.compensation.invariant-violation step={} reason=missing-clonedProductId",
                    STEP_CREATE_CLONED_PRODUCT);
            return Mono.empty();
        }
        return productApi.deleteProduct(clonedProductId, UUID.randomUUID().toString())
                .onErrorResume(err -> {
                    log.error("saga.compensation.delete-failed step={} clonedProductId={} err={}",
                            STEP_CREATE_CLONED_PRODUCT, clonedProductId, err.getMessage());
                    return Mono.empty();
                });
    }

    // ============================== STEP 3: CLONE CONFIGURATIONS ==============================

    @SagaStep(id = STEP_CLONE_CONFIGURATIONS,
            compensate = COMPENSATE_DELETE_CLONED_CONFIGURATIONS,
            dependsOn = STEP_CREATE_CLONED_PRODUCT)
    @StepEvent(type = EVENT_CONFIGURATIONS_CLONED)
    public Mono<List<UUID>> cloneConfigurations(ExecutionContext ctx) {
        UUID sourceProductId = ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class);
        UUID clonedProductId = ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class);

        return productConfigurationApi.filterConfigurations(sourceProductId,
                        new FilterRequestProductConfigurationDTO(), null)
                .map(CloneProductSaga::safeContent)
                .defaultIfEmpty(Collections.emptyList())
                .flatMapMany(Flux::fromIterable)
                .map(item -> objectMapper.convertValue(item, ProductConfigurationDTO.class))
                .flatMap(src -> {
                    ProductConfigurationDTO copy = new ProductConfigurationDTO()
                            .productId(clonedProductId)
                            .configType(src.getConfigType())
                            .configKey(src.getConfigKey())
                            .configValue(src.getConfigValue());
                    return productConfigurationApi.createConfiguration(clonedProductId, copy, UUID.randomUUID().toString())
                            .map(resp -> requireId(resp.getProductConfigurationId(), "clonedConfiguration"));
                })
                .collectList()
                .doOnNext(ids -> ctx.putVariable(CTX_CLONED_CONFIG_IDS, ids));
    }

    public Mono<Void> deleteClonedConfigurations(List<UUID> clonedConfigIds, ExecutionContext ctx) {
        UUID clonedProductId = ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class);
        if (clonedProductId == null) {
            log.error("saga.compensation.invariant-violation step={} reason=missing-clonedProductId",
                    STEP_CLONE_CONFIGURATIONS);
            return Mono.empty();
        }
        List<UUID> ids = resolveIds(clonedConfigIds, ctx, CTX_CLONED_CONFIG_IDS);
        return Flux.fromIterable(ids)
                .flatMap(id -> productConfigurationApi.deleteConfiguration(clonedProductId, id, UUID.randomUUID().toString())
                        .onErrorResume(err -> logAndContinue(STEP_CLONE_CONFIGURATIONS, "configId", id, err)))
                .then();
    }

    // ============================== STEP 4: CLONE LOCALIZATIONS ==============================

    @SagaStep(id = STEP_CLONE_LOCALIZATIONS,
            compensate = COMPENSATE_DELETE_CLONED_LOCALIZATIONS,
            dependsOn = STEP_CREATE_CLONED_PRODUCT)
    @StepEvent(type = EVENT_LOCALIZATIONS_CLONED)
    public Mono<List<UUID>> cloneLocalizations(ExecutionContext ctx) {
        UUID sourceProductId = ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class);
        UUID clonedProductId = ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class);

        return productLocalizationApi.filterLocalizations(sourceProductId,
                        new FilterRequestProductLocalizationDTO(), null)
                .map(CloneProductSaga::safeContent)
                .defaultIfEmpty(Collections.emptyList())
                .flatMapMany(Flux::fromIterable)
                .map(item -> objectMapper.convertValue(item, ProductLocalizationDTO.class))
                .flatMap(src -> {
                    ProductLocalizationDTO copy = new ProductLocalizationDTO()
                            .productId(clonedProductId)
                            .languageCode(src.getLanguageCode())
                            .localizedName(src.getLocalizedName())
                            .localizedDescription(src.getLocalizedDescription());
                    return productLocalizationApi.createLocalization(clonedProductId, copy, UUID.randomUUID().toString())
                            .map(resp -> requireId(resp.getProductLocalizationId(), "clonedLocalization"));
                })
                .collectList()
                .doOnNext(ids -> ctx.putVariable(CTX_CLONED_LOCALIZATION_IDS, ids));
    }

    public Mono<Void> deleteClonedLocalizations(List<UUID> clonedLocalizationIds, ExecutionContext ctx) {
        UUID clonedProductId = ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class);
        if (clonedProductId == null) {
            log.error("saga.compensation.invariant-violation step={} reason=missing-clonedProductId",
                    STEP_CLONE_LOCALIZATIONS);
            return Mono.empty();
        }
        List<UUID> ids = resolveIds(clonedLocalizationIds, ctx, CTX_CLONED_LOCALIZATION_IDS);
        return Flux.fromIterable(ids)
                .flatMap(id -> productLocalizationApi.deleteLocalization(clonedProductId, id, UUID.randomUUID().toString())
                        .onErrorResume(err -> logAndContinue(STEP_CLONE_LOCALIZATIONS, "localizationId", id, err)))
                .then();
    }

    // ============================== STEP 5: CLONE RELATIONSHIPS ==============================

    @SagaStep(id = STEP_CLONE_RELATIONSHIPS,
            compensate = COMPENSATE_DELETE_CLONED_RELATIONSHIPS,
            dependsOn = STEP_CREATE_CLONED_PRODUCT)
    @StepEvent(type = EVENT_RELATIONSHIPS_CLONED)
    public Mono<List<UUID>> cloneRelationships(ExecutionContext ctx) {
        UUID sourceProductId = ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class);
        UUID clonedProductId = ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class);

        return productRelationshipApi.filterRelationships(sourceProductId,
                        new FilterRequestProductRelationshipDTO(), null)
                .map(CloneProductSaga::safeContent)
                .defaultIfEmpty(Collections.emptyList())
                .flatMapMany(Flux::fromIterable)
                .map(item -> objectMapper.convertValue(item, ProductRelationshipDTO.class))
                .flatMap(src -> {
                    ProductRelationshipDTO copy = new ProductRelationshipDTO()
                            .productId(clonedProductId)
                            .relatedProductId(src.getRelatedProductId())
                            .relationshipType(src.getRelationshipType())
                            .description(src.getDescription());
                    return productRelationshipApi.createRelationship(clonedProductId, copy, UUID.randomUUID().toString())
                            .map(resp -> requireId(resp.getProductRelationshipId(), "clonedRelationship"));
                })
                .collectList()
                .doOnNext(ids -> ctx.putVariable(CTX_CLONED_RELATIONSHIP_IDS, ids));
    }

    public Mono<Void> deleteClonedRelationships(List<UUID> clonedRelationshipIds, ExecutionContext ctx) {
        UUID clonedProductId = ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class);
        if (clonedProductId == null) {
            log.error("saga.compensation.invariant-violation step={} reason=missing-clonedProductId",
                    STEP_CLONE_RELATIONSHIPS);
            return Mono.empty();
        }
        List<UUID> ids = resolveIds(clonedRelationshipIds, ctx, CTX_CLONED_RELATIONSHIP_IDS);
        return Flux.fromIterable(ids)
                .flatMap(id -> productRelationshipApi.deleteRelationship(clonedProductId, id, UUID.randomUUID().toString())
                        .onErrorResume(err -> logAndContinue(STEP_CLONE_RELATIONSHIPS, "relationshipId", id, err)))
                .then();
    }

    // ============================== STEP 6: CLONE DOCUMENTATION REQUIREMENTS ==============================

    @SagaStep(id = STEP_CLONE_DOCUMENTATION_REQUIREMENTS,
            compensate = COMPENSATE_DELETE_CLONED_DOCUMENTATION_REQUIREMENTS,
            dependsOn = STEP_CREATE_CLONED_PRODUCT)
    @StepEvent(type = EVENT_DOCUMENTATION_REQUIREMENTS_CLONED)
    public Mono<List<UUID>> cloneDocumentationRequirements(ExecutionContext ctx) {
        UUID sourceProductId = ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class);
        UUID clonedProductId = ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class);

        return productDocumentationRequirementsApi.filterDocumentationRequirements(sourceProductId,
                        new FilterRequestProductDocumentationRequirementDTO(), null)
                .map(CloneProductSaga::safeContent)
                .defaultIfEmpty(Collections.emptyList())
                .flatMapMany(Flux::fromIterable)
                .map(item -> objectMapper.convertValue(item, ProductDocumentationRequirementDTO.class))
                .flatMap(src -> {
                    ProductDocumentationRequirementDTO copy = new ProductDocumentationRequirementDTO()
                            .productId(clonedProductId)
                            .docType(src.getDocType())
                            .isMandatory(src.getIsMandatory())
                            .description(src.getDescription());
                    return productDocumentationRequirementsApi.createDocumentationRequirement(clonedProductId, copy, UUID.randomUUID().toString())
                            .map(resp -> requireId(resp.getProductDocRequirementId(), "clonedDocumentationRequirement"));
                })
                .collectList()
                .doOnNext(ids -> ctx.putVariable(CTX_CLONED_DOCUMENTATION_REQUIREMENT_IDS, ids));
    }

    public Mono<Void> deleteClonedDocumentationRequirements(List<UUID> clonedDocumentationRequirementIds, ExecutionContext ctx) {
        UUID clonedProductId = ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class);
        if (clonedProductId == null) {
            log.error("saga.compensation.invariant-violation step={} reason=missing-clonedProductId",
                    STEP_CLONE_DOCUMENTATION_REQUIREMENTS);
            return Mono.empty();
        }
        List<UUID> ids = resolveIds(clonedDocumentationRequirementIds, ctx, CTX_CLONED_DOCUMENTATION_REQUIREMENT_IDS);
        return Flux.fromIterable(ids)
                .flatMap(id -> productDocumentationRequirementsApi.deleteDocumentationRequirement(clonedProductId, id, UUID.randomUUID().toString())
                        .onErrorResume(err -> logAndContinue(STEP_CLONE_DOCUMENTATION_REQUIREMENTS, "documentationRequirementId", id, err)))
                .then();
    }

    // ============================== HELPERS ==============================

    private static List<Object> safeContent(PaginationResponse resp) {
        if (resp == null || resp.getContent() == null) {
            return Collections.emptyList();
        }
        return resp.getContent();
    }

    @SuppressWarnings("unchecked")
    private static List<UUID> resolveIds(List<UUID> fromParam, ExecutionContext ctx, String ctxKey) {
        if (fromParam != null) {
            return fromParam;
        }
        List<?> raw = Optional.ofNullable(ctx.getVariableAs(ctxKey, List.class)).orElse(Collections.emptyList());
        return (List<UUID>) raw.stream().filter(Objects::nonNull).toList();
    }

    private static Mono<Void> logAndContinue(String step, String idLabel, UUID id, Throwable err) {
        log.error("saga.compensation.delete-failed step={} {}={} err={}", step, idLabel, id, err.getMessage());
        return Mono.empty();
    }

    private static UUID requireId(UUID id, String label) {
        if (id == null) {
            throw new IllegalStateException(label + " returned without id — cannot participate in compensation");
        }
        return id;
    }
}
