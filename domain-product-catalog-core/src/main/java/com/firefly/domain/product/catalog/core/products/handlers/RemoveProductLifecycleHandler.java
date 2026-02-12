package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductLifecycleApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductLifecycleCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductLifecycleHandler extends CommandHandler<RemoveProductLifecycleCommand, Void> {

    private final ProductLifecycleApi productLifecycleApi;

    public RemoveProductLifecycleHandler(ProductLifecycleApi productLifecycleApi) {
        this.productLifecycleApi = productLifecycleApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductLifecycleCommand cmd) {
        return productLifecycleApi.deleteProductLifecycle(cmd.productId(), cmd.productLifecycleId()).then();
    }
}