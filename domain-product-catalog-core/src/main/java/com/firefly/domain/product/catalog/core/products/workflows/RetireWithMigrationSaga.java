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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.core.product.sdk.model.ProductConfigurationDTO;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.commands.RetireWithMigrationCommand;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.RetireWithMigrationSagaConstants.*;

/**
 * Atomically transitions a source product to {@code RETIRED} while recording a
 * migration pointer that tells downstream consumers which active product
 * replaces it during the grace period.
 *
 * <p>Steps:
 * <ol>
 *   <li>validateTargetProduct — ensure the target exists and is ACTIVE.</li>
 *   <li>retireSourceProduct — flip source status to RETIRED, keeping the
 *       previous status in {@link ExecutionContext} for compensation.</li>
 *   <li>createMigrationPointer — attach a {@code MIGRATION_POINTER}
 *       configuration to the source product whose JSON payload carries the
 *       target product id, grace period end date and human-readable reason.</li>
 * </ol>
 *
 * <p>If step 3 fails, step 2's compensation re-applies the previous status so
 * the source product is not left stranded in RETIRED without a pointer.
 */
@Saga(name = SAGA_RETIRE_WITH_MIGRATION)
@Service
@Slf4j
public class RetireWithMigrationSaga {

    private static final String MIGRATION_JSON_KEY_TARGET = "targetProductId";
    private static final String MIGRATION_JSON_KEY_GRACE_END = "gracePeriodEndDate";
    private static final String MIGRATION_JSON_KEY_REASON = "reason";

    private final ProductApi productApi;
    private final ProductConfigurationApi productConfigurationApi;
    private final ObjectMapper objectMapper;

    @Autowired
    public RetireWithMigrationSaga(ProductApi productApi,
                                   ProductConfigurationApi productConfigurationApi,
                                   ObjectMapper objectMapper) {
        this.productApi = productApi;
        this.productConfigurationApi = productConfigurationApi;
        this.objectMapper = objectMapper;
    }

    // ============================== STEP 1: VALIDATE TARGET ==============================

    @SagaStep(id = STEP_VALIDATE_TARGET_PRODUCT, compensate = COMPENSATE_NOOP_VALIDATE_TARGET_PRODUCT)
    @StepEvent(type = EVENT_TARGET_PRODUCT_VALIDATED)
    public Mono<UUID> validateTargetProduct(RetireWithMigrationCommand cmd, ExecutionContext ctx) {
        ctx.putVariable(CTX_SOURCE_PRODUCT_ID, cmd.getSourceProductId());
        ctx.putVariable(CTX_TARGET_PRODUCT_ID, cmd.getTargetProductId());
        if (cmd.getGracePeriodEndDate() != null) {
            ctx.putVariable(CTX_GRACE_PERIOD_END_DATE, cmd.getGracePeriodEndDate());
        }
        if (cmd.getReason() != null) {
            ctx.putVariable(CTX_REASON, cmd.getReason());
        }
        return productApi.getProductById(cmd.getTargetProductId(), null)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Target product not found: " + cmd.getTargetProductId())))
                .flatMap(target -> {
                    if (target.getProductStatus() != ProductDTO.ProductStatusEnum.ACTIVE) {
                        return Mono.error(new IllegalStateException(
                                "Target product must be ACTIVE but was " + target.getProductStatus()
                                        + " for productId=" + cmd.getTargetProductId()));
                    }
                    return Mono.just(cmd.getTargetProductId());
                });
    }

    /**
     * Intentional no-op. {@link #validateTargetProduct} only reads the target
     * product and seeds ctx — no side effects to undo on downstream failure.
     */
    public Mono<Void> noopValidateTargetProductCompensation() {
        return Mono.empty();
    }

    // ============================== STEP 2: RETIRE SOURCE ==============================

