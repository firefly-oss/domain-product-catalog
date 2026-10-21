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


package io.fireflysoftware.domain.product.catalog.core.products.workflows;

import io.fireflysoftware.core.product.sdk.api.ProductApi;
import io.fireflysoftware.core.product.sdk.model.ProductDTO;
import io.fireflysoftware.domain.product.catalog.core.products.commands.ChangeProductStatusCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Status moves among the values the product core stores. Delete is limited to
 * draft and proposed. Active can be suspended or retired. Suspended can return
 * to active or be retired.
 */
@Saga(name = "ChangeProductStatusSaga")
@Service
public class ChangeProductStatusSaga {

    private static final Map<ProductDTO.ProductStatusEnum, Set<ProductDTO.ProductStatusEnum>> ALLOWED = Map.of(
            ProductDTO.ProductStatusEnum.DRAFT, EnumSet.of(
                    ProductDTO.ProductStatusEnum.PROPOSED, ProductDTO.ProductStatusEnum.ACTIVE),
            ProductDTO.ProductStatusEnum.PROPOSED, EnumSet.of(
                    ProductDTO.ProductStatusEnum.DRAFT, ProductDTO.ProductStatusEnum.ACTIVE),
            ProductDTO.ProductStatusEnum.ACTIVE, EnumSet.of(
                    ProductDTO.ProductStatusEnum.SUSPENDED, ProductDTO.ProductStatusEnum.RETIRED),
            ProductDTO.ProductStatusEnum.SUSPENDED, EnumSet.of(
                    ProductDTO.ProductStatusEnum.ACTIVE, ProductDTO.ProductStatusEnum.RETIRED),
            ProductDTO.ProductStatusEnum.RETIRED, EnumSet.noneOf(ProductDTO.ProductStatusEnum.class)
    );

    private final CommandBus commandBus;
    private final ProductApi productApi;

    public ChangeProductStatusSaga(CommandBus commandBus, ProductApi productApi) {
        this.commandBus = commandBus;
        this.productApi = productApi;
    }

    @SagaStep(id = "readProductStatus")
    @StepEvent(type = "product.read")
    public Mono<ProductDTO> readProductStatus(ChangeProductStatusCommand cmd, ExecutionContext ctx) {
        return productApi.getProductById(cmd.productId(), UUID.randomUUID().toString())
                .doOnNext(current -> ctx.putVariable("previousProduct", current));
    }

    @SagaStep(id = "changeProductStatus", dependsOn = "readProductStatus", compensate = "restoreProductStatus")
    @StepEvent(type = "product.statusChanged")
    public Mono<UUID> changeProductStatus(ChangeProductStatusCommand cmd, ExecutionContext ctx) {
        ProductDTO current = ctx.getVariableAs("previousProduct", ProductDTO.class);
        if (current == null) {
            return Mono.error(new IllegalStateException("Product was not found"));
        }
        ProductDTO.ProductStatusEnum next = parse(cmd.status());
        ProductDTO.ProductStatusEnum from = current.getProductStatus() == null
                ? ProductDTO.ProductStatusEnum.DRAFT
                : current.getProductStatus();
        if (from != next && !ALLOWED.getOrDefault(from, Set.of()).contains(next)) {
            return Mono.error(new IllegalStateException(
                    "Product " + cmd.productId() + " cannot move from " + from + " to " + next));
        }
        UpdateProductInfoCommand update = new UpdateProductInfoCommand();
        BeanUtils.copyProperties(current, update);
        update.setProductId(cmd.productId());
        update.setProductStatus(next);
        return commandBus.send(update);
    }

    public Mono<Void> restoreProductStatus(UUID productId, ExecutionContext ctx) {
        ProductDTO previous = ctx.getVariableAs("previousProduct", ProductDTO.class);
        if (previous == null) {
            return Mono.empty();
        }
        UpdateProductInfoCommand restore = new UpdateProductInfoCommand();
        BeanUtils.copyProperties(previous, restore);
        restore.setProductId(productId);
        return commandBus.send(restore).then();
    }

    private static ProductDTO.ProductStatusEnum parse(String status) {
        return ProductDTO.ProductStatusEnum.fromValue(status);
    }
}
