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
