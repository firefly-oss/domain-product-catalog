package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductLifecycleApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductLifecycleCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductLifecycleHandler extends CommandHandler<RegisterProductLifecycleCommand, UUID> {

    private final ProductLifecycleApi productLifecycleApi;

    public RegisterProductLifecycleHandler(ProductLifecycleApi productLifecycleApi) {
        this.productLifecycleApi = productLifecycleApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductLifecycleCommand cmd) {
        return productLifecycleApi.createProductLifecycle(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productLifecycleDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productLifecycleDTO)).getProductLifecycleId());
    }
}