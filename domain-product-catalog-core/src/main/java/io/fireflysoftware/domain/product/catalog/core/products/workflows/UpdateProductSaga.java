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
import io.fireflysoftware.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.EVENT_PRODUCT_UPDATED;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.SAGA_UPDATE_PRODUCT;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.STEP_UPDATE_PRODUCT;

@Saga(name = SAGA_UPDATE_PRODUCT)
@Service
public class UpdateProductSaga {

    private final CommandBus commandBus;
    private final ProductApi productApi;

    @Autowired
    public UpdateProductSaga(CommandBus commandBus, ProductApi productApi) {
        this.commandBus = commandBus;
        this.productApi = productApi;
    }

    @SagaStep(id = "readProduct")
    @StepEvent(type = "product.read")
    public Mono<ProductDTO> readProduct(UpdateProductInfoCommand cmd, ExecutionContext ctx) {
        return productApi.getProductById(cmd.getProductId(), UUID.randomUUID().toString())
                .doOnNext(current -> ctx.putVariable("previousProduct", current));
    }

    @SagaStep(id = STEP_UPDATE_PRODUCT, dependsOn = "readProduct", compensate = "restoreProduct")
    @StepEvent(type = EVENT_PRODUCT_UPDATED)
    public Mono<UUID> updateProduct(UpdateProductInfoCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd);
    }

    public Mono<Void> restoreProduct(UUID productId, ExecutionContext ctx) {
        ProductDTO previous = ctx.getVariableAs("previousProduct", ProductDTO.class);
        if (previous == null) {
            return Mono.empty();
        }
        UpdateProductInfoCommand restore = new UpdateProductInfoCommand();
        BeanUtils.copyProperties(previous, restore);
        restore.setProductId(productId);
        return commandBus.send(restore).then();
    }
}
