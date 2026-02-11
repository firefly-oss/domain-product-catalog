package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductLimitApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductLimitCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductLimitHandler extends CommandHandler<RemoveProductLimitCommand, Void> {

    private final ProductLimitApi productLimitApi;

    public RemoveProductLimitHandler(ProductLimitApi productLimitApi) {
        this.productLimitApi = productLimitApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductLimitCommand cmd) {
        return productLimitApi.deleteProductLimit(cmd.productId(), cmd.productLimitId()).then();
    }
}