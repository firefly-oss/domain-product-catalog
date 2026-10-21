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

import org.fireflyframework.cqrs.command.CommandBus;
import io.fireflysoftware.domain.product.catalog.core.products.commands.*;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static io.fireflysoftware.domain.product.catalog.core.utils.constants.GlobalConstants.*;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.*;


@Saga(name = SAGA_REGISTER_PRODUCT_FEE_STRUCTURE)
@Service
public class RegisterProductFeeStructureSaga {

    private final CommandBus commandBus;

    @Autowired
    public RegisterProductFeeStructureSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_FEE_STRUCTURE)
    @StepEvent(type = EVENT_PRODUCT_FEE_STRUCTURE_REGISTERED)
    public Mono<UUID> registerProductFeeStructure(RegisterProductFeeStructureCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd);
    }

}
