package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductBundleApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductBundleCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductBundleHandler extends CommandHandler<RegisterProductBundleCommand, UUID> {

    private final ProductBundleApi productBundleApi;

    public RegisterProductBundleHandler(ProductBundleApi productBundleApi) {
        this.productBundleApi = productBundleApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductBundleCommand cmd) {
        return productBundleApi.create(cmd, UUID.randomUUID().toString())
                .mapNotNull(bundleDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(bundleDTO)).getProductBundleId());
    }
}