package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductVersionApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductVersionCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductVersionHandler extends CommandHandler<RegisterProductVersionCommand, UUID> {

    private final ProductVersionApi productVersionApi;

    public RegisterProductVersionHandler(ProductVersionApi productVersionApi) {
        this.productVersionApi = productVersionApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductVersionCommand cmd) {
        return productVersionApi.createProductVersion(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productVersionDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productVersionDTO)).getProductVersionId());
    }
}