package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductBundleItemApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductBundleItemCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductBundleItemHandler extends CommandHandler<RegisterProductBundleItemCommand, UUID> {

    private final ProductBundleItemApi productBundleItemApi;

    public RegisterProductBundleItemHandler(ProductBundleItemApi productBundleItemApi) {
        this.productBundleItemApi = productBundleItemApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductBundleItemCommand cmd) {
        return productBundleItemApi.createProductBundleItem(cmd.getProductBundleId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(bundleItemDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(bundleItemDTO)).getProductBundleItemId());
    }
}