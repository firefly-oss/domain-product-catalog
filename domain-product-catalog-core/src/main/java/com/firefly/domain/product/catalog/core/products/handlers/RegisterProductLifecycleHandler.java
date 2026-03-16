package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductLifecycleCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductLifecycleHandler extends CommandHandler<RegisterProductLifecycleCommand, UUID> {

    private final ProductConfigurationApi productConfigurationApi;

    public RegisterProductLifecycleHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductLifecycleCommand cmd) {
        return productConfigurationApi.createConfiguration(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productConfigurationDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productConfigurationDTO)).getProductConfigurationId());
    }
}
