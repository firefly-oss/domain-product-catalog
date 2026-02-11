package com.firefly.domain.product.catalog.core.products.workflows;

import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.domain.product.catalog.core.products.commands.*;
import com.firefly.transactional.saga.annotations.Saga;
import com.firefly.transactional.saga.annotations.SagaStep;
import com.firefly.transactional.saga.annotations.StepEvent;
import com.firefly.transactional.saga.core.SagaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.GlobalConstants.*;
import static com.firefly.domain.product.catalog.core.utils.constants.RegisterProductConstants.*;


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
    public Mono<UUID> registerProductFeeStructure(RegisterProductFeeStructureCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd);
    }

}
