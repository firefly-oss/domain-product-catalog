package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductBundleItemApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductBundleItemCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveProductBundleItemHandler extends CommandHandler<RemoveProductBundleItemCommand, Void> {

    private final ProductBundleItemApi productBundleItemApi;

    public RemoveProductBundleItemHandler(ProductBundleItemApi productBundleItemApi) {
        this.productBundleItemApi = productBundleItemApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductBundleItemCommand cmd) {
        return productBundleItemApi.deleteProductBundleItem(cmd.productBundleId(), cmd.productBundleItemId()).then();
    }
}