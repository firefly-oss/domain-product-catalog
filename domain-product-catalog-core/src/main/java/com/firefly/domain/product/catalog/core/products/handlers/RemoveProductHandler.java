package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductHandler extends CommandHandler<RemoveProductCommand, Void> {

    private final ProductApi productApi;

    public RemoveProductHandler(ProductApi productApi) {
        this.productApi = productApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductCommand cmd) {
        return productApi.deleteProduct(cmd.productId(), null).then();
    }
}