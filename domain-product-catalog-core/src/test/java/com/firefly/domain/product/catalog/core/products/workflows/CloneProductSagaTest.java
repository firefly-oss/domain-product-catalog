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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.firefly.domain.product.catalog.core.utils.constants.CloneProductSagaConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloneProductSagaTest {

    private ProductApi productApi;
    private ProductConfigurationApi productConfigurationApi;
    private ProductLocalizationApi productLocalizationApi;
    private ProductRelationshipApi productRelationshipApi;
    private ProductDocumentationRequirementsApi productDocumentationRequirementsApi;
    private ObjectMapper objectMapper;
    private CloneProductSaga saga;

    @BeforeEach
    void setUp() {
        productApi = mock(ProductApi.class);
        productConfigurationApi = mock(ProductConfigurationApi.class);
        productLocalizationApi = mock(ProductLocalizationApi.class);
        productRelationshipApi = mock(ProductRelationshipApi.class);
        productDocumentationRequirementsApi = mock(ProductDocumentationRequirementsApi.class);
        objectMapper = new ObjectMapper();
        saga = new CloneProductSaga(productApi, productConfigurationApi, productLocalizationApi,
                productRelationshipApi, productDocumentationRequirementsApi, objectMapper);
    }

    // ============================== STEP 1 ==============================

    @Test
    void loadSourceProduct_storesCtxAndReturnsProduct() {
        UUID sourceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ProductDTO sourceProduct = buildSourceProduct(sourceId);
        when(productApi.getProductById(eq(sourceId), any())).thenReturn(Mono.just(sourceProduct));

        CloneProductCommand cmd = new CloneProductCommand(sourceId, "NEW-CODE", tenantId);
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);

        StepVerifier.create(saga.loadSourceProduct(cmd, ctx))
                .expectNext(sourceProduct)
                .verifyComplete();

        assertThat(ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class)).isEqualTo(sourceId);
        assertThat(ctx.getVariableAs(CTX_NEW_PRODUCT_CODE, String.class)).isEqualTo("NEW-CODE");
        assertThat(ctx.getVariableAs(CTX_TENANT_ID, UUID.class)).isEqualTo(tenantId);
        assertThat(ctx.getVariableAs(CTX_SOURCE_PRODUCT, ProductDTO.class)).isSameAs(sourceProduct);
    }

    @Test
    void noopLoadSourceProductCompensation_completesEmpty() {
        StepVerifier.create(saga.noopLoadSourceProductCompensation()).verifyComplete();
    }

    @Test
    void loadSourceProduct_failsFast_whenSourceMissing() {
        UUID sourceId = UUID.randomUUID();
        when(productApi.getProductById(eq(sourceId), any())).thenReturn(Mono.empty());

        CloneProductCommand cmd = new CloneProductCommand(sourceId, "NEW-CODE", null);
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);

        StepVerifier.create(saga.loadSourceProduct(cmd, ctx))
                .expectError(IllegalStateException.class)
                .verify();
    }

    // ============================== STEP 2 ==============================

    @Test
    void createClonedProduct_copiesFieldsAndFlipsStatusToDraft() {
        UUID sourceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ProductDTO source = buildSourceProduct(sourceId);
        source.setProductStatus(ProductDTO.ProductStatusEnum.ACTIVE);

        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);
        ctx.putVariable(CTX_SOURCE_PRODUCT, source);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_NEW_PRODUCT_CODE, "CLONED-001");
        ctx.putVariable(CTX_TENANT_ID, tenantId);

        UUID newId = UUID.randomUUID();
        ProductDTO createdResponse = new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), newId)
                .productCode("CLONED-001")
                .productStatus(ProductDTO.ProductStatusEnum.DRAFT);

        ArgumentCaptor<ProductDTO> bodyCaptor = ArgumentCaptor.forClass(ProductDTO.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(productApi.createProduct(bodyCaptor.capture(), keyCaptor.capture()))
                .thenReturn(Mono.just(createdResponse));

        StepVerifier.create(saga.createClonedProduct(ctx))
                .expectNext(newId)
                .verifyComplete();

        ProductDTO sentToApi = bodyCaptor.getValue();
        assertThat(sentToApi.getProductCode()).isEqualTo("CLONED-001");
        assertThat(sentToApi.getProductStatus()).isEqualTo(ProductDTO.ProductStatusEnum.DRAFT);
        assertThat(sentToApi.getTenantId()).isEqualTo(tenantId);
        assertThat(sentToApi.getProductName()).isEqualTo(source.getProductName());
        assertThat(ctx.getVariableAs(CTX_CLONED_PRODUCT_ID, UUID.class)).isEqualTo(newId);
        assertUuid(keyCaptor.getValue());
    }

    @Test
    void createClonedProduct_fails_whenSdkReturnsProductWithoutId() {
        UUID sourceId = UUID.randomUUID();
        ProductDTO source = buildSourceProduct(sourceId);
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);
        ctx.putVariable(CTX_SOURCE_PRODUCT, source);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_NEW_PRODUCT_CODE, "CLONED-X");

        when(productApi.createProduct(any(), any())).thenReturn(Mono.just(new ProductDTO()));

        StepVerifier.create(saga.createClonedProduct(ctx))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void deleteClonedProduct_invokesDeleteWithIdempotencyKey() {
        UUID clonedId = UUID.randomUUID();
        when(productApi.deleteProduct(eq(clonedId), any())).thenReturn(Mono.empty());

        StepVerifier.create(saga.deleteClonedProduct(clonedId)).verifyComplete();
        verify(productApi, times(1)).deleteProduct(eq(clonedId), any());
    }

    @Test
    void deleteClonedProduct_isNoOp_whenIdIsNull() {
        StepVerifier.create(saga.deleteClonedProduct(null)).verifyComplete();
        verify(productApi, never()).deleteProduct(any(), any());
    }

    @Test
    void deleteClonedProduct_swallowsSdkErrors() {
        UUID clonedId = UUID.randomUUID();
        when(productApi.deleteProduct(eq(clonedId), any()))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(saga.deleteClonedProduct(clonedId)).verifyComplete();
    }

    // ============================== STEP 3: CONFIGURATIONS ==============================

    @Test
    void cloneConfigurations_copiesEachSourceConfig_andStoresIds() {
        UUID sourceId = UUID.randomUUID();
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = buildBaseCtx(sourceId, clonedId);

        ProductConfigurationDTO cfg1 = new ProductConfigurationDTO()
                .configType(ProductConfigurationDTO.ConfigTypeEnum.PRICING)
                .configKey("BASE_RATE").configValue("0.02");
        ProductConfigurationDTO cfg2 = new ProductConfigurationDTO()
                .configType(ProductConfigurationDTO.ConfigTypeEnum.LIMITS)
                .configKey("DAILY_LIMIT").configValue("500.00");

        when(productConfigurationApi.filterConfigurations(eq(sourceId), any(FilterRequestProductConfigurationDTO.class), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(cfg1, cfg2))));

        AtomicInteger counter = new AtomicInteger();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(productConfigurationApi.createConfiguration(eq(clonedId), any(ProductConfigurationDTO.class), keyCaptor.capture()))
                .thenAnswer(inv -> {
                    int i = counter.incrementAndGet();
                    ProductConfigurationDTO resp = new ProductConfigurationDTO(LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID())
                            .productId(clonedId)
                            .configKey("K" + i);
                    return Mono.just(resp);
                });

        StepVerifier.create(saga.cloneConfigurations(ctx))
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list).allSatisfy(id -> assertThat(id).isNotNull());
                })
                .verifyComplete();

        @SuppressWarnings("unchecked")
        List<UUID> stored = (List<UUID>) (List<?>) ctx.getVariableAs(CTX_CLONED_CONFIG_IDS, List.class);
        assertThat(stored).hasSize(2);
        assertThat(keyCaptor.getAllValues()).hasSize(2).allSatisfy(CloneProductSagaTest::assertUuid);
        assertThat(keyCaptor.getAllValues()).doesNotHaveDuplicates();
    }

    @Test
    void cloneConfigurations_fails_whenCreatedConfigHasNoId() {
        UUID sourceId = UUID.randomUUID();
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = buildBaseCtx(sourceId, clonedId);

        ProductConfigurationDTO cfg = new ProductConfigurationDTO().configKey("K");
        when(productConfigurationApi.filterConfigurations(eq(sourceId), any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(cfg))));
        when(productConfigurationApi.createConfiguration(eq(clonedId), any(), any()))
                .thenReturn(Mono.just(new ProductConfigurationDTO())); // missing id

        StepVerifier.create(saga.cloneConfigurations(ctx))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void cloneConfigurations_returnsEmptyList_whenSourceHasNone() {
        UUID sourceId = UUID.randomUUID();
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = buildBaseCtx(sourceId, clonedId);

        when(productConfigurationApi.filterConfigurations(eq(sourceId), any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of())));

        StepVerifier.create(saga.cloneConfigurations(ctx))
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();

        verify(productConfigurationApi, never()).createConfiguration(any(), any(), any());
    }

    @Test
    void deleteClonedConfigurations_deletesAllStoredIds() {
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);
        ctx.putVariable(CTX_CLONED_PRODUCT_ID, clonedId);

        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(productConfigurationApi.deleteConfiguration(eq(clonedId), any(), keyCaptor.capture()))
                .thenReturn(Mono.empty());

        StepVerifier.create(saga.deleteClonedConfigurations(ids, ctx)).verifyComplete();
        verify(productConfigurationApi, times(3)).deleteConfiguration(eq(clonedId), any(), any());
        assertThat(keyCaptor.getAllValues()).hasSize(3).allSatisfy(CloneProductSagaTest::assertUuid);
    }

    @Test
    void deleteClonedConfigurations_fallsBackToCtxWhenParamIsNull() {
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);
        ctx.putVariable(CTX_CLONED_PRODUCT_ID, clonedId);
        ctx.putVariable(CTX_CLONED_CONFIG_IDS, List.of(UUID.randomUUID(), UUID.randomUUID()));

        when(productConfigurationApi.deleteConfiguration(eq(clonedId), any(), any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(saga.deleteClonedConfigurations(null, ctx)).verifyComplete();
        verify(productConfigurationApi, times(2)).deleteConfiguration(eq(clonedId), any(), any());
    }

    @Test
    void deleteClonedConfigurations_isNoOp_whenClonedProductIdMissing() {
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);

        StepVerifier.create(saga.deleteClonedConfigurations(List.of(UUID.randomUUID()), ctx)).verifyComplete();
        verify(productConfigurationApi, never()).deleteConfiguration(any(), any(), any());
    }

    // ============================== STEP 4: LOCALIZATIONS ==============================

    @Test
    void cloneLocalizations_copiesAndStoresIds() {
        UUID sourceId = UUID.randomUUID();
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = buildBaseCtx(sourceId, clonedId);

        ProductLocalizationDTO loc = new ProductLocalizationDTO()
                .languageCode("es-ES").localizedName("Producto").localizedDescription("Desc");

        when(productLocalizationApi.filterLocalizations(eq(sourceId), any(FilterRequestProductLocalizationDTO.class), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(loc))));
        when(productLocalizationApi.createLocalization(eq(clonedId), any(), any()))
                .thenReturn(Mono.just(new ProductLocalizationDTO(LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID())));

        StepVerifier.create(saga.cloneLocalizations(ctx))
                .assertNext(list -> assertThat(list).hasSize(1))
                .verifyComplete();

        assertThat(ctx.getVariableAs(CTX_CLONED_LOCALIZATION_IDS, List.class)).hasSize(1);
    }

    @Test
    void deleteClonedLocalizations_deletesAll() {
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);
        ctx.putVariable(CTX_CLONED_PRODUCT_ID, clonedId);

        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(productLocalizationApi.deleteLocalization(eq(clonedId), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(saga.deleteClonedLocalizations(ids, ctx)).verifyComplete();
        verify(productLocalizationApi, times(2)).deleteLocalization(eq(clonedId), any(), any());
    }

    // ============================== STEP 5: RELATIONSHIPS ==============================

    @Test
    void cloneRelationships_copiesAndStoresIds() {
        UUID sourceId = UUID.randomUUID();
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = buildBaseCtx(sourceId, clonedId);

        ProductRelationshipDTO rel = new ProductRelationshipDTO()
                .relatedProductId(UUID.randomUUID())
                .relationshipType(ProductRelationshipDTO.RelationshipTypeEnum.CROSS_SELL)
                .description("cross-sell");

        when(productRelationshipApi.filterRelationships(eq(sourceId), any(FilterRequestProductRelationshipDTO.class), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(rel))));
        when(productRelationshipApi.createRelationship(eq(clonedId), any(), any()))
                .thenReturn(Mono.just(new ProductRelationshipDTO(LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID())));

        StepVerifier.create(saga.cloneRelationships(ctx))
                .assertNext(list -> assertThat(list).hasSize(1))
                .verifyComplete();

        assertThat(ctx.getVariableAs(CTX_CLONED_RELATIONSHIP_IDS, List.class)).hasSize(1);
    }

    @Test
    void cloneRelationships_propagatesDownstreamErrors() {
        UUID sourceId = UUID.randomUUID();
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = buildBaseCtx(sourceId, clonedId);

        ProductRelationshipDTO rel = new ProductRelationshipDTO()
                .relatedProductId(UUID.randomUUID());

        when(productRelationshipApi.filterRelationships(eq(sourceId), any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(rel))));
        when(productRelationshipApi.createRelationship(eq(clonedId), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("downstream-boom")));

        StepVerifier.create(saga.cloneRelationships(ctx))
                .expectErrorMatches(err -> err.getMessage().contains("downstream-boom"))
                .verify();
    }

    @Test
    void deleteClonedRelationships_deletesAll() {
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);
        ctx.putVariable(CTX_CLONED_PRODUCT_ID, clonedId);

        List<UUID> ids = List.of(UUID.randomUUID());
        when(productRelationshipApi.deleteRelationship(eq(clonedId), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(saga.deleteClonedRelationships(ids, ctx)).verifyComplete();
        verify(productRelationshipApi, times(1)).deleteRelationship(eq(clonedId), any(), any());
    }

    // ============================== STEP 6: DOCUMENTATION REQUIREMENTS ==============================

    @Test
    void cloneDocumentationRequirements_copiesAndStoresIds() {
        UUID sourceId = UUID.randomUUID();
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = buildBaseCtx(sourceId, clonedId);

        ProductDocumentationRequirementDTO req = new ProductDocumentationRequirementDTO()
                .docType(ProductDocumentationRequirementDTO.DocTypeEnum.IDENTIFICATION)
                .isMandatory(true)
                .description("ID required");

        when(productDocumentationRequirementsApi.filterDocumentationRequirements(eq(sourceId), any(FilterRequestProductDocumentationRequirementDTO.class), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(req))));
        when(productDocumentationRequirementsApi.createDocumentationRequirement(eq(clonedId), any(), any()))
                .thenReturn(Mono.just(new ProductDocumentationRequirementDTO(LocalDateTime.now(), LocalDateTime.now(), UUID.randomUUID())));

        StepVerifier.create(saga.cloneDocumentationRequirements(ctx))
                .assertNext(list -> assertThat(list).hasSize(1))
                .verifyComplete();

        assertThat(ctx.getVariableAs(CTX_CLONED_DOCUMENTATION_REQUIREMENT_IDS, List.class)).hasSize(1);
    }

    @Test
    void deleteClonedDocumentationRequirements_deletesAll() {
        UUID clonedId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);
        ctx.putVariable(CTX_CLONED_PRODUCT_ID, clonedId);

        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(productDocumentationRequirementsApi.deleteDocumentationRequirement(eq(clonedId), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(saga.deleteClonedDocumentationRequirements(ids, ctx)).verifyComplete();
        verify(productDocumentationRequirementsApi, times(2)).deleteDocumentationRequirement(eq(clonedId), any(), any());
    }

    // ============================== HELPERS ==============================

    private ExecutionContext buildBaseCtx(UUID sourceId, UUID clonedId) {
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_CLONE_PRODUCT);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_CLONED_PRODUCT_ID, clonedId);
        return ctx;
    }

    private ProductDTO buildSourceProduct(UUID id) {
        return new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), id)
                .tenantId(UUID.randomUUID())
                .productCategoryId(UUID.randomUUID())
                .productType(ProductDTO.ProductTypeEnum.FINANCIAL)
                .productName("Base Product")
                .productCode("BASE-001")
                .productDescription("Base description")
                .productStatus(ProductDTO.ProductStatusEnum.ACTIVE);
    }

    private static void assertUuid(String value) {
        assertThat(value).isNotNull().isNotBlank();
        UUID.fromString(value); // throws IllegalArgumentException if not a UUID
    }
}
