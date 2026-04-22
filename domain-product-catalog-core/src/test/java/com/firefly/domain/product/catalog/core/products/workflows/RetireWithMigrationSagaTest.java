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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.core.product.sdk.model.ProductConfigurationDTO;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.commands.RetireWithMigrationCommand;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.RetireWithMigrationSagaConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetireWithMigrationSagaTest {

    private ProductApi productApi;
    private ProductConfigurationApi productConfigurationApi;
    private ObjectMapper objectMapper;
    private RetireWithMigrationSaga saga;

    @BeforeEach
    void setUp() {
        productApi = mock(ProductApi.class);
        productConfigurationApi = mock(ProductConfigurationApi.class);
        objectMapper = new ObjectMapper();
        saga = new RetireWithMigrationSaga(productApi, productConfigurationApi, objectMapper);
    }

    // ============================== STEP 1: VALIDATE TARGET ==============================

    @Test
    void validateTargetProduct_activeTarget_storesCtxAndReturnsId() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        ProductDTO activeTarget = new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), targetId)
                .productStatus(ProductDTO.ProductStatusEnum.ACTIVE);
        when(productApi.getProductById(eq(targetId), any())).thenReturn(Mono.just(activeTarget));

        RetireWithMigrationCommand cmd = new RetireWithMigrationCommand(sourceId, targetId,
                LocalDate.of(2026, 12, 31), "End of life");
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);

        StepVerifier.create(saga.validateTargetProduct(cmd, ctx))
                .expectNext(targetId)
                .verifyComplete();

        assertThat(ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class)).isEqualTo(sourceId);
        assertThat(ctx.getVariableAs(CTX_TARGET_PRODUCT_ID, UUID.class)).isEqualTo(targetId);
        assertThat(ctx.getVariableAs(CTX_GRACE_PERIOD_END_DATE, LocalDate.class)).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(ctx.getVariableAs(CTX_REASON, String.class)).isEqualTo("End of life");
    }

    @Test
    void noopValidateTargetProductCompensation_completesEmpty() {
        StepVerifier.create(saga.noopValidateTargetProductCompensation()).verifyComplete();
    }

    @Test
    void validateTargetProduct_inactiveTarget_failsFast() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        ProductDTO draftTarget = new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), targetId)
                .productStatus(ProductDTO.ProductStatusEnum.DRAFT);
        when(productApi.getProductById(eq(targetId), any())).thenReturn(Mono.just(draftTarget));

        RetireWithMigrationCommand cmd = new RetireWithMigrationCommand(sourceId, targetId, LocalDate.now(), "r");
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);

        StepVerifier.create(saga.validateTargetProduct(cmd, ctx))
                .expectError(IllegalStateException.class)
                .verify();

        verify(productApi, never()).updateProduct(any(), any(), any());
        verify(productConfigurationApi, never()).createConfiguration(any(), any(), any());
    }

    @Test
    void validateTargetProduct_missingTarget_failsFast() {
        UUID targetId = UUID.randomUUID();
        when(productApi.getProductById(eq(targetId), any())).thenReturn(Mono.empty());

        RetireWithMigrationCommand cmd = new RetireWithMigrationCommand(
                UUID.randomUUID(), targetId, LocalDate.now(), "r");
        StepVerifier.create(saga.validateTargetProduct(cmd,
                        ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION)))
                .expectError(IllegalStateException.class)
                .verify();
    }

    // ============================== STEP 2: RETIRE SOURCE ==============================

    @Test
    void retireSourceProduct_flipsToRetiredAndStoresPreviousStatus() {
        UUID sourceId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);

        ProductDTO source = new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), sourceId)
                .productCode("SRC-001")
                .productStatus(ProductDTO.ProductStatusEnum.ACTIVE);
        when(productApi.getProductById(eq(sourceId), any())).thenReturn(Mono.just(source));

        ArgumentCaptor<ProductDTO> bodyCaptor = ArgumentCaptor.forClass(ProductDTO.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(productApi.updateProduct(eq(sourceId), bodyCaptor.capture(), keyCaptor.capture()))
                .thenReturn(Mono.just(source));

        StepVerifier.create(saga.retireSourceProduct(ctx))
                .expectNext(sourceId)
                .verifyComplete();

        assertThat(bodyCaptor.getValue().getProductStatus()).isEqualTo(ProductDTO.ProductStatusEnum.RETIRED);
        assertThat(bodyCaptor.getValue().getProductCode()).isEqualTo("SRC-001");
        assertUuid(keyCaptor.getValue());
        assertThat(ctx.getVariableAs(CTX_PREVIOUS_SOURCE_STATUS, ProductDTO.ProductStatusEnum.class))
                .isEqualTo(ProductDTO.ProductStatusEnum.ACTIVE);
    }

    @Test
    void retireSourceProduct_sourceMissing_failsFast() {
        UUID sourceId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);

        when(productApi.getProductById(eq(sourceId), any())).thenReturn(Mono.empty());

        StepVerifier.create(saga.retireSourceProduct(ctx))
                .expectError(IllegalStateException.class)
                .verify();

        verify(productApi, never()).updateProduct(any(), any(), any());
    }

    @Test
    void reactivateSourceProduct_restoresPreviousStatusAndPassesIdempotencyKey() {
        UUID sourceId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_PREVIOUS_SOURCE_STATUS, ProductDTO.ProductStatusEnum.ACTIVE);

        ProductDTO current = new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), sourceId)
                .productCode("SRC-001")
                .productStatus(ProductDTO.ProductStatusEnum.RETIRED);
        when(productApi.getProductById(eq(sourceId), any())).thenReturn(Mono.just(current));

        ArgumentCaptor<ProductDTO> bodyCaptor = ArgumentCaptor.forClass(ProductDTO.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(productApi.updateProduct(eq(sourceId), bodyCaptor.capture(), keyCaptor.capture()))
                .thenReturn(Mono.just(current));

        StepVerifier.create(saga.reactivateSourceProduct(sourceId, ctx)).verifyComplete();
        assertThat(bodyCaptor.getValue().getProductStatus()).isEqualTo(ProductDTO.ProductStatusEnum.ACTIVE);
        // The in-place mutation preserves the server-managed code/id for round-tripping.
        assertThat(bodyCaptor.getValue().getProductCode()).isEqualTo("SRC-001");
        assertUuid(keyCaptor.getValue());
    }

    @Test
    void reactivateSourceProduct_preservesDraftStatusWhenPreviousWasDraft() {
        UUID sourceId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_PREVIOUS_SOURCE_STATUS, ProductDTO.ProductStatusEnum.DRAFT);

        ProductDTO current = new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), sourceId)
                .productStatus(ProductDTO.ProductStatusEnum.RETIRED);
        when(productApi.getProductById(eq(sourceId), any())).thenReturn(Mono.just(current));

        ArgumentCaptor<ProductDTO> bodyCaptor = ArgumentCaptor.forClass(ProductDTO.class);
        when(productApi.updateProduct(eq(sourceId), bodyCaptor.capture(), any()))
                .thenReturn(Mono.just(current));

        StepVerifier.create(saga.reactivateSourceProduct(sourceId, ctx)).verifyComplete();
        assertThat(bodyCaptor.getValue().getProductStatus()).isEqualTo(ProductDTO.ProductStatusEnum.DRAFT);
    }

    @Test
    void reactivateSourceProduct_isNoOpWhenPreviousStatusMissing() {
        UUID sourceId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);

        StepVerifier.create(saga.reactivateSourceProduct(sourceId, ctx)).verifyComplete();
        verify(productApi, never()).updateProduct(any(), any(), any());
    }

    @Test
    void reactivateSourceProduct_swallowsSdkErrors() {
        UUID sourceId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_PREVIOUS_SOURCE_STATUS, ProductDTO.ProductStatusEnum.ACTIVE);

        when(productApi.getProductById(eq(sourceId), any())).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(saga.reactivateSourceProduct(sourceId, ctx)).verifyComplete();
    }

    // ============================== STEP 3: MIGRATION POINTER ==============================

    @Test
    void createMigrationPointer_buildsJsonPayloadAndStoresId() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalDate graceEnd = LocalDate.of(2026, 12, 31);

        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_TARGET_PRODUCT_ID, targetId);
        ctx.putVariable(CTX_GRACE_PERIOD_END_DATE, graceEnd);
        ctx.putVariable(CTX_REASON, "End of life");

        UUID configId = UUID.randomUUID();
        ProductConfigurationDTO responseDto = new ProductConfigurationDTO(LocalDateTime.now(), LocalDateTime.now(), configId);

        ArgumentCaptor<ProductConfigurationDTO> bodyCaptor = ArgumentCaptor.forClass(ProductConfigurationDTO.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(productConfigurationApi.createConfiguration(eq(sourceId), bodyCaptor.capture(), keyCaptor.capture()))
                .thenReturn(Mono.just(responseDto));

        StepVerifier.create(saga.createMigrationPointer(ctx))
                .expectNext(configId)
                .verifyComplete();

        ProductConfigurationDTO sent = bodyCaptor.getValue();
        assertThat(sent.getConfigType()).isEqualTo(ProductConfigurationDTO.ConfigTypeEnum.CUSTOM);
        assertThat(sent.getConfigKey()).isEqualTo(MIGRATION_POINTER_CONFIG_KEY);
        JsonNode payload = objectMapper.readTree(sent.getConfigValue());
        assertThat(payload.get("targetProductId").asText()).isEqualTo(targetId.toString());
        assertThat(payload.get("gracePeriodEndDate").asText()).isEqualTo("2026-12-31");
        assertThat(payload.get("reason").asText()).isEqualTo("End of life");
        assertUuid(keyCaptor.getValue());
        assertThat(ctx.getVariableAs(CTX_MIGRATION_POINTER_ID, UUID.class)).isEqualTo(configId);
    }

    @Test
    void createMigrationPointer_failsWhenResponseHasNoId() {
        UUID sourceId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_TARGET_PRODUCT_ID, UUID.randomUUID());

        when(productConfigurationApi.createConfiguration(eq(sourceId), any(), any()))
                .thenReturn(Mono.just(new ProductConfigurationDTO()));

        StepVerifier.create(saga.createMigrationPointer(ctx))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void deleteMigrationPointer_deletesConfig() {
        UUID sourceId = UUID.randomUUID();
        UUID pointerId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(productConfigurationApi.deleteConfiguration(eq(sourceId), eq(pointerId), keyCaptor.capture()))
                .thenReturn(Mono.empty());

        StepVerifier.create(saga.deleteMigrationPointer(pointerId, ctx)).verifyComplete();
        verify(productConfigurationApi, times(1)).deleteConfiguration(eq(sourceId), eq(pointerId), any());
        assertUuid(keyCaptor.getValue());
    }

    @Test
    void deleteMigrationPointer_fallsBackToCtxWhenPointerIdParamIsNull() {
        UUID sourceId = UUID.randomUUID();
        UUID pointerId = UUID.randomUUID();
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, sourceId);
        ctx.putVariable(CTX_MIGRATION_POINTER_ID, pointerId);

        when(productConfigurationApi.deleteConfiguration(eq(sourceId), eq(pointerId), any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(saga.deleteMigrationPointer(null, ctx)).verifyComplete();
        verify(productConfigurationApi, times(1)).deleteConfiguration(eq(sourceId), eq(pointerId), any());
    }

    @Test
    void deleteMigrationPointer_isNoOpWhenRequiredCtxMissing() {
        ExecutionContext ctx = ExecutionContext.forSaga("corr", SAGA_RETIRE_WITH_MIGRATION);

        StepVerifier.create(saga.deleteMigrationPointer(null, ctx)).verifyComplete();
        verify(productConfigurationApi, never()).deleteConfiguration(any(), any(), any());
    }

    private static void assertUuid(String value) {
        assertThat(value).isNotNull().isNotBlank();
        UUID.fromString(value);
    }
}
