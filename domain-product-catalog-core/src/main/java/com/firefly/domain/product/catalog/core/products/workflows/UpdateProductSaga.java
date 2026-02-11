package com.firefly.domain.product.catalog.core.products.workflows;

import com.firefly.common.cqrs.command.CommandBus;
import com.firefly.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import com.firefly.transactional.saga.annotations.Saga;
import com.firefly.transactional.saga.annotations.SagaStep;
import com.firefly.transactional.saga.annotations.StepEvent;
import com.firefly.transactional.saga.core.SagaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.RegisterProductConstants.*;


@Saga(name = SAGA_UPDATE_PRODUCT)
@Service
public class UpdateProductSaga {

    private final CommandBus commandBus;

    @Autowired
    public UpdateProductSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_UPDATE_PRODUCT)
    @StepEvent(type = EVENT_PRODUCT_UPDATED)
    public Mono<UUID> updateProduct(UpdateProductInfoCommand cmd, SagaContext ctx) {
        return commandBus.send(cmd);
    }

}