    @SagaStep(id = STEP_RETIRE_SOURCE_PRODUCT,
            compensate = COMPENSATE_REACTIVATE_SOURCE_PRODUCT,
            dependsOn = STEP_VALIDATE_TARGET_PRODUCT)
    @StepEvent(type = EVENT_SOURCE_PRODUCT_RETIRED)
    public Mono<UUID> retireSourceProduct(ExecutionContext ctx) {
        UUID sourceProductId = ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class);
        return productApi.getProductById(sourceProductId, null)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Source product not found: " + sourceProductId)))
                .flatMap(source -> {
                    ProductDTO.ProductStatusEnum previous = source.getProductStatus();
                    if (previous != null) {
                        ctx.putVariable(CTX_PREVIOUS_SOURCE_STATUS, previous);
                    }
                    // Mutate the fetched DTO in place so we preserve any fields the
                    // server returned but our local type does not enumerate.
                    source.setProductStatus(ProductDTO.ProductStatusEnum.RETIRED);
                    return productApi.updateProduct(sourceProductId, source, UUID.randomUUID().toString())
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Retire update returned empty response for productId=" + sourceProductId)))
                            .thenReturn(sourceProductId);
                });
    }

    public Mono<Void> reactivateSourceProduct(UUID sourceProductId, ExecutionContext ctx) {
        UUID targetId = sourceProductId != null
                ? sourceProductId
                : ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class);
        if (targetId == null) {
            log.error("saga.compensation.invariant-violation step={} reason=missing-sourceProductId",
                    STEP_RETIRE_SOURCE_PRODUCT);
            return Mono.empty();
        }
        ProductDTO.ProductStatusEnum previous = ctx.getVariableAs(CTX_PREVIOUS_SOURCE_STATUS,
                ProductDTO.ProductStatusEnum.class);
        if (previous == null) {
            log.error("saga.compensation.invariant-violation step={} sourceProductId={} reason=missing-previousStatus",
                    STEP_RETIRE_SOURCE_PRODUCT, targetId);
            return Mono.empty();
        }
        return productApi.getProductById(targetId, null)
                .switchIfEmpty(Mono.empty())
                .flatMap(current -> {
                    current.setProductStatus(previous);
                    return productApi.updateProduct(
                                    targetId, current, UUID.randomUUID().toString())
                            .then();
                })
                .onErrorResume(err -> {
                    // Swallowed here because compensation must not propagate; ops must watch
                    // for this log entry — the product is left RETIRED without its pointer.
                    log.error("saga.compensation.reactivate-failed sourceProductId={} err={}",
                            targetId, err.getMessage());
                    return Mono.empty();
                });
    }

    // ============================== STEP 3: CREATE MIGRATION POINTER ==============================

    @SagaStep(id = STEP_CREATE_MIGRATION_POINTER,
            compensate = COMPENSATE_DELETE_MIGRATION_POINTER,
            dependsOn = STEP_RETIRE_SOURCE_PRODUCT)
    @StepEvent(type = EVENT_MIGRATION_POINTER_CREATED)
    public Mono<UUID> createMigrationPointer(ExecutionContext ctx) {
        UUID sourceProductId = ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class);
        UUID targetProductId = ctx.getVariableAs(CTX_TARGET_PRODUCT_ID, UUID.class);
        LocalDate gracePeriodEndDate = ctx.getVariableAs(CTX_GRACE_PERIOD_END_DATE, LocalDate.class);
        String reason = ctx.getVariableAs(CTX_REASON, String.class);

        String payload = serializeMigrationPayload(targetProductId, gracePeriodEndDate, reason);

        ProductConfigurationDTO config = new ProductConfigurationDTO()
                .productId(sourceProductId)
                .configType(ProductConfigurationDTO.ConfigTypeEnum.CUSTOM)
                .configKey(MIGRATION_POINTER_CONFIG_KEY)
                .configValue(payload);

        return productConfigurationApi.createConfiguration(sourceProductId, config, UUID.randomUUID().toString())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Migration pointer create returned empty for productId=" + sourceProductId)))
                .map(resp -> {
                    UUID id = resp.getProductConfigurationId();
                    if (id == null) {
                        throw new IllegalStateException("Migration pointer create returned without id");
                    }
                    return id;
                })
                .doOnNext(id -> ctx.putVariable(CTX_MIGRATION_POINTER_ID, id));
    }

    public Mono<Void> deleteMigrationPointer(UUID migrationPointerId, ExecutionContext ctx) {
        UUID sourceProductId = ctx.getVariableAs(CTX_SOURCE_PRODUCT_ID, UUID.class);
        UUID id = migrationPointerId != null
                ? migrationPointerId
                : ctx.getVariableAs(CTX_MIGRATION_POINTER_ID, UUID.class);
        if (sourceProductId == null || id == null) {
            log.error("saga.compensation.invariant-violation step={} sourceProductId={} pointerId={}",
                    STEP_CREATE_MIGRATION_POINTER, sourceProductId, id);
            return Mono.empty();
        }
        return productConfigurationApi.deleteConfiguration(sourceProductId, id, UUID.randomUUID().toString())
                .onErrorResume(err -> {
                    log.error("saga.compensation.delete-failed step={} sourceProductId={} pointerId={} err={}",
                            STEP_CREATE_MIGRATION_POINTER, sourceProductId, id, err.getMessage());
                    return Mono.empty();
                });
    }

    // ============================== HELPERS ==============================

    private String serializeMigrationPayload(UUID targetProductId, LocalDate gracePeriodEndDate, String reason) {
        Objects.requireNonNull(targetProductId, "targetProductId must be present when serializing migration pointer");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MIGRATION_JSON_KEY_TARGET, targetProductId.toString());
        payload.put(MIGRATION_JSON_KEY_GRACE_END, gracePeriodEndDate != null ? gracePeriodEndDate.toString() : null);
        payload.put(MIGRATION_JSON_KEY_REASON, reason);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize migration pointer payload", e);
        }
    }
}
