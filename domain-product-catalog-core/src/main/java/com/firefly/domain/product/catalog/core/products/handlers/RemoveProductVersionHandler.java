package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductVersionApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductVersionCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductVersionHandler extends CommandHandler<RemoveProductVersionCommand, Void> {

    private final ProductVersionApi productVersionApi;

    public RemoveProductVersionHandler(ProductVersionApi productVersionApi) {
        this.productVersionApi = productVersionApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductVersionCommand cmd) {
        return productVersionApi.deleteProductVersion(cmd.productId(), cmd.productVersionId()).then();
    }
}