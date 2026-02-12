package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductLimitApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductLimitCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductLimitHandler extends CommandHandler<RegisterProductLimitCommand, UUID> {

    private final ProductLimitApi productLimitApi;

    public RegisterProductLimitHandler(ProductLimitApi productLimitApi) {
        this.productLimitApi = productLimitApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductLimitCommand cmd) {
        return productLimitApi.createProductLimit(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productLimitDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productLimitDTO)).getProductLimitId());
    }
}