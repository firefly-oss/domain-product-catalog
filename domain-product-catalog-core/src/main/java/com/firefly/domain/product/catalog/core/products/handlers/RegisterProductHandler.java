package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductInfoCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductHandler extends CommandHandler<RegisterProductInfoCommand, UUID> {

    private final ProductApi productApi;

    public RegisterProductHandler(ProductApi productApi) {
        this.productApi = productApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductInfoCommand cmd) {
        return productApi.createProduct(cmd, UUID.randomUUID().toString())
                .mapNotNull(productDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productDTO)).getProductId());
    }
}