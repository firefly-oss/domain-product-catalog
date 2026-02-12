package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductSubtypeApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductSubtypeCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductSubtypeHandler extends CommandHandler<RegisterProductSubtypeCommand, UUID> {

    private final ProductSubtypeApi productSubtypeApi;

    public RegisterProductSubtypeHandler(ProductSubtypeApi productSubtypeApi) {
        this.productSubtypeApi = productSubtypeApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductSubtypeCommand cmd) {
        return productSubtypeApi.createSubtype(cmd.getProductCategoryId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productSubtypeDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productSubtypeDTO)).getProductSubtypeId());
    }
}