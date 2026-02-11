package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductBundleApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductBundleCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductBundleHandler extends CommandHandler<RemoveProductBundleCommand, Void> {

    private final ProductBundleApi productBundleApi;

    public RemoveProductBundleHandler(ProductBundleApi productBundleApi) {
        this.productBundleApi = productBundleApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductBundleCommand cmd) {
        return productBundleApi.delete(cmd.productBundleId()).then();
    }
}