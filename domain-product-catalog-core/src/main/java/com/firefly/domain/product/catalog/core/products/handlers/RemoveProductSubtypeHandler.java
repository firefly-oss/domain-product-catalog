package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductSubtypeApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductSubtypeCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductSubtypeHandler extends CommandHandler<RemoveProductSubtypeCommand, Void> {

    private final ProductSubtypeApi productSubtypeApi;

    public RemoveProductSubtypeHandler(ProductSubtypeApi productSubtypeApi) {
        this.productSubtypeApi = productSubtypeApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductSubtypeCommand cmd) {
        return productSubtypeApi.deleteSubtype(cmd.productCategoryId(), cmd.productSubtypeId()).then();
    }
}