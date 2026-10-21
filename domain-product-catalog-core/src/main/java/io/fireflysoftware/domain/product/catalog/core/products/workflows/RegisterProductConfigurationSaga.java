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

import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductConfigurationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductConfigurationCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Saga(name = "RegisterProductConfigurationSaga")
@Service
public class RegisterProductConfigurationSaga {

    private final CommandBus commandBus;

    public RegisterProductConfigurationSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = "registerProductConfiguration", compensate = "removeProductConfiguration")
    @StepEvent(type = "productConfiguration.registered")
    public Mono<UUID> registerProductConfiguration(RegisterProductConfigurationCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd)
                .doOnNext(id -> {
                    ctx.putVariable("productId", cmd.getProductId());
                    ctx.putVariable("configurationId", id);
                });
    }

    public Mono<Void> removeProductConfiguration(UUID configurationId, ExecutionContext ctx) {
        UUID productId = ctx.getVariableAs("productId", UUID.class);
        UUID id = configurationId != null ? configurationId : ctx.getVariableAs("configurationId", UUID.class);
        if (productId == null || id == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductConfigurationCommand(productId, id));
    }
}
